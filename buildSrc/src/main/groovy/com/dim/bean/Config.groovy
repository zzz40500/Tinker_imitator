package com.dim.bean

import com.dim.common.Logger;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils

/**
 * Config <br/>
 * Created by dim on 2016-06-22.
 */
public class Config {

    private String flavor;
    private String buildType;
    public String buildToolsVersion;
    public String compileSdkVersion;
    public String sdkPath;
    private String pushTargetFilePath = "/sdcard/hot/";

    private List<String> resList = new ArrayList<>();

    public Config(String buildToolsVersion, String compileSdkVersion, String sdkPath,String flavor,String buildType ) {
        this.buildToolsVersion = buildToolsVersion;
        this.compileSdkVersion = compileSdkVersion;
        this.sdkPath = sdkPath;
        this.flavor = flavor;
        this.buildType = buildType;
    }

    public boolean addResPath(String resPath) {

        resPath = resPath.trim();
//        if (!new File(resPath).exists()) {
//            return false;
//        }
        if (resList.contains(resPath)) {
            return false;
        }
        resList.add(resPath);
        return true;
    }

    public void save(File file) {
        try {

            Logger.dim("save  ---> "+file.toString());
            FileUtils.writeStringToFile(file, toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
