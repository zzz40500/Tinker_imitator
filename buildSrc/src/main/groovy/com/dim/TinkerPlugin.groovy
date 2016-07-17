package com.dim

import com.android.build.api.transform.Format
import com.android.build.api.transform.Transform
import com.android.build.gradle.internal.transforms.DexTransform
import com.dim.bean.Config
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
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency

import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 * Created by dim on 16/7/9.
 */
class TinkerPlugin implements Plugin<Project> {

    private static final String BASIS_DIR = "basis"
    private static final String PATCH_SERIAL_NUMBER = "serial_number"
    File basisDir
    Project project;
    Map<String, Project> allProjectMap = new HashMap<>();

    public static String getProperty(Project project, String property) {
        if (project.hasProperty(property)) {
            return project.getProperties()[property];
        }
        return null;
    }

    def getAndroid() {
        return project.android
    }

    @Override
    void apply(Project project) {
        this.project = project;
        project.afterEvaluate {
            project.android.applicationVariants.each { variant ->

                String basisPath = getProperty(project, BASIS_DIR);
                if (basisPath != null) {
                    basisDir = new File(basisPath);
                }
                //todo versionCode 暂时从defaultConfig 中获取
                Patch patch = new Patch(project.projectDir.getAbsolutePath(), project.android.defaultConfig.versionCode + "", variant.name, (basisDir != null && basisDir.exists()));
                patch.setPatchSerialNumber(getProperty(project, PATCH_SERIAL_NUMBER))
                Config config = new Config(android.buildToolsVersion, android.compileSdkVersion, android.sdkDirectory.absolutePath, variant.flavorName, variant.buildType.name,);

                boolean minifyEnabled = variant.buildType.minifyEnabled
                if (minifyEnabled) {
                    String transformClassesAndResourcesWithProguardForName = "transformClassesAndResourcesWithProguardFor${variant.name.capitalize()}";
                    def transformClassesAndResourcesWithProguardForTask = project.tasks.findByName(transformClassesAndResourcesWithProguardForName);
                    if (transformClassesAndResourcesWithProguardForTask) {
                        Transform transform = transformClassesAndResourcesWithProguardForTask.transform
                        def outputProvider = transformClassesAndResourcesWithProguardForTask.outputStream.asOutput()
                        String mergedJar = outputProvider.getContentLocation("main",
                                transform.getOutputTypes(),
                                transform.getScopes(), Format.JAR)
                        patch.setCombinedJar(mergedJar);
                    } else {
                        Logger.dim("task ${transformClassesAndResourcesWithProguardForName} not found ")
                        return;
                    }
                } else {
                    def transformClassesWithJarMergingFor = project.tasks["transformClassesWithJarMergingFor${variant.name.capitalize()}"];
                    if (transformClassesWithJarMergingFor) {
                        Transform transform = transformClassesWithJarMergingFor.transform
                        def outputProvider = transformClassesWithJarMergingFor.outputStream.asOutput()
                        String mergedJar = outputProvider.getContentLocation("combined",
                                transform.getOutputTypes(),
                                transform.getScopes(), Format.JAR)
                        patch.setCombinedJar(mergedJar);
                    } else {
                        Logger.dim("task transformClassesWithJarMergingFor${variant.name.capitalize()} not found ")
                        return;
                    }
                }
                findResPath(project, config, variant)
                config.save(patch.getPluginConfigFile())
                if (basisDir != null && minifyEnabled) {
                    //适应mapping
                    String buildTypeName = variant.buildType.name;
                    def buildType = android.buildTypes.properties["asMap"][buildTypeName];
                    List<File> proguardFiles = buildType.proguardFiles;
                    if (proguardFiles.size() > 0) {

                        if (proguardFiles.size() > 1) {
                            proguardFiles.set(1, new File(basisDir.absolutePath + "/" + Patch.BASIS_FILE_NAME + "/" + "proguard-mapping.pro"));
                        } else {
                            proguardFiles.set(0, new File(basisDir.absolutePath + "/" + Patch.BASIS_FILE_NAME + "/" + "proguard-mapping.pro"));
                        }
                    }
                }

                def transformClassesWithDexFor = project.tasks.findByName("transformClassesWithDexFor${variant.name.capitalize()}");
                if (transformClassesWithDexFor) {
                    String assemblePatch = "assemble${variant.name.capitalize()}Patch";
                    Logger.dim(assemblePatch);
                    project.task(assemblePatch) << {
                    }
                    def assemblePatchTask = project.tasks[assemblePatch];
                    assemblePatchTask.dependsOn transformClassesWithDexFor;

                    DexTransform dexTransform = transformClassesWithDexFor.transform
                    if (dexTransform) {
                        transformClassesWithDexFor.doFirst {
                            if (!patch.mainDexListFile.exists()) {
                                // 记录hash值;
                                if (!patch.hashFile.exists()) {
                                    patch.hashFile.createNewFile();
                                } else {
                                    patch.hashFile.delete();
                                    patch.hashFile.createNewFile();
                                }
                                Set<String> addParams = new HashSet<>();
                                File fileAdtMainList = dexTransform.mainDexListFile
                                if (fileAdtMainList != null) {
                                    addParams.add("--main-dex-list=" + fileAdtMainList.absolutePath);

                                }
                                addParams.add("--minimal-main-dex");
                                //每个dex 最多40k 为后面添加留下空间.
                                addParams.add("--set-max-idx-number=400000");
                                addParams.add("--verbose");
                                // 替换 AndroidBuilder
                                MultiDexAndroidBuilder.proxyAndroidBuilder(dexTransform,
                                        addParams, patch)
                                if (patch.combinedJar != null) {
                                    processJar(patch.hashFile, new File(patch.combinedJar));
                                }
                                if (fileAdtMainList != null) {
                                    project.copy {
                                        from fileAdtMainList
                                        into patch.basisFile
                                    }
                                }
                                def mapFile = new File("${project.buildDir}/outputs/mapping/${variant.dirName}/mapping.txt")
                                if (mapFile.exists() && !patch.mappingFile.exists()) {
                                    FileUtils.copyFile(mapFile, patch.mappingFile)
                                    File mappingProguardFile = new File(patch.mappingFile.getParent(), "proguard-mapping.pro")
                                    FileUtils.writeStringToFile(mappingProguardFile, "-applymapping ${patch.mappingFile.absolutePath}\n-dontwarn **")
                                }
                                def rFile = new File("${project.buildDir}/generated/source/r/${variant.dirName}");
                                File targetRFile = patch.RFile;
                                if (rFile.exists() && targetRFile.listFiles() != null) {
                                    FileUtils.copyDirectory(rFile, targetRFile);
                                }
                            } else {

                                if (basisDir == null) {
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
                                            addParams, null)
                                }
                            }

                        }
                        assemblePatchTask << {
                            if (patch.getCombinedJar() != null) {
                                Logger.dim("开始生成插件")
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
                    Logger.dim("change : " + entryName)
                }
                inputStream.close();
            }
            file.close();
        }

        classHolder.dexList.values().each {
            if (it.hasChange) {
                dex(project, new File(patch.getPatchPath() + File.separator
                        + it.dexName), patch.getPatchPath());
                File old = new File(patch.patchPath + File.separator + "classes.dex");
                File rname = new File(patch.patchPath + File.separator + it.dexName + ".dex");
                boolean to = old.renameTo(rname);
                Logger.dim(" "+old.exists()+ " -> "+rname.exists() );
                Logger.dim(old.getAbsolutePath()+ " -> "+rname.getAbsolutePath() + "   "+to);
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
    /**
     * 生成配置文件,给as 插件使用.
     * @param baseVariant
     */
    def findResPath(Project project, Config config, def variant) {

        //找到本身的res   靠前资源覆盖后面的资源
        findResPath(variant, config)
        if (project.configurations.hasProperty("compile")) {
            project.configurations.compile.dependencies.findAll {
                findDependencyProject(it, config)
            }
        }
        if (project.configurations.hasProperty("${variant.name}Compile")) {
            project.configurations.getByName("${variant.name}Compile").dependencies.findAll {
                findDependencyProject(it, config)
            }
        }

    }

    private void findDependencyProject(Dependency it, Config config) {
        if (it.hasProperty('dependencyProject')) {
            def dependenciesP = allProjectMap.get(it.name);
            if (dependenciesP != null) {
                String buildType = "release";
                def configuration = it.properties.get("configuration");
                if (configuration != null && !configuration.equals("default")) {
                    buildType = configuration;
                } else {
                    if (dependenciesP.hasProperty("android")) {
                        def android = dependenciesP.properties.get("android");
                        if (android.hasProperty("defaultPublishConfig")) {
                            buildType = android.properties.get("defaultPublishConfig");
                        }
                    }

                }
                def android = dependenciesP.properties.get("android");
                if (android != null && android.hasProperty("libraryVariants")) {
                    android.libraryVariants.all { bv ->
                        if (bv.name.equals(buildType)) {
                            findResPath(dependenciesP, config, bv)
                        }
                    }
                } else {
                    //添加默认的res地址
                    config.addResPath(dependenciesP.getProjectDir().absolutePath + "/src/main/res");
                }
            }
        }
    }

    static void findResPath(def variant, Config config) {
        for (int i = variant.sourceSets.size() - 1; i >= 0; i--) {
            Collection directories = variant.sourceSets.get(i).resDirectories
            for (int j = directories.size() - 1; j >= 0; j--) {
                String resPath = directories.getAt(j).absolutePath
                config.addResPath(resPath)
            }
        }
    }

}
