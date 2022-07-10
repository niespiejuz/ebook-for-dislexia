package com.example.epubminiapp

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.widget.ProgressBar
import android.os.Bundle
import android.widget.GridView
import android.widget.AdapterView.OnItemClickListener
import android.os.AsyncTask
import com.github.mertakdut.exception.ReadingException
import android.widget.Toast
import android.os.Environment
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.mertakdut.Reader
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.lang.RuntimeException
import java.util.ArrayList

class MenuActivity : AppCompatActivity() {

    private var bookList: MutableList<BookInfo>? = null;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

       // val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
       // setSupportActionBar(toolbar)

        val settingsBtn = findViewById<View>(R.id.Settings_Button)
        settingsBtn.setOnClickListener{
            showSettings()
        }
        (findViewById<View>(R.id.grid_book_info) as GridView).onItemClickListener = OnItemClickListener { adapterView, view, i, l ->
            val clickedItemFilePath = (adapterView.adapter.getItem(i) as BookInfo).filePath
            askForWidgetToUse(clickedItemFilePath)
        }
        //progressBar = findViewById<View>(R.id.progressbar) as ProgressBar
        this.bookList = ListBookInfoTask().execute().get() as MutableList<BookInfo>
    }

    private inner class ListBookInfoTask : AsyncTask<Any?, Any?, List<BookInfo>?>() {
        private var occuredException: Exception? = null
        override fun onPreExecute() {
            super.onPreExecute()
           // progressBar!!.visibility = View.VISIBLE
        }

        override fun onPostExecute(bookInfoList: List<BookInfo>?) {
            super.onPostExecute(bookInfoList)
            //progressBar!!.visibility = View.GONE
            if (bookInfoList != null) {
                val adapter = BookInfoGridAdapter(this@MenuActivity, bookInfoList)
                (findViewById<View>(R.id.grid_book_info) as GridView).adapter = adapter
            }
            if (occuredException != null) {
                Toast.makeText(this@MenuActivity, occuredException!!.message, Toast.LENGTH_LONG).show()
            }
        }

        override fun doInBackground(vararg p0: Any?): List<BookInfo>? {
            val bookInfoList = searchForEpubFiles()
            val reader = Reader()
            for (bookInfo in bookInfoList!!) {
                try {
                    reader.setInfoContent(bookInfo.filePath)
                    val title = reader.infoPackage.metadata.title
                    val author = reader.infoPackage.metadata.creator
                    if (title != null && title != "") {
                        bookInfo.title = reader.infoPackage.metadata.title

                    } else { // If title doesn't exist, use fileName instead.
                        val dotIndex = bookInfo.title!!.lastIndexOf('.')
                        bookInfo.title = bookInfo.title!!.substring(0, dotIndex)
                    }
                    if( author != null && author != ""){
                        bookInfo.author = author
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

    val sActivityResultLauncher: ActivityResultLauncher<Intent>  = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result: ActivityResult ->
            if(result.resultCode == Activity.RESULT_OK) {
                val data: Intent = result.data!!
                val uri: Uri = data.data!!
                val file = File(uri.path)
                val bookInfo = BookInfo()
                bookInfo.title = file.name
                bookInfo.filePath = file.path
                bookList!!.add(bookInfo)
                val adapter = BookInfoGridAdapter(this@MenuActivity, bookList!!)
                (findViewById<View>(R.id.grid_book_info) as GridView).adapter = adapter

            }
    }

    public final fun showSettings(){
        val intent = Intent(this@MenuActivity, SettingsView::class.java)
        startActivity(intent)
    }
    public final fun openFileDialog(view: android.view.View){
        var data = Intent(Intent.ACTION_OPEN_DOCUMENT)
        data.setType("*/*")
        data = Intent.createChooser(data,"Choose a file")
        sActivityResultLauncher.launch(data)
    }
    private fun searchForEpubFiles(): List<BookInfo>? {
        val isSDPresent = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        var bookInfoList: MutableList<BookInfo>? = null

        if(checkPermission()){
            if (isSDPresent) {
                bookInfoList = ArrayList()
                Log.d("FILES",Environment.getExternalStoragePublicDirectory("Download").absolutePath)
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
        }
        else{
            requestPermission();
        }


        return bookInfoList
    }
    // request permissions from the user
    private fun requestPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        {
            try{
                var intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                val uri = Uri.fromParts("package",packageName,null)
                intent.data = uri
                startActivity(intent)
            } catch(e:Exception){
                var intent = Intent();
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivity(intent)
            }
        }
        else{
            ActivityCompat.requestPermissions(this@MenuActivity, arrayOf(WRITE_EXTERNAL_STORAGE,
                READ_EXTERNAL_STORAGE),30)
        }
    }
    // check permissions for writing and reading external storage
    private fun checkPermission(): Boolean
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            return Environment.isExternalStorageManager()
        }
        val readcheck = ContextCompat.checkSelfPermission(applicationContext, READ_EXTERNAL_STORAGE)
        val writecheck = ContextCompat.checkSelfPermission(applicationContext,
            WRITE_EXTERNAL_STORAGE)
        return readcheck == PackageManager.PERMISSION_GRANTED && writecheck == PackageManager.PERMISSION_GRANTED
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
        startActivity(intent)
    }
}