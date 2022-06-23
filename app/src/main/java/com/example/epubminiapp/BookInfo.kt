package com.example.epubminiapp

import android.graphics.Bitmap

class BookInfo {
    var title: String? = null
    var coverImage: ByteArray? = null
    var filePath: String? = null
    var isCoverImageNotExists = false
    var coverImageBitmap: Bitmap? = null
}