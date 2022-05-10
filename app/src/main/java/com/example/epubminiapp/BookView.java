package com.example.epubminiapp;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.epub.EpubReader;

public class BookView extends Activity {
    private String currentChapterContent;
    private int currentChapterIndex;
    private int chapterCount;
    private List<TOCReference> tocReferenceList;
    private TextView chaptCntView;
    WebView bookContentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.book_viewer);
        AssetManager assetManager = getAssets();

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
            bookContentView = (WebView)findViewById(R.id.bookText);
            // set webview settings
            WebSettings bookWebSettings = bookContentView.getSettings();
            bookWebSettings.setJavaScriptEnabled(true);
            bookWebSettings.setDomStorageEnabled(true);
            bookContentView.setWebChromeClient(new WebChromeClient(){
                @Override
                public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                    Log.d("MyApplication", consoleMessage.message() + " -- From line "
                            + consoleMessage.lineNumber() + " of "
                            + consoleMessage.sourceId());
                    return super.onConsoleMessage(consoleMessage);
                }

            });
            bookContentView.setWebViewClient(new WebViewClient(){
                @Override
                public void onPageFinished(WebView view, String url) {
                    injectCSS("readium-before.css");
                    injectCSS("readium-default.css");
                    injectCSS("readium-after.css");
                }
            });

            this.tocReferenceList= book.getTableOfContents().getTocReferences();
            chapterCount = tocReferenceList.size()-1;
            chaptCntView = findViewById(R.id.chapterCounter);
            chaptCntView.setText("1/"+String.valueOf(this.chapterCount));

            loadChapter(1);

            // bind buttons
            Button nxtBtn = (Button)findViewById(R.id.next_btn);
            Button prevBtn = (Button)findViewById(R.id.prev_btn);


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

        bookContentView.loadData(this.currentChapterContent,"text/html", "UTF-8");
        Log.i("wholething",contents);
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

    public void injectCSS(String cssFile) {
        try {
            InputStream inputStream = getAssets().open("webassets/"+cssFile);
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
                    "parent.appendChild(style);" +
                    "})()",new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String s) {
                    Log.d("LogName", s); // Prints "" (Two double quotes)
                }
            });

            this.bookContentView.evaluateJavascript("j\n" +
                    "document.addEventListener(\"DOMContentLoaded\", function(event) {\n" +
                    "  var frame = document.getElementById(\"page\");\n" +
                    "  var scrollLeft = function() {\n" +
                    "    var gap = parseInt(window.getComputedStyle(frame.contentWindow.document.documentElement).getPropertyValue(\"column-gap\"));\n" +
                    "    frame.contentWindow.scrollTo(frame.contentWindow.scrollX - frame.contentWindow.innerWidth - gap, 0);\n" +
                    "  };\n" +
                    "\n" +
                    "  var scrollRight = function() {\n" +
                    "    var gap = parseInt(window.getComputedStyle(frame.contentWindow.document.documentElement).getPropertyValue(\"column-gap\"));\n" +
                    "    frame.contentWindow.scrollTo(frame.contentWindow.scrollX + frame.contentWindow.innerWidth + gap, 0);\n" +
                    "  };\n" +
                    "\n" +
                    "  document.body.addEventListener('click', function(e) {\n" +
                    "    e.preventDefault();\n" +
                    "    if (e.clientX > (window.innerWidth / 2)) {\n" +
                    "      scrollRight();\n" +
                    "    } else {\n" +
                    "      scrollLeft();\n" +
                    "    }\n" +
                    "  });\n" +
                    "  document.body.addEventListener('keydown', function(e) {\n" +
                    "    if (e.keyCode == \"39\") {\n" +
                    "      scrollRight();\n" +
                    "    } else if (e.keyCode == \"37\") {\n" +
                    "      scrollLeft();\n" +
                    "    }\n" +
                    "  });\n" +
                    "})",new ValueCallback<String>() {
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
