package com.ultraman.battle;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

/**
 * 奥特曼对战桌游 —— 原生 WebView 外壳
 * 游戏本体是 assets/index.html（纯离线单文件，无任何网络请求）。
 */
public class MainActivity extends Activity {

    private WebView web;
    private ProgressBar loading;
    private long lastBackPressed = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // 刘海屏/挖孔屏：内容延伸到状态栏区域，由 HTML 里的 env(safe-area-inset-*) 负责避让
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#000000"));
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        web = new WebView(this);
        web.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        web.setBackgroundColor(Color.parseColor("#0a0a1a"));
        root.addView(web);

        // 首屏加载指示：HTML 解析完成前不出现白屏
        loading = new ProgressBar(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        loading.setLayoutParams(lp);
        root.addView(loading);

        setContentView(root);

        configureWebView(web);
        hideSystemBars();

        // 把原生能力桥接进 JS：window.Android.vibrate(ms) / keepScreenOn(true) / exit()
        web.addJavascriptInterface(new NativeBridge(this), "Android");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                loading.setVisibility(View.GONE);
                injectVibrateBridge(view);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                Toast.makeText(MainActivity.this, "资源加载失败，请重启应用", Toast.LENGTH_SHORT).show();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                return false;   // 单文件离线游戏，不跳外部浏览器
            }
        });
        web.setWebChromeClient(new WebChromeClient());

        web.loadUrl("file:///android_asset/index.html");
    }

    @SuppressWarnings("SetJavaScriptEnabled")
    private void configureWebView(WebView w) {
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        // localStorage / IndexedDB：游戏存档依赖它，必须开
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        // 禁掉所有浏览器缩放控件，双击/双指不会误触
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setTextZoom(100);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }
        // 禁用长按选中的浏览器放大镜菜单，让触控手感贴近原生
        w.setLongClickable(false);
        w.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) { return true; }
        });
        w.setHapticFeedbackEnabled(false);
        w.setVerticalScrollBarEnabled(false);
        w.setHorizontalScrollBarEnabled(false);
        w.setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    private void injectVibrateBridge(WebView w) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            w.evaluateJavascript(
                "(function(){try{navigator.vibrate=function(p){" +
                "if(window.Android&&window.Android.vibrate){" +
                "var ms=Array.isArray(p)?(p[0]|0):(parseInt(p)||0);window.Android.vibrate(ms);}" +
                "return true;};}catch(e){}})();", null);
        }
    }

    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            InsetsHelper.hide(getWindow());
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            long now = System.currentTimeMillis();
            if (now - lastBackPressed < 2000) {
                finishAffinity();
                return true;
            }
            lastBackPressed = now;
            Toast.makeText(this, "再按一次退出游戏", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        if (web != null) { web.onPause(); web.pauseTimers(); }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) { web.resumeTimers(); web.onResume(); }
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.getSettings().setJavaScriptEnabled(false);
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    /** 供 JS 调用的原生能力：震动 / 常亮 / 退出 */
    public static class NativeBridge {
        private final Context ctx;
        NativeBridge(Context c) { this.ctx = c; }

        @android.webkit.JavascriptInterface
        public void vibrate(int ms) {
            try {
                if (ms <= 0) return;
                Vibrator v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
                if (v == null) return;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(Math.min(ms, 1000), VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(Math.min(ms, 1000));
                }
            } catch (Exception ignored) { }
        }

        @android.webkit.JavascriptInterface
        public void keepScreenOn(final boolean on) {
            try {
                ((Activity) ctx).runOnUiThread(new Runnable() {
                    @Override public void run() {
                        if (on) ((Activity) ctx).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        else ((Activity) ctx).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                });
            } catch (Exception ignored) { }
        }

        @android.webkit.JavascriptInterface
        public void exit() { ((Activity) ctx).finishAffinity(); }
    }

    /** 隔离 API 30+ 的 WindowInsetsController 调用，避免低版本类加载失败 */
    private static final class InsetsHelper {
        static void hide(Window window) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.view.WindowInsetsController c = window.getInsetsController();
                if (c != null) {
                    c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    c.setSystemBarsBehavior(android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            }
        }
    }
}
