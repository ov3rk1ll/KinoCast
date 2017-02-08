package com.winsontan520.wversionmanager.library;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.ov3rk1ll.kinocast.BuildConfig;
import com.ov3rk1ll.kinocast.R;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WVersionManager {
    private static final String TAG = "WVersionManager";

    private static final int MODE_CHECK_VERSION = 100;
    private static final int MODE_ASK_FOR_RATE = 200;


    private CustomTagHandler customTagHandler;

    private String PREF_IGNORE_VERSION_CODE = "w.ignore.version.code";
    private String PREF_REMINDER_TIME = "w.reminder.time";

    private Activity activity;
    private Drawable icon;
    private String title;
    private String message;
    private String updateNowLabel;
    private String remindMeLaterLabel;
    private String ignoreThisVersionLabel;
    private String updateUrl;
    private String versionContentUrl;
    private int reminderTimer;
    private int versionCode;
    private AlertDialogButtonListener listener;
    private boolean mDialogCancelable = true;
    private boolean mIsAskForRate = false;
    private String mAskForRatePositiveLabel;
    private String mAskForRateNegativeLabel;
    private int mMode = 100; // default mode

    ProgressDialog mProgressDialog;
    private boolean mToastIfUpToDate = false;

    public WVersionManager(Activity act){
        this.activity = act;
        this.listener = new AlertDialogButtonListener();
        this.customTagHandler = new CustomTagHandler();
    }

    private Drawable getDefaultAppIcon() {
        Drawable d = activity.getApplicationInfo().loadIcon(activity.getPackageManager());
        return d;
    }

    public void setShowToastIfUpToDate(boolean toastIfUpToDate){
        this.mToastIfUpToDate = toastIfUpToDate;
    }

    public void checkVersion() {
        checkVersion(false);
    }

    public void checkVersion(boolean forceCheck) {
        mMode = MODE_CHECK_VERSION;
        String versionContentUrl = getVersionContentUrl();
        if(versionContentUrl == null){
            Log.e(TAG, "Please set versionContentUrl first");
            return;
        }

        Calendar c = Calendar.getInstance();
        long currentTimeStamp = c.getTimeInMillis();
        long reminderTimeStamp = getReminderTime();
        if(BuildConfig.DEBUG){
            Log.v(TAG, "currentTimeStamp="+currentTimeStamp);
            Log.v(TAG, "reminderTimeStamp="+reminderTimeStamp);
        }

        if(currentTimeStamp > reminderTimeStamp || forceCheck){
            // fire request to get update version content
            if(BuildConfig.DEBUG){
                Log.v(TAG, "getting update content...");
            }
            VersionContentRequest request = new VersionContentRequest(activity, mToastIfUpToDate);
            request.execute(getVersionContentUrl());
        }
    }

    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setIcon(getIcon());
        builder.setTitle(getTitle());
        builder.setMessage(getMessage());

        switch (mMode) {
            case MODE_CHECK_VERSION:
                builder.setPositiveButton(getUpdateNowLabel(), listener);
                builder.setNeutralButton(getRemindMeLaterLabel(), listener);
                //builder.setNegativeButton(getIgnoreThisVersionLabel(), listener);
                break;
            case MODE_ASK_FOR_RATE:
                builder.setPositiveButton(getAskForRatePositiveLabel(), listener);
                builder.setNegativeButton(getAskForRateNegativeLabel(), listener);
                break;
            default:
                return;
        }

        builder.setCancelable(isDialogCancelable());


        AlertDialog dialog = builder.create();
        if(activity != null && !activity.isFinishing()){
            dialog.show();
        }
    }

    public String getUpdateNowLabel() {
        return updateNowLabel != null? updateNowLabel : "Update now";
    }

    public void setUpdateNowLabel(String updateNowLabel) {
        this.updateNowLabel = updateNowLabel;
    }

    public String getRemindMeLaterLabel() {
        return remindMeLaterLabel != null? remindMeLaterLabel : "Maybe later";
    }

    public void setRemindMeLaterLabel(String remindMeLaterLabel) {
        this.remindMeLaterLabel = remindMeLaterLabel;
    }

    public String getIgnoreThisVersionLabel() {
        return ignoreThisVersionLabel != null? ignoreThisVersionLabel : "Ignore this version";
    }

    public void setIgnoreThisVersionLabel(String ignoreThisVersionLabel) {
        this.ignoreThisVersionLabel = ignoreThisVersionLabel;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        String defaultMessage = null;
        switch(mMode){
            case MODE_CHECK_VERSION:
                defaultMessage = "What's new in this version";
                break;
            case MODE_ASK_FOR_RATE:
                defaultMessage = "Please rate us!";
                break;
        }

        return message != null? message : defaultMessage;
    }

    public String getTitle() {
        String defaultTitle = null;
        switch(mMode){
            case MODE_CHECK_VERSION:
                defaultTitle = "New Update Available";
                break;
            case MODE_ASK_FOR_RATE:
                defaultTitle = "Rate this app";
                break;
        }
        return title != null? title : defaultTitle;
    }

    public Drawable getIcon() {
        return icon != null?  icon : getDefaultAppIcon();
    }

    public String getUpdateUrl() {
        return updateUrl != null ? updateUrl : getGooglePlayStoreUrl();
    }

    public void setUpdateUrl(String updateUrl) {
        this.updateUrl = updateUrl;
    }

    public String getVersionContentUrl() {
        return versionContentUrl;
    }

    public void setVersionContentUrl(String versionContentUrl) {
        this.versionContentUrl = versionContentUrl;
    }

    public int getReminderTimer() {
        return reminderTimer > 0 ? reminderTimer : (1 * 60); // default value 60 minutes
    }

    public void setReminderTimer(int minutes){
        if(minutes > 0){
            reminderTimer = minutes;
        }
    }

    //TODO Download Dialog and open when ready
    private void updateNow(String url) {
        if(url != null){
            try{
                mProgressDialog = new ProgressDialog(this.activity);
                mProgressDialog.setMessage("Download Update");
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.setCancelable(true);

                // execute this when the downloader must be fired
                final DownloadTask downloadTask = new DownloadTask(this.activity);
                downloadTask.execute(url);

                mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        downloadTask.cancel(true);
                    }
                });
                /*Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                activity.startActivity(intent);*/
            }catch(Exception e){
                Log.e(TAG, "is update url correct?" + e );
            }
        }

    }


    private void remindMeLater(int reminderTimer) {
        Calendar c = Calendar.getInstance();
        long currentTimeStamp = c.getTimeInMillis();

        c.add(Calendar.MINUTE, reminderTimer);
        long reminderTimeStamp = c.getTimeInMillis();

        if(BuildConfig.DEBUG){
            Log.v(TAG, "currentTimeStamp="+currentTimeStamp);
            Log.v(TAG, "reminderTimeStamp="+reminderTimeStamp);
        }

        setReminderTime(reminderTimeStamp);
    }

    private void setReminderTime(long reminderTimeStamp) {
        PreferenceManager.getDefaultSharedPreferences(activity).edit().putLong(PREF_REMINDER_TIME, reminderTimeStamp).commit();
    }

    private long getReminderTime(){
        return PreferenceManager.getDefaultSharedPreferences(activity).getLong(PREF_REMINDER_TIME, 0);
    }

    private void ignoreThisVersion() {
        PreferenceManager.getDefaultSharedPreferences(activity).edit().putInt(PREF_IGNORE_VERSION_CODE, versionCode).commit();
    }

    private String getGooglePlayStoreUrl(){
        String id = activity.getApplicationInfo().packageName; // current google play is using package name as id
        return "market://details?id=" + id;
    }

    private class AlertDialogButtonListener implements DialogInterface.OnClickListener{

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case AlertDialog.BUTTON_POSITIVE:
                    updateNow(getUpdateUrl());
                    break;
                case AlertDialog.BUTTON_NEUTRAL:
                    remindMeLater(getReminderTimer());
                    break;
                case AlertDialog.BUTTON_NEGATIVE:
                    ignoreThisVersion();
                    break;
            }
        }
    }

    class VersionInformation{
        private int versionCode;
        private String name;
        private String body;
        private String downloadUrl;

        public VersionInformation(int versionCode, String name, String body, String downloadUrl) {
            this.versionCode = versionCode;
            this.name = name;
            this.body = body;
            this.downloadUrl = downloadUrl;
        }
    }

    class VersionContentRequest extends AsyncTask<String, Void, VersionInformation>{
        Context context;
        int statusCode;
        boolean toastIfUpToDate;

        public VersionContentRequest(Context context, boolean toastIfUpToDate) {
            this.context = context;
            this.toastIfUpToDate = toastIfUpToDate;
        }

        @Override
        protected VersionInformation doInBackground(String... uri) {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(uri[0]).build();
            Log.d(TAG, "GET " + uri[0]);

            try {
                Response response = client.newCall(request).execute();
                JSONObject json = new JSONObject(response.body().string());
                int currentVersionCode = getCurrentVersionCode();
                int versionCode = json.optInt("version_code", 0);
                if(currentVersionCode < versionCode){
                    request = new Request.Builder().url(json.getString("gitlab")).build();
                    response = client.newCall(request).execute();
                    JSONObject gitlab = new JSONObject(response.body().string());
                    return new VersionInformation(versionCode,
                            gitlab.getString("name"),
                            gitlab.getString("body"),
                            gitlab.getJSONArray("assets").getJSONObject(0).getString("browser_download_url")
                    );
                }
            }catch (Exception e) {
                Log.e(TAG, e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(VersionInformation result) {
            if(result == null){
                if(toastIfUpToDate){
                    Toast.makeText(activity, R.string.update_uptodate, Toast.LENGTH_SHORT).show();
                }
                Log.e(TAG, "Response invalid");
            }else{
                setUpdateUrl(result.downloadUrl);
                setMessage(result.body);
                setTitle("Update " + result.name);
                showDialog();
            }
        }
    }

    class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;
        private File localFile;

        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                context.getExternalFilesDir(null).mkdirs();
                String file = url.getFile();
                localFile = new File(context.getExternalFilesDir(null), file.substring(file.lastIndexOf("/") + 1));
                if(localFile.exists()) localFile.delete();
                input = connection.getInputStream();
                output = new FileOutputStream(localFile);

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            mProgressDialog.dismiss();
            if (result != null)
                Toast.makeText(context, "Download error: " + result, Toast.LENGTH_LONG).show();
            else
                Toast.makeText(context, "File downloaded", Toast.LENGTH_SHORT).show();

            //Open file
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(localFile), "application/vnd.android.package-archive");
            context.startActivity(intent);
        }
    }

    public int getCurrentVersionCode() {
        return BuildConfig.VERSION_CODE;
    }

    public int getIgnoreVersionCode() {
        return PreferenceManager.getDefaultSharedPreferences(activity).getInt(PREF_IGNORE_VERSION_CODE, 1);
    }

    public CustomTagHandler getCustomTagHandler() {
        return customTagHandler;
    }

    public void setCustomTagHandler(CustomTagHandler customTagHandler) {
        this.customTagHandler = customTagHandler;
    }

    public boolean isDialogCancelable() {
        return mDialogCancelable;
    }

    public void setDialogCancelable(boolean dialogCancelable) {
        mDialogCancelable = dialogCancelable;
    }

    public void askForRate(){
        mMode = MODE_ASK_FOR_RATE;
        showDialog();
    }

    public String getAskForRatePositiveLabel() {
        return mAskForRatePositiveLabel == null? "OK":mAskForRatePositiveLabel;
    }

    public void setAskForRatePositiveLabel(String askForRatePositiveLabel) {
        mAskForRatePositiveLabel = askForRatePositiveLabel;
    }

    public String getAskForRateNegativeLabel() {
        return mAskForRateNegativeLabel == null? "Not now":mAskForRateNegativeLabel;
    }

    public void setAskForRateNegativeLabel(String askForRateNegativeLabel) {
        mAskForRateNegativeLabel = askForRateNegativeLabel;
    }

}