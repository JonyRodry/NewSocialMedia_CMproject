package com.example.cmproject;

import android.graphics.Bitmap;

import androidx.collection.LruCache;
import androidx.lifecycle.ViewModel;

public class MyViewModel extends ViewModel {

    private User userLoggedIn;
    private User userForProfile;
    private String page;
    private LruCache<String, Bitmap> memoryCache;

    public User getUserLoggedIn() {
        return userLoggedIn;
    }

    public void setUserLoggedIn(User userLoggedIn) {
        this.userLoggedIn = userLoggedIn;
    }

    public User getUserForProfile() {
        return userForProfile;
    }

    public void setUserForProfile(User userForProfile) {
        this.userForProfile = userForProfile;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public void setMemoryCache(LruCache<String, Bitmap> memoryCache) {
        this.memoryCache = memoryCache;
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        memoryCache.put(key, bitmap);
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return memoryCache.get(key);
    }
}
