package com.dim.bean;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * HashRecord <br/>
 * Created by dim on 2016-07-10.
 */
public class HashRecord {


    public Map<String, String> map = new HashMap<>();


    public static String getItemString(String path, String hash) {
        return String.format("%s:%s\n", path, hash);
    }

    public HashRecord(File file) {
        try {
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            while ((line = reader.readLine()) != null) {
                String[] split = line.split(":");
                if (split.length == 2) {
                    map.put(split[0], split[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean hasChange(String name, String hash) {
        String values = map.get(name);
        if (values != null) {
            return !values.equals(hash);
        }
        return false;
    }
}
