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
    private static final String TELEVISION = "https://dooliveball.com/ดูทีวีออนไลน์";
    private static final int PINK = Color.rgb(235, 68, 142);
    private WebView[] browsers = new WebView[2];
    private TextView[] tabs = new TextView[2];
    private FrameLayout browserHost;
    private FrameLayout overlay;
    private View videoView;
    private WebChromeClient.CustomViewCallback videoCallback;
    private int active = 0;

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
        reload.setOnClickListener(v -> browsers[active].reload());
        root.addView(header);

        browserHost = new FrameLayout(this);
        root.addView(browserHost, new LinearLayout.LayoutParams(-1, 0, 1));
        for (int i = 0; i < 2; i++) {
            browsers[i] = createBrowser();
            browserHost.addView(browsers[i], new FrameLayout.LayoutParams(-1, -1));
            browsers[i].setVisibility(i == active ? View.VISIBLE : View.GONE);
            browsers[i].loadUrl(i == 0 ? FOOTBALL : TELEVISION);
        }

        LinearLayout nav = new LinearLayout(this);
        nav.setBackgroundColor(Color.rgb(22, 31, 53));
        String[] labels = {"⚽  ดูบอลสด", "▣  ทีวีออนไลน์"};
        for (int i = 0; i < 2; i++) {
            final int selected = i;
            tabs[i] = makeLabel(labels[i], 16, Color.WHITE, true);
            tabs[i].setGravity(Gravity.CENTER);
            nav.addView(tabs[i], new LinearLayout.LayoutParams(0, dp(62), 1));
            tabs[i].setOnClickListener(v -> switchTab(selected));
        }
        root.addView(nav);
        overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.BLACK);
        addContentView(overlay, new ViewGroup.LayoutParams(-1, -1));
        overlay.setVisibility(View.GONE);
        updateTabs();
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

    private void switchTab(int next) {
        if (videoView != null) closeVideo();
        active = next;
        for (int i = 0; i < browsers.length; i++) browsers[i].setVisibility(i == next ? View.VISIBLE : View.GONE);
        updateTabs();
    }
    private void updateTabs() {
        for (int i = 0; i < tabs.length; i++) {
            tabs[i].setTextColor(i == active ? PINK : 0xFFB8C4D6);
            tabs[i].setBackgroundColor(i == active ? 0xFF29304B : 0xFF161F35);
        }
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
        if (browsers[active].canGoBack()) { browsers[active].goBack(); return; }
        if (active != 0) { switchTab(0); return; }
        super.onBackPressed();
    }
    @Override protected void onPause() { super.onPause(); for (WebView v: browsers) if (v != null) v.onPause(); }
    @Override protected void onResume() { super.onResume(); for (WebView v: browsers) if (v != null) v.onResume(); }
    @Override protected void onDestroy() {
        if (videoView != null) closeVideo();
        for (WebView v: browsers) { if (v != null) { v.stopLoading(); v.destroy(); } }
        super.onDestroy();
    }
}
