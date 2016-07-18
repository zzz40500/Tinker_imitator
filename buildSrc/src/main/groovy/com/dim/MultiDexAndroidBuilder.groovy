package com.dim

import com.android.annotations.NonNull
import com.android.build.gradle.internal.transforms.DexTransform
import com.android.builder.core.AndroidBuilder
import com.android.builder.core.DexOptions
import com.android.builder.core.DexProcessBuilder
import com.android.builder.core.ErrorReporter
import com.android.ide.common.blame.ParsingProcessOutputHandler
import com.android.ide.common.process.JavaProcessExecutor
import com.android.ide.common.process.ProcessException
import com.android.ide.common.process.ProcessExecutor
import com.android.ide.common.process.ProcessOutputHandler
import com.android.utils.ILogger
import com.dim.bean.Patch
import com.dim.common.Logger
import com.google.common.collect.ImmutableList

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

import static com.android.SdkConstants.DOT_CLASS
import static com.android.SdkConstants.DOT_DEX

/**
 * proxy the androidBuilder that plugin 1.5.0 to add '--minimal-main-dex' options.
 *
 * @author ceabie
 */
public class MultiDexAndroidBuilder extends AndroidBuilder {

    Set<String> mAddParams;
    Patch patch
    String dxPath;

    public MultiDexAndroidBuilder(String projectId, String createdBy, ProcessExecutor processExecutor, JavaProcessExecutor javaProcessExecutor, ErrorReporter errorReporter, ILogger logger, boolean verboseExec, Set<String> addParams, Patch patch, String dxPath) {
        super(projectId, createdBy, processExecutor, javaProcessExecutor, errorReporter, logger, verboseExec)
        this.mAddParams = addParams;
        this.patch = patch;
        this.dxPath = dxPath
    }

    @Override
    public void convertByteCode(Collection<File> inputs,
                                File outDexFolder,
                                boolean multidex,
                                File mainDexList,
                                DexOptions dexOptions,
                                List<String> additionalParameters,
                                boolean incremental,
                                boolean optimize,
                                ProcessOutputHandler processOutputHandler)

            throws IOException, InterruptedException, ProcessException {
        if (mAddParams != null) {
            if (additionalParameters == null) {
                additionalParameters = []
            }
            mAddParams.each {
                additionalParameters += it //'--minimal-main-dex'
            }
        }
        if (patch == null) {
            super.convertByteCode(inputs, outDexFolder, multidex, mainDexList, dexOptions,
                    additionalParameters, incremental, optimize, processOutputHandler);
        } else {
            HookProcessOutputHandler hookProcessOutputHandler = new HookProcessOutputHandler(processOutputHandler as ParsingProcessOutputHandler, patch, outDexFolder);

            try {

                ImmutableList.Builder<File> verifiedInputs = ImmutableList.builder();
                for (File input : inputs) {
                    if (checkLibraryClassesJar(input)) {
                        verifiedInputs.add(input);
                    }
                }

                DexProcessBuilder builder = new HockDexProcessBuilder(outDexFolder,dxPath);

                builder.setVerbose(true)
                        .setIncremental(incremental)
                        .setNoOptimize(!optimize)
                        .setMultiDex(multidex)
                        .setMainDexList(mainDexList)
                        .addInputs(verifiedInputs.build());

                if (additionalParameters != null) {
                    builder.additionalParameters(additionalParameters);
                }
                def method = AndroidBuilder.class.getDeclaredMethod("runDexer", DexProcessBuilder.class, DexOptions.class, ProcessOutputHandler.class);
                method.setAccessible(true);
                method.invoke(this, builder, dexOptions, hookProcessOutputHandler)

            } catch (Exception e) {
                e.printStackTrace();
                super.convertByteCode(inputs, outDexFolder, multidex, mainDexList, dexOptions,
                        additionalParameters, incremental, optimize, hookProcessOutputHandler);
            }


        }
    }
    /**
     * Returns true if the library (jar or folder) contains class files, false otherwise.
     */
    private static boolean checkLibraryClassesJar(@NonNull File input) throws IOException {

        if (!input.exists()) {
            return false;
        }

        if (input.isDirectory()) {
            return checkFolder(input);
        }

        ZipFile zipFile = new ZipFile(input);
        try {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.endsWith(DOT_CLASS) || name.endsWith(DOT_DEX)) {
                    return true;
                }
            }
            return false;
        } finally {
            zipFile.close();
        }
    }
    /**
     * Returns true if this folder or one of its subfolder contains a class file, false otherwise.
     */
    private static boolean checkFolder(@NonNull File folder) {
        File[] subFolders = folder.listFiles();
        if (subFolders != null) {
            for (File childFolder : subFolders) {
                if (childFolder.isFile()) {
                    String name = childFolder.getName();
                    if (name.endsWith(DOT_CLASS) || name.endsWith(DOT_DEX)) {
                        return true;
                    }
                }
                if (childFolder.isDirectory()) {
                    // if childFolder returns false, continue search otherwise return success.
                    if (checkFolder(childFolder)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    public
    static void proxyAndroidBuilder(DexTransform transform, Collection<String> addParams, Patch patch1,String dxPath) {
        if (addParams != null && addParams.size() > 0) {
            def get = accessibleField(DexTransform.class, "androidBuilder").get(transform);
            if (!get.getClass().getSimpleName().equals("MultiDexAndroidBuilder")) {
                def builder = getProxyAndroidBuilder(get, addParams, patch1,dxPath);
                accessibleField(DexTransform.class, "androidBuilder")
                        .set(transform, builder)

            }
        }
    }

    private static AndroidBuilder getProxyAndroidBuilder(AndroidBuilder orgAndroidBuilder,
                                                         Collection<String> addParams, Patch patch,String dxPath) {
        MultiDexAndroidBuilder myAndroidBuilder = new MultiDexAndroidBuilder(
                orgAndroidBuilder.mProjectId,
                orgAndroidBuilder.mCreatedBy,
                orgAndroidBuilder.getProcessExecutor(),
                orgAndroidBuilder.mJavaProcessExecutor,
                orgAndroidBuilder.getErrorReporter(),
                orgAndroidBuilder.getLogger(),
                orgAndroidBuilder.mVerboseExec, addParams, patch,dxPath)

        myAndroidBuilder.setTargetInfo(
                orgAndroidBuilder.getSdkInfo(),
                orgAndroidBuilder.getTargetInfo(),
                orgAndroidBuilder.mLibraryRequests)

        myAndroidBuilder;

    }

    private static Field accessibleField(Class cls, String field) {
        Field f = cls.getDeclaredField(field)
        f.setAccessible(true)
        return f;
    }

}
