package com.samgang.contract;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 전자근로계약서 앱: assets/index.html 을 WebView 로 띄우고, 공유·복사·PDF 저장을 안드로이드 기능으로 연결합니다. */
public class MainActivity extends Activity {
    static final String HOME = "file:///android_asset/index.html";
    static final Pattern PAYLOAD = Pattern.compile("#(v[12]-[A-Za-z0-9-]{16,}|[a-z0-9]{10})(?![A-Za-z0-9-])");
    WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xFF1E3A5F);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setTextZoom(100);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handle(request.getUrl());
            }
            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handle(Uri.parse(url));
            }
        });
        setContentView(web);
        String frag = fragmentFrom(getIntent());
        web.loadUrl(frag != null ? HOME + "#" + frag : HOME);
    }

    /** 앱 안의 페이지(file://)만 WebView 에서 열고, 나머지 주소는 브라우저·카카오톡 등 바깥 앱으로 넘깁니다. */
    boolean handle(Uri u) {
        if (u == null) return false;
        String scheme = u.getScheme();
        if (scheme == null || "file".equals(scheme) || "about".equals(scheme) || "data".equals(scheme)) return false;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, u));
        } catch (Exception e) {
            toast("열 수 있는 앱이 없습니다");
        }
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String frag = fragmentFrom(intent);
        if (frag != null && web != null) web.loadUrl(HOME + "#" + frag);
    }

    /** 카카오톡 등에서 '공유'로 넘어온 글이나 링크에서 계약서 링크 부분만 뽑아냅니다. */
    static String fragmentFrom(Intent i) {
        if (i == null) return null;
        String text = null;
        if (Intent.ACTION_SEND.equals(i.getAction())) text = i.getStringExtra(Intent.EXTRA_TEXT);
        else if (Intent.ACTION_VIEW.equals(i.getAction()) && i.getData() != null) text = i.getData().toString();
        if (text == null) return null;
        Matcher m = PAYLOAD.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    void toast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() { Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show(); }
        });
    }

    static String safeName(String name) {
        String n = name == null ? "file.pdf" : name.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1f]", "_").trim();
        if (n.isEmpty()) n = "file.pdf";
        if (!n.toLowerCase().endsWith(".pdf")) n = n + ".pdf";
        return n;
    }

    /** 페이지의 window.Android 로 노출되는 함수들 (WebView 의 백그라운드 스레드에서 호출됨) */
    class Bridge {
        @JavascriptInterface
        public void share(final String title, final String text) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_SUBJECT, title);
                    i.putExtra(Intent.EXTRA_TEXT, text);
                    try { startActivity(Intent.createChooser(i, "보내기")); } catch (Exception e) { toast("공유할 수 있는 앱이 없습니다"); }
                }
            });
        }

        @JavascriptInterface
        public void openUrl(final String url) {
            runOnUiThread(new Runnable() {
                public void run() {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception e) { toast("열 수 있는 앱이 없습니다"); }
                }
            });
        }

        @JavascriptInterface
        public void copy(final String text) {
            runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(ClipData.newPlainText("link", text));
                    } catch (Exception e) { toast("복사하지 못했습니다"); }
                }
            });
        }

        /** PDF 를 다운로드 폴더에 저장하고(안드로이드 10 이상), share 가 true 면 공유 창을 엽니다. */
        @JavascriptInterface
        public String savePdf(String base64, String name, final boolean share) {
            try {
                byte[] data = Base64.decode(base64, Base64.DEFAULT);
                final String fname = safeName(name);
                File dir = new File(getCacheDir(), "share");
                dir.mkdirs();
                File f = new File(dir, fname);
                FileOutputStream fo = new FileOutputStream(f);
                fo.write(data);
                fo.close();
                boolean saved = false;
                if (Build.VERSION.SDK_INT >= 29) {
                    saved = saveToDownloads(data, fname);
                }
                final Uri content = Uri.parse("content://" + getPackageName() + ".files/" + Uri.encode(fname));
                if (share || !saved) {
                    final String title = share ? "PDF 보내기" : "PDF 저장할 곳 선택";
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Intent i = new Intent(Intent.ACTION_SEND);
                            i.setType("application/pdf");
                            i.putExtra(Intent.EXTRA_STREAM, content);
                            i.putExtra(Intent.EXTRA_SUBJECT, fname);
                            i.setClipData(ClipData.newRawUri(fname, content));
                            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            Intent chooser = Intent.createChooser(i, title);
                            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            try { startActivity(chooser); } catch (Exception e) { toast("공유할 수 있는 앱이 없습니다"); }
                        }
                    });
                    return saved ? "shared+saved" : "shared";
                }
                toast("다운로드 폴더에 저장했습니다: " + fname);
                return "saved";
            } catch (Exception e) {
                toast("PDF 저장 실패: " + e.getMessage());
                return "error:" + e.getMessage();
            }
        }
    }

    boolean saveToDownloads(byte[] data, String fname) {
        try {
            ContentValues v = new ContentValues();
            v.put(MediaStore.Downloads.DISPLAY_NAME, fname);
            v.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
            v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (u == null) return false;
            OutputStream os = getContentResolver().openOutputStream(u);
            os.write(data);
            os.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
