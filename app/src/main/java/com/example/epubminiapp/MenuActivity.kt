package com.example.epubminiapp

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.widget.ProgressBar
import android.os.Bundle
import com.example.epubminiapp.R
import android.widget.GridView
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView
import com.example.epubminiapp.BookInfo
import com.example.epubminiapp.MenuActivity.ListBookInfoTask
import android.os.AsyncTask
import com.github.mertakdut.exception.ReadingException
import com.example.epubminiapp.BookInfoGridAdapter
import android.widget.Toast
import android.os.Environment
import android.content.Intent
import com.example.epubminiapp.MainActivity
import android.content.DialogInterface
import android.util.Log
import android.view.View
import androidx.appcompat.widget.Toolbar
import com.github.mertakdut.Reader
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.lang.RuntimeException
import java.util.ArrayList

class MenuActivity : AppCompatActivity() {
    private var progressBar: ProgressBar? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        (findViewById<View>(R.id.grid_book_info) as GridView).onItemClickListener = OnItemClickListener { adapterView, view, i, l ->
            val clickedItemFilePath = (adapterView.adapter.getItem(i) as BookInfo).filePath
            askForWidgetToUse(clickedItemFilePath)
        }
        progressBar = findViewById<View>(R.id.progressbar) as ProgressBar
        ListBookInfoTask().execute()
    }

    private inner class ListBookInfoTask : AsyncTask<Any?, Any?, List<BookInfo>?>() {
        private var occuredException: Exception? = null
        override fun onPreExecute() {
            super.onPreExecute()
            progressBar!!.visibility = View.VISIBLE
        }

        override fun onPostExecute(bookInfoList: List<BookInfo>?) {
            super.onPostExecute(bookInfoList)
            progressBar!!.visibility = View.GONE
            if (bookInfoList != null) {
                val adapter = BookInfoGridAdapter(this@MenuActivity, bookInfoList)
                (findViewById<View>(R.id.grid_book_info) as GridView).adapter = adapter
            }
            if (occuredException != null) {
                Toast.makeText(this@MenuActivity, occuredException!!.message, Toast.LENGTH_LONG).show()
            }
        }

        override fun doInBackground(vararg p0: Any?): List<BookInfo>? {
            val bookInfoList = searchForPdfFiles()
            val reader = Reader()
            for (bookInfo in bookInfoList!!) {
                try {
                    reader.setInfoContent(bookInfo.filePath)
                    val title = reader.infoPackage.metadata.title
                    if (title != null && title != "") {
                        bookInfo.title = reader.infoPackage.metadata.title
                    } else { // If title doesn't exist, use fileName instead.
                        val dotIndex = bookInfo.title!!.lastIndexOf('.')
                        bookInfo.title = bookInfo.title!!.substring(0, dotIndex)
                    }
                    bookInfo.coverImage = reader.coverImage
                } catch (e: ReadingException) {
                    occuredException = e
                    e.printStackTrace()
                }
            }
            return bookInfoList
        }
    }
    private fun searchForPdfFiles(): List<BookInfo>? {
        val isSDPresent = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        var bookInfoList: MutableList<BookInfo>? = null
        if (isSDPresent) {
            bookInfoList = ArrayList()
            val files = getListFiles(File(Environment.getExternalStorageDirectory().absolutePath))
            val sampleFile = getFileFromAssets("pg28885-images_new.epub")
            files.add(0, sampleFile)
            for (file in files) {
                val bookInfo = BookInfo()
                bookInfo.title = file.name
                bookInfo.filePath = file.path
                bookInfoList.add(bookInfo)
            }
        }
        return bookInfoList
    }

    fun getFileFromAssets(fileName: String): File {
        val file = File("$cacheDir/$fileName")
        if (!file.exists()) try {
            val `is` = assets.open(fileName)
            val size = `is`.available()
            val buffer = ByteArray(size)
            `is`.read(buffer)
            `is`.close()
            val fos = FileOutputStream(file)
            fos.write(buffer)
            fos.close()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        return file
    }

    private fun getListFiles(parentDir: File): MutableList<File> {
        val inFiles:MutableList<File> = ArrayList()
        val files = parentDir.listFiles()
        if (files != null) {
            for (file in files) {
                Log.d("FILES","igothere")
                Log.d("FILES",file.name)
                if (file.isDirectory) {
                    inFiles.addAll(getListFiles(file))
                } else {
                    if (file.name.endsWith(".epub")) {
                        inFiles.add(file)
                    }
                }
            }
        }
        return inFiles
    }

    private fun askForWidgetToUse(filePath: String?) {
        val intent = Intent(this@MenuActivity, MainActivity::class.java)
        intent.putExtra("filePath", filePath)
        AlertDialog.Builder(this@MenuActivity)
                .setTitle("Pick your widget")
                .setMessage("Textview or WebView?")
                .setPositiveButton("TextView") { dialog, which ->
                    intent.putExtra("isWebView", false)
                    startActivity(intent)
                }
                .setNegativeButton("WebView") { dialog, which ->
                    intent.putExtra("isWebView", true)
                    startActivity(intent)
                }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
    }
}