package com.example.epubminiapp

import android.content.Context
import android.content.SharedPreferences

object AppSettings {
    var endingHighlighting = false
    var lineHighlighting = true
    var fontSize:Int = 1
    private lateinit var savedPreferences:SharedPreferences

    fun init(context: Context) {
        savedPreferences = context.getSharedPreferences("Prefs", Context.MODE_PRIVATE)
        style = savedPreferences.getInt("style",0)
        endingHighlighting = savedPreferences.getBoolean("ending_highlighting", false)
        lineHighlighting = savedPreferences.getBoolean("line_highlighting", false)
        fontSize = savedPreferences.getInt("font_scale", 1)
    }


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