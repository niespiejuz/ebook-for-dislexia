package com.example.epubminiapp

import androidx.appcompat.app.AppCompatActivity
import com.example.epubminiapp.PageFragment.OnFragmentReadyListener
import androidx.viewpager.widget.ViewPager
import com.example.epubminiapp.MainActivity.SectionsPagerAdapter
import android.os.Bundle
import com.example.epubminiapp.R
import com.github.mertakdut.CssStatus
import com.github.mertakdut.exception.ReadingException
import android.widget.Toast
import com.github.mertakdut.BookSection
import com.github.mertakdut.exception.OutOfPagesException
import android.widget.FrameLayout
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ScrollView
import android.widget.TextView
import android.text.Html
import android.text.Html.ImageGetter
import android.graphics.drawable.Drawable
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.util.Base64
import android.util.DisplayMetrics
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.example.epubminiapp.PageFragment
import com.github.mertakdut.Reader

class MainActivity : AppCompatActivity(), OnFragmentReadyListener {
    private var reader: Reader? = null
    private var mViewPager: ViewPager? = null
    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null
    private var pageCount = Int.MAX_VALUE
    private var pxScreenWidth = 0
    private var textSize: Float = 24f
    private var isSkippedToPage = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        pxScreenWidth = resources.displayMetrics.widthPixels
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)
        mViewPager = findViewById<View>(R.id.container) as ViewPager
        mViewPager!!.offscreenPageLimit = 0
        mViewPager!!.adapter = mSectionsPagerAdapter
        if (intent != null && intent.extras != null) {
            val filePath = intent.extras!!.getString("filePath")
            try {
                reader = Reader()
                // Setting optionals once per file is enough.
                reader!!.setMaxContentPerSection((400/32) * this.textSize.toInt() )
                reader!!.setCssStatus(CssStatus.OMIT)
                reader!!.setIsIncludingTextContent(true)
                reader!!.setIsOmittingTitleTag(true)

                // This method must be called before readSection.
                reader!!.setFullContent(filePath)

//                int lastSavedPage = reader.setFullContentWithProgress(filePath);
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
        scrollView.layoutParams = layoutParams
        val textView = TextView(this@MainActivity)
        textView.layoutParams = layoutParams
        textView.setTextColor(Color.WHITE)
        textView.setTextSize(this.textSize)
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
        val pxPadding = dpToPx(12)
        textView.setPadding(pxPadding, pxPadding, pxPadding, pxPadding)
        scrollView.addView(textView)
        return scrollView
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