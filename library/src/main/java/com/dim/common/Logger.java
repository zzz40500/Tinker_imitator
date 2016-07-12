package com.dim.common;

import android.util.Log;

/**
 * Logger <br/>
 * Created by dim on 2016-07-08.
 */
public class Logger {

    private static boolean debug = true;

    public static void d(String tag, String message) {
        if (debug) {
            Log.d(tag, message);
        }
    }

    public static void e(String tag, String message) {
        if (debug) {
            Log.d(tag, message);
        }
    }
}
