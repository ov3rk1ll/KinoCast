package com.ov3rk1ll.kinocast.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
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
import android.widget.ProgressBar;

import com.google.sample.castcompanionlibrary.cast.BaseCastManager;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.cast.callbacks.IVideoCastConsumer;
import com.google.sample.castcompanionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.sample.castcompanionlibrary.cast.player.VideoCastControllerActivity;
import com.google.sample.castcompanionlibrary.widgets.MiniController;
import com.kskkbys.rate.RateThisApp;
import com.ov3rk1ll.kinocast.BuildConfig;
import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.api.Parser;
import com.ov3rk1ll.kinocast.ui.helper.NavigationDrawerFragment;
import com.ov3rk1ll.kinocast.ui.helper.layout.SearchSuggestionAdapter;
import com.ov3rk1ll.kinocast.utils.CastHelper;
import com.ov3rk1ll.kinocast.utils.ShowcaseHelper;
import com.ov3rk1ll.kinocast.utils.Utils;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private static final String STATE_TITLE = "state_title";
    private static final String STATE_IS_SEARCHVIEW = "state_is_searchview";

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private CharSequence mTitle;
    private boolean mIsSearchView = false;
    private ProgressBar mProgressBar;

    private VideoCastManager mVideoCastManager;
    private MiniController mMini;
    private SearchSuggestionAdapter searchSuggestionAdapter;
    private IVideoCastConsumer mCastConsumer;
    private MenuItem mediaRouteMenuItem;

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

        /*BackupManager bm = new BackupManager(this);
        bm.requestRestore(new RestoreObserver() {
            @Override
            public void restoreFinished(int error) {
                Log.i("BackupManager", "RestoreObserver.restoreFinished with " + error);
                super.restoreFinished(error);
            }
        });*/

        mTitle = getTitle();
        if (savedInstanceState != null) {
            mTitle = savedInstanceState.getString(STATE_TITLE);
            mIsSearchView = savedInstanceState.getBoolean(STATE_IS_SEARCHVIEW);
        }

        mProgressBar = new ProgressBar(this);
        mProgressBar.setVisibility(View.GONE);
        mProgressBar.setIndeterminate(true);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mProgressBar.setIndeterminateTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorAccentTint)));
        }

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar_actionbar);
        Toolbar.LayoutParams layoutParams = new Toolbar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.RIGHT;
        toolbar.addView(mProgressBar, layoutParams);
        setSupportActionBar(toolbar);


        if(BuildConfig.GMS_CHECK) BaseCastManager.checkGooglePlayServices(this);
        mVideoCastManager = CastHelper.getVideoCastManager(this);
        mMini = (MiniController) findViewById(R.id.miniController);
        mVideoCastManager.addMiniController(mMini);
        mVideoCastManager.reconnectSessionIfPossible(this, false, 5);


        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

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

        RateThisApp.onStart(this);
        RateThisApp.showRateDialogIfNeeded(this);
    }

    @Override
    protected void onDestroy() {
        mVideoCastManager.removeMiniController(mMini);

        /*BackupManager bm = new BackupManager(this);
        bm.dataChanged();*/

        super.onDestroy();
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
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mVideoCastManager.onDispatchVolumeKeyEvent(event, VideoCastControllerActivity.DEFAULT_VOLUME_INCREMENT)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        String query = null;
        boolean isSpecial = false;
        switch (position) {
            case 0:
                query = Parser.getInstance().getCineMovies();
                mTitle = getString(R.string.title_section1);
                break;
            case 1:
                query = Parser.getInstance().getPopularMovies();
                mTitle = getString(R.string.title_section2);
                break;
            case 2:
                query = Parser.getInstance().getLatestMovies();
                mTitle = getString(R.string.title_section3);
                break;
            case 3:
                query = Parser.getInstance().getPopularSeries();
                mTitle = getString(R.string.title_section4);
                break;
            case 4:
                query = Parser.getInstance().getLatestSeries();
                mTitle = getString(R.string.title_section5);
                break;
            case 5:
                query = String.valueOf(ListFragment.SPECIAL_BOOKMARKS);
                isSpecial = true;
                mTitle = getString(R.string.title_section6);
                break;
            case 6:
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
    public void onNavigationModeItemSelected(int position) {
        Log.i("onNavigationModeItemSelected", "position: " + position);
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
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
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
        if (mNavigationDrawerFragment.isDrawerOpen()) {
            mNavigationDrawerFragment.closeDrawer();
        }else if(mIsSearchView) {
            mIsSearchView = false;
            onNavigationDrawerItemSelected(mNavigationDrawerFragment.getCurrentSelectedPosition());
        }else{
            super.onBackPressed();
        }
    }
}
