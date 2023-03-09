package com.tr.checkpacking.checkdependencies

import com.tr.checkpacking.utils.DingDingUtils
import com.tr.checkpacking.utils.ProjectUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration

/**
 * 检查依赖
 * 对比宿主存在的依赖白名单
 * 在检查完毕之后调用更新依赖白名单task更新白名单
 */
class CheckDependenciesPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        applyExtension(project)
        applyTask(project)
    }

    private static void applyExtension(Project project) {
        ProjectUtils.createExtension(project, DependenciesExtension.EXTENSION_NAME, DependenciesExtension.class)
    }

    private static void applyTask(Project project) {
        DependenciesExtension dependenciesExtension = ProjectUtils.findExtension(project, DependenciesExtension.class)
        if (dependenciesExtension.pluginState) {
            project.afterEvaluate {

                project.android.applicationVariants.all { variant ->
                    if (variant.name.capitalize().endsWith("Release")) {
                        // 更新依赖白名单
                        project.tasks.create(name: "updateWhiteDependencies${variant.name.capitalize()}", group: "更新依赖白名单") {
                            doLast {
                                Configuration configuration
                                try {
                                    // 3.x
                                    configuration = project.configurations."${variant.name}CompileClasspath"
                                } catch (Exception e) {
                                    // 2.x
                                    configuration = project.configurations."_${variant.name}Compile"
                                }

                                StringBuffer buffer = new StringBuffer()
                                configuration.resolvedConfiguration.lenientConfiguration.allModuleDependencies.each {
                                    def identifier = it.module.id
                                    buffer.append("----------------------------------------------------\n")
                                    buffer.append("具体依赖：${identifier.group}:${identifier.name}:${identifier.version}\n")
                                    it.parents.each { parent ->
                                        buffer.append("parent：${parent.name}\n")
                                    }

                                    new File(dependenciesExtension.whiteDependenciesName).withWriter { writer ->
                                        writer.writeLine buffer.toString()
                                        writer.close()
                                    }
                                }
                                println("已更新依赖白名单")
                            }
                        }

                        // 依赖变更检查
                        Task task = project.tasks.create(name: "showDependencies${variant.name.capitalize()}", group: "依赖变更检查") {
                            doLast {
                                Configuration configuration
                                try {
                                    // 3.x
                                    configuration = project.configurations."${variant.name}CompileClasspath"
                                } catch (Exception e) {
                                    // 2.x
                                    configuration = project.configurations."_${variant.name}Compile"
                                }
                                def whiteDependencies = new HashMap<String, String>()
                                def value = ""
                                new File(dependenciesExtension.whiteDependenciesName).eachLine {
                                    if (it.startsWith("具体依赖：")) {
                                        value = it.replace("具体依赖：", "")
                                        whiteDependencies.put(value.substring(0, value.lastIndexOf(":")), value.substring(value.lastIndexOf(":") + 1, value.length()))
                                    }
                                }

                                def parents = new ArrayList<String>()

                                def changeList = new ArrayList<String>()
                                def addList = new ArrayList<String>()
                                configuration.resolvedConfiguration.lenientConfiguration.allModuleDependencies.each {
                                    def identifier = it.module.id
                                    def key = identifier.group + ":" + identifier.name
                                    if (whiteDependencies.containsKey(key)) {
                                        if (!whiteDependencies.get(key).equals(identifier.version)) {
                                            parents.clear()
                                            it.parents.each { parent ->
                                                if (!dependenciesExtension.mainProjectName.contains(parent.name.trim())) {
                                                    parents.add(parent.name)
                                                }
                                            }
                                            if (parents.size() > 0) {
                                                changeList.add("- U：${key}，由" + whiteDependencies.get(key) + "改为" + identifier.version)
                                                if (VersionUtil.isVersionDown(whiteDependencies.get(key),identifier.version) == 1){
                                                    changeList.add("    " + "该sdk降级了，请谨慎处理！！！！")
                                                }else if (VersionUtil.isVersionDown(whiteDependencies.get(key),identifier.version) == 0){
                                                    changeList.add("    " + "该sdk大版本相同，本次正式/快照版本切换，请谨慎处理！！！！")
                                                }
                                                changeList.add("    " + "影响范围：")
                                                parents.each {
                                                    changeList.add("    " + it)
                                                }
                                            } else {
                                                changeList.add("- U：${key}，由" + whiteDependencies.get(key) + "改为" + identifier.version + "，对其他依赖没有影响，放心使用。")
                                                if (VersionUtil.isVersionDown(whiteDependencies.get(key),identifier.version) == 1){
                                                    changeList.add("    " + "该sdk降级了，请谨慎处理！！！！")
                                                }else if (VersionUtil.isVersionDown(whiteDependencies.get(key),identifier.version) == 0){
                                                    changeList.add("    " + "该sdk大版本相同，本次正式/快照版本切换，请谨慎处理！！！！")
                                                }
                                            }
                                        }
                                        whiteDependencies.remove(key)
                                    } else {
                                        if (!dependenciesExtension.mainProjectName.contains("${key}:${identifier.version}".trim())) {
                                            addList.add("- A：${key}:" + identifier.version)
                                            addList.add("    " + "添加途径：")
                                            it.parents.each { parent ->
                                                if (dependenciesExtension.mainProjectName.contains(parent.name.trim())) {
                                                    addList.add("    " + "主项目")
                                                } else {
                                                    addList.add("    " + parent.name)
                                                }
                                            }
                                        }
                                    }
                                }

                                StringBuffer buffer = new StringBuffer()
                                if (whiteDependencies.size() > 0 || addList.size() > 0 || changeList.size() > 0) {
                                    buffer.append("#### ${applicationId} 依赖变动如下  \n  ")
                                    whiteDependencies.each {
                                        if (!dependenciesExtension.mainProjectName.contains("${it.key}:${it.value}".trim())) {
                                            buffer.append("- D：${it.key}:${it.value}  \n  ")
                                        }
                                    }
                                    addList.each {
                                        buffer.append("${it}  \n  ")
                                    }
                                    changeList.each {
                                        buffer.append("${it}  \n  ")
                                    }
                                    println(buffer.toString())
                                    if (buffer.toString().equals("#### ${applicationId} 依赖变动如下  \n  ")) {
                                        println("本次无改动")
                                        buffer.append("本次无改动")
                                    }
                                    if (dependenciesExtension.enableSendDingTalk) DingDingUtils.send2DingDing(buffer.toString())
                                }
                            }
                        }

                        def assemble = project.tasks.findByName("assemble${variant.name.capitalize()}")
                        assemble.finalizedBy(task)// 打包完成后执行依赖检查
                    }
                }
            }
        }
    }
}