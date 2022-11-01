package com.example.epubminiapp

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.annotation.ColorInt
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
    val default: Map<String,Int> = mapOf(
        "bg" to R.color.default_bg,
        "hi" to R.color.default_hi,
        "etc" to R.color.default_etc,
        "syl" to R.color.default_syl)

    val dark: Map<String, Int> = mapOf(
        "bg" to R.color.dark_bg,
        "hi" to R.color.dark_hi,
        "etc" to R.color.dark_etc,
        "syl" to R.color.dark_syl)

    val mysterious: Map<String,Int> = mapOf(
        "bg" to R.color.mysterious_bg,
        "hi" to R.color.mysterious_hi,
        "etc" to R.color.mysterious_etc,
        "syl" to R.color.mysterious_syl)

    val stylesArray = arrayOf(default,dark, mysterious)

    fun getStyle(): Map<String,Int> {
        return stylesArray[style]
    }
}