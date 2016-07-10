package com.dim.bean;

import org.apache.commons.io.FileUtils

/**
 * Dex <br/>
 * Created by dim on 2016-07-10.
 */
public class Dex {


    public Set<String> classs = new HashSet<>();


    public Dex(File patch) {
        try {
            String s = FileUtils.readFileToString(patch);
            String[] split = s.split("\n");
            for (String s1 : split) {
                if (s1 != null && s1.length() > 0) {
                    classs.add(s1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Dex() {
    }

    public void addClassItem(String item) {
        classs.add(item);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dex)) return false;

        Dex aClass = (Dex) o;

        return classs != null ? classs.equals(aClass.classs) : aClass.classs == null;

    }

    public Object[] getAll() {
        return classs.toArray();
    }

    public boolean contains(String name) {
        return classs.contains(name);
    }

    public void remove(String className) {
        classs.remove(className);
    }
}
