package com.ov3rk1ll.kinocast.ui.util.glide;

import android.content.Context;

import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.ov3rk1ll.kinocast.utils.TheMovieDb;

import java.io.InputStream;

import okhttp3.Call;
import okhttp3.OkHttpClient;

/**
 * A simple model loader for fetching media over http/https using OkHttp.
 */
public class OkHttpViewModelUrlLoader implements ModelLoader<ViewModelGlideRequest, InputStream> {

    private final Call.Factory client;
    private TheMovieDb tmdbCache;

    private OkHttpViewModelUrlLoader(Context context, Call.Factory client) {
        this.client = client;
        this.tmdbCache = new TheMovieDb(context);
    }

    @Override
    public DataFetcher<InputStream> getResourceFetcher(ViewModelGlideRequest model, int width, int height) {
        return new OkHttpViewModelStreamFetcher(client, model, tmdbCache);
    }

    /**
     * The default factory for {@link OkHttpViewModelUrlLoader}s.
     */
    public static class Factory implements ModelLoaderFactory<ViewModelGlideRequest, InputStream> {
        private static volatile Call.Factory internalClient;
        private Call.Factory client;

        /**
         * Constructor for a new Factory that runs requests using a static singleton client.
         */
        public Factory() {
            this(getInternalClient());
        }

        /**
         * Constructor for a new Factory that runs requests using given client.
         *
         * @param client this is typically an instance of {@code OkHttpClient}.
         */
        Factory(Call.Factory client) {
            this.client = client;
        }

        private static Call.Factory getInternalClient() {
            if (internalClient == null) {
                synchronized (Factory.class) {
                    if (internalClient == null) {
                        internalClient = new OkHttpClient();
                    }
                }
            }
            return internalClient;
        }

        @Override
        public ModelLoader<ViewModelGlideRequest, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new OkHttpViewModelUrlLoader(context, client);
        }

        @Override
        public void teardown() {
            // Do nothing, this instance doesn't own the client.
        }
    }
}