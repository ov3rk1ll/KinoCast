package com.ov3rk1ll.kinocast.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.api.mirror.DivxStage;
import com.ov3rk1ll.kinocast.api.mirror.NowVideo;
import com.ov3rk1ll.kinocast.api.mirror.SharedSx;
import com.ov3rk1ll.kinocast.api.mirror.StreamCloud;
import com.ov3rk1ll.kinocast.utils.Utils;
import com.woxthebox.draglistview.DragItemAdapter;
import com.woxthebox.draglistview.DragListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class OrderHostlistActivity extends AppCompatActivity {
    private ArrayList<Item> mItemArray;
    SparseArray<Integer> sortedList = new SparseArray<>();
    private DragListView mDragListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_hostlist);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mDragListView = (DragListView) findViewById(R.id.drag_list_view);
        mDragListView.getRecyclerView().setVerticalScrollBarEnabled(true);

        mItemArray = new ArrayList<>();
        // mItemArray.add(new Item(Sockshare.HOST_ID, "Sockshare", R.drawable.ic_launcher));
        mItemArray.add(new Item(DivxStage.HOST_ID, "DivxStage", R.drawable.ic_drag_handle_black_48dp));
        mItemArray.add(new Item(StreamCloud.HOST_ID, "StreamCloud", R.drawable.ic_drag_handle_black_48dp));
        mItemArray.add(new Item(NowVideo.HOST_ID, "NowVideo", R.drawable.ic_drag_handle_black_48dp));
        mItemArray.add(new Item(SharedSx.HOST_ID, "SharedSx", R.drawable.ic_drag_handle_black_48dp));

        sortedList = Utils.getWeightedHostList(getApplicationContext());
        if(sortedList != null) {
            Collections.sort(mItemArray, new Comparator<Item>() {
                @Override
                public int compare(Item o1, Item o2) {
                    int x = sortedList.get(o1.getId());
                    int y = sortedList.get(o2.getId());
                    return (x < y) ? -1 : ((x == y) ? 0 : 1);
                }
            });
        }

        mDragListView.setLayoutManager(new LinearLayoutManager(this));
        ItemAdapter listAdapter = new ItemAdapter(mItemArray, R.layout.order_hostlist_item, R.id.image, false);
        mDragListView.setAdapter(listAdapter, true);
        mDragListView.setCanDragHorizontally(false);
    }

    @Override
    public void onBackPressed() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = preferences.edit();
        for(int i = 0; i < mItemArray.size(); i++){
            editor.putInt("order_hostlist_" + i, mItemArray.get(i).getId());
        }
        editor.putInt("order_hostlist_count", mItemArray.size());
        editor.commit();
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            supportFinishAfterTransition();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class Item{
        private int id;
        private String name;
        private int resId;

        public Item(int id, String name, int resId) {
            this.id = id;
            this.name = name;
            this.resId = resId;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getResId() {
            return resId;
        }
    }

    public static class ItemAdapter extends DragItemAdapter<Item, ItemAdapter.ViewHolder> {

        private int mLayoutId;
        private int mGrabHandleId;

        public ItemAdapter(ArrayList<Item> list, int layoutId, int grabHandleId, boolean dragOnLongPress) {
            super(dragOnLongPress);
            mLayoutId = layoutId;
            mGrabHandleId = grabHandleId;
            setHasStableIds(true);
            setItemList(list);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);
            String text = mItemList.get(position).getName();
            holder.mText.setText(text);
            holder.itemView.setTag(text);
            holder.mImage.setImageResource(mItemList.get(position).getResId());
        }

        @Override
        public long getItemId(int position) {
            return mItemList.get(position).getId();
        }

        public class ViewHolder extends DragItemAdapter<Item, ItemAdapter.ViewHolder>.ViewHolder {
            public TextView mText;
            public ImageView mImage;

            public ViewHolder(final View itemView) {
                super(itemView, mGrabHandleId);
                mText = (TextView) itemView.findViewById(R.id.text);
                mImage = (ImageView) itemView.findViewById(R.id.image);
            }

            @Override
            public void onItemClicked(View view) {
                Toast.makeText(view.getContext(), "Item clicked", Toast.LENGTH_SHORT).show();
            }

            @Override
            public boolean onItemLongClicked(View view) {
                Toast.makeText(view.getContext(), "Item long clicked", Toast.LENGTH_SHORT).show();
                return true;
            }
        }
    }
}
