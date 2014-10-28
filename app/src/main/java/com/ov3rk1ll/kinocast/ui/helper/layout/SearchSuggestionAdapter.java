package com.ov3rk1ll.kinocast.ui.helper.layout;


import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.widget.FilterQueryProvider;

import com.ov3rk1ll.kinocast.api.Parser;

public class SearchSuggestionAdapter extends SimpleCursorAdapter {

    public SearchSuggestionAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);

        setFilterQueryProvider(new FilterQueryProvider() {
            public Cursor runQuery(CharSequence constraint) {
                MatrixCursor matrixCursor = new MatrixCursor(new String[] { "_id", "item" });

                if(constraint == null) return matrixCursor;

                String suggestions[] = Parser.getInstance().getSearchSuggestions(constraint.toString());
                if(suggestions == null) return matrixCursor;

                for (int i = 0; i < suggestions.length; i++) {
                    matrixCursor.addRow(new Object[]{(i + 1), suggestions[i]});
                }

                return matrixCursor;
            }
        });
    }

}
