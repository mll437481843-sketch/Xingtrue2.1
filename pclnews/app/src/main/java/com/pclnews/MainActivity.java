package com.pclnews;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    WebView web;
    final ExecutorService pool = Executors.newSingleThreadExecutor();
    final ExecutorService imgPool = Executors.newFixedThreadPool(3);
    final ConcurrentHashMap<String, String> imgCache = new ConcurrentHashMap<>();
    volatile boolean modalOpen = false;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Window w = getWindow();
        w.setStatusBarColor(Color.parseColor("#F5F6F8"));
        w.setNavigationBarColor(Color.parseColor("#FFFFFF"));
        if (Build.VERSION.SDK_INT >= 23) {
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#F5F6F8"));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setTextZoom(100);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        if (Build.VERSION.SDK_INT >= 21) s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.addJavascriptInterface(new Bridge(), "PC");

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#F5F6F8"));
        root.addView(web, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        web.loadUrl("file:///android_asset/index.html");

        if (Store.getInt(this, Store.K_HOUR, -1) < 0) Alarm.schedule(this, 8, 0);
        askNotification();
    }

    void askNotification() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 9);
        }
    }

    static String J(String s) { return "\"" + Store.esc(s) + "\""; }

    void js(final String code) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (web != null) web.evaluateJavascript(code, null);
            }
        });
    }

    public void onBackPressed() {
        if (modalOpen) js("window.closeModal && window.closeModal()");
        else super.onBackPressed();
    }

    protected void onDestroy() {
        pool.shutdownNow();
        imgPool.shutdownNow();
        if (web != null) web.destroy();
        super.onDestroy();
    }

    public class Bridge {
        @JavascriptInterface
        public String cached() {
            String c = Store.get(MainActivity.this, Store.K_CACHE, "");
            return c.isEmpty() ? "{\"updated\":0,\"items\":[]}" : c;
        }

        @JavascriptInterface
        public String settings() {
            return "{\"hour\":" + Store.getInt(MainActivity.this, Store.K_HOUR, 8)
                    + ",\"minute\":" + Store.getInt(MainActivity.this, Store.K_MIN, 0)
                    + ",\"notify\":" + Store.getBool(MainActivity.this, Store.K_NOTIFY, true)
                    + ",\"lty\":" + Store.getBool(MainActivity.this, Store.K_LTY, true)
                    + ",\"img\":" + Store.getBool(MainActivity.this, Store.K_IMG, true) + "}";
        }

        @JavascriptInterface
        public void save(int hour, int minute, boolean notify, boolean lty, boolean img) {
            Store.putBool(MainActivity.this, Store.K_NOTIFY, notify);
            Store.putBool(MainActivity.this, Store.K_LTY, lty);
            Store.putBool(MainActivity.this, Store.K_IMG, img);
            Alarm.schedule(MainActivity.this, hour, minute);
            final String msg = String.format(java.util.Locale.US, "\u5df2\u8bbe\u7f6e\uff1a\u6bcf\u5929 %02d:%02d %s", hour, minute,
                    notify ? "\u63a8\u9001" : "\u4ec5\u9759\u9ed8\u66f4\u65b0");
            runOnUiThread(new Runnable() {
                public void run() { Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show(); }
            });
        }

        @JavascriptInterface
        public void refresh() {
            pool.execute(new Runnable() {
                public void run() {
                    js("window.onLoading && window.onLoading()");
                    String payload;
                    try {
                        List<Fetcher.Item> items = Fetcher.fetchAllWithCache(MainActivity.this,
                                Store.getBool(MainActivity.this, Store.K_LTY, true));
                        if (items.isEmpty()) {
                            payload = "{\"ok\":false,\"msg\":\"\u7f51\u7edc\u4e0d\u901a\u6216\u6240\u6709\u6e90\u90fd\u6293\u53d6\u5931\u8d25\",\"items\":[]}";
                        } else {
                            Store.saveCache(MainActivity.this, items);
                            payload = "{\"ok\":true,\"ms\":" + items.size() + "," + Store.buildJson(items).substring(1);
                        }
                    } catch (Exception e) {
                        payload = "{\"ok\":false,\"msg\":\"" + Store.esc(e.toString()) + "\",\"items\":[]}";
                    }
                    js("window.onNews(" + payload + ")");
                }
            });
        }

        @JavascriptInterface
        public void ogImage(final String url) {
            if (url == null || url.isEmpty()) return;
            final String hit = imgCache.get(url);
            if (hit != null) {
                if (!hit.isEmpty()) js("window.onImg && window.onImg(" + J(url) + "," + J(hit) + ")");
                return;
            }
            imgPool.execute(new Runnable() {
                public void run() {
                    String img = "";
                    try { img = Fetcher.fetchOgImage(url); } catch (Exception ignored) { }
                    imgCache.put(url, img);
                    if (!img.isEmpty()) js("window.onImg && window.onImg(" + J(url) + "," + J(img) + ")");
                }
            });
        }

        @JavascriptInterface
        public void open(final String url) {
            if (url == null || url.isEmpty()) return;
            runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "\u6ca1\u6709\u53ef\u7528\u7684\u6d4f\u89c8\u5668", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        @JavascriptInterface
        public void testNotify() {
            pool.execute(new Runnable() {
                public void run() {
                    String head = Store.get(MainActivity.this, Store.K_HEAD, "\u5148\u5237\u65b0\u4e00\u6b21\u518d\u53d1\u901a\u77e5");
                    Notify.lty(MainActivity.this, "\u6d4b\u8bd5\u901a\u77e5", "1 \u6761\uff1a" + head + "\n\uff08\u901a\u77e5\u6e20\u9053 OK\uff09");
                }
            });
        }

        @JavascriptInterface
        public void setModal(boolean open) { modalOpen = open; }
    }
}
