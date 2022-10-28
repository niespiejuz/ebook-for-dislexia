package com.example.epubminiapp

import android.content.Context
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Switch
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity

class SettingsView : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)
        var savedPreferences = this@SettingsView.getPreferences(Context.MODE_PRIVATE)


        val toggle_follow: Switch = findViewById(R.id.StOp_TxtFollow_s)
        toggle_follow.isChecked = savedPreferences.getBoolean("line_highlighting", false)

        toggle_follow.setOnCheckedChangeListener { _, isChecked ->
            Settings.lineHighlighting = isChecked
            with(savedPreferences.edit()){
                putBoolean("line_highlighting", isChecked)
                apply()
            }
        }

        val toggle_highlight: Switch = findViewById(R.id.stOp_Color_Suf_s)
        toggle_highlight.isChecked = savedPreferences.getBoolean("ending_highlighting", false)
        toggle_highlight.setOnCheckedChangeListener { _, isChecked ->
            Settings.endingHighlighting = isChecked
            with(savedPreferences.edit()){
                putBoolean("ending_highlighting", isChecked)
                apply()
            }
        }

        val toggle_size: SeekBar = findViewById(R.id.seekBar)
        toggle_size.progress = (savedPreferences.getFloat("font_size", 50.0f).toInt()*40)/100
        toggle_size?.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar,
                                           progress: Int, fromUser: Boolean) {
                val min_font_size = 20.0f
                val max_font_size = 40.0f
                Settings.fontSize = (progress/100.0f)*(max_font_size-min_font_size)+ min_font_size
                with(savedPreferences.edit()){
                    putFloat("font_size", Settings.fontSize)
                    apply()
                }
            }

            override fun onStartTrackingTouch(seek: SeekBar) {
                // write custom code for progress is started 1
            }

            override fun onStopTrackingTouch(seek: SeekBar) {
            }
        })
    }


}
