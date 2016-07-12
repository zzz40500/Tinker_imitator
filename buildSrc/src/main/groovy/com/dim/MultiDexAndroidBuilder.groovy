package com.dim

import com.android.build.gradle.internal.transforms.DexTransform
import com.android.builder.core.AndroidBuilder
import com.android.builder.core.DexOptions
import com.android.builder.core.ErrorReporter
import com.android.ide.common.blame.ParsingProcessOutputHandler
import com.android.ide.common.process.JavaProcessExecutor
import com.android.ide.common.process.ProcessException
import com.android.ide.common.process.ProcessExecutor
import com.android.ide.common.process.ProcessOutputHandler
import com.android.utils.ILogger
import com.dim.bean.Patch
import com.dim.common.Logger

import java.lang.reflect.Field

/**
 * proxy the androidBuilder that plugin 1.5.0 to add '--minimal-main-dex' options.
 *
 * @author ceabie
 */
public class MultiDexAndroidBuilder extends AndroidBuilder {

    Set<String> mAddParams;
    Patch patch

    public MultiDexAndroidBuilder(String projectId,
                                  String createdBy,
                                  ProcessExecutor processExecutor,
                                  JavaProcessExecutor javaProcessExecutor,
                                  ErrorReporter errorReporter,
                                  ILogger logger,
                                  boolean verboseExec, Set<String> addParams, Patch patch) {
        super(projectId, createdBy, processExecutor, javaProcessExecutor, errorReporter, logger, verboseExec)
        this.mAddParams = addParams;
        this.patch = patch;
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
        Logger.dim("mainDexList : " + mainDexList.absolutePath);
        if (mAddParams != null) {
            if (additionalParameters == null) {
                additionalParameters = []
            }
            mAddParams.each {
                Logger.dim("additionalParameters ---- " + it)
                additionalParameters += it //'--minimal-main-dex'
            }
        }
        if (patch == null) {
            super.convertByteCode(inputs, outDexFolder, multidex, mainDexList, dexOptions,
                    additionalParameters, incremental, optimize, processOutputHandler);
        } else {
            HookProcessOutputHandler hookProcessOutputHandler = new HookProcessOutputHandler(processOutputHandler as ParsingProcessOutputHandler, patch, outDexFolder);

            super.convertByteCode(inputs, outDexFolder, multidex, mainDexList, dexOptions,
                    additionalParameters, incremental, optimize, hookProcessOutputHandler)
        }
    }


    public static void proxyAndroidBuilder(DexTransform transform, Collection<String> addParams, Patch patch1) {
        if (addParams != null && addParams.size() > 0) {
            def get = accessibleField(DexTransform.class, "androidBuilder").get(transform);
            if (!get.getClass().getSimpleName().equals("MultiDexAndroidBuilder")) {
                def builder = getProxyAndroidBuilder(get, addParams, patch1);
                accessibleField(DexTransform.class, "androidBuilder")
                        .set(transform, builder)
            }
        }
    }

    private static AndroidBuilder getProxyAndroidBuilder(AndroidBuilder orgAndroidBuilder,
                                                         Collection<String> addParams, Patch patch) {
        MultiDexAndroidBuilder myAndroidBuilder = new MultiDexAndroidBuilder(
                orgAndroidBuilder.mProjectId,
                orgAndroidBuilder.mCreatedBy,
                orgAndroidBuilder.getProcessExecutor(),
                orgAndroidBuilder.mJavaProcessExecutor,
                orgAndroidBuilder.getErrorReporter(),
                orgAndroidBuilder.getLogger(),
                orgAndroidBuilder.mVerboseExec, addParams, patch)

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
