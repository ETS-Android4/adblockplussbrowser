package org.adblockplus.adblockplussbrowser.base.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Subscription(
    val url: String,
    val title: String,
    val lastUpdated: Long,
    val languages: List<String>
) : Parcelable