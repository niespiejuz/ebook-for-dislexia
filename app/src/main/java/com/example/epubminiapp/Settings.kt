package com.example.epubminiapp

import android.content.SharedPreferences
import android.util.Log

object Settings {

    init {
        Log.d("settings","Initialised settings")
    }

    var endingHighlighting = false;
    var lineHighlighting = true;
    var fontSize:Float = 34.0f
    var savedPreferences:SharedPreferences? = null
}