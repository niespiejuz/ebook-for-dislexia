package com.example.epubminiapp

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SettingsView : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)
        var savedPreferences = this@SettingsView.getSharedPreferences("Prefs", Context.MODE_PRIVATE)


        val toggle_follow: Switch = findViewById(R.id.StOp_TxtFollow_s)
        val toggle_highlight: Switch = findViewById(R.id.stOp_Color_Suf_s)
        val toggle_size: SeekBar = findViewById(R.id.seekBar)
        toggle_follow.isChecked = savedPreferences.getBoolean("line_highlighting", false)

        toggle_follow.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked)
            {
                toggle_highlight.isChecked = false;
                AppSettings.endingHighlighting = false;
                with(savedPreferences.edit()){
                    putBoolean("ending_highlighting", !isChecked)
                    apply()
                }
            }
            AppSettings.lineHighlighting = isChecked
            with(savedPreferences.edit()){
                putBoolean("line_highlighting", isChecked)
                apply()
            }
        }

        //val toggle_highlight: Switch = findViewById(R.id.stOp_Color_Suf_s)
        toggle_highlight.isChecked = savedPreferences.getBoolean("ending_highlighting", false)
        toggle_highlight.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked)
            {
                toggle_follow.isChecked = false;
                AppSettings.lineHighlighting = false;
                with(savedPreferences.edit()){
                    putBoolean("line_highlighting", !isChecked)
                    apply()
                }
            }
            AppSettings.endingHighlighting = isChecked
            with(savedPreferences.edit()){
                putBoolean("ending_highlighting", isChecked)
                apply()
            }
        }

        // val toggle_size: SeekBar = findViewById(R.id.seekBar)
        toggle_size.progress = (savedPreferences.getFloat("font_size", 50.0f).toInt()*40)/100
        toggle_size?.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar,
                                           progress: Int, fromUser: Boolean) {
                val min_font_size = 20.0f
                val max_font_size = 40.0f
                AppSettings.fontSize = (progress/100.0f)*(max_font_size-min_font_size)+ min_font_size
                with(savedPreferences.edit()){
                    putFloat("font_size", AppSettings.fontSize)
                    apply()
                }
            }
            override fun onStartTrackingTouch(seek: SeekBar) {
                // write custom code for progress is started
            }

            override fun onStopTrackingTouch(seek: SeekBar) {
            }
        })

        // Spinner for styles
        val stylesSpinner: Spinner = findViewById(R.id.styleSpinner)
        val list: MutableList<String> = mutableListOf<String>("Default","Dark","Mysterious")
        val adapter = ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, list)
        stylesSpinner.adapter = adapter

        stylesSpinner.setSelection(AppSettings.style)
        adapter.notifyDataSetChanged()
        stylesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                with(savedPreferences.edit()){
                    putInt("style", p2)
                    apply()
                }
                AppSettings.style = p2
                stylesSpinner.setSelection(AppSettings.style)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                // not yet implemented
            }

        }
    }


}
