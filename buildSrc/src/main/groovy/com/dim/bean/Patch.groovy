package com.dim.bean

import com.dim.common.IoUtils
/**
 * PatchBean <br/>
 * Created by dim on 2016-06-19.
 */
public class Patch {


    public static final String ROOT_NAME = "patch";
    public static final String PATCH_FILE_NAME = "patch";
    public static final String MAPPING_FILE_NAME = "mapping.txt";
    public static final String MAIN_DEX_LIST_FILE_NAME = "maindexlist.txt";
    public static final String HASH_FILE_NAME = "hash.txt";
    public static final String DEX_FILE_NAME = "dex.txt";
    public static final String BASIS_FILE_NAME = "basis";

    String version;
    String variantName;
    String appModuleFile;
    String combinedJar;
    String patchSerialNumber;
    String patchPath;
    boolean buildPatch;


    Patch(String appModuleFile, String version, String variantName, boolean buildPatch) {
        this.buildPatch = buildPatch
        this.appModuleFile = appModuleFile
        this.version = version
        this.variantName = variantName
    }

    public String getRootPath() {

        return appModuleFile + File.separator + ROOT_NAME;
    }

    public String getVersionFileName() {
        return "version_" + version;
    }

    public File getRootFile() {
        return new File(getRootPath());
    }

    private String getVariantFilePath() {
        return getRootPath() + File.separator + getVersionFileName() +
                File.separator + variantName;
    }

    public File getVariantFile() {
        File file = new File(getVariantFilePath());
        IoUtils.mkdir(file);
        return file;
    }


    public String getPatchSerialNumber() {
        if (patchSerialNumber == null) {
            patchSerialNumber = System.currentTimeMillis() + "";
        }
        return patchSerialNumber;
    }

    void setPatchSerialNumber(String patchSerialNumber) {
        this.patchSerialNumber = patchSerialNumber
    }


    public String getPatchPath() {
        if (patchPath == null) {
            patchPath = getVariantFilePath() + File.separator + PATCH_FILE_NAME + getPatchSerialNumber();
        }
        return patchPath;
    }

    public File getPatchFile() {
        File file = new File(getPatchPath());
        if (buildPatch)
            IoUtils.mkdir(file);
        return file;
    }

    public File getConfigFile() {
        File file = new File(getPatchPath() + File.separator + "config.txt");
        if (buildPatch)
            IoUtils.mkdir(file);
        return file;
    }

    public String getRFilePath() {
        return getVariantFilePath() + File.separator + BASIS_FILE_NAME + File.separator + "r"
    }

    public File getBasisFile() {
        return new File(getVariantFilePath() + File.separator + BASIS_FILE_NAME)
    }

    public File getRFile() {
        File file = new File(getRFilePath());
        IoUtils.mkdir(file);
        return file;
    }

    public File getMappingFile() {
        File file = new File(getVariantFilePath() + File.separator + BASIS_FILE_NAME + File.separator + MAPPING_FILE_NAME);
        IoUtils.mkdir(file);
        return file;
    }

    public File getMainDexListFile() {
        File file = new File(getMainDexListPath());
        IoUtils.mkdir(file);
        return file;
    }

    public String getMainDexListPath() {

        return getVariantFilePath() + File.separator + BASIS_FILE_NAME + File.separator + MAIN_DEX_LIST_FILE_NAME;
    }


    public File getDexInfoFile() {
        File file = new File(getVariantFilePath() + File.separator + BASIS_FILE_NAME + File.separator + DEX_FILE_NAME);
        IoUtils.mkdir(file);
        return file
    }

    public File getHashFile() {
        File file = new File(getVariantFilePath() + File.separator + BASIS_FILE_NAME + File.separator + HASH_FILE_NAME);
        IoUtils.mkdir(file);
        return file;
    }


    public String setCombinedJar(String combinedJar) {
        this.combinedJar = combinedJar;
    }

    String getCombinedJar() {
        return combinedJar
    }


    public File getPluginConfigFile() {
        File file = new File(getVariantFilePath() + File.separator + BASIS_FILE_NAME + File.separator + "config.json");
        IoUtils.mkdir(file);
        return file;
    }

    File getClassFile() {
        File file = new File(getVariantFilePath() + File.separator + BASIS_FILE_NAME + File.separator + "dex");
        IoUtils.mkdir(file);
        return file;
    }
}
