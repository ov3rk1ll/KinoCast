package com.ov3rk1ll.kinocast.ui.util.glide;

import com.ov3rk1ll.kinocast.data.ViewModel;


public class ViewModelGlideRequest {
    private ViewModel viewModel;
    private int screenWidthPx;
    private String type;

    public ViewModelGlideRequest(ViewModel viewModel, int screenWidthPx, String type) {
        this.viewModel = viewModel;
        this.screenWidthPx = screenWidthPx;
        this.type = type;
    }

    ViewModel getViewModel() {
        return viewModel;
    }

    int getScreenWidthPx() {
        return screenWidthPx;
    }

    public String getType() {
        return type;
    }
}