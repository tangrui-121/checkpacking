package com.tr.checkpacking.checkpermissions

import com.tr.checkpacking.utils.ProjectUtils
import com.tr.checkpacking.utils.StringUtils
import com.tr.checkpacking.utils.SystemUtils
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 检查权限
 * 对比宿主存在的权限白名单，打印改动的权限
 * 请各端开发在确认无误后手动更新白名单
 */
class CheckPermissionsPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        applyExtension(project)
        applyTask(project)
    }

    private static void applyExtension(Project project) {
        ProjectUtils.createExtension(project, PermissionsExtension.EXTENSION_NAME, PermissionsExtension.class)
    }

    private static void applyTask(Project project) {
        PermissionsExtension permissionsExtension = ProjectUtils.findExtension(project, PermissionsExtension.class)
        if (permissionsExtension.pluginState) {
            project.afterEvaluate {
                def apkAnalyzer = findApkAnalyzer(project)
                if (apkAnalyzer == null) {
                    System.err.println("未找到apkanalyzer，将不进行权限检查，请检查ANDROID_HOME环境变量")
                    return
                }
                project.android.applicationVariants.all { variant ->
                    def variantName = variant.name.capitalize()
                    File apkFile = variant.outputs[0].outputFile

                    CheckPermissionsTask task = project.tasks.create("check${variantName}Permissions", CheckPermissionsTask.class)
                    task.setGroup("权限变更检查")
                    task.configureExec(apkAnalyzer,
                            apkFile,
                            permissionsExtension.whitePermissionsName,
                            project.android.defaultConfig.applicationId,
                            permissionsExtension.enableSendDingTalk,
                            permissionsExtension.enableStopTask())

                    def assemble = project.tasks.findByName("assemble${variantName}")
                    assemble.finalizedBy(task)// 打包完成后执行权限检查
                }
            }
        }
    }

    private static File findApkAnalyzer(Project project) {
        def properties = new Properties()
        try {
            def stream = new File(project.getRootDir(), "local.properties")
                    .newInputStream()
            properties.load(stream)
            stream.close()
        } catch (IOException ignore) {
        }
        def androidHome = properties.getProperty("sdk.dir")
        if (StringUtils.isEmpty(androidHome)) {
            androidHome = SystemUtils.getenv("ANDROID_HOME")
        }
        if (StringUtils.isEmpty(androidHome)) {
            return null
        }
        String[] paths = ["cmdline-tools/latest/bin/apkanalyzer", "tools/bin/apkanalyzer"]
        for (String path : paths) {
            def file = new File(androidHome, SystemUtils.isWin() ? path + ".bat" : path)
            if (file.exists()) {
                return file
            }
        }
        return null
    }

}