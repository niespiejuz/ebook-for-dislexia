package com.example.epubminiapp

import android.content.ContentValues.TAG
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.epubminiapp.PageFragment.OnFragmentReadyListener
import com.github.mertakdut.BookSection
import com.github.mertakdut.CssStatus
import com.github.mertakdut.Reader
import com.github.mertakdut.exception.OutOfPagesException
import com.github.mertakdut.exception.ReadingException
import org.w3c.dom.Text
import java.lang.reflect.Field
import kotlin.math.pow


class MainActivity : AppCompatActivity(), OnFragmentReadyListener {
    private var reader: Reader? = null
    private var mViewPager: ViewPager? = null
    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null
    private var pageCount = Int.MAX_VALUE
    private var pxScreenWidth = 0
    private var textSize: Float = 1.0f
    private var isSkippedToPage = false
    private var lineCounter = 0
    private var currentTextView:TextView? = null
    private var currentScrollView:ScrollView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        this.textSize = Settings.fontSize

        pxScreenWidth = resources.displayMetrics.widthPixels
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)
        mViewPager = findViewById<View>(R.id.container) as ViewPager

        mViewPager!!.offscreenPageLimit = 1
        mViewPager!!.adapter = mSectionsPagerAdapter
        mViewPager!!.setBackgroundResource(Settings.getStyle().get("bg")!!)


        if (intent != null && intent.extras != null) {
            val filePath = intent.extras!!.getString("filePath")
            try {
                reader = Reader()
                // Setting optionals once per file is enough.
                reader!!.setMaxContentPerSection(400000/this.textSize.pow(2).toInt() )
                reader!!.setCssStatus(CssStatus.OMIT)
                reader!!.setIsIncludingTextContent(true)
                reader!!.setIsOmittingTitleTag(true)

                // This method must be called before readSection.
                reader!!.setFullContent(filePath)
                if (reader!!.isSavedProgressFound) {
                    val lastSavedPage = reader!!.loadProgress()
                    mViewPager!!.currentItem = lastSavedPage
                }
            } catch (e: ReadingException) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onFragmentReady(position: Int): View? {
        var bookSection: BookSection? = null
        try {
            bookSection = reader!!.readSection(position)
        } catch (e: ReadingException) {
            e.printStackTrace()
            Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
        } catch (e: OutOfPagesException) {
            e.printStackTrace()
            pageCount = e.pageCount
            if (isSkippedToPage) {
                Toast.makeText(this@MainActivity, "Max page number is: " + pageCount, Toast.LENGTH_LONG).show()
            }
            mSectionsPagerAdapter!!.notifyDataSetChanged()
        }
        isSkippedToPage = false
        return if (bookSection != null) {
            setFragmentView(bookSection.sectionContent, "text/html", "UTF-8") // reader.isContentStyled
        } else null
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onStop() {
        super.onStop()
        try {
            reader!!.saveProgress(mViewPager!!.currentItem)
            Toast.makeText(this@MainActivity, "Saved page: " + mViewPager!!.currentItem + "...", Toast.LENGTH_LONG).show()
        } catch (e: ReadingException) {
            e.printStackTrace()
            Toast.makeText(this@MainActivity, "Progress is not saved: " + e.message, Toast.LENGTH_LONG).show()
        } catch (e: OutOfPagesException) {
            e.printStackTrace()
            Toast.makeText(this@MainActivity, "Progress is not saved. Out of Bounds. Page Count: " + e.pageCount, Toast.LENGTH_LONG).show()
        }
    }

    private fun setFragmentView( data: String, mimeType: String, encoding: String): View {
        val layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        val scrollView = ScrollView(this@MainActivity)
        currentScrollView = scrollView

        scrollView.layoutParams = layoutParams
        val textView = TextView(this@MainActivity)
        textView.setBackgroundResource(Settings.getStyle().get("bg")!!)

        currentTextView = textView
        textView.layoutParams = layoutParams
        textView.setTextColor(ContextCompat.getColor(this,Settings.getStyle().get("etc")!!))

        textView.setTextSize(this.textSize)
        var typeface = ResourcesCompat.getFont(this, R.font.comicmono)
        textView.typeface = typeface
        lineCounter = -1

        textView.text = Html.fromHtml(data, { source ->
            val imageAsStr = source.substring(source.indexOf(";base64,") + 8)
            val imageAsBytes = Base64.decode(imageAsStr, Base64.DEFAULT)
            val imageAsBitmap = BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.size)
            val imageWidthStartPx = (pxScreenWidth - imageAsBitmap.width) / 2
            val imageWidthEndPx = pxScreenWidth - imageWidthStartPx
            val imageAsDrawable: Drawable = BitmapDrawable(resources, imageAsBitmap)
            imageAsDrawable.setBounds(imageWidthStartPx, 0, imageWidthEndPx, imageAsBitmap.height)
            imageAsDrawable
        }, null)


        if(Settings.endingHighlighting){
            colorEndings(textView.text)
        }

        var nextLineButton = findViewById<View>(R.id.NextLineButton)
        nextLineButton.visibility = View.GONE
        if(Settings.lineHighlighting){
            var nextLineButton = findViewById<View>(R.id.NextLineButton)
            nextLineButton.visibility = View.VISIBLE
        }

        val pxPadding = dpToPx(12)
        textView.setPadding(pxPadding, pxPadding, pxPadding, pxPadding)
        scrollView.addView(textView)
        return scrollView
    }

    public fun colorEndings(text_string: CharSequence){
        // nata code końcówki

        val word_list = text_string.split(" ")
        var word_counter = 0
        var text_spanned = SpannableStringBuilder("")
        do
        {
            val current_word_spannable = SpannableStringBuilder(word_list[word_counter] + " ")
            val fgd_color_value = Settings.getStyle().get("syl")!!
            val fgd_color = ForegroundColorSpan(ContextCompat.getColor(this,fgd_color_value))
            var work_index = word_list[word_counter].length
            //val span_begin = work_index-2
            var span_begin = 0
            val span_end = work_index
            var vowel_count = 0
            var i = 0
            var last_vowel = 0

            while (vowel_count < 2)
            {
                if (current_word_spannable[work_index-i] == 'a' || current_word_spannable[work_index-i] == 'e' || current_word_spannable[work_index-i] == 'ą' || current_word_spannable[work_index-i] == 'ę' || current_word_spannable[work_index-i] == 'i' || current_word_spannable[work_index-i] == 'o' || current_word_spannable[work_index-i] == 'ó' || current_word_spannable[work_index-i] == 'u' || current_word_spannable[work_index-i] == 'y')
                {
                    vowel_count = vowel_count + 1
                    if (last_vowel == 0)
                    {last_vowel = i}
                    if (vowel_count == 2)
                    {
                        if (current_word_spannable[work_index-i] == 'i' && (last_vowel + 1 == i))
                        {
                            vowel_count = vowel_count - 1
                        }
                        span_begin = work_index - i + 1
                    }
                    i = i + 1
                    if (i == work_index)
                    {
                        vowel_count = 2
                    }
                }
                else
                {
                    i = i + 1
                    if (i == work_index)
                    {
                        vowel_count = 2
                        span_begin = 0
                    }
                }
            }


            current_word_spannable.setSpan(fgd_color,
                span_begin, span_end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text_spanned.append(current_word_spannable)
        }
        while(++word_counter < word_list.count())

        currentTextView!!.setText(text_spanned)
    }

    public fun colorLines(){
        var current_view = mViewPager!![1] as RelativeLayout
        var current_scroll = current_view[0] as ScrollView
        var text_view = current_scroll[0] as TextView
        ++lineCounter
        val the_layout = text_view!!.getLayout()
        val text_spanned = SpannableStringBuilder(text_view!!.text)
        if(lineCounter>=0 && lineCounter<text_view!!.lineCount)
        {
            val span_begin = the_layout.getLineStart(lineCounter)
            val span_end = the_layout.getLineEnd(lineCounter)
            val bckd_color = BackgroundColorSpan(ContextCompat.
                    getColor(this,Settings.getStyle().get("hi")!!))
            text_spanned.setSpan(bckd_color,
                span_begin, span_end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if(lineCounter>0 && lineCounter<=text_view!!.lineCount)
        {
            val span_begin2 = the_layout.getLineStart(lineCounter-1)
            val span_end2 = the_layout.getLineEnd(lineCounter-1)
            val bckd_color2 = BackgroundColorSpan(ContextCompat.
                getColor(this,Settings.getStyle().get("etc")!!))
            text_spanned.setSpan(bckd_color2,
                span_begin2, span_end2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        text_view!!.setText(text_spanned)
    }

    public final fun onNextLineButtonPress(vw: android.view.View) {
        colorLines()
    }

    public final fun onNextButtonPress(vw: android.view.View) {
        val currElement = mViewPager!!.currentItem;
        mViewPager!!.setCurrentItem(currElement+1);
    }

    public final fun onPrevButtonPress(vw: android.view.View) {
        val currElement = mViewPager!!.currentItem;
        mViewPager!!.setCurrentItem(currElement-1);
    }
    private fun dpToPx(dp: Int): Int {
        val displayMetrics = resources.displayMetrics
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))
    }

    inner class SectionsPagerAdapter internal constructor(fm: FragmentManager?) : FragmentStatePagerAdapter(fm!!) {
        override fun getCount(): Int {
            return pageCount
        }

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            return PageFragment.newInstance(position)
        }
    }

    fun openFileDialog(view: View) {}
}