package com.ov3rk1ll.kinocast.ui;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.media.MediaRouter;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;
import com.google.sample.castcompanionlibrary.cast.BaseCastManager;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.sample.castcompanionlibrary.cast.player.VideoCastControllerActivity;
import com.google.sample.castcompanionlibrary.utils.Utils;
import com.google.sample.castcompanionlibrary.widgets.MiniController;
import com.ov3rk1ll.kinocast.BuildConfig;
import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.api.Parser;
import com.ov3rk1ll.kinocast.api.mirror.Host;
import com.ov3rk1ll.kinocast.data.Season;
import com.ov3rk1ll.kinocast.data.ViewModel;
import com.ov3rk1ll.kinocast.ui.helper.PaletteManager;
import com.ov3rk1ll.kinocast.ui.helper.smartimageview.CoverImage;
import com.ov3rk1ll.kinocast.ui.helper.smartimageview.SmartImageTask;
import com.ov3rk1ll.kinocast.ui.helper.smartimageview.SmartImageView;
import com.ov3rk1ll.kinocast.ui.helper.widget.ObservableScrollView;
import com.ov3rk1ll.kinocast.ui.util.AlphaForegroundColorSpan;
import com.ov3rk1ll.kinocast.utils.BookmarkManager;
import com.ov3rk1ll.kinocast.utils.CastHelper;
import com.ov3rk1ll.kinocast.utils.ShowcaseHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DetailActivity extends ActionBarActivity {
    public static final String ARG_ITEM = "param_item";
    private ViewModel item;
    private ColorDrawable mActionBarBackgroundDrawable;

    private VideoCastManager mVideoCastManager;
    private MiniController mMini;

    private VideoCastConsumerImpl mCastConsumer;
    private MenuItem mediaRouteMenuItem;

    private boolean SHOW_ADS = true;

    private BookmarkManager bookmarkManager;

    private Drawable.Callback mDrawableCallback = new Drawable.Callback() {
        @Override
        public void invalidateDrawable(Drawable who) {
            getSupportActionBar().setBackgroundDrawable(who);
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {
        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {
        }
    };
    private int mRestoreSeasonIndex = -1;
    private int mRestoreEpisodeIndex = -1;

    private AlphaForegroundColorSpan mAlphaForegroundColorSpan;
    private SpannableString mSpannableString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        bookmarkManager = new BookmarkManager(getApplication());
        bookmarkManager.restore();

        if(BuildConfig.GMS_CHECK) BaseCastManager.checkGooglePlayServices(this);
        mVideoCastManager = CastHelper.getVideoCastManager(this);
        mMini = (MiniController) findViewById(R.id.miniController);
        mVideoCastManager.addMiniController(mMini);
        mVideoCastManager.reconnectSessionIfPossible(this, false, 5);

        item = (ViewModel) getIntent().getSerializableExtra(ARG_ITEM);

        if(item == null){
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        mActionBarBackgroundDrawable = new ColorDrawable(Color.BLACK);
        mActionBarBackgroundDrawable.setAlpha(0);


        ActionBar actionBar = getSupportActionBar();

        actionBar.setBackgroundDrawable(mActionBarBackgroundDrawable);
        actionBar.setDisplayShowHomeEnabled(false);

        actionBar.setDisplayHomeAsUpEnabled(true);
        mSpannableString = new SpannableString(item.getTitle());
        mAlphaForegroundColorSpan = new AlphaForegroundColorSpan(Color.WHITE);
        setTitleAlpha(0);

        AdView mAdView = (AdView)findViewById(R.id.adView);
        if(SHOW_ADS){
            mAdView.setVisibility(View.VISIBLE);
            findViewById(R.id.hr2).setVisibility(View.VISIBLE);
            AdRequest adRequest = new AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .addTestDevice("9E6DD569F43AF307B296CE50D7766370")
                    .build();
            mAdView.loadAd(adRequest);
        }else{
            mAdView.setVisibility(View.GONE);
            findViewById(R.id.hr2).setVisibility(View.GONE);
        }

        mCastConsumer = new VideoCastConsumerImpl() {
            @Override
            public void onFailed(int resourceId, int statusCode) {
            }

            @Override
            public void onConnectionSuspended(int cause) {
            }

            @Override
            public void onConnectivityRecovered() {
            }

            @Override
            public void onCastDeviceDetected(final MediaRouter.RouteInfo info) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mediaRouteMenuItem.isVisible()) {
                            ShowcaseHelper.showChromecastHelp(DetailActivity.this);
                        }
                    }
                }, 1000);
            }
        };

        int screenWidthPx = getResources().getDisplayMetrics().widthPixels;

        FrameLayout topContent = (FrameLayout)findViewById(R.id.top_content);
        ObservableScrollView scrollView = (ObservableScrollView) findViewById(R.id.scroll_view);
        scrollView.parallaxViewBy(topContent, 0.5f);

        ((TextView)findViewById(R.id.title)).setText(item.getTitle());
        ((TextView)findViewById(R.id.detail)).setText(item.getSummary());

        final SmartImageView headerImage = (SmartImageView) findViewById(R.id.image_header);
        headerImage.setVisibility(View.GONE);
        headerImage.setImageItem(item.getImageRequest(screenWidthPx, "backdrop"), R.drawable.ic_loading_placeholder, new SmartImageTask.OnCompleteListener() {
            @Override
            public void onComplete() { }

            @TargetApi(11)
            @Override
            public void onComplete(Bitmap bitmap) {
                headerImage.setVisibility(View.VISIBLE);
                findViewById(R.id.progressBar).setVisibility(View.GONE);
                final String key = item.getSlug();
                PaletteManager.getInstance().getPalette(key, bitmap, new PaletteManager.Callback() {
                    @Override
                    public void onPaletteReady(Palette palette) {
                        Palette.Swatch swatch = palette.getDarkVibrantSwatch();
                        if(swatch != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                                mActionBarBackgroundDrawable.setColor(swatch.getRgb());
                            } else {
                                mActionBarBackgroundDrawable = new ColorDrawable(swatch.getRgb());
                            }

                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                                mActionBarBackgroundDrawable.setCallback(mDrawableCallback);
                                mActionBarBackgroundDrawable.invalidateSelf();
                            }

                            mActionBarBackgroundDrawable.setAlpha(0);

                            findViewById(R.id.hr1).setBackgroundColor(swatch.getRgb());
                            findViewById(R.id.hr2).setBackgroundColor(swatch.getRgb());

                            int alpha = mAlphaForegroundColorSpan.getAlpha();
                            mAlphaForegroundColorSpan = new AlphaForegroundColorSpan(swatch.getTitleTextColor());
                            setTitleAlpha(alpha);
                        }
                    }
                });
            }
        });

        ((ImageView)findViewById(R.id.language)).setImageResource(item.getLanguageResId());

        ((ObservableScrollView) findViewById(R.id.scroll_view)).setOnScrollChangedListener(new ObservableScrollView.OnScrollChangedListener() {
            public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
                final int headerHeight = findViewById(R.id.image_header).getHeight() - getSupportActionBar().getHeight();
                final float ratio = (float) Math.min(Math.max(t, 0), headerHeight) / headerHeight;
                final int newAlpha = (int) (ratio * 255);
                mActionBarBackgroundDrawable.setAlpha(newAlpha);
                setTitleAlpha(newAlpha);
            }
        });

        ((Spinner) findViewById(R.id.spinnerSeason)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final Spinner spinnerEpisode = (Spinner) findViewById(R.id.spinnerEpisode);

                spinnerEpisode.setAdapter(
                        new ArrayAdapter<String>(DetailActivity.this, android.R.layout.simple_list_item_1, item.getSeasons()[position].episodes));

                if(mRestoreEpisodeIndex != -1){
                    spinnerEpisode.setSelection(mRestoreEpisodeIndex);
                    mRestoreEpisodeIndex = -1;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ((Spinner) findViewById(R.id.spinnerEpisode)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                (new QueryHosterTask()).execute((Void) null);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        findViewById(R.id.buttonPlay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!com.ov3rk1ll.kinocast.utils.Utils.isWifiConnected(DetailActivity.this)){
                    new AlertDialog.Builder(DetailActivity.this)
                            .setMessage(getString(R.string.player_warn_no_wifi))
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    (new QueryPlayTask(DetailActivity.this)).execute((Void) null);
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                } else {
                    (new QueryPlayTask(DetailActivity.this)).execute((Void) null);
                }
            }
        });

        (new QueryDetailTask()).execute((Void) null);
    }

    private void setTitleAlpha(int alpha) {
        mAlphaForegroundColorSpan.setAlpha(alpha);
        mSpannableString.setSpan(mAlphaForegroundColorSpan, 0, mSpannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        getSupportActionBar().setTitle(mSpannableString);
    }

    @Override
    protected void onDestroy() {
        mVideoCastManager.removeMiniController(mMini);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mVideoCastManager.addVideoCastConsumer(mCastConsumer);
        mVideoCastManager.incrementUiCounter();
        //TODO Check if we are playing the current item

        //if(mVideoCastManager.getRemoteMediaInformation())
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVideoCastManager.removeVideoCastConsumer(mCastConsumer);
        mVideoCastManager.decrementUiCounter();

        //Update Bookmark to keep series info
        if(item.getType() == ViewModel.Type.SERIES) {
            BookmarkManager.Bookmark b = new BookmarkManager.Bookmark(Parser.getInstance().getParserId(), Parser.getInstance().getPageLink(item));
            b.setSeason(((Spinner) findViewById(R.id.spinnerSeason)).getSelectedItemPosition());
            b.setEpisode(((Spinner) findViewById(R.id.spinnerEpisode)).getSelectedItemPosition());
            int idx = bookmarkManager.indexOf(b);
            if(idx == -1){
                bookmarkManager.add(b);
            } else {
                b.setInternal(bookmarkManager.get(idx).isInternal());
                bookmarkManager.set(idx, b);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.detail, menu);
        mediaRouteMenuItem = mVideoCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
        // Set visibility depending on detail data
        menu.findItem(R.id.action_imdb).setVisible(item.getImdbId() != null);

        BookmarkManager.Bookmark b = bookmarkManager.findItem(this.item);
        if(b != null && !b.isInternal()){
            menu.findItem(R.id.action_bookmark_on).setVisible(true);
            menu.findItem(R.id.action_bookmark_off).setVisible(false);
        }else{
            menu.findItem(R.id.action_bookmark_on).setVisible(false);
            menu.findItem(R.id.action_bookmark_off).setVisible(true);
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ShowcaseHelper.showBookmarkHelp(DetailActivity.this, new OnShowcaseEventListener() {
                        @Override
                        public void onShowcaseViewHide(ShowcaseView showcaseView) {

                        }

                        @Override
                        public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                            if (item.getType() == ViewModel.Type.SERIES) {
                                if (findViewById(R.id.layoutSeries).getVisibility() == View.VISIBLE) {
                                    doShowSeriesHelp();
                                }
                            }else{
                                ShowcaseHelper.showMirrorHelp(DetailActivity.this);
                            }
                        }

                        @Override
                        public void onShowcaseViewShow(ShowcaseView showcaseView) {

                        }
                    });
                }
            }, 200);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home){
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }else if (id == R.id.action_share){
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Parser.getInstance().getPageLink(this.item)));
            startActivity(intent);
            return true;
        }else if (id == R.id.action_imdb){
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.imdb.com/title/" + this.item.getImdbId()));
            startActivity(intent);
            return true;
        }else if (id == R.id.action_bookmark_on) {
            //Remove bookmark
            bookmarkManager.remove(new BookmarkManager.Bookmark(
                    Parser.getInstance().getParserId(),
                    Parser.getInstance().getPageLink(this.item))
            );
            //Show confirmation
            Toast.makeText(getApplication(), getString(R.string.detail_bookmark_on_confirm), Toast.LENGTH_SHORT).show();
            supportInvalidateOptionsMenu();
            return true;
        }else if (id == R.id.action_bookmark_off) {
            //Add bookmark
            bookmarkManager.addAsPublic(new BookmarkManager.Bookmark(
                            Parser.getInstance().getParserId(),
                            Parser.getInstance().getPageLink(this.item))
            );
            //Show confirmation
            Toast.makeText(getApplication(), getString(R.string.detail_bookmark_off_confirm), Toast.LENGTH_SHORT).show();
            supportInvalidateOptionsMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mVideoCastManager.onDispatchVolumeKeyEvent(event, VideoCastControllerActivity.DEFAULT_VOLUME_INCREMENT)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void setMirrorSpinner(Host mirrors[]){
        if(mirrors != null && mirrors.length > 0) {
            ((Spinner) findViewById(R.id.spinnerMirror)).setAdapter(
                    new ArrayAdapter<Host>(DetailActivity.this, android.R.layout.simple_list_item_1,
                            mirrors));
            findViewById(R.id.spinnerMirror).setEnabled(true);
            findViewById(R.id.buttonPlay).setEnabled(true);
        } else {
            ((Spinner) findViewById(R.id.spinnerMirror)).setAdapter(
                    new ArrayAdapter<String>(DetailActivity.this, android.R.layout.simple_list_item_1,
                            new String[]{ getString(R.string.no_host_found) }));
            findViewById(R.id.spinnerMirror).setEnabled(false);
            findViewById(R.id.buttonPlay).setEnabled(false);
        }
        findViewById(R.id.layoutMirror).setVisibility(View.VISIBLE);
    }

    public class QueryDetailTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            // Set loader for content
            findViewById(R.id.buttonPlay).setEnabled(false);
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            com.ov3rk1ll.kinocast.utils.Utils.trackPath(DetailActivity.this, "Stream/" + item.getSlug() + ".html");
            item = Parser.getInstance().loadDetail(item);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

            if(item.getType() == ViewModel.Type.SERIES) {
                if(item.getType() == ViewModel.Type.SERIES) {
                    BookmarkManager.Bookmark b = bookmarkManager.findItem(item);
                    if (b != null) {
                        mRestoreSeasonIndex = b.getSeason();
                        mRestoreEpisodeIndex = b.getEpisode();
                    }
                }

                String seasons[] = new String[item.getSeasons().length];
                for (int i = 0; i < seasons.length; i++) {
                    seasons[i] = String.valueOf(item.getSeasons()[i].id);
                }
                ((Spinner) findViewById(R.id.spinnerSeason)).setAdapter(
                        new ArrayAdapter<String>(DetailActivity.this, android.R.layout.simple_list_item_1, seasons));

                if(mRestoreSeasonIndex != -1){
                    ((Spinner) findViewById(R.id.spinnerSeason)).setSelection(mRestoreSeasonIndex);
                    mRestoreSeasonIndex = -1;
                }
                doShowSeriesHelp();
                findViewById(R.id.layoutSeries).setVisibility(View.VISIBLE);
            }else{
                findViewById(R.id.layoutSeries).setVisibility(View.GONE);
                setMirrorSpinner(item.getMirrors());
            }
            ActivityCompat.invalidateOptionsMenu(DetailActivity.this);
        }
    }

    public void doShowSeriesHelp(){
        ShowcaseHelper.showSeriesHelp(DetailActivity.this, new OnShowcaseEventListener() {
            @Override
            public void onShowcaseViewHide(ShowcaseView showcaseView) {

            }

            @Override
            public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                if(findViewById(R.id.layoutMirror).getVisibility() == View.VISIBLE){
                    ShowcaseHelper.showMirrorHelp(DetailActivity.this);
                }
            }

            @Override
            public void onShowcaseViewShow(ShowcaseView showcaseView) {

            }
        });
    }

    public class QueryHosterTask extends AsyncTask<Void, Void, List<Host>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            findViewById(R.id.layoutMirror).setVisibility(View.GONE);
        }

        @Override
        protected List<Host> doInBackground(Void... params) {
            if(item.getType() == ViewModel.Type.SERIES) {
                Season s = item.getSeasons()[((Spinner) findViewById(R.id.spinnerSeason)).getSelectedItemPosition()];
                int position = ((Spinner) findViewById(R.id.spinnerEpisode)).getSelectedItemPosition();
                return Parser.getInstance().getHosterList(item, s.id, s.episodes[position]);
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Host> list) {
            super.onPostExecute(list);
            setMirrorSpinner(list.toArray(new Host[list.size()]));
        }
    }

    public class QueryPlayTask extends AsyncTask<Void, Void, String> {
        private ProgressDialog progressDialog;

        public QueryPlayTask(Context context){
            progressDialog = new ProgressDialog(context);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage(getString(R.string.loading));
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    cancel(true);
                }
            });
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    cancel(true);
                }
            });
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.show();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            progressDialog.dismiss();
        }

        @Override
        protected String doInBackground(Void... params) {
            String link;
            ArrayAdapter<Host> hosts = (ArrayAdapter<Host>) ((Spinner) findViewById(R.id.spinnerMirror)).getAdapter();
            Host host = hosts.getItem(((Spinner) findViewById(R.id.spinnerMirror)).getSelectedItemPosition());
            com.ov3rk1ll.kinocast.utils.Utils.trackPath(DetailActivity.this, "Stream/" + item.getSlug() + ".html?host=" + host.toString());

            if (item.getType() == ViewModel.Type.SERIES) {
                Season s = item.getSeasons()[((Spinner) findViewById(R.id.spinnerSeason)).getSelectedItemPosition()];
                String e = s.episodes[((Spinner) findViewById(R.id.spinnerEpisode)).getSelectedItemPosition()];
                link = Parser.getInstance().getMirrorLink(item, host.getId(), host.getMirror(), s.id, e);
            } else {
                link = Parser.getInstance().getMirrorLink(item, host.getId(), host.getMirror());
            }

            host.setUrl(link);
            return host.getVideoPath();
        }

        @Override
        protected void onPostExecute(final String link) {
            super.onPostExecute(link);
            progressDialog.dismiss();
            if (link != null) {
                Log.i("Play", "Getting player for '" + link + "'");
                final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                intent.setDataAndType(Uri.parse(link), "video/mp4");

                AlertDialog.Builder builder = new AlertDialog.Builder(DetailActivity.this);
                builder.setTitle(getString(R.string.player_select_dialog_title));

                PackageManager pm = getPackageManager();
                List<ResolveInfo> launchables = pm.queryIntentActivities(intent, 0);
                List<AppAdapter.App> apps = new ArrayList<AppAdapter.App>();
                Collections.sort(launchables, new ResolveInfo.DisplayNameComparator(pm));
                if (mVideoCastManager.isConnected()) {
                    apps.add(new AppAdapter.App(
                            getString(R.string.player_chromecast_list_entry),
                            getResources().getDrawable(R.drawable.ic_player_chromecast),
                            null
                    ));
                }
                apps.add(new AppAdapter.App(
                        getString(R.string.player_internal_list_entry),
                        getResources().getDrawable(R.drawable.ic_player),
                        new ComponentName(DetailActivity.this, PlayerActivity.class)
                ));
                for (ResolveInfo resolveInfo : launchables) {
                    ActivityInfo activity = resolveInfo.activityInfo;
                    AppAdapter.App app = new AppAdapter.App(
                            resolveInfo.loadLabel(pm),
                            resolveInfo.loadIcon(pm),
                            new ComponentName(activity.applicationInfo.packageName, activity.name)
                    );
                    apps.add(app);
                }

                final AppAdapter adapter = new AppAdapter(DetailActivity.this, apps, pm);
                builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int position) {
                        AppAdapter.App app = adapter.getItem(position);
                        if (app.getComponent() == null) {
                            startPlaybackOnChromecast(link);
                        } else {
                            intent.setComponent(app.getComponent());
                            startActivity(intent);
                        }
                        dialog.dismiss();
                    }
                });
                final AlertDialog dialog = builder.create();
                //dialog.getListView().setDivider(getResources().getDrawable(R.drawable.abc_list_divider_holo_light));
                dialog.getListView().setDividerHeight(1);
                dialog.show();

            } else { // no link found
                Toast.makeText(DetailActivity.this, getString(R.string.host_resolve_error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void startPlaybackOnChromecast(String link){
        MediaMetadata mediaMetadata;
        if(item.getType() == ViewModel.Type.SERIES){
            Season s = item.getSeasons()[((Spinner) findViewById(R.id.spinnerSeason)).getSelectedItemPosition()];
            String e = s.episodes[((Spinner) findViewById(R.id.spinnerEpisode)).getSelectedItemPosition()];
            mediaMetadata= new MediaMetadata(MediaMetadata.MEDIA_TYPE_TV_SHOW);
            mediaMetadata.putString(MediaMetadata.KEY_TITLE, item.getTitle() + " - " + s + "x" + e);
        }else{
            mediaMetadata= new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
            mediaMetadata.putString(MediaMetadata.KEY_TITLE, item.getTitle());
        }
        mediaMetadata.putString(MediaMetadata.KEY_SUBTITLE, getString(R.string.chromecast_subtitle));
        mediaMetadata.addImage(new WebImage(Uri.parse(
                new CoverImage(item.getImageRequest(96, "poster")).getBitmapUrl(getApplication()))
        ));
        mediaMetadata.addImage(new WebImage(Uri.parse(
                new CoverImage(item.getImageRequest(getResources().getDisplayMetrics().widthPixels, "poster")).getBitmapUrl(getApplication()))
        ));
        MediaInfo mediaInfo = new MediaInfo.Builder(link)
                .setContentType("video/mp4")
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(mediaMetadata)
                .build();

        mVideoCastManager.startCastControllerActivity(DetailActivity.this, Utils.fromMediaInfo(mediaInfo), 0, true);
    }

    /*public void startCastControllerActivity(Context context, Bundle mediaWrapper, int position, boolean shouldStart) {
        Intent intent = new Intent(context, ColorfulVideoCastControllerActivity.class);
        intent.putExtra(VideoCastManager.EXTRA_MEDIA, mediaWrapper);
        intent.putExtra(VideoCastManager.EXTRA_START_POINT, position);
        intent.putExtra(VideoCastManager.EXTRA_SHOULD_START, shouldStart);
        intent.putExtra(ColorfulVideoCastControllerActivity.EXTRA_AB_COLOR, mActionBarBackgroundDrawable.getColor());
        intent.putExtra(ColorfulVideoCastControllerActivity.EXTRA_TEXT_COLOR, mTitleColor);
        context.startActivity(intent);
    }*/

    public static class AppAdapter extends ArrayAdapter<AppAdapter.App> {
        PackageManager pm;

        public AppAdapter(Context context, List<AppAdapter.App> objects, PackageManager pm) {
            super(context, R.layout.player_list_item, android.R.id.text1, objects);
            this.pm = pm;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            App item = getItem(position);
            View view = super.getView(position, convertView, parent);
            ((TextView) view.findViewById(android.R.id.text1)).setText(item.getLabel());
            ((ImageView) view.findViewById(android.R.id.icon)).setImageDrawable(item.getIcon());
            return view;
        }

        public static class App {
            private CharSequence label;
            private Drawable icon;
            private ComponentName component;

            public App(CharSequence label, Drawable icon, ComponentName component) {
                this.label = label;
                this.icon = icon;
                this.component = component;
            }

            public CharSequence getLabel() {
                return label;
            }

            public void setLabel(CharSequence label) {
                this.label = label;
            }

            public Drawable getIcon() {
                return icon;
            }

            public void setIcon(Drawable icon) {
                this.icon = icon;
            }

            public ComponentName getComponent() {
                return component;
            }

            public void setComponent(ComponentName component) {
                this.component = component;
            }
        }
    }
}
