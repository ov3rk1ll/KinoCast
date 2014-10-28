package com.ov3rk1ll.kinocast.data;

public class Season {
    public int id;
    public String name;
    public String[] episodes;

    public Season() { }

    @Override
    public String toString() {
        return this.name;
    }
}
