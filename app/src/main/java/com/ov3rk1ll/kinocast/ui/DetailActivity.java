package com.ov3rk1ll.kinocast.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;
import com.google.android.libraries.cast.companionlibrary.cast.BaseCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.ov3rk1ll.kinocast.BuildConfig;
import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.api.Parser;
import com.ov3rk1ll.kinocast.api.mirror.Host;
import com.ov3rk1ll.kinocast.data.Season;
import com.ov3rk1ll.kinocast.data.ViewModel;
import com.ov3rk1ll.kinocast.ui.helper.PaletteManager;
import com.ov3rk1ll.kinocast.ui.util.glide.OkHttpViewModelStreamFetcher;
import com.ov3rk1ll.kinocast.ui.util.glide.ViewModelGlideRequest;
import com.ov3rk1ll.kinocast.utils.BookmarkManager;
import com.ov3rk1ll.kinocast.utils.TheMovieDb;
import com.ov3rk1ll.kinocast.utils.Utils;
import com.ov3rk1ll.kinocast.utils.WeightedHostComparator;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("ConstantConditions")
public class DetailActivity extends AppCompatActivity implements ActionMenuView.OnMenuItemClickListener {
    public static final String ARG_ITEM = "param_item";
    private ViewModel item;
    private RelativeLayout  mAdView;

    private VideoCastManager mVideoCastManager;

    @SuppressWarnings("FieldCanBeLocal")
    private boolean SHOW_ADS = true;

    private BookmarkManager bookmarkManager;
    private CollapsingToolbarLayout collapsingToolbarLayout;

    private int mRestoreSeasonIndex = -1;
    private int mRestoreEpisodeIndex = -1;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        supportFinishAfterTransition();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.GMS_CHECK) BaseCastManager.checkGooglePlayServices(this);
        mVideoCastManager = Utils.initializeCastManager(this);

        setContentView(R.layout.activity_detail);

        mVideoCastManager.reconnectSessionIfPossible();

        // actionBar.setDisplayHomeAsUpEnabled(true);

        //((ActionMenuView) findViewById(R.id.bar_split)).setOnMenuItemClickListener(this);

        bookmarkManager = new BookmarkManager(getApplication());
        bookmarkManager.restore();

        item = (ViewModel) getIntent().getSerializableExtra(ARG_ITEM);

        if (item == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        initToolbar();
        initInstances();
        attemptColor(null);

        findViewById(R.id.button_donate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.paypal_donate)));
                startActivity(intent);
            }
        });

        mAdView = (RelativeLayout)findViewById(R.id.adView);
        if (SHOW_ADS) {
            findViewById(R.id.donateView).setVisibility(View.GONE);
            String mAdSpaceName = "Detail Banner";


            //mAdView.setInventoryHash(getString(R.string.mobfox_hash));
            //mAdView.load();
        } else {
            mAdView.setVisibility(View.GONE);
            findViewById(R.id.donateView).setVisibility(View.GONE);
            findViewById(R.id.hr2).setVisibility(View.GONE);
        }

        int screenWidthPx = getResources().getDisplayMetrics().widthPixels;

        ((TextView) findViewById(R.id.detail)).setText(item.getSummary());

        final ImageView headerImage = (ImageView) findViewById(R.id.image_header);
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        findViewById(R.id.progressBar).invalidate();
        Glide.with(this)
                .load(new ViewModelGlideRequest(item, screenWidthPx, "backdrop"))
                .placeholder(R.drawable.ic_loading_placeholder)
                .listener(new RequestListener<ViewModelGlideRequest, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, ViewModelGlideRequest model, Target<GlideDrawable> target, boolean isFirstResource) {
                        e.printStackTrace();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, ViewModelGlideRequest model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        findViewById(R.id.progressBar).clearAnimation();
                        findViewById(R.id.progressBar).setVisibility(View.GONE);
                        findViewById(R.id.progressBar).invalidate();
                        findViewById(R.id.top_content).invalidate();
                        attemptColor(((GlideBitmapDrawable)resource.getCurrent()).getBitmap());
                        return false;
                    }
                })
                .into(headerImage);
        headerImage.setVisibility(View.VISIBLE);

        ((ImageView) findViewById(R.id.language)).setImageResource(item.getLanguageResId());

        ((Spinner) findViewById(R.id.spinnerSeason)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final Spinner spinnerEpisode = (Spinner) findViewById(R.id.spinnerEpisode);

                spinnerEpisode.setAdapter(
                        new ArrayAdapter<>(DetailActivity.this, android.R.layout.simple_list_item_1, item.getSeasons()[position].episodes));

                if (mRestoreEpisodeIndex != -1) {
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
                if (!com.ov3rk1ll.kinocast.utils.Utils.isWifiConnected(DetailActivity.this)) {
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

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void initInstances() {
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //getSupportActionBar().setTitle(item.getTitle());

        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsingToolbarLayout);
        collapsingToolbarLayout.setTitle(item.getTitle());
    }

    boolean hasColors = false;

    private void attemptColor(Bitmap bitmap) {
        if (hasColors) return;
        PaletteManager.getInstance().getPalette(item.getSlug(), bitmap, new PaletteManager.Callback() {
            @Override
            public void onPaletteReady(Palette palette) {
                if (palette == null) return;
                Palette.Swatch swatch = palette.getDarkVibrantSwatch();
                if (swatch != null) {
                    collapsingToolbarLayout.setContentScrimColor(swatch.getRgb());

                    findViewById(R.id.hr1).setBackgroundColor(swatch.getRgb());
                    findViewById(R.id.hr2).setBackgroundColor(swatch.getRgb());

                    //collapsingToolbarLayout.setCollapsedTitleTextColor(swatch.getTitleTextColor());
                    //collapsingToolbarLayout.setExpandedTitleColor(swatch.getTitleTextColor());

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Log.i("progressBar", "set color to " + swatch.getRgb());
                        ((ProgressBar) findViewById(R.id.progressBar)).setIndeterminateTintList(ColorStateList.valueOf(swatch.getRgb()));
                        float hsv[] = new float[3];
                        Color.colorToHSV(swatch.getRgb(), hsv);
                        hsv[2] = 0.2f;
                        getWindow().setStatusBarColor(Color.HSVToColor(hsv));
                        Log.i("progressBar", "Visibility in color = " + findViewById(R.id.progressBar).getVisibility());
                    }

                    hasColors = true;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mVideoCastManager.incrementUiCounter();
        //TODO Check if we are playing the current item
        //if(mAdView != null) mAdView.onResume();

        //if(mVideoCastManager.getRemoteMediaInformation())
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVideoCastManager.decrementUiCounter();

        //Update Bookmark to keep series info
        if (item.getType() == ViewModel.Type.SERIES) {
            BookmarkManager.Bookmark b = new BookmarkManager.Bookmark(Parser.getInstance().getParserId(), Parser.getInstance().getPageLink(item));
            b.setSeason(((Spinner) findViewById(R.id.spinnerSeason)).getSelectedItemPosition());
            b.setEpisode(((Spinner) findViewById(R.id.spinnerEpisode)).getSelectedItemPosition());
            int idx = bookmarkManager.indexOf(b);
            if (idx == -1) {
                bookmarkManager.add(b);
            } else {
                b.setInternal(bookmarkManager.get(idx).isInternal());
                bookmarkManager.set(idx, b);
            }
        }
        //if(mAdView != null) mAdView.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.detail, menu);
        //menu = ((ActionMenuView) findViewById(R.id.bar_split)).getMenu();
        menu.clear();
        getMenuInflater().inflate(R.menu.detail, menu);
        mVideoCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
        // Set visibility depending on detail data
        menu.findItem(R.id.action_imdb).setVisible(item.getImdbId() != null);

        BookmarkManager.Bookmark b = bookmarkManager.findItem(this.item);
        if (b != null && !b.isInternal()) {
            menu.findItem(R.id.action_bookmark_on).setVisible(true);
            menu.findItem(R.id.action_bookmark_off).setVisible(false);
        } else {
            menu.findItem(R.id.action_bookmark_on).setVisible(false);
            menu.findItem(R.id.action_bookmark_off).setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            supportFinishAfterTransition();
            //NavUtils.navigateUpFromSameTask(this);
            return true;
        } else if (id == R.id.action_share) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Parser.getInstance().getPageLink(this.item)));
            startActivity(intent);
            return true;
        } else if (id == R.id.action_imdb) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.imdb.com/title/" + this.item.getImdbId()));
            startActivity(intent);
            return true;
        } else if (id == R.id.action_bookmark_on) {
            //Remove bookmark
            bookmarkManager.remove(new BookmarkManager.Bookmark(
                            Parser.getInstance().getParserId(),
                            Parser.getInstance().getPageLink(this.item))
            );
            //Show confirmation
            Toast.makeText(getApplication(), getString(R.string.detail_bookmark_on_confirm), Toast.LENGTH_SHORT).show();
            supportInvalidateOptionsMenu();
            return true;
        } else if (id == R.id.action_bookmark_off) {
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
        return mVideoCastManager.onDispatchVolumeKeyEvent(event, 0.05) || super.dispatchKeyEvent(event);
    }

    private void setMirrorSpinner(Host mirrors[]) {
        if (mirrors != null && mirrors.length > 0) {
            Arrays.sort(mirrors, new WeightedHostComparator(Utils.getWeightedHostList(getApplicationContext())));
            ((Spinner) findViewById(R.id.spinnerMirror)).setAdapter(
                    new ArrayAdapter<>(DetailActivity.this, android.R.layout.simple_list_item_1,
                            mirrors));
            findViewById(R.id.spinnerMirror).setEnabled(true);
            findViewById(R.id.buttonPlay).setEnabled(true);
        } else {
            ((Spinner) findViewById(R.id.spinnerMirror)).setAdapter(
                    new ArrayAdapter<>(DetailActivity.this, android.R.layout.simple_list_item_1,
                            new String[]{getString(R.string.no_host_found)}));
            findViewById(R.id.spinnerMirror).setEnabled(false);
            findViewById(R.id.buttonPlay).setEnabled(false);
        }
        findViewById(R.id.layoutMirror).setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        return this.onOptionsItemSelected(menuItem);
    }

    private class QueryDetailTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            // Set loader for content
            findViewById(R.id.buttonPlay).setEnabled(false);
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            Map<String, String> articleParams = new HashMap<>();
            articleParams.put("Name", item.getTitle());
            articleParams.put("Type", item.getType() == ViewModel.Type.MOVIE ? "Movie" : "Series");
            articleParams.put("Id", item.getSlug());
            item = Parser.getInstance().loadDetail(item);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

            if (item.getType() == ViewModel.Type.SERIES) {
                BookmarkManager.Bookmark b = bookmarkManager.findItem(item);
                if (b != null) {
                    mRestoreSeasonIndex = b.getSeason();
                    mRestoreEpisodeIndex = b.getEpisode();
                }

                String seasons[] = new String[item.getSeasons().length];
                for (int i = 0; i < seasons.length; i++) {
                    seasons[i] = String.valueOf(item.getSeasons()[i].id);
                }
                ((Spinner) findViewById(R.id.spinnerSeason)).setAdapter(
                        new ArrayAdapter<>(DetailActivity.this, android.R.layout.simple_list_item_1, seasons));

                if (mRestoreSeasonIndex != -1) {
                    ((Spinner) findViewById(R.id.spinnerSeason)).setSelection(mRestoreSeasonIndex);
                    mRestoreSeasonIndex = -1;
                }
                findViewById(R.id.layoutSeries).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.layoutSeries).setVisibility(View.GONE);
                setMirrorSpinner(item.getMirrors());
            }
            ActivityCompat.invalidateOptionsMenu(DetailActivity.this);
        }
    }

    private class QueryHosterTask extends AsyncTask<Void, Void, List<Host>> {
        Season s;
        int position;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            findViewById(R.id.layoutMirror).setVisibility(View.GONE);
            s = item.getSeasons()[((Spinner) findViewById(R.id.spinnerSeason)).getSelectedItemPosition()];
            position = ((Spinner) findViewById(R.id.spinnerEpisode)).getSelectedItemPosition();
        }

        @Override
        protected List<Host> doInBackground(Void... params) {
            if (item.getType() == ViewModel.Type.SERIES) {
                return Parser.getInstance().getHosterList(item, s.id, s.episodes[position]);
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Host> list) {
            super.onPostExecute(list);
            setMirrorSpinner(list == null ? null : list.toArray(new Host[list.size()]));
        }
    }

    public class QueryPlayTask extends AsyncTask<Void, String, String> {
        private ProgressDialog progressDialog;
        private Context context;
        Host host;
        int spinnerSeasonItemPosition;
        int spinnerEpisodeItemPosition;

        public QueryPlayTask(Context context) {
            this.context = context;
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
            //noinspection unchecked
            ArrayAdapter<Host> hosts = (ArrayAdapter<Host>) ((Spinner) findViewById(R.id.spinnerMirror)).getAdapter();
            host = hosts.getItem(((Spinner) findViewById(R.id.spinnerMirror)).getSelectedItemPosition());
            spinnerSeasonItemPosition = ((Spinner) findViewById(R.id.spinnerSeason)).getSelectedItemPosition();
            spinnerEpisodeItemPosition = ((Spinner) findViewById(R.id.spinnerEpisode)).getSelectedItemPosition();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            progressDialog.dismiss();
        }

        public void updateProgress(String... values){
            publishProgress(values);
        }

        @Override
        protected String doInBackground(Void... params) {
            String link;
            if (item.getType() == ViewModel.Type.SERIES) {
                Season s = item.getSeasons()[spinnerSeasonItemPosition];
                String e = s.episodes[spinnerEpisodeItemPosition];
                link = Parser.getInstance().getMirrorLink(this, item, host, s.id, e);
            } else {
                link = Parser.getInstance().getMirrorLink(this, item, host);
            }

            host.setUrl(link);
            return host.getVideoPath(this);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            progressDialog.setMessage(getString(R.string.loading) + "\n" + values[0]);
        }

        @SuppressWarnings("deprecation")
        @Override
        protected void onPostExecute(final String link) {
            super.onPostExecute(link);
            progressDialog.dismiss();
            if (!TextUtils.isEmpty(link)) {
                Log.i("Play", "Getting player for '" + link + "'");
                final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                intent.setDataAndType(Uri.parse(link), "video/mp4");

                AlertDialog.Builder builder = new AlertDialog.Builder(DetailActivity.this);
                builder.setTitle(getString(R.string.player_select_dialog_title));

                PackageManager pm = getPackageManager();
                List<ResolveInfo> launchables = pm.queryIntentActivities(intent, 0);
                List<AppAdapter.App> apps = new ArrayList<>();
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
                        Map<String, String> articleParams = new HashMap<>();
                        articleParams.put("Name", item.getTitle());
                        articleParams.put("Type", item.getType() == ViewModel.Type.MOVIE ? "Movie" : "Series");
                        articleParams.put("Id", item.getSlug());

                        articleParams.put("Hoster", host.getName());
                        articleParams.put("Movie", item.getTitle());
                        if (app.getComponent() == null) {
                            startPlaybackOnChromecast(link);
                            articleParams.put("Player", "Chromecast");
                        } else {
                            intent.setComponent(app.getComponent());
                            articleParams.put("Player", app.getComponent().toString());
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

        public Context getContext() {
            return context;
        }
    }

    public void startPlaybackOnChromecast(String link) {
        MediaMetadata mediaMetadata;
        if (item.getType() == ViewModel.Type.SERIES) {
            Season s = item.getSeasons()[((Spinner) findViewById(R.id.spinnerSeason)).getSelectedItemPosition()];
            String e = s.episodes[((Spinner) findViewById(R.id.spinnerEpisode)).getSelectedItemPosition()];
            mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_TV_SHOW);
            mediaMetadata.putString(MediaMetadata.KEY_TITLE, item.getTitle() + " - Folge " + s.id + "x" + e);
        } else {
            mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
            mediaMetadata.putString(MediaMetadata.KEY_TITLE, item.getTitle());
        }
        mediaMetadata.putString(MediaMetadata.KEY_SUBTITLE, getString(R.string.chromecast_subtitle));



        // Use TheMovieDb to get the image
        String url = getCachedImage(96, "poster");
        Log.i("Chromecast", "use image: " + url);
        if(TextUtils.isEmpty(url)) url = "http://kinocast.ov3rk1ll.com/img/kinocast_icon_512.png";
        mediaMetadata.addImage(new WebImage(Uri.parse(url)));

        // TODO Use Glide to get image
        url = getCachedImage(getResources().getDisplayMetrics().widthPixels, "poster"); // new CoverImage(item.getImageRequest(getResources().getDisplayMetrics().widthPixels, "poster")).getBitmapUrl(getApplication());
        Log.i("Chromecast", "use image: " + url);
        if(TextUtils.isEmpty(url)) url = "http://kinocast.ov3rk1ll.com/img/kinocast_icon_512.png";
        mediaMetadata.addImage(new WebImage(Uri.parse(url)));
        Log.i("cast", "play " + link);
        MediaInfo mediaInfo = new MediaInfo.Builder(link)
                .setContentType("video/mp4")
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(mediaMetadata)
                .build();

        mVideoCastManager.startVideoCastControllerActivity(DetailActivity.this, mediaInfo, 0, true);
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

    private String getCachedImage(int size, String type){
        TheMovieDb tmdbCache = new TheMovieDb(getApplication());
        String cacheUrl = Parser.getInstance().getPageLink(item);
        JSONObject json = tmdbCache.get(cacheUrl, false);
        if(json != null){
            try {
                String key = type + "_path";
                if (type.equals("backdrop"))
                    return TheMovieDb.IMAGE_BASE_PATH + OkHttpViewModelStreamFetcher.getBackdropSize(size) + json.getString(key);
                else
                    return TheMovieDb.IMAGE_BASE_PATH + OkHttpViewModelStreamFetcher.getPosterSize(size) + json.getString(key);
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
        return "";
    }

    static class AppAdapter extends ArrayAdapter<AppAdapter.App> {
        PackageManager pm;

        AppAdapter(Context context, List<AppAdapter.App> objects, PackageManager pm) {
            super(context, R.layout.player_list_item, android.R.id.text1, objects);
            this.pm = pm;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            App item = getItem(position);
            View view = super.getView(position, convertView, parent);
            ((TextView) view.findViewById(android.R.id.text1)).setText(item.getLabel());
            ((ImageView) view.findViewById(android.R.id.icon)).setImageDrawable(item.getIcon());
            return view;
        }

        @SuppressWarnings("unused")
        static class App {
            private CharSequence label;
            private Drawable icon;
            private ComponentName component;

            App(CharSequence label, Drawable icon, ComponentName component) {
                this.label = label;
                this.icon = icon;
                this.component = component;
            }

            CharSequence getLabel() {
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

            ComponentName getComponent() {
                return component;
            }

            public void setComponent(ComponentName component) {
                this.component = component;
            }
        }
    }
}
