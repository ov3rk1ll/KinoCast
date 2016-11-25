package com.ov3rk1ll.kinocast.ui;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.libraries.cast.companionlibrary.cast.BaseCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.ov3rk1ll.kinocast.BuildConfig;
import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.api.KinoxParser;
import com.ov3rk1ll.kinocast.api.Movie4kParser;
import com.ov3rk1ll.kinocast.api.Parser;
import com.ov3rk1ll.kinocast.ui.helper.layout.SearchSuggestionAdapter;
import com.ov3rk1ll.kinocast.utils.Utils;
import com.winsontan520.wversionmanager.library.WVersionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, GoogleApiClient.OnConnectionFailedListener {
    // private final int RESOLVE_CONNECTION_REQUEST_CODE = 5001;

    private static final String STATE_TITLE = "state_title";
    private static final String STATE_IS_SEARCHVIEW = "state_is_searchview";
    private static final String NAV_ITEM_ID = "navItemId";

    private CharSequence mTitle;
    private boolean mIsSearchView = false;
    private ProgressBar mProgressBar;

    private SearchSuggestionAdapter searchSuggestionAdapter;
    private DrawerLayout mDrawerLayout;
    private int mNavItemId;
    private final Handler mDrawerActionHandler = new Handler();
    private MenuItem searchMenuItem;
    private GoogleApiClient mGoogleApiClient;
    private NavigationView mNavigationView;

    @SuppressWarnings("deprecation")
    @Override
    public void setSupportProgressBarIndeterminateVisibility(boolean visible) {
        //super.setSupportProgressBarIndeterminateVisibility(visible);
        mProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(BuildConfig.GMS_CHECK) BaseCastManager.checkGooglePlayServices(this);
        Utils.initializeCastManager(this);
        setContentView(R.layout.activity_main);
        //supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        WVersionManager versionManager = new WVersionManager(this);
        versionManager.setVersionContentUrl("http://ov3rk1ll.github.io/KinoCast/update2.json");
        versionManager.checkVersion();

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

        VideoCastManager.getInstance().reconnectSessionIfPossible();


        // listen for navigation events
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mNavigationView = (NavigationView) findViewById(R.id.navigation);
        mNavigationView.setNavigationItemSelectedListener(this);

        mNavigationView.getMenu().findItem(mNavItemId).setChecked(true);

        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        navigate(mNavItemId);

        final ArrayAdapter<Parser> adapter = new ArrayAdapter<Parser>(this,
                android.R.layout.simple_list_item_1,
                new Parser[]{new KinoxParser(), new Movie4kParser()});


        /*((Spinner)findViewById(R.id.spinner)).setAdapter(adapter);
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
        */

        // Login stuff
        mGoogleApiClient = Utils.buildAuthClient(this, this, this);

        //Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        //startActivityForResult(signInIntent, RC_SIGN_IN);

    }

    void handleSignInResult(GoogleSignInResult result){
        try {
            GoogleSignInAccount acct = result.getSignInAccount();
            String authCode = acct.getServerAuthCode();

            Log.i("userdata", "GET " + getString(R.string.api_server) + "/auth/authcode/callback?code=" + authCode);
            OkHttpClient client = Utils.buildHttpClient(this);
            Request request = new Request.Builder()
                    .url(getString(R.string.api_server) + "/auth/authcode/callback?code=" + authCode)
                    .build();

            Response response = client.newCall(request).execute();
            int statusCode = response.code();
            if(statusCode == 200){
                String responseBody = response.body().string();
                final JSONObject json = new JSONObject(responseBody);

                // TODO Is pro user
                String role = json.getString("role");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        View headerLayout = mNavigationView.getHeaderView(0);
                        try {
                            final View backgroundLayout = headerLayout.findViewById(R.id.layoutBackground);
                            ((TextView)headerLayout.findViewById(R.id.name)).setText(json.getString("name"));
                            ((TextView)headerLayout.findViewById(R.id.email)).setText(json.getString("email"));

                            String avatar = json.getString("image");
                            Glide.with(MainActivity.this).load(avatar).into((CircleImageView)headerLayout.findViewById(R.id.circleView));

                            String background = json.getString("cover");
                            Glide.with(MainActivity.this).load(background).asBitmap().into(new SimpleTarget<Bitmap>(backgroundLayout.getWidth(), backgroundLayout.getHeight()) {
                                @Override
                                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                    Drawable drawable = new BitmapDrawable(resource);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                        backgroundLayout.setBackground(drawable);
                                    }
                                }
                            });

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
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
        VideoCastManager.getInstance().incrementUiCounter();
        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation);
        // remove active state from settings
        navigationView.setCheckedItem(mNavItemId);

        OptionalPendingResult<GoogleSignInResult> pendingResult =
                Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (pendingResult.isDone()) {
            final GoogleSignInResult result = pendingResult.get();
            // There's immediate result available.
            Log.i("GoogleSignInResult", "immediate " + (result.getSignInAccount() != null));
            if(result.getSignInAccount() != null){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        handleSignInResult(result);
                    }
                }).start();
            }
        } else {

            Log.i("GoogleSignInResult", "so async");
            pendingResult.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(@NonNull final GoogleSignInResult result) {
                    Log.i("GoogleSignInResult", "onResult " + (result.getSignInAccount() != null));
                    if(result.getSignInAccount() != null){
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                handleSignInResult(result);
                            }
                        }).start();
                    }
                }
            });
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        VideoCastManager.getInstance().decrementUiCounter();
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
        return VideoCastManager.getInstance().onDispatchVolumeKeyEvent(event, 0.05) || super.dispatchKeyEvent(event);
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
            VideoCastManager.getInstance().addMediaRouterButton(menu, R.id.media_route_menu_item);

            searchMenuItem = menu.findItem(R.id.action_search);

            final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
            searchView.setQueryHint(getResources().getString(R.string.searchable_hint));

            searchSuggestionAdapter = new SearchSuggestionAdapter(
                    getSupportActionBar().getThemedContext(),
                    android.R.layout.simple_list_item_1,
                    null,
                    new String[]{"item"},
                    new int[]{android.R.id.text1},
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
                    MenuItemCompat.collapseActionView(searchMenuItem);
                    return true;
                }
            });

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextChange(String newText) {
                    if(newText.length() >= 2)
                        searchSuggestionAdapter.getFilter().filter(newText);
                    return true;
                }

                @Override
                public boolean onQueryTextSubmit(String query) {
                    MenuItemCompat.collapseActionView(searchMenuItem);
                    doSearch(query);
                    return true;
                }
            });
            
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            //restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            doSearch(query);
            MenuItemCompat.collapseActionView(searchMenuItem);
        }
    }

    private void doSearch(String query){
        FragmentManager fragmentManager = getSupportFragmentManager();
        mIsSearchView = true;
        mTitle = "\"" + query + "\"";
        restoreActionBar();
        // TODO Utils.trackPath(MainActivity.this, "Search.html?q=" + query);
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
        menuItem.setChecked(true);
        if(menuItem.getItemId() != R.string.title_section7) {
            mNavItemId = menuItem.getItemId();
        }

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
            // TODO Utils.trackPath(MainActivity.this, query);
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
