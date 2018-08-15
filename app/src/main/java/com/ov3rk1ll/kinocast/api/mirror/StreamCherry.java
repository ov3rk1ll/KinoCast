package com.ov3rk1ll.kinocast.api.mirror;

import android.annotation.SuppressLint;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.ui.DetailActivity;
import com.ov3rk1ll.kinocast.ui.MainActivity;
import com.ov3rk1ll.kinocast.utils.Utils;

import org.jsoup.nodes.Document;

public class StreamCherry extends Host {
    private static final String TAG = StreamCherry.class.getSimpleName();
    public static final int HOST_ID = 82;


    @Override
    public int getId() {
        return HOST_ID;
    }

    @Override
    public String getName() {
        return "StreamCherry";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }


    public static String getMirrorLink(Document doc){
        try {

            String href = doc.select("iframe").attr("src");

            return href;
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return null;
    }

    @Override
    public String getVideoPath(DetailActivity.QueryPlayTask queryTask) {
        if(TextUtils.isEmpty(url)) return null;

        url = "https:" + url;
        queryTask.updateProgress(queryTask.getContext().getString(R.string.host_progress_getdatafrom, url));

        Log.d(TAG, "resolve " + url);
        queryTask.updateProgress(queryTask.getContext().getString(R.string.host_progress_getvideoforid,"0"));
        String link = getLink(url);

        Log.d(TAG, "Request. Got " + link);
        queryTask.updateProgress(queryTask.getContext().getString(R.string.host_progress_1sttry));

        return link;
    }

    private String getLink(final String url){

        final boolean[] requestDone = {false};
        final String[] solvedUrl = {null};

        MainActivity.activity.runOnUiThread(new Runnable() {
            @SuppressLint("SetJavaScriptEnabled")
            @Override
            public void run() {
                // Virtual WebView
                final WebView webView = MainActivity.webView;

                webView.setVisibility(View.GONE);
                webView.getSettings().setUserAgentString(Utils.USER_AGENT);
                webView.getSettings().setJavaScriptEnabled(true);
                webView.setWebViewClient(new WebViewClient() {

                    public void onPageFinished(WebView view, String url) {

                        webView.evaluateJavascript(
                                "(function() { return document.querySelector(\"video\").getAttribute(\"src\") })();",
                                new ValueCallback<String>() {
                                    @Override
                                    public void onReceiveValue(String html) {
                                        // code here
                                        html = "https:" + html.replaceAll("^\"|\"$", "");
                                        Log.d("HTML", html);
                                        solvedUrl[0] = html;
                                        requestDone[0] = true;
                                    }
                                });

                    }

                });
                webView.loadUrl(url);
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


        return solvedUrl[0];

    }
}
