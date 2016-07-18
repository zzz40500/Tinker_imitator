package com.dim.library;

import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.dim.common.Logger;

/**
 * Created by dim on 16/7/17.
 */
public class NoneService extends android.app.Service {

    private static final String TAG = "Service";
    @Override
    public void onCreate() {
        super.onCreate();
        Logger.d(TAG, "onCreate: " );
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
