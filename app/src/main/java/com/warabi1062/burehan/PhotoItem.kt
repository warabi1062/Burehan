package com.warabi1062.burehan

import android.net.Uri

data class PhotoItem(
    val uri: Uri,
    val score: Int,
    val variance: Double = 0.0,
    val mse: Double = 0.0,
)
