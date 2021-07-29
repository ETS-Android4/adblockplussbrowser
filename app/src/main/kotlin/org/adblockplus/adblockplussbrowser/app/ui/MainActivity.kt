package org.adblockplus.adblockplussbrowser.app.ui

import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.PackageInfoCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.ui.setupActionBarWithNavController
import dagger.hilt.android.AndroidEntryPoint
import org.adblockplus.adblockplussbrowser.app.R
import org.adblockplus.adblockplussbrowser.app.databinding.ActivityMainBinding
import org.adblockplus.adblockplussbrowser.base.navigation.navControllerFromFragmentContainerView

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val navController: NavController
        get() = navControllerFromFragmentContainerView(R.id.nav_host_fragment)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding =
            DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController)
    }

    override fun onResume() {
        super.onResume()
        if (!hasSamsungInternetVersion5OrNewer() && !hasSamsungInternetBeta()) {
            showInstallSamsungInternetDialog()
        }
    }

    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp() || super.onSupportNavigateUp()

    /**
     * Starting with Samsung Internet 5.0, the way to enable ad blocking has changed. As a result, we
     * need to check for the version of Samsung Internet and apply text changes to the first run slide.
     *
     * @return a boolean that indicates, if the user has Samsung Internet version 5.x
     */
    private fun hasSamsungInternetVersion5OrNewer(): Boolean {
        return try {
            val packageInfo = packageManager.getPackageInfo(SBROWSER_APP_ID, NO_FLAG)
            PackageInfoCompat.getLongVersionCode(packageInfo) >= OLDEST_SAMSUNG_INTERNET_5_VERSIONCODE
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun hasSamsungInternetBeta(): Boolean {
        var result = true
        try {
            packageManager.getPackageInfo(SBROWSER_APP_ID_BETA, NO_FLAG)
        } catch (e: PackageManager.NameNotFoundException) {
            result = false
        }
        return result
    }

    private fun showInstallSamsungInternetDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_install_si)
        val installSamsungInternetButton =
            dialog.findViewById<View>(R.id.install_si_button)
        installSamsungInternetButton.setOnClickListener {
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=${SBROWSER_APP_ID}")
                    )
                )
            } catch (t: Throwable) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=${SBROWSER_APP_ID}")
                    )
                )
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    companion object {
        const val SBROWSER_APP_ID = "com.sec.android.app.sbrowser"
        private const val SBROWSER_APP_ID_BETA = "com.sec.android.app.sbrowser.beta"
        private const val OLDEST_SAMSUNG_INTERNET_5_VERSIONCODE = 500000000
        private const val NO_FLAG = 0
    }
}