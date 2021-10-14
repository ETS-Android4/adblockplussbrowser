package org.adblockplus.adblockplussbrowser.core.downloader

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.coroutineScope
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.BufferedSource
import okio.buffer
import okio.sink
import okio.source
import org.adblockplus.adblockplussbrowser.base.data.model.Subscription
import org.adblockplus.adblockplussbrowser.core.AppInfo
import org.adblockplus.adblockplussbrowser.core.data.CoreRepository
import org.adblockplus.adblockplussbrowser.core.data.model.DownloadedSubscription
import org.adblockplus.adblockplussbrowser.core.data.model.exists
import org.adblockplus.adblockplussbrowser.core.data.model.ifExists
import org.adblockplus.adblockplussbrowser.core.extensions.sanatizeUrl
import org.adblockplus.adblockplussbrowser.core.retryIO
import ru.gildor.coroutines.okhttp.await
import timber.log.Timber
import java.io.File
import java.net.HttpURLConnection.HTTP_NOT_MODIFIED
import java.net.HttpURLConnection.HTTP_OK
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


@ExperimentalTime
internal class OkHttpDownloader(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val repository: CoreRepository,
    private val appInfo: AppInfo
) : Downloader {

    private val connectivityManager = ContextCompat.getSystemService(context,
        ConnectivityManager::class.java)

    override suspend fun download(
        subscription: Subscription,
        forced: Boolean,
        periodic: Boolean,
        newSubscription: Boolean,
    ): DownloadResult = coroutineScope {
        try {
            val previousDownload = getDownloadedSubscription(subscription)

            if (canSkipDownload(previousDownload, forced, periodic, newSubscription)) {
                Timber.d("Returning pre-downloaded subscription: ${previousDownload.url}")
                return@coroutineScope DownloadResult.NotModified(previousDownload)
            }

            val url = createUrl(subscription, previousDownload)
            val downloadFile = File(previousDownload.path)
            val request = createDownloadRequest(url, downloadFile, previousDownload, forced)

            Timber.d("Downloading $url - previous subscription: $previousDownload")
            val response = retryIO(description = subscription.title) {
                okHttpClient.newCall(request).await()
            }

            val result = when (response.code) {
                HTTP_OK -> {
                    val tempFile = writeTempFile(response.body!!.source())
                    context.downloadsDir().mkdirs()
                    tempFile.renameTo(downloadFile)

                    DownloadResult.Success(previousDownload.copy(
                        lastUpdated = System.currentTimeMillis(),
                        lastModified = response.headers["Last-Modified"] ?: "",
                        version = extractVersion(downloadFile),
                        etag = response.headers["ETag"] ?: "",
                        downloadCount = previousDownload.downloadCount + 1
                    ))
                }
                HTTP_NOT_MODIFIED -> {
                    DownloadResult.NotModified(previousDownload.copy(
                        lastUpdated = System.currentTimeMillis()
                    ))
                }
                else -> {
                    Timber.e("Error downloading $url, response code: ${response.code}")
                    DownloadResult.Failed(previousDownload.ifExists())
                }
            }
            response.close()
            result
        } catch (ex: Exception) {
            ex.printStackTrace()
            val previousDownload = getDownloadedSubscription(subscription)
            Timber.e(ex, "Error downloading ${previousDownload.url}")
            DownloadResult.Failed(previousDownload.ifExists())
        }
    }

    private fun canSkipDownload(
        previousDownload: DownloadedSubscription,
        forced: Boolean,
        periodic: Boolean,
        newSubscription: Boolean
    ): Boolean {
        val isMetered = connectivityManager?.isActiveNetworkMetered ?: false
        val expired = previousDownload.isExpired(newSubscription, isMetered)
        val exists = previousDownload.exists()

        Timber.d("Url: %s: forced: %b, periodic: %b, new: %b, expired: %b, exists: %b, metered: %b",
            previousDownload.url, forced, periodic, newSubscription, expired, exists, isMetered)
        /* We check for some conditions here:
         *  - NEVER SKIP force refresh updates.
         *  - If this is a new subscription or a periodic update, DO NOT SkIP if it is not expired,
         *    AND the file still exists.
         *  - Otherwise if the file still exists, SKIP the update
         *
         *  Subscription expiration logic:
         *   - New subscriptions expires in MIN_REFRESH_INTERVAL (1 hour)
         *   - Metered connection in METERED_REFRESH_INTERVAL (3 days)
         *   - Unmetered connection in UNMETERED_REFRESH_INTERVAL (24 hours)
         */
        return if (forced) {
            false
        } else if (newSubscription || periodic) {
            !expired && exists
        } else {
            exists
        }
    }

    override suspend fun validate(subscription: Subscription): Boolean = coroutineScope {
        try {
            val url = createUrl(subscription)
            val request = createHeadRequest(url)

            val response = retryIO(description = subscription.title) {
                okHttpClient.newCall(request).await()
            }
            response.code == 200
        } catch (ex: Exception) {
            Timber.e(ex, "Error downloading ${subscription.url}")
            false
        }
    }

    private suspend fun getDownloadedSubscription(subscription: Subscription): DownloadedSubscription {
        return try {
            val url = subscription.url.sanatizeUrl().toHttpUrl()
            val coreData = repository.getDataSync()
            return coreData.downloadedSubscription.firstOrNull {
                it.url == subscription.url
            } ?: DownloadedSubscription(
                subscription.url,
                path = context.downloadFile(url.toFileName()).absolutePath
            )
        } catch (ex: Exception) {
            Timber.e(ex, "Error parsing url: ${subscription.url}")
            DownloadedSubscription(subscription.url)
        }
    }

    private fun createUrl(subscription: Subscription,
                          previousDownload: DownloadedSubscription =
                              DownloadedSubscription(subscription.url)
    ): HttpUrl {
        return subscription.url.sanatizeUrl().toHttpUrl().newBuilder().apply {
            addQueryParameter("addonName", appInfo.addonName)
            addQueryParameter("addonVersion", appInfo.addonVersion)
            addQueryParameter("application", appInfo.application)
            addQueryParameter("applicationVersion", appInfo.applicationVersion)
            addQueryParameter("platform", appInfo.platform)
            addQueryParameter("platformVersion", appInfo.platformVersion)
            addQueryParameter("lastVersion", previousDownload.version)
            addQueryParameter("downloadCount", previousDownload.downloadCount.asDownloadCount())
        }.build()
    }

    private fun createDownloadRequest(
        url: HttpUrl,
        file: File,
        previousDownload: DownloadedSubscription,
        forced: Boolean
    ): Request =
        Request.Builder().url(url).apply {
            // Don't apply If-Modified-Since and If-None-Match if the file doesn't exists on the filesystem
            if (!forced && file.exists()) {
                if (previousDownload.lastModified.isNotEmpty()) {
                    addHeader("If-Modified-Since", previousDownload.lastModified)
                }
                if (previousDownload.etag.isNotEmpty()) {
                    addHeader("If-None-Match", previousDownload.etag)
                }
            }
        }.build()

    private fun createHeadRequest(url: HttpUrl): Request =
        Request.Builder().url(url).head().build()

    private fun writeTempFile(input: BufferedSource): File {
        val file = File.createTempFile("list", ".txt", context.cacheDir)
        input.use { source ->
            file.sink().buffer().use { dest -> dest.writeAll(source) }
        }
        return file
    }

    // Parse Version from subscription file
    private fun extractVersion(file: File): String {
        val version = readHeader(file).asSequence().map { it.trim() }
            .filter { it.startsWith("!") }
            .filter { it.contains(":") }
            .map { line ->
                val split = line.split(":", limit = 2)
                Pair(split[0].trim(), split[1].trim())
            }
            .filter { pair -> pair.first.contains("version", true) }
            .map { pair -> pair.second }
            .firstOrNull()

        return version ?: "0"
    }

    private fun readHeader(file: File): List<String> {
        file.source().buffer().use { source ->
            return generateSequence { source.readUtf8Line() }
                .takeWhile { it.isNotEmpty() && (it[0] == '[' || it[0] == '!') }
                .toList()
        }
    }

    private fun HttpUrl.toFileName(): String = "${this.toString().hashCode()}.txt"

    private fun DownloadedSubscription.isNotExpired(newSubscription: Boolean, isMetered: Boolean) =
        !this.isExpired(newSubscription, isMetered)

    private fun DownloadedSubscription.isExpired(newSubscription: Boolean, isMetered: Boolean): Boolean {
        val elapsed = Duration.milliseconds(System.currentTimeMillis()) - Duration.milliseconds(this.lastUpdated)
        Timber.d("Elapsed: $elapsed, newSubscription: $newSubscription, isMetered: $isMetered")
        Timber.d("Min: $MIN_REFRESH_INTERVAL, Metered: $METERED_REFRESH_INTERVAL, Wifi: $UNMETERED_REFRESH_INTERVAL")
        val interval = if (newSubscription) {
            MIN_REFRESH_INTERVAL
        } else {
            if (isMetered) METERED_REFRESH_INTERVAL else UNMETERED_REFRESH_INTERVAL
        }

        Timber.d("Expired: ${elapsed > interval}")

        return elapsed > interval
    }

    companion object {
        private val MIN_REFRESH_INTERVAL = Duration.hours(1)
        private val UNMETERED_REFRESH_INTERVAL: Duration = Duration.hours(24)
        private val METERED_REFRESH_INTERVAL = Duration.days(3)
    }
}

private fun Int.asDownloadCount(): String = if (this < 4) this.toString() else "4+"

private fun Context.downloadsDir(): File =
    File(applicationContext.filesDir, "downloads")

private fun Context.downloadFile(filename: String): File =
    File(downloadsDir(), filename)