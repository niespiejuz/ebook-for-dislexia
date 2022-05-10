package com.example.epubminiapp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.domain.TOCReference;

import androidx.appcompat.app.AppCompatActivity;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.apache.commons.lang3.StringEscapeUtils;

public class MainActivity extends AppCompatActivity {
    private String currentChapterContent;
    private int currentChapterIndex;
    private int chapterCount;
    private List<TOCReference> tocReferenceList;
    private TextView chaptCntView;
    WebView bookContentView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AssetManager assetManager = getAssets();
        System.out.println("Working Directory = " + System.getProperty("user.dir"));
        try {
            // find InputStream for book
            String bookFileName = "Jacek Santorski - Jak przetrwać w stresie.epub";
            InputStream epubInputStream = assetManager.open(bookFileName);

            // Load Book from inputStream
            Book book = (new EpubReader()).readEpub(epubInputStream);
            // Load and show cover image
            Bitmap coverImage = BitmapFactory.decodeStream(book.getCoverImage()
                    .getInputStream());

            ImageView coverImageView = findViewById(R.id.titleImage);
            coverImageView.setImageBitmap(coverImage);

            // Load book and initialize webview

            this.bookContentView = findViewById(R.id.bookText);
            bookContentView.getSettings().setJavaScriptEnabled(true);

            //this.bookContentView.setWebViewClient(new WebViewClient());
            this.tocReferenceList= book.getTableOfContents().getTocReferences();
            this.chapterCount = tocReferenceList.size()-1;
            this.chaptCntView = findViewById(R.id.chapterCounter);
            this.chaptCntView.setText("1/"+String.valueOf(this.chapterCount));

            loadChapter(1);


            // bind buttons
            Button nxtBtn = (Button)findViewById(R.id.btnNext);
            Button prevBtn = (Button)findViewById(R.id.btnPrev);


        } catch (IOException e) {
            Log.e("epublib", e.getMessage());
        }
    }

    protected void loadChapter(int chapterNumber) throws IOException {
        this.currentChapterIndex = chapterNumber;
        byte[] chapterResource;
        chapterResource = this.tocReferenceList.get(chapterNumber).getResource().getData();


        String contents = new String(chapterResource, StandardCharsets.UTF_8);
        contents = StringEscapeUtils.unescapeHtml4(contents);
        this.currentChapterContent = contents;
        Log.i("wholething",contents);
        bookContentView.loadData(this.currentChapterContent,"text/html", "UTF-8");
        injectCSS();
        return;
    }

    public void nextChapter(View view) {
            if (this.currentChapterIndex < this.chapterCount) {
                this.currentChapterIndex += 1;
                this.chaptCntView.setText(String.valueOf(this.currentChapterIndex)+
                        "/"+String.valueOf(this.chapterCount));

                try {
                    loadChapter(this.currentChapterIndex);

                } catch (IOException e) {
                    Log.e("nextChapter", e.getMessage());
                }
            }
            return;
        }

    public void prevChapter(View view) {
        if (this.currentChapterIndex > 1) {
            this.currentChapterIndex -= 1;
            this.chaptCntView.setText(String.valueOf(this.currentChapterIndex)+
                    "/"+String.valueOf(this.chapterCount));
            try {
                loadChapter(this.currentChapterIndex);

            } catch (IOException e) {
                Log.e("nextChapter", e.getMessage());
            }
        }
        return;
    }

    public void injectCSS() {
        try {
            InputStream inputStream = getAssets().open("webassets/epub.css");
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
            String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
            Log.d("CSS","CSS INJECTION TRIGGERED RIGHT NOW");
            this.bookContentView.evaluateJavascript("javascript:(function() {" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "var style = document.createElement('style');" +
                    "style.type = 'text/css';" +
                    // Tell the browser to BASE64-decode the string into your script !!!
                    "style.innerHTML = window.atob('" + encoded + "');" +
                    "parent.appendChild(style)" +
                    "})()",new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String s) {
                    Log.d("LogName", s); // Prints "" (Two double quotes)
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

