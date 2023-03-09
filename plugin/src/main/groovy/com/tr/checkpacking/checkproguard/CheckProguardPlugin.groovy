package com.tr.checkpacking.checkproguard

import com.tr.checkpacking.utils.ProjectUtils
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 检查混淆文件
 * 对比宿主存在的混淆白名单，打印改动的混淆
 * 请各端开发在确认无误后手动更新白名单
 * 已处理注释和空行，所以可以直接复制build下的混淆全量文件至白名单
 */
class CheckProguardPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        applyExtension(project)
        applyTask(project)
    }

    private static void applyExtension(Project project) {
        ProjectUtils.createExtension(project, ProguardExtension.EXTENSION_NAME, ProguardExtension.class)
    }

    private static void applyTask(Project project) {
        ProguardExtension proguardExtension = ProjectUtils.findExtension(project, ProguardExtension.class)
        if (proguardExtension.pluginState) {
            project.afterEvaluate {
                project.android.applicationVariants.all { variant ->
                    def variantName = variant.name.capitalize()
                    if (variantName.endsWith("Release")) {
                        // 检查混淆白名单
                        CheckProguardTask task = project.tasks.create("check${variantName}Proguard", CheckProguardTask.class)
                        task.setGroup("混淆变更检查")
                        task.newProguardPath = proguardExtension.newProguardPath
                        task.whiteProguardPath = proguardExtension.whiteProguardPath
                        task.applicationId = project.android.defaultConfig.applicationId
                        task.enableSendDingtalk = proguardExtension.enableSendDingTalk
                        task.enableStopTask = proguardExtension.enableStopTask()

                        def assemble = project.tasks.findByName("assemble${variantName}")
                        assemble.finalizedBy(task)

                        // 更新混淆白名单
                        UpdateProguardTask updateProguardTask = project.tasks.create("update${variantName}Proguard", UpdateProguardTask.class)
                        updateProguardTask.setGroup("更新混淆白名单")
                        updateProguardTask.newProguardPath = proguardExtension.newProguardPath
                        updateProguardTask.whiteProguardPath = proguardExtension.whiteProguardPath
                        updateProguardTask.dependsOn assemble
                    }
                }
            }
        }
    }
}