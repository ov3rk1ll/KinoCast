package com.ov3rk1ll.kinocast.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.ov3rk1ll.kinocast.api.Parser;
import com.ov3rk1ll.kinocast.ui.MainActivity;

import java.io.IOException;

import okhttp3.Cookie;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

@SuppressWarnings("deprecation")
public class CloudflareDdosInterceptor implements Interceptor {
    private static final String TAG = CloudflareDdosInterceptor.class.getSimpleName();

    private Context context;

    public CloudflareDdosInterceptor(Context context) {
        this.context = context;
    }

    @Override
    public Response intercept(final Chain chain) throws IOException {
        final Request request = chain.request();
        final Response response = chain.proceed(request);
        if(response.code() == 503) {
            Log.d(TAG, "intercept: Status " + response.code() + " for " + request.url());
            Log.v(TAG, "intercept: try to handle request to " + request.url().toString());
            String body = response.body().string();
            final boolean[] requestDone = {false};
            final String[] solvedUrl = {null};
            if (body.contains("DDoS protection by Cloudflare") && !request.url().toString().contains("/cdn-cgi/l/chk_jschl")) {
                MainActivity.activity.runOnUiThread(new Runnable() {
                    @SuppressLint("SetJavaScriptEnabled")
                    @Override
                    public void run() {
                        // Virtual WebView
                        final WebView webView = MainActivity.webView;

                        // Delete all cookies
                        CookieSyncManager cookieSyncMngr = CookieSyncManager.createInstance(context);
                        cookieSyncMngr.startSync();
                        CookieManager cookieManager = CookieManager.getInstance();
                        cookieManager.removeAllCookie();
                        cookieManager.removeSessionCookie();
                        cookieSyncMngr.stopSync();
                        cookieSyncMngr.sync();
                        cookieManager.setCookie(request.url().toString(), response.header("Set-Cookie"));
                        cookieSyncMngr.sync();

                        webView.setVisibility(View.GONE);
                        webView.clearCache(true);
                        webView.getSettings().setUserAgentString(Utils.USER_AGENT);
                        webView.getSettings().setJavaScriptEnabled(true);
                        webView.setWebViewClient(new WebViewClient() {
                            @Override
                            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                                String raw = CookieManager.getInstance().getCookie(url);
                                Log.v("CloudflareDdos", "shouldOverrideUrlLoading: wants to load " + url + " with cookies " + raw);
                                solvedUrl[0] = url;
                                requestDone[0] = true;
                                return true;
                            }
                        });
                        webView.loadUrl(request.url().toString());
                        Log.v("CloudflareDdos", "load " + request.url().toString() + " in webview");

                    }
                });

                int timeout = 50;
                // Wait for the webView to load the correct url
                while (!requestDone[0]){
                    SystemClock.sleep(200);
                    timeout--;
                    if(timeout <= 0)
                        break;
                }
                if(solvedUrl[0] != null) {
                    // Store the cookies from the WebView in the OkHttpClient's jar
                    InjectedCookieJar jar = (InjectedCookieJar) Parser.getInstance().getClient().cookieJar();
                    String raw = CookieManager.getInstance().getCookie(solvedUrl[0]);
                    Log.v("CloudflareDdos", "load " + solvedUrl[0] + ", raw-cookies=" + raw);
                    String[] cookies = raw.split(";");
                    for (String c : cookies) {
                        jar.addCookie(Cookie.parse(request.url(), c.trim()));
                    }

                    // call the check url to get and store the correct cookies
                    Parser.getInstance().getBody(solvedUrl[0]);

                    // run the action request again
                    Log.v(TAG, "intercept: will retry request to " + request.url());
                    return chain.proceed(request);
                }
                return response;
            }
        }
        return response;
    }
}
