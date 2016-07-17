package com.dim.tinkerimitator;

import android.support.multidex.MultiDexApplication;

import com.dim.library.Tinker;

/**
 * App
 * Created by dim on 2016-07-09.
 */
public class App extends MultiDexApplication {

    private static final String TAG = "App";

    @Override
    public void onCreate() {
        super.onCreate();
        Tinker.init(this);
        Tinker.setBackgroundPolicy(new Tinker.BackgroundPolicy() {
            @Override
            public boolean isReadyForFix() {
                return true;
            }
        });
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Tinker.onTrimMemory(level);
    }
}
