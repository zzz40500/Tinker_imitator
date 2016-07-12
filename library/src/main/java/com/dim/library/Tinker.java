package com.dim.library;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.text.TextUtils;

import com.dim.common.Bsdiff;
import com.dim.common.Logger;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Tinker <br/>
 * Created by dim on 2016-07-09.
 */
public class Tinker {

    private static final String DEX_OPT_DIR = "patchOpt";
    private static final String TAG = "Tinker";
    private static String sHotPath;
    private static String sDexPath;

    private static Application mApplication;


    public static void init(Application application) {
        mApplication = application;
        sHotPath = Environment.getExternalStorageDirectory() + "/" + "and" + "/" + mApplication.getPackageName() + "/hot";
        sDexPath = Environment.getExternalStorageDirectory() + "/" + "and" + "/" + mApplication.getPackageName() + "/dex";
        iniFiles();
        mountAllDex();
    }

    private static void mountAllDex() {
        File[] files = new File(sHotPath).listFiles();
        if (files != null) {
            for (File file : files) {
                mountDex(file);
            }
        }

    }

    public static List<String> install() {
        File hotFile = new File(Environment.getExternalStorageDirectory() + "/hot");

        List<String> result = new ArrayList<>();
        for (File file : hotFile.listFiles()) {
            boolean install = install(file);
            if (install) {
                result.add(file.getAbsolutePath());
            }
        }
        return result;
    }

    public static boolean install(File file) {

        if (file.getName().matches("patchclasses\\d?.dex")) {
            String classDex = file.getName().substring(5);
            File oldDex = new File(sDexPath, classDex);
            File newDex = new File(sHotPath, classDex);
            if (oldDex.exists()) {
                Logger.d(TAG, oldDex.getPath());
                Logger.d(TAG, file.getPath());
                Logger.d(TAG, newDex.getPath());
                int bspatch = Bsdiff.bspatch(oldDex.getPath(), newDex.getPath(), file.getPath());
                if (bspatch == 0) {
                    Logger.d(TAG, "install success " + file);
                    file.delete();
                    return true;
                }

            }

        }
        return false;

    }

    private static void iniFiles() {
        createHotFile();
        try {
            copyDex();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createHotFile() {
        File hotFile = new File(Environment.getExternalStorageDirectory() + "/hot");
        if (!hotFile.exists()) {
            hotFile.mkdirs();
        }
        File anHot = new File(sHotPath);
        if (!anHot.exists()) {
            anHot.mkdirs();
        }
    }

    private static void copyDex() throws IOException {
        File hotFile = new File(sDexPath);
        Logger.d(TAG, "hotFilePath " + sDexPath);
        if (!hotFile.exists()) {
            hotFile.mkdirs();
        }
        if (hotFile.listFiles() != null && hotFile.listFiles().length != 0) {
            return;
        }

        String sourceApkPath = getSourceApkPath(mApplication, mApplication.getPackageName());
        JarFile jarFile = new JarFile(sourceApkPath);
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entity = entries.nextElement();
            Logger.d(TAG, entity.getName());
            if (entity.getName().matches("classes\\d?.dex")) {
                InputStream inputStream = jarFile.getInputStream(entity);
                OutputStream out = new FileOutputStream(new File(sDexPath, entity.getName()));
                IOUtils.copy(inputStream, out);
            }
        }
    }


    private static String getSourceApkPath(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName))
            return null;

        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(packageName, 0);
            return appInfo.sourceDir;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static void mountDex(File file) {
        File dexOptDir = new File(mApplication.getFilesDir(), DEX_OPT_DIR);
        dexOptDir.mkdir();
        try {
            DexUtils.injectDexAtFirst(file.getPath(), dexOptDir.getAbsolutePath());
            Logger.d(TAG, "loadPatch success:" + file);
        } catch (Exception e) {
            Logger.e(TAG, "inject " + file + " failed");
        }
    }

}
