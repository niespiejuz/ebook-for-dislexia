package com.example.epubminiapp

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

object Settings {

    init {
        Log.d("settings","Initialised settings")
    }

    var endingHighlighting = false;
    var lineHighlighting = true;
    var fontSize:Float = 34.0f
    //var savedPreferences:SharedPreferences? = null
    var style: Int = 0

    val default: Array<Int> = arrayOf(R.color.default_bg,R.color.default_hi,R.color.default_etc,R.color.default_syl)
    val dark: Array<Int> = arrayOf(R.color.dark_bg,R.color.dark_hi,R.color.dark_etc,R.color.dark_syl)
    val mysterious: Array<Int> = arrayOf(R.color.mysterious_bg,R.color.mysterious_hi,R.color.mysterious_etc,R.color.mysterious_syl)


}