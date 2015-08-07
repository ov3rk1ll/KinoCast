package com.ov3rk1ll.kinocast.ui;

import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.media.MediaRouter;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.libraries.cast.companionlibrary.cast.BaseCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.widgets.MiniController;
import com.ov3rk1ll.kinocast.BuildConfig;
import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.api.KinoxParser;
import com.ov3rk1ll.kinocast.api.Movie4kParser;
import com.ov3rk1ll.kinocast.api.Parser;
import com.ov3rk1ll.kinocast.ui.helper.layout.SearchSuggestionAdapter;
import com.ov3rk1ll.kinocast.utils.BookmarkManager;
import com.ov3rk1ll.kinocast.utils.CastHelper;
import com.ov3rk1ll.kinocast.utils.ShowcaseHelper;
import com.ov3rk1ll.kinocast.utils.Utils;
import com.winsontan520.wversionmanager.library.WVersionManager;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private final int RESOLVE_CONNECTION_REQUEST_CODE = 5001;

    private static final String STATE_TITLE = "state_title";
    private static final String STATE_IS_SEARCHVIEW = "state_is_searchview";
    private static final String NAV_ITEM_ID = "navItemId";

    private CharSequence mTitle;
    private boolean mIsSearchView = false;
    private ProgressBar mProgressBar;

    private VideoCastManager mVideoCastManager;
    private MiniController mMini;
    private SearchSuggestionAdapter searchSuggestionAdapter;
    private MenuItem mediaRouteMenuItem;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private int mNavItemId;
    private final Handler mDrawerActionHandler = new Handler();
    private VideoCastConsumerImpl mCastConsumer;
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void setSupportProgressBarIndeterminateVisibility(boolean visible) {
        //super.setSupportProgressBarIndeterminateVisibility(visible);
        mProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        WVersionManager versionManager = new WVersionManager(this);
        versionManager.setVersionContentUrl("http://ov3rk1ll.github.io/KinoCast/update.json");
        versionManager.checkVersion();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addScope(Drive.SCOPE_APPFOLDER)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        /*BackupManager bm = new BackupManager(this);
        bm.requestRestore(new RestoreObserver() {
            @Override
            public void restoreFinished(int error) {
                Log.i("BackupManager", "RestoreObserver.restoreFinished with " + error);
                super.restoreFinished(error);
            }
        });*/

        mTitle = getTitle();
        mNavItemId = R.string.title_section1;
        if (savedInstanceState != null) {
            mTitle = savedInstanceState.getString(STATE_TITLE);
            mIsSearchView = savedInstanceState.getBoolean(STATE_IS_SEARCHVIEW);
            mNavItemId = savedInstanceState.getInt(NAV_ITEM_ID);
        }

        mProgressBar = new ProgressBar(this);
        mProgressBar.setVisibility(View.GONE);
        mProgressBar.setIndeterminate(true);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mProgressBar.setIndeterminateTintList(ColorStateList.valueOf(getResources().getColor(R.color.accent)));
        }

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar_actionbar);
        Toolbar.LayoutParams layoutParams = new Toolbar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.END;
        toolbar.addView(mProgressBar, layoutParams);
        setSupportActionBar(toolbar);

        if(BuildConfig.GMS_CHECK) BaseCastManager.checkGooglePlayServices(this);
        mVideoCastManager = CastHelper.getVideoCastManager(this);
        mMini = (MiniController) findViewById(R.id.miniController);
        mVideoCastManager.addMiniController(mMini);
        mVideoCastManager.reconnectSessionIfPossible();


        // listen for navigation events
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation);
        navigationView.setNavigationItemSelectedListener(this);

        navigationView.getMenu().findItem(mNavItemId).setChecked(true);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        navigate(mNavItemId);

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
                        if (mediaRouteMenuItem != null && mediaRouteMenuItem.isVisible()) {
                            ShowcaseHelper.showChromecastHelp(MainActivity.this);
                        }
                    }
                }, 1000);
            }
        };

        final ArrayAdapter<Parser> adapter = new ArrayAdapter<Parser>(this,
                android.R.layout.simple_list_item_1,
                new Parser[]{new KinoxParser(), new Movie4kParser()});


        ((Spinner)findViewById(R.id.spinner)).setAdapter(adapter);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int parserId = preferences.getInt("parser", KinoxParser.PARSER_ID);
        ((Spinner)findViewById(R.id.spinner)).setSelection(parserId);
        ((Spinner)findViewById(R.id.spinner)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                int parserId = adapter.getItem(position).getParserId();
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                preferences.edit().putInt("parser", parserId).apply();
                Parser.selectParser(parserId);
                mDrawerLayout.closeDrawers();
                navigate(mNavItemId);
                //mCallbacks.onNavigationModeItemSelected(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        mVideoCastManager.removeMiniController(mMini);

        /*BackupManager bm = new BackupManager(this);
        bm.dataChanged();*/

        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // TODO mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mVideoCastManager.addVideoCastConsumer(mCastConsumer);
        mVideoCastManager.incrementUiCounter();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVideoCastManager.removeVideoCastConsumer(mCastConsumer);
        mVideoCastManager.decrementUiCounter();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_TITLE, mTitle.toString());
        outState.putBoolean(STATE_IS_SEARCHVIEW, mIsSearchView);
        outState.putInt(NAV_ITEM_ID, mNavItemId);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return mVideoCastManager.onDispatchVolumeKeyEvent(event, 0.05) || super.dispatchKeyEvent(event);
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            //actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(mTitle);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        if (!mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            getMenuInflater().inflate(R.menu.main, menu);
            mediaRouteMenuItem = mVideoCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);

            if(mCastConsumer != null){
                mCastConsumer.onCastDeviceDetected(null);
            }

            final SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));

            String[] from = {"item"};
            int[] to = {android.R.id.text1};

            searchSuggestionAdapter = new SearchSuggestionAdapter(
                    getSupportActionBar().getThemedContext(),
                    android.R.layout.simple_list_item_1,
                    null,
                    from,
                    to,
                    0);

            searchView.setSuggestionsAdapter(searchSuggestionAdapter);
            searchView.setOnSuggestionListener(new android.support.v7.widget.SearchView.OnSuggestionListener() {
                @Override
                public boolean onSuggestionSelect(int position) {
                    return false;
                }

                @Override
                public boolean onSuggestionClick(int position) {
                    Cursor cursor = (Cursor) searchSuggestionAdapter.getItem(position);
                    doSearch(cursor.getString(1));
                    MenuItemCompat.collapseActionView(menu.findItem(R.id.action_search));
                    return true;
                }
            });

            SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextChange(String newText) {
                    if(newText.length() >= 2)
                        searchSuggestionAdapter.getFilter().filter(newText);
                    return true;
                }

                @Override
                public boolean onQueryTextSubmit(String query) {
                    MenuItemCompat.collapseActionView(menu.findItem(R.id.action_search));
                    doSearch(query);
                    return true;
                }
            };
            searchView.setOnQueryTextListener(queryTextListener);
            //restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void doSearch(String query){
        FragmentManager fragmentManager = getSupportFragmentManager();
        mIsSearchView = true;
        mTitle = "\"" + query + "\"";
        restoreActionBar();
        Utils.trackPath(MainActivity.this, "Search.html?q=" + query);
        fragmentManager.beginTransaction()
                .replace(R.id.container, ListFragment.newInstance(Parser.getInstance().getSearchPage(query)))
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawers();
        }else if(mIsSearchView) {
            mIsSearchView = false;
            navigate(mNavItemId);
        }else{
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(final MenuItem menuItem) {
        //TODO onNavigationItemSelected
        menuItem.setChecked(true);
        mNavItemId = menuItem.getItemId();

        // allow some time after closing the drawer before performing real navigation
        // so the user can see what is happening
        mDrawerLayout.closeDrawer(GravityCompat.START);
        mDrawerActionHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                navigate(menuItem.getItemId());
            }
        }, 250);
        return true;
    }

    private void navigate(int menuItemId){
        String query = null;
        boolean isSpecial = false;
        switch (menuItemId) {
            case R.string.title_section1:
                query = Parser.getInstance().getCineMovies();
                mTitle = getString(R.string.title_section1);
                break;
            case R.string.title_section2:
                query = Parser.getInstance().getPopularMovies();
                mTitle = getString(R.string.title_section2);
                break;
            case R.string.title_section3:
                query = Parser.getInstance().getLatestMovies();
                mTitle = getString(R.string.title_section3);
                break;
            case R.string.title_section4:
                query = Parser.getInstance().getPopularSeries();
                mTitle = getString(R.string.title_section4);
                break;
            case R.string.title_section5:
                query = Parser.getInstance().getLatestSeries();
                mTitle = getString(R.string.title_section5);
                break;
            case R.string.title_section6:
                query = String.valueOf(ListFragment.SPECIAL_BOOKMARKS);
                isSpecial = true;
                mTitle = getString(R.string.title_section6);
                break;
            case R.string.title_section7:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }
        restoreActionBar();

        if(query != null) {
            ListFragment fragment;
            if(isSpecial){
                int special = Integer.valueOf(query);
                fragment = ListFragment.newInstance(special);
                query = "Special/";
                switch (special){
                    case ListFragment.SPECIAL_BOOKMARKS: query += "Bookmarks.html";
                }
            } else {
                fragment = ListFragment.newInstance(query);
            }
            Utils.trackPath(MainActivity.this, query);
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    mGoogleApiClient.connect();
                }
                break;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(driveContentsCallback);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
        }
    }

    final private ResultCallback<DriveApi.DriveContentsResult> driveContentsCallback =
            new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(DriveApi.DriveContentsResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.i("Drive", "Error while trying to create new file contents");
                        return;
                    }

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle("bookmark.dat")
                            .setMimeType("text/plain")
                            .build();
                    Drive.DriveApi.getAppFolder(mGoogleApiClient)
                            .createFile(mGoogleApiClient, changeSet, result.getDriveContents())
                            .setResultCallback(fileCallback);
                }
            };

    final private ResultCallback<DriveFolder.DriveFileResult> fileCallback = new
            ResultCallback<DriveFolder.DriveFileResult>() {
                @Override
                public void onResult(DriveFolder.DriveFileResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.i("Drive", "Error while trying to create the file");
                        return;
                    }

                    BookmarkManager bookmarks = new BookmarkManager(MainActivity.this);
                    bookmarks.backup(result, mGoogleApiClient);

                    Log.i("Drive", "Should do backup");

                }
            };
}
