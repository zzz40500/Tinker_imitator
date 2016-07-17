package com.dim.library;

import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.os.Environment;
import android.support.annotation.MainThread;
import android.support.annotation.RequiresPermission;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.widget.Toast;

import com.dim.common.Bsdiff;
import com.dim.common.Logger;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Permission;
import java.security.Permissions;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Tinker
 * Created by dim on 2016-07-09.
 */
public class Tinker {

    private static final String DEX_OPT_DIR = "patchOpt";
    private static final String TAG = "Tinker";
    private static String sHotPath;
    private static String sDexPath;
    private static String sUnVerifyDexPath;
    private static Application mApplication;
    private static boolean debug = true;
    private static BackgroundPolicy mBackgroundPolicy;
    private static boolean needFlush = false;


    @MainThread
    public static void init(Application application) {
        mApplication = application;
        sHotPath = application.getFilesDir() + "/" + "and" + "/" + application.getPackageName() + "/hot";
        sDexPath = application.getFilesDir() + "/" + "and" + "/" + application.getPackageName() + "/dex";
        sUnVerifyDexPath = Environment.getExternalStorageDirectory() + "/hot";
        debug = appIsDebug(application);
        Logger.setDebug(debug);
        iniFiles();
        install();
        mountAllDex();
    }

    public static void onTrimMemory(int level) {
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN && needFlush && mBackgroundPolicy != null && mBackgroundPolicy.isReadyForFix()) {
            Intent intent = new Intent(mApplication, NoneService.class);
            mApplication.startService(intent);
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }
    }

    private static void mountAllDex() {
        File[] files = new File(sHotPath).listFiles();
        if (files != null) {
            for (File file : files) {
                mountDex(file);
            }
        }
    }

    @WorkerThread
    @RequiresPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static List<String> install() {
        File hotFile = new File(sUnVerifyDexPath);
        List<String> result = new ArrayList<>();
        File[] files = hotFile.listFiles();
        if (files == null) {
            return result;
        }
        for (File file : files) {
            boolean install = install(file);
            if (install) {
                result.add(file.getAbsolutePath());
            }
        }
        return result;
    }

    @WorkerThread
    public static boolean install(File file) {
        try {
            if (file.getName().matches("patchclasses\\d?.dex")) {
                String classDex = file.getName().substring(5);
                File oldDex = new File(sDexPath, classDex);
                copyDex(classDex);
                File newDex = new File(sHotPath, classDex);
                if (oldDex.exists()) {
                    int bspatch = Bsdiff.bspatch(oldDex.getPath(), newDex.getPath(), file.getPath());
                    if (bspatch == 0) {
                        Logger.d(TAG, "install success " + file);
                        file.delete();
                        needFlush = true;
                        return true;
                    }
                }
            }
        } finally {
            File dexFile = new File(sDexPath);
            File[] files = dexFile.listFiles();
            if (files != null) {
                for (File item : files) {
                    item.delete();
                }
            }
        }
        return false;
    }

    private static void iniFiles() {
        createHotFile();
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

    private static void copyDex(String className) {
        File hotFile = new File(sDexPath);
        if (!hotFile.exists()) {
            hotFile.mkdirs();
        }
        if (new File(hotFile, className).exists()) {
            return;
        }
        try {
            String sourceApkPath = getSourceApkPath(mApplication, mApplication.getPackageName());
            JarFile jarFile = new JarFile(sourceApkPath);
            Enumeration<JarEntry> entries = jarFile.entries();
            ZipEntry entry = jarFile.getEntry(className);
            if (entry != null) {
                InputStream inputStream = jarFile.getInputStream(entry);
                OutputStream out = new FileOutputStream(new File(sDexPath, entry.getName()));
                IOUtils.copy(inputStream, out);
            }
        } catch (IOException e) {
            e.printStackTrace();
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

    private static boolean appIsDebug(Application application) {

        try {
            Class<?> aClass = Class.forName(application.getPackageName() + ".BuildConfig");
            Object debug = ReflectionUtils.getField(null, aClass, "DEBUG");
            return (boolean) debug;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        return false;
    }


    private static void mountDex(File file) {
        File dexOptDir = new File(mApplication.getFilesDir(), DEX_OPT_DIR);
        dexOptDir.mkdir();
        try {
            DexUtils.injectDexAtFirst(file.getPath(), dexOptDir.getAbsolutePath());
            Logger.d(TAG, "loadPatch success:" + file);
            if (debug) {
                Toast.makeText(mApplication, "loadPatch success: " + file.getName(), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Logger.e(TAG, "inject " + file + " failed");
            if (debug) {
                Toast.makeText(mApplication, "inject failed : " + file.getName(), Toast.LENGTH_SHORT).show();
            }
        }
    }


    public static void setBackgroundPolicy(BackgroundPolicy mBackgroundPolicy) {
        Tinker.mBackgroundPolicy = mBackgroundPolicy;
    }

    public interface BackgroundPolicy {
        boolean isReadyForFix();
    }
}
