package com.ov3rk1ll.kinocast.api.mirror;

import com.ov3rk1ll.kinocast.ui.DetailActivity;

public class Host {
    protected int id;
    protected int mirror;
    protected String name;
    protected String url;

    public static Host selectById(int id){
        switch (id){
            case DivxStage.HOST_ID: return new DivxStage(id);
            case NowVideo.HOST_ID: return new NowVideo(id);
            case SharedSx.HOST_ID: return new SharedSx(id);
            case Sockshare.HOST_ID: return new Sockshare(id);
            case StreamCloud.HOST_ID: return new StreamCloud(id);
            case Vodlocker.HOST_ID: return new Vodlocker(id);
        }
        return new Host(id);
    }

    public Host(int id){
        this.id = id;
    }

    public Host(int id, String name, int mirror){
        this.id = id;
        this.name = name;
        this.mirror = mirror;
    }

    public boolean isEnabled(){
        return false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMirror() {
        return mirror;
    }

    public void setMirror(int mirror) {
        this.mirror = mirror;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getVideoPath(DetailActivity.QueryPlayTask queryTask){
        return null;
    }

    @Override
    public String toString() {
        return name + " #" + mirror;
    }
}
