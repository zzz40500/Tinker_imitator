package com.dim.common;

/**
 * Bsdiff <br/>
 * Created by dim on 2016-07-12.
 */
public class Bsdiff {

    static {
        System.loadLibrary("bsdiff");
    }
    public native static int bspatch(String old, String combine, String patch);
}
