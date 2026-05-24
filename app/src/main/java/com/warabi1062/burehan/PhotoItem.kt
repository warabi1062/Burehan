package com.warabi1062.burehan

import android.net.Uri

data class PhotoItem(
    val uri: Uri,
    val score: Int,
    val dateTaken: Long = 0L,
)
