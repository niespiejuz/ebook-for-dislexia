package com.example.epubminiapp

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Base64
import android.util.DisplayMetrics
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


class MainActivity : AppCompatActivity(), OnFragmentReadyListener {
    private var reader: Reader? = null
    private var mViewPager: ViewPager? = null
    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null
    private var pageCount = Int.MAX_VALUE
    private var pxScreenWidth = 0
    private var isSkippedToPage = false
    private var lineCounter = 0
    private var currentTextView:TextView? = null
    private var currentScrollView:ScrollView? = null

    // first line bug hotfix variables
    private var firstPageId : Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        pxScreenWidth = resources.displayMetrics.widthPixels
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)
        mViewPager = findViewById<View>(R.id.container) as ViewPager

        mViewPager!!.offscreenPageLimit = 1
        mViewPager!!.adapter = mSectionsPagerAdapter
        mViewPager!!.setBackgroundResource(AppSettings.getStyle()["bg"]!!)


        if (intent != null && intent.extras != null) {
            val filePath = intent.extras!!.getString("filePath")
            try {
                reader = Reader()
                // Setting optionals once per file is enough.
                val font_size = AppSettings.fontSize
                var text_amount = 1250
                when(font_size){
                    0 -> text_amount = 750
                    1 -> text_amount = 300
                    2 -> text_amount = 80
                }
                reader!!.setMaxContentPerSection(text_amount)
                reader!!.setCssStatus(CssStatus.OMIT)
                reader!!.setIsIncludingTextContent(true)
                reader!!.setIsOmittingTitleTag(true)

                // This method must be called before readSection.
                reader!!.setFullContent(filePath)
                if (reader!!.isSavedProgressFound) {
                    val lastSavedPage = reader!!.loadProgress()
                    mViewPager!!.currentItem = lastSavedPage
                }
                firstPageId = mViewPager!!.currentItem

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
        textView.setBackgroundResource(AppSettings.getStyle().get("bg")!!)
        textView.tag = "TheTextView"
        textView.layoutParams = layoutParams
        textView.setTextColor(ContextCompat.getColor(this,AppSettings.getStyle().get("etc")!! ))
        val font_size = AppSettings.fontSize
        var font_size_px = 0.0f
        when(font_size){
            0 -> font_size_px = 22.0f
            1 -> font_size_px = 28.0f
            2 -> font_size_px = 34.0f
        }
        textView.textSize = font_size_px

        var typeface = ResourcesCompat.getFont(this, R.font.robotomedium)
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

        currentTextView=textView

        if(AppSettings.endingHighlighting){
            colorEndings(textView.text)
        }

        val pxPadding = dpToPx(12)
        textView.setPadding(pxPadding, pxPadding, pxPadding, pxPadding)
        scrollView.addView(textView)
        return scrollView
    }

    public fun colorEndings(text_string: CharSequence){

        val word_list = text_string.split(" ")
        var word_counter = 0
        var text_spanned = SpannableStringBuilder("")
        do
        {
            val current_word_spannable = SpannableStringBuilder(word_list[word_counter] + " ")
            val fgd_color_value = AppSettings.getStyle().get("syl")!!
            val fgd_color = ForegroundColorSpan(ContextCompat.getColor(this,fgd_color_value))
            var work_index = word_list[word_counter].length
            //val span_begin = work_index-2
            var span_begin = 0
            val span_end = work_index
            var vowel_count = 0
            var i = 0
            var last_vowel = 0

            /**
             * In this fragment letters to be highlighted are selected
             * when "endings_colouring" option is enabled
             */
            if (work_index>3)
            {
                while (vowel_count < 2)
                {
                    if (current_word_spannable[work_index-i] == 'a' || current_word_spannable[work_index-i] == 'e' || current_word_spannable[work_index-i] == 'ą' || current_word_spannable[work_index-i] == 'ę' || current_word_spannable[work_index-i] == 'i' || current_word_spannable[work_index-i] == 'o' || current_word_spannable[work_index-i] == 'ó' || current_word_spannable[work_index-i] == 'u' || current_word_spannable[work_index-i] == 'y')
                    {
                        vowel_count = vowel_count + 1
                        if (last_vowel == 0)
                        { last_vowel = i }
                        if (vowel_count == 2)
                        {
                            if (current_word_spannable[work_index-i] == 'i' && (last_vowel + 1 == i))
                            { vowel_count = vowel_count - 1 }
                            else
                            { span_begin = work_index - i + 1 }
                        }

                        if (i == work_index)
                        {
                            vowel_count = 2
                            span_begin = 0
                        }
                        i = i + 1
                    }
                    else
                    {

                        if (i == work_index)
                        {
                            vowel_count = 2
                            span_begin = 0
                        }
                        i = i + 1
                    }
                }
            }

            current_word_spannable.setSpan(fgd_color,
                span_begin, span_end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text_spanned.append(current_word_spannable)
        }
        while(++word_counter < word_list.count())

        currentTextView!!.text = text_spanned
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