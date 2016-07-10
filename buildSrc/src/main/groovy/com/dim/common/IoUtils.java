package com.dim.common;

import java.io.File;

/**
 * Created by dim on 16/6/21.
 */
public class IoUtils {


    public static void mkdir(String filePath) {
        mkdir(new File(filePath));
    }

    public static void mkdir(File file) {
        if (file.getName().contains(".")) {
            if (!file.getParentFile().exists()) {
                System.out.println(file.getParentFile().getAbsoluteFile() + " mkdir");

                file.getParentFile().mkdirs();
            }
        } else {
            if (!file.exists()) {
                System.out.println(file.getAbsoluteFile() + " mkdir");
                file.mkdirs();
            }
        }
    }

    public static void delete(File file) {
        if (file.exists()) {
            file.delete();
        }
    }
}
