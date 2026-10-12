package net.velalab.dooliveballtv;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Typeface;

public class MainActivity extends Activity {
    private static final String FOOTBALL = "https://dooliveball.com/";
    private WebView browser;
    private FrameLayout browserHost;
    private FrameLayout overlay;
    private View videoView;
    private WebChromeClient.CustomViewCallback videoCallback;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(16, 23, 43));
        getWindow().setNavigationBarColor(Color.rgb(16, 23, 43));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(16, 23, 43));
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), 0, dp(12), 0);
        header.setBackgroundColor(Color.rgb(16, 23, 43));
        TextView brand = makeLabel("▶  DOOLIVEBALL", 17, Color.WHITE, true);
        header.addView(brand, new LinearLayout.LayoutParams(0, dp(52), 1));
        TextView reload = makeLabel("↻", 28, Color.WHITE, false);
        reload.setGravity(Gravity.CENTER);
        header.addView(reload, new LinearLayout.LayoutParams(dp(48), dp(48)));
        reload.setOnClickListener(v -> browser.reload());
        root.addView(header);

        browserHost = new FrameLayout(this);
        root.addView(browserHost, new LinearLayout.LayoutParams(-1, 0, 1));
        browser = createBrowser();
        browserHost.addView(browser, new FrameLayout.LayoutParams(-1, -1));
        browser.loadUrl(FOOTBALL);

        overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.BLACK);
        addContentView(overlay, new ViewGroup.LayoutParams(-1, -1));
        overlay.setVisibility(View.GONE);
    }

    private TextView makeLabel(String text, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text); t.setTextSize(sp); t.setTextColor(color); t.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private int dp(int size) { return Math.round(getResources().getDisplayMetrics().density * size); }

    private WebView createBrowser() {
        WebView browser = new WebView(this);
        browser.setBackgroundColor(Color.WHITE);
        browser.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleLink(request.getUrl(), request.isForMainFrame());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleLink(Uri.parse(url), true);
            }
        });
        WebSettings s = browser.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(browser, true);
        browser.setWebChromeClient(new WebChromeClient() {
            @Override public void onShowCustomView(View view, CustomViewCallback callback) {
                if (videoView != null) { callback.onCustomViewHidden(); return; }
                videoView = view;
                videoCallback = callback;
                overlay.addView(view, new FrameLayout.LayoutParams(-1, -1));
                overlay.setVisibility(View.VISIBLE);
                getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }
            @Override public void onHideCustomView() { closeVideo(); }
        });
        return browser;
    }

    private boolean handleLink(Uri uri, boolean mainFrame) {
        String scheme = uri.getScheme();
        if (scheme == null) return true;
        if (scheme.equals("http") || scheme.equals("https")) return false;
        if (!mainFrame) return true;
        if (scheme.equals("intent")) return true;
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
        catch (ActivityNotFoundException e) { Toast.makeText(this, "ไม่มีแอปรองรับลิงก์นี้", Toast.LENGTH_SHORT).show(); }
        return true;
    }

    private void closeVideo() {
        if (videoView == null) return;
        overlay.removeView(videoView);
        overlay.setVisibility(View.GONE);
        videoView = null;
        getWindow().getDecorView().setSystemUiVisibility(0);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        if (videoCallback != null) { videoCallback.onCustomViewHidden(); videoCallback = null; }
    }
    @Override public void onBackPressed() {
        if (videoView != null) { closeVideo(); return; }
        if (browser.canGoBack()) { browser.goBack(); return; }
        super.onBackPressed();
    }
    @Override protected void onPause() { super.onPause(); if (browser != null) browser.onPause(); }
    @Override protected void onResume() { super.onResume(); if (browser != null) browser.onResume(); }
    @Override protected void onDestroy() {
        if (videoView != null) closeVideo();
        if (browser != null) { browser.stopLoading(); browser.destroy(); }
        super.onDestroy();
    }
}
