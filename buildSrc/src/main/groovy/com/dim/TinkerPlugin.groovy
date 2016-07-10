package com.dim

import com.android.build.api.transform.Format
import com.android.build.api.transform.Transform
import com.android.build.gradle.internal.transforms.DexTransform
import com.dim.bean.Dex
import com.dim.bean.DexHolder
import com.dim.bean.HashRecord
import com.dim.bean.Patch
import com.dim.common.IoUtils
import com.dim.common.Logger
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 * Created by dim on 16/7/9.
 */
class TinkerPlugin implements Plugin<Project> {

    private static final String BASIS_DIR = "basis"
    File basisDir

    File getBasisDir(Project project) {
        if (project.hasProperty(BASIS_DIR)) {
            def file = new File(project.properties.get(BASIS_DIR) as String);
            if (!file.exists() || file.isDirectory()) {
                return null;
            } else {
                return file;
            }
        }
        return null;

    }

    Project project;

    @Override
    void apply(Project project) {
        this.project = project;
        project.afterEvaluate {
            project.android.applicationVariants.each { variant ->

                basisDir = getBasisDir(project);
                //todo versionCode 暂时从defaultConfig 中获取
                Patch patch = new Patch(project.projectDir.getAbsolutePath(), project.android.defaultConfig.versionCode + "", variant.name, (basisDir != null && basisDir.exists()));

                def transformClassesWithJarMergingFor = project.tasks["transformClassesWithJarMergingFor${variant.name.capitalize()}"];

                if (transformClassesWithJarMergingFor) {
                    Transform transform = transformClassesWithJarMergingFor.transform
                    def outputProvider = transformClassesWithJarMergingFor.outputStream.asOutput()
                    String mergedJar = outputProvider.getContentLocation("combined",
                            transform.getOutputTypes(),
                            transform.getScopes(), Format.JAR)
                    Logger.dim(mergedJar);
                    patch.setCombinedJar(mergedJar);
                }

                def transformClassesWithDexFor = project.tasks["transformClassesWithDexFor${variant.name.capitalize()}"];
                if (transformClassesWithDexFor) {
                    DexTransform dexTransform = transformClassesWithDexFor.transform
                    if (dexTransform) {
                        transformClassesWithDexFor.doFirst {
                            if (!patch.mainDexListFile.exists()) {
                                // 记录hash值;
                                Logger.dim("记录hash值")
                                if (!patch.hashFile.exists()) {
                                    patch.hashFile.createNewFile();
                                } else {
                                    patch.hashFile.delete();
                                    patch.hashFile.createNewFile();
                                }
                                dexTransform.secondaryFileInputs
                                Set<String> addParams = new HashSet<>();
                                File fileAdtMainList = dexTransform.mainDexListFile

                                addParams.add("--main-dex-list=" + fileAdtMainList.absolutePath);
                                addParams.add("--minimal-main-dex");
                                //每个dex 最多40k 为后面添加留下空间.
                                addParams.add("--set-max-idx-number=400000");
                                addParams.add("--verbose");
                                // 替换 AndroidBuilder
                                MultiDexAndroidBuilder.proxyAndroidBuilder(dexTransform,
                                        addParams, patch)
                                processJar(patch.hashFile, new File(patch.combinedJar));
                                project.copy {
                                    from fileAdtMainList
                                    into patch.basisFile
                                }
                            } else {

                                Logger.dim("----开始--")
                                File fileAdtMainList = dexTransform.mainDexListFile
                                HashRecord hashRecord = new HashRecord(patch.hashFile);
                                DexHolder classHolder = new DexHolder(patch.dexInfoFile);
                                Dex cl = new Dex(fileAdtMainList);
                                classHolder.setMainClass(cl);
                                createDex(patch, classHolder, hashRecord, new File(patch.combinedJar));
                            }
                        }

                    }
                }
            }
        }

    }

    def createDex(Patch patch, DexHolder classHolder, HashRecord hashRecord, File jarFile) {
        if (jarFile) {
            Logger.dim("createDex:" + jarFile.absolutePath)
            def file = new JarFile(jarFile);
            DexHolder.Entity mainEntity = classHolder.getMainClassDex();
            Enumeration enumeration = file.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement();
                String entryName = jarEntry.getName();
                InputStream inputStream = file.getInputStream(jarEntry);
                def byteArray = IOUtils.toByteArray(inputStream, jarEntry.size);
                def hash = DigestUtils.shaHex(byteArray);
                DexHolder.Entity dexEntity = classHolder.dexEntityOfClassName(entryName);
                if (dexEntity == null) {
                    dexEntity = mainEntity;
                }
                File targetFile = new File(patch.getPatchPath() + File.separator
                        + dexEntity.dexName + File.separator + entryName);
                IoUtils.mkdir(targetFile);
                FileUtils.writeByteArrayToFile(targetFile, byteArray);
                if (hashRecord.hasChange(entryName, hash)) {
                    dexEntity.hasChange = true;
                }
                inputStream.close();
            }
            file.close();
        }

        classHolder.mCLASSList.values().each {

            if (it.hasChange) {
                dex(project, new File(patch.getPatchPath() + File.separator
                        + it.dexName), patch.getPatchPath());

//                new File(patch.dexInfoFile.absolutePath + File.separator + "classes.dex").renameTo((it.dexName + ".dex"));
//                Logger.dim(patch.dexInfoFile.absolutePath + File.separator + it.dexName + ".dex");
//                Logger.dim(patch.getPatchPath() + File.separator + it.dexName + ".dex")
//                Logger.dim(patch.getPatchPath() + File.separator + it.dexName + "patch.dex")
//                DiffUtils.genDiff(patch.dexInfoFile.absolutePath + File.separator + it.dexName + ".dex",
//                        patch.getPatchPath() + File.separator + it.dexName + ".dex",
//                        patch.getPatchPath() + File.separator + it.dexName + "patch.dex");

            }
        }


    }


    public static dex(Project project, File classDir, String patchFileName) {
        if (classDir.listFiles().size()) {
            def sdkDir

            Properties properties = new Properties()
            File localProps = project.rootProject.file("local.properties")
            if (localProps.exists()) {
                properties.load(localProps.newDataInputStream())
                sdkDir = properties.getProperty("sdk.dir")
            } else {
                sdkDir = System.getenv("ANDROID_HOME")
            }
            if (sdkDir) {
                def cmdExt = Os.isFamily(Os.FAMILY_WINDOWS) ? '.bat' : ''
                def stdout = new ByteArrayOutputStream()
                project.exec {
                    commandLine "${sdkDir}/build-tools/${project.android.buildToolsVersion}/dx${cmdExt}",
                            '--dex',
                            "--output=${patchFileName}",
                            "${classDir.absolutePath}"
                    standardOutput = stdout
                }
                println(" \"${sdkDir}/build-tools/${project.android.buildToolsVersion}/dx${cmdExt}" +
                        "                            '--dex',\n" +
                        "                            \"--output=${patchFileName}\",\n" +
                        "                            \"${classDir.absolutePath}\"")
                def error = stdout.toString().trim()
                if (error) {
                    println "dex error:" + error
                }
            } else {
                throw new InvalidUserDataException('$ANDROID_HOME is not defined')
            }
        }
    }

    public void processJar(File hashFile, File jarFile) {
        if (jarFile) {
            Logger.dim("processJar:" + jarFile.absolutePath)
            def file = new JarFile(jarFile);
            Enumeration enumeration = file.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement();
                String entryName = jarEntry.getName();
                InputStream inputStream = file.getInputStream(jarEntry);
                def hash = DigestUtils.shaHex(IOUtils.toByteArray(inputStream, jarEntry.size));
                if (hashFile != null) {
                    hashFile.append(HashRecord.getItemString(entryName, hash))
                }
                inputStream.close();
            }
            file.close();
        }
    }

}
