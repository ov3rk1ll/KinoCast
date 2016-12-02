package com.ov3rk1ll.kinocast.api.mirror;

import com.ov3rk1ll.kinocast.ui.DetailActivity;

import java.lang.reflect.InvocationTargetException;

public class Host {
    protected int id;
    protected int mirror;
    protected String name;
    protected String url;

    public static Class<?>[] HOSTER_LIST = {
            DivxStage.class,
            NowVideo.class,
            SharedSx.class,
            Sockshare.class,
            StreamCloud.class,
            Vodlocker.class,
    };

    public static Host selectById(int id){
        for (Class<?> h: HOSTER_LIST) {
            try {
                Host host = (Host) h.getConstructor(int.class).newInstance(id);
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