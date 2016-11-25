package com.ov3rk1ll.kinocast.ui.helper.layout;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.data.ViewModel;
import com.ov3rk1ll.kinocast.ui.helper.PaletteManager;
import com.ov3rk1ll.kinocast.ui.util.glide.OkHttpViewModelUrlLoader;
import com.ov3rk1ll.kinocast.ui.util.glide.ViewModelGlideRequest;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ResultRecyclerAdapter extends RecyclerView.Adapter<ResultRecyclerAdapter.ViewHolder> implements View.OnClickListener {

    private Context context;
    private List<ViewModel> items;
    private OnRecyclerViewItemClickListener<ViewModel> itemClickListener;
    private int itemLayout;

    public ResultRecyclerAdapter(Context context, int itemLayout) {
        this.items = new ArrayList<ViewModel>();
        this.itemLayout = itemLayout;
        this.context = context;

        Glide.get(context)
                .register(ViewModelGlideRequest.class, InputStream.class, new OkHttpViewModelUrlLoader.Factory());
    }

    public ResultRecyclerAdapter(List<ViewModel> items, int itemLayout) {
        this.items = items;
        this.itemLayout = itemLayout;
    }

    @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(itemLayout, parent, false);
        v.setOnClickListener(this);
        return new ViewHolder(v);
    }

    @Override public void onBindViewHolder(final ViewHolder holder, int position) {
        final ViewModel item = items.get(position);
        holder.itemView.setTag(item);
        holder.title.setText(item.getTitle());
        holder.rating.setRating(item.getRating() / 2.0f);
        holder.detail.setText(item.getGenre());
        holder.language.setImageResource(item.getLanguageResId());

        int px = holder.image.getContext().getResources().getDimensionPixelSize(R.dimen.list_item_width);

        Glide.with(context)
                .load(new ViewModelGlideRequest(item, px, "poster"))
                .placeholder(R.drawable.ic_loading_placeholder)
                .listener(new RequestListener<ViewModelGlideRequest, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, ViewModelGlideRequest model, Target<GlideDrawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, ViewModelGlideRequest model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        holder.updatePalette(((GlideBitmapDrawable)resource.getCurrent()).getBitmap());
                        holder.image.setVisibility(View.VISIBLE);
                        holder.progressBar.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(holder.image);
                holder.image.setVisibility(View.VISIBLE);

        /*holder.image.setImageItem(item.getImageRequest(px, "poster"), R.drawable.ic_loading_placeholder, new SmartImageTask.OnCompleteListener() {
            @Override
            public void onComplete() {
                holder.updatePalette();
                holder.image.setVisibility(View.VISIBLE);
                holder.progressBar.setVisibility(View.GONE);
            }
        });*/
        if(position == 1){
            holder.itemView.setSelected(true);
        }

    }


    @Override public int getItemCount() {
        return items.size();
    }

    @Override public void onClick(View view) {
        if (itemClickListener != null) {
            ViewModel model = (ViewModel) view.getTag();
            itemClickListener.onItemClick(view, model);
        }
    }

    public void add(ViewModel item, int position) {
        items.add(position, item);
        notifyItemInserted(position);
    }

    public void remove(ViewModel item) {
        int position = items.indexOf(item);
        items.remove(position);
        notifyItemRemoved(position);
    }

    public void setOnItemClickListener(OnRecyclerViewItemClickListener<ViewModel> listener) {
        this.itemClickListener = listener;
    }

    private static int setColorAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00ffffff);
    }

    public ViewModel[] getItems() {
        return items.toArray(new ViewModel[items.size()]);
    }

    public void addAll(ViewModel[] values) {
        for(ViewModel m: values){
            this.add(m, this.getItemCount());
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private RelativeLayout background;
        public ImageView image;
        public TextView title;
        public ImageView language;
        public RatingBar rating;
        public TextView detail;
        public ProgressBar progressBar;
        public Palette palette;

        public ViewHolder(View itemView) {
            super(itemView);
            background = (RelativeLayout) itemView.findViewById(R.id.layoutInfo);
            image = (ImageView) itemView.findViewById(R.id.image);
            title = (TextView) itemView.findViewById(R.id.title);
            language = (ImageView) itemView.findViewById(R.id.language);
            rating = (RatingBar) itemView.findViewById(R.id.rating);
            detail = (TextView) itemView.findViewById(R.id.detail);
            progressBar = (ProgressBar) itemView.findViewById(R.id.progressBar);
            image.setVisibility(View.GONE);
        }

        public void updatePalette(Bitmap bitmap) {
            final String key = ((ViewModel)itemView.getTag()).getSlug();
            PaletteManager.getInstance().getPalette(key, bitmap, new PaletteManager.Callback() {
                @Override
                public void onPaletteReady(Palette palette) {
                    Palette.Swatch swatch = palette.getDarkVibrantSwatch();
                    if(swatch != null) {
                        background.setBackgroundColor(setColorAlpha(swatch.getRgb(), 192));
                        title.setTextColor(swatch.getTitleTextColor());
                        detail.setTextColor(swatch.getBodyTextColor());
                    }
                }
            });
        }


    }

    public interface OnRecyclerViewItemClickListener<Model> {
        public void onItemClick(View view, Model model);
    }
}