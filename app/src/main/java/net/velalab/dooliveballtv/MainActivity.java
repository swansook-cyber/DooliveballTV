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
    private boolean longOkHandled = false;
    private long okPressedAt = 0L;

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
        UiModeManager mode = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
        remoteMode = mode != null && mode.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
        remotePointer = makeLabel("+", 30, 0xFFFF5C9D, true);
        remotePointer.setGravity(Gravity.CENTER);
        remotePointer.setShadowLayer(3f, 1f, 1f, Color.BLACK);
        remotePointer.setVisibility(View.GONE);
        browserHost.addView(remotePointer, new FrameLayout.LayoutParams(dp(46), dp(46)));
        browserHost.post(() -> {
            pointerX = browserHost.getWidth() / 2f;
            pointerY = browserHost.getHeight() / 2f;
            positionPointer();
            browser.setFocusable(true);
            browser.setFocusableInTouchMode(true);
            if (remoteMode) { browser.requestFocus(); moveDomFocus(0, 1); }
        });

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

    // Spatial DOM navigation: move to the nearest visible control in the pressed direction.
    // The pointer remains available for cross-origin iframes and canvas-based players.
    private void moveDomFocus(int dx, int dy) {
        if (browser == null) return;
        String js = "(function(){"
            + "const nodes=Array.from(document.querySelectorAll('a[href],button,[role=button],input,select,video,[tabindex],summary'));"
            + "const items=nodes.filter(e=>{const r=e.getBoundingClientRect(),s=getComputedStyle(e);return r.width>9&&r.height>9&&s.visibility!=='hidden'&&s.display!=='none'&&r.bottom>=0&&r.top<=innerHeight&&r.right>=0&&r.left<=innerWidth});"
            + "if(!items.length){scrollBy(0," + (dy * 260) + ");return 'empty';}"
            + "let current=document.activeElement;if(!items.includes(current))current=null;"
            + "let cr=current?current.getBoundingClientRect():{left:innerWidth/2,right:innerWidth/2,top:innerHeight/2,bottom:innerHeight/2};"
            + "let x=(cr.left+cr.right)/2,y=(cr.top+cr.bottom)/2;"
            + "let candidates=items.map(e=>{let r=e.getBoundingClientRect(),cx=(r.left+r.right)/2,cy=(r.top+r.bottom)/2,ox=cx-x,oy=cy-y;"
            + "let along=ox*" + dx + "+oy*" + dy + ",cross=Math.abs(ox*" + dy + "-oy*" + dx + ");"
            + "return {e,along,cross,score:along+cross*2.8}}).filter(o=>o.along>4);"
            + "candidates.sort((a,b)=>a.score-b.score);let next=candidates.length?candidates[0].e:null;"
            + "if(!next){scrollBy({top:" + (dy * 260) + ",left:" + (dx * 260) + ",behavior:'smooth'});return 'scroll';}"
            + "if(current){current.classList.remove('vela-focus');current.style.outline='';current.style.outlineOffset='';}"
            + "next.setAttribute('tabindex','0');next.focus({preventScroll:true});next.scrollIntoView({block:'nearest',inline:'nearest',behavior:'smooth'});"
            + "next.classList.add('vela-focus');next.style.outline='4px solid #ff1597';next.style.outlineOffset='3px';return 'focused';"
            + "})();";
        browser.evaluateJavascript(js, null);
    }

    private void activateDomFocus() {
        if (browser == null) return;
        browser.evaluateJavascript("(function(){let e=document.activeElement;if(e&&e!==document.body){e.click();if(e.tagName==='VIDEO'&&e.paused)e.play();return true}return false})();", null);
    }

    private void togglePointerMode() {
        pointerMode = !pointerMode;
        remotePointer.setVisibility(pointerMode ? View.VISIBLE : View.GONE);
        if (pointerMode) {
            if (pointerX <= 0 || pointerY <= 0) {
                pointerX = browserHost.getWidth() / 2f;
                pointerY = browserHost.getHeight() / 2f;
            }
            positionPointer();
        } else {
            browser.requestFocus();
        }
        Toast.makeText(this, pointerMode ? "โหมดเมาส์: ลูกศรเลื่อน / OK คลิก" : "โหมดเลือกเมนู", Toast.LENGTH_SHORT).show();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        final int key=event.getKeyCode();
        if (key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_ESCAPE)
            return super.dispatchKeyEvent(event);
        if (videoView != null) return super.dispatchKeyEvent(event);
        boolean ok=key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER||key==KeyEvent.KEYCODE_NUMPAD_ENTER;
        boolean up=key==KeyEvent.KEYCODE_DPAD_UP, down=key==KeyEvent.KEYCODE_DPAD_DOWN;
        boolean left=key==KeyEvent.KEYCODE_DPAD_LEFT, right=key==KeyEvent.KEYCODE_DPAD_RIGHT;
        if(key==KeyEvent.KEYCODE_MENU||key==KeyEvent.KEYCODE_INFO){
            if(event.getAction()==KeyEvent.ACTION_UP)togglePointerMode();
            return true;
        }
        if(ok){
            if(event.getAction()==KeyEvent.ACTION_DOWN){
                if(event.getRepeatCount()==0){
                    longOkHandled=false;
                    okPressedAt=android.os.SystemClock.uptimeMillis();
                }else if(!longOkHandled && (event.isLongPress() || event.getRepeatCount()>=3)){
                    longOkHandled=true;
                    togglePointerMode();
                }
            }else if(event.getAction()==KeyEvent.ACTION_UP){
                if(!longOkHandled && android.os.SystemClock.uptimeMillis()-okPressedAt>=650L){
                    longOkHandled=true;
                    togglePointerMode();
                }
                if(!longOkHandled){if(pointerMode)tapAtPointer();else activateDomFocus();}
            }
            return true;
        }
        if(up||down||left||right){
            if(event.getAction()==KeyEvent.ACTION_DOWN){
                if(pointerMode){
                    float step=dp(event.getRepeatCount()>5?75:32);
                    if(up)pointerY-=step;if(down)pointerY+=step;
                    if(left)pointerX-=step;if(right)pointerX+=step;
                    positionPointer();
                    if((up&&pointerY<=dp(15))||(down&&pointerY>=browserHost.getHeight()-dp(15)))
                        browser.scrollBy(0, up?-dp(110):dp(110));
                }else if(event.getRepeatCount()==0||event.getRepeatCount()%3==0){
                    moveDomFocus(right?1:left?-1:0,down?1:up?-1:0);
                }
            }
            return true;
        }
        if(event.getAction()==KeyEvent.ACTION_DOWN){
            if(key==KeyEvent.KEYCODE_PAGE_DOWN||key==KeyEvent.KEYCODE_CHANNEL_DOWN){
                browser.scrollBy(0,browser.getHeight()/2);return true;
            }
            if(key==KeyEvent.KEYCODE_PAGE_UP||key==KeyEvent.KEYCODE_CHANNEL_UP){
                browser.scrollBy(0,-browser.getHeight()/2);return true;
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
