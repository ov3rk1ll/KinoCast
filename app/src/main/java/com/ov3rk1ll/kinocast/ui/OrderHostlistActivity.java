package com.ov3rk1ll.kinocast.ui;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.api.mirror.Host;
import com.ov3rk1ll.kinocast.utils.Utils;
import com.woxthebox.draglistview.DragItemAdapter;
import com.woxthebox.draglistview.DragListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class OrderHostlistActivity extends AppCompatActivity {
    private ArrayList<Item> mItemArray;
    SparseIntArray sortedList = new SparseIntArray();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_hostlist);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        DragListView mDragListView = (DragListView) findViewById(R.id.drag_list_view);
        mDragListView.getRecyclerView().setVerticalScrollBarEnabled(true);

        mItemArray = new ArrayList<>();
        for (Class<?> h: Host.HOSTER_LIST) {
            try {
                Host host = (Host) h.getConstructor().newInstance();
                if(host.isEnabled()) {
                    mItemArray.add(new Item(host.getId(), host.getName(), R.drawable.ic_drag_handle_black_48dp));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        sortedList = Utils.getWeightedHostList(getApplicationContext());
        if(sortedList != null) {
            Collections.sort(mItemArray, new Comparator<Item>() {
                @Override
                public int compare(Item o1, Item o2) {
                    int x = sortedList.get(o1.getId(), o1.getId());
                    int y = sortedList.get(o2.getId(), o1.getId());
                    return (x < y) ? -1 : ((x == y) ? 0 : 1);
                }
            });
        }

        mDragListView.setLayoutManager(new LinearLayoutManager(this));
        ItemAdapter listAdapter = new ItemAdapter(mItemArray, R.layout.order_hostlist_item, R.id.image, false);
        mDragListView.setAdapter(listAdapter, true);
        mDragListView.setCanDragHorizontally(false);
    }

    @SuppressLint("ApplySharedPref")
    @Override
    protected void onPause() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = preferences.edit();
        for(int i = 0; i < mItemArray.size(); i++){
            editor.putInt("order_hostlist_" + i, mItemArray.get(i).getId());
        }
        editor.putInt("order_hostlist_count", mItemArray.size());
        editor.commit();
        super.onPause();
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

    static class Item{
        private int id;
        private String name;
        private int resId;

        Item(int id, String name, int resId) {
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

        int getResId() {
            return resId;
        }

        @Override
        public String toString() {
            return "Item {id = " + id + ", name = " + name + "}";
        }
    }

    static class ItemAdapter extends DragItemAdapter<Item, ItemAdapter.ViewHolder> {

        private int mLayoutId;
        private int mGrabHandleId;

        ItemAdapter(ArrayList<Item> list, int layoutId, int grabHandleId, boolean dragOnLongPress) {
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

        class ViewHolder extends DragItemAdapter<Item, ItemAdapter.ViewHolder>.ViewHolder {
            TextView mText;
            ImageView mImage;

            ViewHolder(final View itemView) {
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