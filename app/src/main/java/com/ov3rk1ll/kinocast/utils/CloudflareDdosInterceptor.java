package com.ov3rk1ll.kinocast.utils;

import android.app.Activity;
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
import java.util.Set;

import okhttp3.Cookie;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * TODO: Add a class header comment!
 */

public class CloudflareDdosInterceptor implements Interceptor {
    private static final String TAG = CloudflareDdosInterceptor.class.getSimpleName();

    OkHttpClient client;
    Context context;
    Activity activity;

    public CloudflareDdosInterceptor(Context context) {
        this.context = context;
    }

    public void setClient(OkHttpClient client) {
        this.client = client;
    }

    @Override
    public Response intercept(final Chain chain) throws IOException {
        final Request request = chain.request();
        final Response response = chain.proceed(request);
        Log.i(TAG, "intercept: Status " + response.code() + " for " + request.url());
        if(response.code() == 503) {
            Log.i(TAG, "intercept: try to handle request to " + request.url().toString());
            String body = response.body().string();
            final boolean[] requestDone = {false};
            final String[] solvedUrl = {null};
            if (body.contains("DDoS protection by Cloudflare") && !request.url().toString().contains("/cdn-cgi/l/chk_jschl")) {
                MainActivity.activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Create Virtual WebView
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
                                Log.i("CloudflareDdos", "shouldOverrideUrlLoading: wants to load " + url + " with cookies " + raw);
                                solvedUrl[0] = url;
                                requestDone[0] = true;
                                return true;
                            }
                        });
                        webView.loadUrl(request.url().toString());
                        Log.i("CloudflareDdos", "load " + request.url().toString() + " in webview");

                    }
                });

                // Wait for the webView to load the correct url
                while (!requestDone[0]){
                    SystemClock.sleep(200);
                    // TODO Limit to 10s
                }
                if(solvedUrl[0] != null) {
                    // Store the cookies from the WebView in the OkHttpClient's jar
                    InjectedCookieJar jar = (InjectedCookieJar) Parser.getInstance().getClient().cookieJar();
                    String raw = CookieManager.getInstance().getCookie(solvedUrl[0]);
                    Log.i("CloudflareDdos", "load " + solvedUrl[0] + ", raw-cookies=" + raw);
                    String[] cookies = raw.split(";");
                    for (String c : cookies) {
                        jar.addCookie(Cookie.parse(request.url(), c.trim()));
                    }

                    // call the check url to get and store the correct cookies
                    Parser.getInstance().getBody(solvedUrl[0]);

                    // run the action request again
                    Log.i(TAG, "intercept: will retry request to " + request.url());
                    return chain.proceed(request);
                }
                return response;
            }
        }
        return response;
    }

    private String between(String s, String start, String end){
        int a = s.indexOf(start) + start.length();
        int b = s.indexOf(end, a);
        return s.substring(a, b);
    }

    private Set<Cookie> solveDdos(String url){
        // Load page
        // Eval javascript
        // Get cookies
        /*try {
            WebClient webClient = createWebClient();
            WebRequest wr = new WebRequest(new URL(url), HttpMethod.GET);
            Page page = webClient.getPage(wr);
            if (page instanceof HtmlPage)
                if (((HtmlPage) page).asXml().contains("DDoS protection by CloudFlare")) {
                    // DDOS protection
                    try {
                        Thread.sleep(9000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // restore
                        // interrupted
                        // status
                    }
                    if (webClient.getPage(wr) instanceof UnexpectedPage) {
                        UnexpectedPage unexpectedPage = webClient.getPage(wr);
                        System.out.println("UNEXPECTED PAGE: " + unexpectedPage.getWebResponse().getStatusMessage());
                        return null;
                    }
                }
            Set<Cookie> cookiesMap = webClient.getCookieManager().getCookies();
            webClient.close();
            return cookiesMap;
        } catch (Exception ex) {
            ex.printStackTrace();
        }*/
        return null;
    }
}
