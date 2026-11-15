package care.integrity.emergency;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.*;
import android.widget.*;

/** Android host for the existing Integrity UI. No privileged JavaScript bridge. */
public final class MainActivity extends Activity {
    private static final String HOME = "https://integrity-emergency-care-hazel.vercel.app/";
    private static final int LOCATION = 10, PICK_FILE = 11;
    private WebView web;
    private ProgressBar progress;
    private LinearLayout error;
    private ValueCallback<Uri[]> fileCallback;
    private GeolocationPermissions.Callback locationCallback;
    private String locationOrigin;
    private boolean failed;

    // The existing website requires JavaScript; only the HTTPS site is loaded in-app.
    @android.annotation.SuppressLint("SetJavaScriptEnabled")
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);
        // Android 15 enforces edge-to-edge: keep web controls clear of system bars and keyboard.
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
            android.graphics.Insets bars = insets.getInsets(
                android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else {
                view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        web = new WebView(this);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        FrameLayout.LayoutParams progressLayout = new FrameLayout.LayoutParams(-1, dp(3), Gravity.TOP);
        root.addView(progress, progressLayout);
        error = new LinearLayout(this);
        error.setOrientation(LinearLayout.VERTICAL);
        error.setGravity(Gravity.CENTER);
        error.setPadding(dp(28), dp(28), dp(28), dp(28));
        error.setBackgroundColor(Color.WHITE);
        TextView title = new TextView(this);
        title.setText(R.string.connection_title);
        title.setTextSize(24);
        title.setTextColor(Color.rgb(192,32,42));
        title.setGravity(Gravity.CENTER);
        error.addView(title);
        TextView description = new TextView(this);
        description.setText(R.string.connection_description);
        description.setTextSize(16);
        description.setPadding(0, dp(16), 0, dp(24));
        description.setGravity(Gravity.CENTER);
        error.addView(description);
        Button retry = new Button(this);
        retry.setText(R.string.retry);
        retry.setOnClickListener(v -> { error.setVisibility(View.GONE); web.reload(); });
        error.addView(retry);
        error.setVisibility(View.GONE);
        root.addView(error, new FrameLayout.LayoutParams(-1,-1));
        setContentView(root);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(true);
        CookieManager.getInstance().setAcceptCookie(true);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri uri = req.getUrl();
                if (trusted(uri)) return false;
                openExternal(uri);
                return true;
            }
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
                failed = false;
                error.setVisibility(View.GONE);
                progress.setVisibility(View.VISIBLE);
            }
            @Override public void onPageFinished(WebView view, String url) {
                progress.setVisibility(View.GONE);
                if (trusted(Uri.parse(url)) && !failed) installBackHandler();
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError e) {
                if (req.isForMainFrame()) showError();
            }
            @Override public void onReceivedHttpError(WebView view, WebResourceRequest req, WebResourceResponse response) {
                if (req.isForMainFrame() && response.getStatusCode() >= 400) showError();
            }
            // TLS errors retain WebView's default cancellation behavior.
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int value) { progress.setProgress(value); }
            @Override public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                new AlertDialog.Builder(MainActivity.this).setTitle(R.string.app_name)
                    .setMessage(message).setPositiveButton(android.R.string.ok,(d,w)->result.confirm())
                    .setOnCancelListener(d->result.cancel()).show();
                return true;
            }
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try { startActivityForResult(params.createIntent(), PICK_FILE); }
                catch (ActivityNotFoundException e) { fileCallback.onReceiveValue(null); fileCallback = null; }
                return true;
            }
            @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (!trusted(Uri.parse(origin))) { callback.invoke(origin, false, false); return; }
                if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, false); return;
                }
                if (locationCallback != null) locationCallback.invoke(locationOrigin, false, false);
                locationCallback = callback;
                locationOrigin = origin;
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION);
            }
            @Override public void onGeolocationPermissionsHidePrompt() { clearLocationRequest(); }
        });
        if (state == null || web.restoreState(state) == null) web.loadUrl(HOME);
    }
    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density); }
    private boolean trusted(Uri uri) {
        return "https".equalsIgnoreCase(uri.getScheme()) &&
            "integrity-emergency-care-hazel.vercel.app".equalsIgnoreCase(uri.getHost()) &&
            (uri.getPort() == -1 || uri.getPort() == 443);
    }
    private void showError() { failed = true; progress.setVisibility(View.GONE); error.setVisibility(View.VISIBLE); }
    private void openExternal(Uri uri) {
        String scheme = uri.getScheme();
        if (scheme == null) return;
        Intent intent;
        if ("tel".equals(scheme)) intent = new Intent(Intent.ACTION_DIAL, uri);
        else if ("mailto".equals(scheme)) intent = new Intent(Intent.ACTION_SENDTO, uri);
        else if ("https".equals(scheme) || "geo".equals(scheme)) intent = new Intent(Intent.ACTION_VIEW, uri);
        else return;
        try { startActivity(intent); }
        catch (ActivityNotFoundException e) { Toast.makeText(this, R.string.no_handler, Toast.LENGTH_SHORT).show(); }
    }
    private void clearLocationRequest() {
        if (locationCallback != null) locationCallback.invoke(locationOrigin, false, false);
        locationCallback = null;
        locationOrigin = null;
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request == LOCATION && locationCallback != null) {
            boolean allowed = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            locationCallback.invoke(locationOrigin, allowed, false);
            locationCallback = null; locationOrigin = null;
        }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PICK_FILE && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result, data));
            fileCallback = null;
        }
    }
    private void installBackHandler() {
        web.evaluateJavascript("(function(){window.integrityAndroidBack=function(){" +
            "var s=document.querySelector('.screen-view.active-view');if(!s)return false;" +
            "var b=s.querySelector('[onclick^=goBack],.hosp-back-btn,.dash-back-btn,.emergency-back-btn,.patient-back-btn,.back-btn');" +
            "if(b && getComputedStyle(b).visibility!=='hidden'){b.click();return true;}" +
            "if(s.id!=='role-screen'&&s.id!=='splash-screen'&&typeof goToRoleScreen==='function'){goToRoleScreen();return true;}" +
            "return false;};})();", null);
    }
    @Override public void onBackPressed() {
        if (error.getVisibility() == View.VISIBLE) { super.onBackPressed(); return; }
        web.evaluateJavascript("typeof integrityAndroidBack==='function' && integrityAndroidBack()", result -> {
            if (!"true".equals(result)) { if(web.canGoBack()) web.goBack(); else finish(); }
        });
    }
    @Override protected void onSaveInstanceState(Bundle state) { super.onSaveInstanceState(state); web.saveState(state); }
    @Override protected void onPause() { web.onPause(); super.onPause(); }
    @Override protected void onResume() { super.onResume(); web.onResume(); }
    @Override protected void onDestroy() {
        clearLocationRequest();
        if (fileCallback != null) { fileCallback.onReceiveValue(null); fileCallback = null; }
        web.destroy();
        super.onDestroy();
    }
}
