package com.example.amit.miniflickr;

import android.util.Log;

import java.io.Serializable;

/**
 * Created by Amit on 1/7/2018.
 */

public class GalleryItem implements Serializable {
    private String id;
    private String secret;
    private String server;
    private String farm;

    public GalleryItem(String id, String secret, String server, String farm) {
        this.id = id;
        this.secret = secret;
        this.server = server;
        this.farm = farm;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        Log.d("GalleryItem", "-------------getUrl-------: http://farm" + farm + ".static.flickr.com/" + server + "/" + id + "_" + secret + ".jpg" );

        return "http://farm" + farm + ".static.flickr.com/" + server + "/" + id + "_" + secret + ".jpg" ;
    }

}
