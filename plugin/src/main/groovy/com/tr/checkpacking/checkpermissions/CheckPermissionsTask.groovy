package com.tr.checkpacking.checkpermissions

import com.tr.checkpacking.utils.DingDingUtils
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile

class CheckPermissionsTask extends Exec {

    public File apkAnalyzer
    private File apkFile
    private String applicationId

    public boolean enableSendDingtalk
    public boolean enableStopTask // 出现问题是否终止编译，默认为true，开发期间可直接使用默认值，测试打包时，可使用命令行设为false


    @InputFile
    public File getApkFile() {
        return apkFile
    }

    @Input
    public String getApplicationId() {
        return applicationId
    }

/**
     * 配置task
     * @param apkAnalyzer
     * @param apkFile
     * @param task
     */
    void configureExec(File apkAnalyzer, File apkFile, String whitename, String applicationId, boolean enableSendDingtalk,boolean enableStopTask) {

        this.apkAnalyzer = apkAnalyzer
        this.apkFile = apkFile
        this.applicationId = applicationId
        this.enableSendDingtalk = enableSendDingtalk
        this.enableStopTask = enableStopTask

        def whitePermissions = new LinkedHashSet<String>()
        File whitePermissionsFile = project.file(whitename)
        doFirst {
            if (!apkFile.exists()) {
                throw new IllegalArgumentException("apk文件不存在：" + apkFile.absolutePath)
            }

            if (whitePermissionsFile.exists()) {
                // 读取白名单权限
                def arr = whitePermissionsFile.readLines()
                        .stream()
                        .map { it.trim() }
                        .filter { !it.isEmpty() && !it.startsWith("#") }
                        .toArray()
                whitePermissions.addAll(arr)
            } else {
                println("白名单权限文件不存在，将新生成权限白名单文件")
            }
        }

        def standardOutput = new ByteArrayOutputStream()
        // 配置命令行
        // apkanalyzer manifest permissions apkPath 获取apk所有的权限
        executable(apkAnalyzer.absolutePath)
                .args("manifest", "permissions", apkFile.absolutePath)
                .setStandardOutput(standardOutput)

        doLast {
            def apkPermissions = new HashSet<String>()
            def array = standardOutput.toString().split(System.lineSeparator())
                    .toList()
                    .stream()
                    .map { it.trim() }
                    .filter { !it.isEmpty() }
                    .toArray()
            apkPermissions.addAll(array)

            if (!apkPermissions.isEmpty() && whitePermissions.isEmpty()) {
                // 白名单文件不存在，生成白名单文件
                writePermissionsFile(whitePermissionsFile, apkPermissions)
                println("已生成白名单文件")
                return
            }

            def apkPermissionsCopy = (Set<String>) apkPermissions.clone()
            apkPermissionsCopy.removeAll(whitePermissions)
            // 检查apk权限是否都在白名单内
            if (!apkPermissionsCopy.isEmpty()) {
                StringBuffer buffer = new StringBuffer()
                buffer.append("apk存在多余权限\n\n")
                buffer.append("包名：" + applicationId + "\n\n")
                buffer.append(apkPermissionsCopy)
                if (enableSendDingtalk) DingDingUtils.send2DingDing(buffer.toString())

                if (enableStopTask)
                    throw new IllegalStateException("apk存在多余权限：${apkPermissionsCopy}")
            } else {
                println("权限检查通过")
            }

            def whitePermissionsCopy = (Set<String>) whitePermissions.clone()
            whitePermissionsCopy.removeAll(apkPermissions)
            // 检查白名单是否有多余的权限
            if (!whitePermissionsCopy.isEmpty()) {
                whitePermissionsCopy.forEach {
                    println("白名单有多余的权限配置：${it}")
                }
                // // 去掉多余的白名单权限
                // whitePermissions.removeAll(whitePermissionsCopy)
                // // 更新权限白名单
                // writePermissionsFile(whitePermissionsFile, whitePermissions)
                // println("已更新白名单文件")
            }
        }
    }

    private def writePermissionsFile(File permissionsFile, Collection<String> list) {
        ArrayList<String> copy = new ArrayList<>(list)
        copy.sort()
        permissionsFile.withWriter { writer ->
            copy.forEach {
                writer.writeLine(it)
            }
            writer.close()
        }
    }
}