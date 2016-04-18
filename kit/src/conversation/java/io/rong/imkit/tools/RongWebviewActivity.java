package io.rong.imkit.tools;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import io.rong.common.RongWebView;
import io.rong.imkit.R;

/**
 * Created by weiqinxiao on 15/4/18.
 */
public class RongWebviewActivity extends Activity {

    String mPrevUrl;
    RongWebView mWebView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rc_ac_webview);
        Intent intent = getIntent();
        String url = intent.getStringExtra("url");
        mWebView = (RongWebView)findViewById(R.id.rc_webview);
        mWebView.setVerticalScrollbarOverlay(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setSupportZoom(false);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.setWebViewClient(new RongWebviewClient());
        mPrevUrl = url;
        mWebView.loadUrl(url);
    }

    private class RongWebviewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if(mPrevUrl != null) {
                if(!mPrevUrl.equals(url)) {
                    mPrevUrl = url;
                    mWebView.loadUrl(url);
                    return true;
                } else {
                    return false;
                }
            } else {
                mPrevUrl = url;
                mWebView.loadUrl(url);
                return true;
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()){
            mWebView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
