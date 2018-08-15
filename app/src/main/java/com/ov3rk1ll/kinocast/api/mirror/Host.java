package com.ov3rk1ll.kinocast.api.mirror;

import com.ov3rk1ll.kinocast.ui.DetailActivity;

import java.lang.reflect.InvocationTargetException;

public abstract class Host {
    protected int mirror;
    protected String url;

    public static Class<?>[] HOSTER_LIST = {
            DivxStage.class,
            NowVideo.class,
            SharedSx.class,
            Sockshare.class,
            StreamCloud.class,
            Vodlocker.class,
            StreamCherry.class
    };

    public static Host selectById(int id){
        for (Class<?> h: HOSTER_LIST) {
            try {
                Host host = (Host) h.getConstructor().newInstance();
                if(host.getId() == id){
                    return host;
                }
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public Host(){

    }

    public Host(int mirror){
        this.mirror = mirror;
    }

    public boolean isEnabled(){
        return false;
    }

    public abstract int getId();
    public abstract String getName();

    public int getMirror() {
        return mirror;
    }

    public void setMirror(int mirror) {
        this.mirror = mirror;
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
        return getName() + " #" + mirror;
    }
}
