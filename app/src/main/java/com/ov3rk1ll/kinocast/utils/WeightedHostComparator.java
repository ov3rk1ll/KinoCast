package com.ov3rk1ll.kinocast.utils;

import android.util.SparseArray;

import com.ov3rk1ll.kinocast.api.mirror.Host;

import java.util.Comparator;

/**
 * Created by sg on 07.09.2016.
 */
public class WeightedHostComparator implements Comparator<Host> {
    SparseArray<Integer> weightedList;

    public WeightedHostComparator(SparseArray<Integer> weightedList){
        this.weightedList = weightedList;
    }

    @Override
    public int compare(Host o1, Host o2) {
        if(weightedList == null) return compate(o1.getMirror(), o2.getMirror());
        int w1 = weightedList.get(o1.getId(), o1.getId());
        int w2 = weightedList.get(o2.getId(), o2.getId());
        if(w1 == w2){ // Same host, sort by mirror
            compate(o1.getMirror(), o2.getMirror());
        }
        return compate(w1, w2);
    }

    private int compate(int x, int y){
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }
}
