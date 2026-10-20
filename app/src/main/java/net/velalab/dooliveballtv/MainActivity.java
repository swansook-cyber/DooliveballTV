package net.velalab.dooliveballtv;

import android.app.Activity;
import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
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
import org.json.JSONArray;
import org.json.JSONException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final String FOOTBALL = "https://dooliveball.com/";
    private WebView browser;
    private FrameLayout browserHost;
    private FrameLayout overlay;
    private View videoView;
    private WebChromeClient.CustomViewCallback videoCallback;
    private TextView remotePointer;
    private float pointerX, pointerY;
    private boolean remoteMode;
    private boolean pointerMode = false;
    private boolean longOkHandled;
    private String tvNavigationScript;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        UiModeManager mode = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
        remoteMode = mode != null && mode.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
        if (remoteMode) {
            try (InputStream input = getAssets().open("tv_navigation.js")) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] chunk = new byte[4096];
                int size;
                while ((size = input.read(chunk)) != -1) bytes.write(chunk, 0, size);
                tvNavigationScript = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                pointerMode = true;
            }
        }
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
        TextView brand = makeLabel("▶  VELA SPORT", 17, Color.WHITE, true);
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
        remotePointer = makeLabel("+", 30, 0xFFFF5C9D, true);
        remotePointer.setGravity(Gravity.CENTER);
        remotePointer.setShadowLayer(3f, 1f, 1f, Color.BLACK);
        remotePointer.setVisibility(pointerMode ? View.VISIBLE : View.GONE);
        remotePointer.setClickable(false);
        remotePointer.setFocusable(false);
        browserHost.addView(remotePointer, new FrameLayout.LayoutParams(dp(46), dp(46)));
        browserHost.post(() -> {
            pointerX = browserHost.getWidth() / 2f;
            pointerY = browserHost.getHeight() / 2f;
            positionPointer();
            browser.setFocusable(true);
            browser.setFocusableInTouchMode(true);
            if (remoteMode) browser.requestFocus();
        });
        browserHost.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> positionPointer());

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


    private void positionPointer() {
        if (remotePointer == null) return;
        pointerX = Math.max(dp(12), Math.min(browserHost.getWidth() - dp(12), pointerX));
        pointerY = Math.max(dp(12), Math.min(browserHost.getHeight() - dp(12), pointerY));
        remotePointer.setX(pointerX - dp(23));
        remotePointer.setY(pointerY - dp(23));
    }

    private void tapAtPointer() {
        long now = android.os.SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, pointerX, pointerY, 0);
        MotionEvent up = MotionEvent.obtain(now, now + 80, MotionEvent.ACTION_UP, pointerX, pointerY, 0);
        browser.dispatchTouchEvent(down);
        browser.dispatchTouchEvent(up);
        down.recycle();
        up.recycle();
    }

    private void moveDomFocus(String direction) {
        if (browser == null || tvNavigationScript == null) return;
        browser.evaluateJavascript(tvNavigationScript + "\nVelaTV.move('" + direction + "');", null);
    }

    private void activateDomFocus() {
        if (browser == null || tvNavigationScript == null) return;
        browser.evaluateJavascript(tvNavigationScript + "\nVelaTV.activate();", result -> {
            if (result == null || result.equals("null")) return;
            try {
                JSONArray point = new JSONArray(result);
                pointerX = Math.max(0, Math.min(browser.getWidth() - 1, (float) point.getDouble(0) * browser.getWidth()));
                pointerY = Math.max(0, Math.min(browser.getHeight() - 1, (float) point.getDouble(1) * browser.getHeight()));
                tapAtPointer();
                if (point.getBoolean(2)) setPointerMode(true);
            } catch (JSONException ignored) { }
        });
    }

    private void setPointerMode(boolean enabled) {
        pointerMode = enabled;
        remotePointer.setVisibility(enabled ? View.VISIBLE : View.GONE);
        positionPointer();
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int key = event.getKeyCode();
        if (key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_ESCAPE) {
            if (event.getAction() == KeyEvent.ACTION_UP) onBackPressed();
            return true;
        }
        if (videoView != null) return super.dispatchKeyEvent(event);
        if (!remoteMode) return super.dispatchKeyEvent(event);
        if (key == KeyEvent.KEYCODE_MENU || key == KeyEvent.KEYCODE_INFO) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                setPointerMode(!pointerMode);
                Toast.makeText(this, pointerMode ? "โหมดตัวชี้: ใช้ลูกศรและ OK" : "โหมดเลือกเมนู", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        boolean up = key == KeyEvent.KEYCODE_DPAD_UP;
        boolean down = key == KeyEvent.KEYCODE_DPAD_DOWN;
        boolean left = key == KeyEvent.KEYCODE_DPAD_LEFT;
        boolean right = key == KeyEvent.KEYCODE_DPAD_RIGHT;
        boolean ok = key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER || key == KeyEvent.KEYCODE_NUMPAD_ENTER;
        if (up || down || left || right || ok) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (ok && (event.isLongPress() || event.getRepeatCount() > 0)) {
                    if (!longOkHandled) setPointerMode(!pointerMode);
                    longOkHandled = true;
                    return true;
                }
                if (pointerMode) {
                    float step = dp(35);
                    if (up) pointerY -= step;
                    else if (down) pointerY += step;
                    else if (left) pointerX -= step;
                    else if (right) pointerX += step;
                    positionPointer();
                } else {
                    if (up) moveDomFocus("up");
                    else if (down) moveDomFocus("down");
                    else if (left) moveDomFocus("left");
                    else if (right) moveDomFocus("right");
                }
            }
            if (ok && event.getAction() == KeyEvent.ACTION_UP) {
                if (!longOkHandled) {
                    if (pointerMode) tapAtPointer();
                    else activateDomFocus();
                }
                longOkHandled = false;
            }
            return true;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (key == KeyEvent.KEYCODE_PAGE_DOWN || key == KeyEvent.KEYCODE_CHANNEL_DOWN) {
                browser.scrollBy(0, browser.getHeight() / 2); return true;
            }
            if (key == KeyEvent.KEYCODE_PAGE_UP || key == KeyEvent.KEYCODE_CHANNEL_UP) {
                browser.scrollBy(0, -browser.getHeight() / 2); return true;
            }
        }
        return super.dispatchKeyEvent(event);
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
