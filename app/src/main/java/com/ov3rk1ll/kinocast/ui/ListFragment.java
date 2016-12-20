package com.ov3rk1ll.kinocast.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.api.Parser;
import com.ov3rk1ll.kinocast.data.ViewModel;
import com.ov3rk1ll.kinocast.ui.helper.layout.GridLayoutManager;
import com.ov3rk1ll.kinocast.ui.helper.layout.ResultRecyclerAdapter;
import com.ov3rk1ll.kinocast.utils.BookmarkManager;

import java.util.ArrayList;
import java.util.List;

public class ListFragment extends Fragment {
    private static final String ARG_URL = "param_url";
    private static final String ARG_SPECIAL = "param_special";

    public static final int SPECIAL_BOOKMARKS = 0;

    ResultRecyclerAdapter adapter;
    private String mUrl;
    private int mSpecialId = -1;

    public static ListFragment newInstance(String url) {
        ListFragment fragment = new ListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    public static ListFragment newInstance(int specialId) {
        ListFragment fragment = new ListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SPECIAL, specialId);
        fragment.setArguments(args);
        return fragment;
    }

    public ListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            if(getArguments().containsKey(ARG_URL))
                mUrl = getArguments().getString(ARG_URL);
            if(getArguments().containsKey(ARG_SPECIAL))
                mSpecialId = getArguments().getInt(ARG_SPECIAL);
        }
        adapter = new ResultRecyclerAdapter(R.layout.frament_list_item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity());

        DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();
        int columns = displayMetrics.widthPixels / getActivity().getResources().getDimensionPixelSize(R.dimen.list_item_width);
        if(columns < 2){
            columns = 2;
        }
        gridLayoutManager.setColumns(columns);

        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        adapter.setOnItemClickListener(new ResultRecyclerAdapter.OnRecyclerViewItemClickListener<ViewModel>() {
            @Override public void onItemClick(View view, ViewModel viewModel) {
                Intent intent = new Intent(getActivity(), DetailActivity.class);
                intent.putExtra(DetailActivity.ARG_ITEM, viewModel);
                intent.putExtra(DetailActivity.ARG_ITEM, viewModel);
                //ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), view.findViewById(R.id.image), "photo_hero");
                getActivity().startActivity(intent);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mSpecialId != -1){
            List<ViewModel> itemToRemove = new ArrayList<ViewModel>();
            BookmarkManager bookmarks = new BookmarkManager(getActivity());
            for(ViewModel item: adapter.getItems()){
                if(bookmarks.findItem(item) == null){
                    itemToRemove.add(item);
                }
            }
            for(ViewModel item: itemToRemove){
                adapter.remove(item);
            }
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(mUrl != null)
            (new QueryPageTask()).execute(mUrl);
        else if(mSpecialId != -1)
            (new QueryBookmarkTask()).execute(getActivity());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        GridLayoutManager gridLayoutManager = (GridLayoutManager) ((RecyclerView) getActivity().findViewById(R.id.list)).getLayoutManager();
        DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();
        int columns = displayMetrics.widthPixels / getActivity().getResources().getDimensionPixelSize(R.dimen.list_item_width);
        if(columns < 2){
            columns = 2;
        }
        gridLayoutManager.setColumns(columns);
    }

    public class QueryPageTask extends AsyncTask<String, Void, ViewModel[]>{

        @Override
        protected void onPreExecute() {
            ((AppCompatActivity)getActivity()).setSupportProgressBarIndeterminateVisibility(true);
            super.onPreExecute();
        }

        @Override
        protected ViewModel[] doInBackground(String... params) {
            List<ViewModel> list = Parser.getInstance().parseList(params[0]);
            if(list == null) return null;
            return list.toArray(new ViewModel[list.size()]);
        }

        @Override
        protected void onPostExecute(ViewModel[] viewModels) {
            if(viewModels != null){
                for(ViewModel m: viewModels){
                    adapter.add(m, adapter.getItemCount());
                }
            }else{
                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.connection_error_title))
                        .setMessage(getString(R.string.connection_error_message))
                        .setPositiveButton(getString(R.string.connection_error_button), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                (new QueryPageTask()).execute(mUrl);
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                getActivity().finish();
                            }
                        })
                        .show();
            }
            if(getActivity() != null)
                ((AppCompatActivity)getActivity()).setSupportProgressBarIndeterminateVisibility(false);
            super.onPostExecute(viewModels);
        }
    }

    public class QueryBookmarkTask extends AsyncTask<Context, Void, ViewModel[]>{

        @Override
        protected void onPreExecute() {
            ((AppCompatActivity)getActivity()).setSupportProgressBarIndeterminateVisibility(true);
            super.onPreExecute();
        }

        @Override
        protected ViewModel[] doInBackground(Context... params) {
            BookmarkManager bookmarks = new BookmarkManager(params[0]);
            List<ViewModel> list = new ArrayList<ViewModel>();
            for(int i = 0; i < bookmarks.size(); i++){
                BookmarkManager.Bookmark b = bookmarks.get(i);
                if(!b.isInternal()) {
                    Parser p = Parser.selectByParserId(getActivity(), b.getParserId());
                    list.add(p.loadDetail(b.getUrl()));
                }
            }
            return list.toArray(new ViewModel[list.size()]);
        }

        @Override
        protected void onPostExecute(ViewModel[] viewModels) {
            if(viewModels != null){
                for(ViewModel m: viewModels){
                    adapter.add(m, adapter.getItemCount());
                }
            }else{
                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.connection_error_title))
                        .setMessage(getString(R.string.connection_error_message))
                        .setPositiveButton(getString(R.string.connection_error_button), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                (new QueryPageTask()).execute(mUrl);
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                getActivity().finish();
                            }
                        })
                        .show();
            }
            if(getActivity() != null)
                ((AppCompatActivity)getActivity()).setSupportProgressBarIndeterminateVisibility(false);
            super.onPostExecute(viewModels);
        }
    }
}
