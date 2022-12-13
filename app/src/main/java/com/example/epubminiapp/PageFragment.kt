package com.example.epubminiapp

import android.content.Context
import com.example.epubminiapp.PageFragment.OnFragmentReadyListener
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment


class PageFragment : Fragment() {
    private var onFragmentReadyListener: OnFragmentReadyListener? = null
    private var lineCounter = -1
    private var mainLayout:RelativeLayout? = null

    interface OnFragmentReadyListener {
        fun onFragmentReady(position: Int): View?
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onFragmentReadyListener = context as OnFragmentReadyListener //Activity version is deprecated.
    }

    override fun onDestroy() {
        super.onDestroy()
        onFragmentReadyListener = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_display, container, false)
        mainLayout = rootView.findViewById<View>(R.id.fragment_main_layout) as RelativeLayout
        val view = onFragmentReadyListener!!.onFragmentReady(requireArguments().getInt(ARG_TAB_POSITON))

        var nextLineButton = mainLayout!!.findViewById<Button>(R.id.NextLineButton)
        nextLineButton.visibility = View.GONE
        nextLineButton.setOnClickListener {
            colorLines()
        }
        if(AppSettings.lineHighlighting){
            nextLineButton.visibility = View.VISIBLE
        }
        if (view != null) {
            mainLayout!!.addView(view)
        }
        return rootView
    }


    private fun colorLines(){
        var text_view = mainLayout!!.findViewWithTag<TextView>("TheTextView")
        ++lineCounter
        val the_layout = text_view!!.getLayout()
        val text_spanned = SpannableStringBuilder(text_view!!.text)
        if(lineCounter>0 && lineCounter<=text_view!!.lineCount)
        {
            val span_begin2 = the_layout.getLineStart(lineCounter-1)
            val span_end2 = the_layout.getLineEnd(lineCounter-1)
            val bckd_color2 = ForegroundColorSpan(
                ContextCompat.
                getColor(requireActivity(),AppSettings.getStyle().get("etc")!!))

            if(span_begin2 != span_end2) {
                text_spanned.setSpan(
                    bckd_color2,
                    span_begin2, span_end2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }
        if(lineCounter == text_view!!.lineCount-1)
        {
            lineCounter = 0
        }
        if(lineCounter>=0 && lineCounter<text_view!!.lineCount)
        {
            val span_begin = the_layout.getLineStart(lineCounter)
            val span_end = the_layout.getLineEnd(lineCounter)
            val bckd_color = ForegroundColorSpan(
                ContextCompat.getColor(requireActivity(),AppSettings.getStyle().get("hi")!!))

            if(span_begin != span_end) {
                text_spanned.setSpan(
                    bckd_color, span_begin,
                    span_end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }
        text_view!!.setText(text_spanned)
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_TAB_POSITON = "tab_position"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(tabPosition: Int): PageFragment {
            val fragment = PageFragment()
            val args = Bundle()
            args.putInt(ARG_TAB_POSITON, tabPosition)
            fragment.arguments = args
            return fragment
        }
    }
}