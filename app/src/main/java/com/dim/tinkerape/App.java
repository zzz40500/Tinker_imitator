package com.dim.tinkerape;

import android.support.multidex.MultiDexApplication;

import com.dim.library.Tinker;

/**
 * App <br/>
 * Created by dim on 2016-07-09.
 */
public class App extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        Tinker.init(this);
    }
}
