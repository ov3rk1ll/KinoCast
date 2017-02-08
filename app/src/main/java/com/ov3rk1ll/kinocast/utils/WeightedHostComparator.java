package com.ov3rk1ll.kinocast.utils;

import android.util.SparseIntArray;

import com.ov3rk1ll.kinocast.api.mirror.Host;

import java.util.Comparator;


public class WeightedHostComparator implements Comparator<Host> {
    private SparseIntArray weightedList;

    public WeightedHostComparator(SparseIntArray weightedList){
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
