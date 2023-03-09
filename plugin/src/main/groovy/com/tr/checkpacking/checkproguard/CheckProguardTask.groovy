package com.tr.checkpacking.checkproguard

import com.tr.checkpacking.utils.DingDingUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

class CheckProguardTask extends DefaultTask {

    public String newProguardPath
    public String whiteProguardPath
    public String applicationId
    public boolean enableSendDingtalk
    public boolean enableStopTask // 出现问题是否终止编译，默认为true，开发期间可直接使用默认值，测试打包时，可使用命令行设为false

    @Input
    String getNewProguardPath() {
        return newProguardPath
    }

    @Input
    String getWhiteProguardPath() {
        return whiteProguardPath
    }

    @Input
    String getApplicationId() {
        return applicationId
    }

    @Input
    boolean getEnableSendDingtalk() {
        return enableSendDingtalk
    }

    @Input
    boolean getEnableStopTask() {
        return enableStopTask
    }

    @TaskAction
    void action() {
        getDif(new File(whiteProguardPath), new File(newProguardPath))
    }

    private void getDif(File whiteFile, File newFile) {
        if (!whiteFile.exists()) {
            println "混淆白名单不存在"
            return
        }
        if (!newFile.exists()) {
            println "新混淆文件未生成"
            return
        }
        def oldProguards = new ArrayList<String>()
        def newProguards = new ArrayList<String>()
        def midProguards = new ArrayList<String>()
        whiteFile.eachLine {
            if (!it.isEmpty() && !it.startsWith("#")) {
                if (!it.trim().startsWith("-")) {
                    oldProguards.set(oldProguards.size() - 1, oldProguards.get(oldProguards.size() - 1) + "\n" + it)
                } else {
                    oldProguards.add(it)
                }
            }
        }
        newFile.eachLine {
            if (!it.isEmpty() && !it.startsWith("#")) {
                if (!it.trim().startsWith("-")) {
                    newProguards.set(newProguards.size() - 1, newProguards.get(newProguards.size() - 1) + "\n" + it)
                } else {
                    newProguards.add(it)
                }
            }
        }
        for (int i = 0; i < oldProguards.size(); i++) {
            if (newProguards.contains(oldProguards.get(i))) {
                midProguards.add(oldProguards.get(i))
            }
        }
        oldProguards.removeAll(midProguards)
        newProguards.removeAll(midProguards)

        // 由于篇幅过长，小于10行的直接发送钉钉，否则直接给出链接跳转Jenkins完事
        if (oldProguards.size() > 0 || newProguards.size() > 0) {

            def lines = 0
            StringBuffer buffer_dingtalk = new StringBuffer()

            println("本次打包删除混淆：")
            buffer_dingtalk.append("### " + applicationId + " 混淆文件有变动\n\n")
            buffer_dingtalk.append("- 本次打包删除混淆：\n\n")
            buffer_dingtalk.append("```\n\n")
            oldProguards.forEach {
                println(it)
                if (lines < 8) {
                    lines++
                    buffer_dingtalk.append("${it.toString()}\n")
                }
            }
            buffer_dingtalk.append("```\n\n")
            println("本次打包新增混淆：")
            if (lines < 8) {
                lines++
                buffer_dingtalk.append("- 本次打包新增混淆：\n\n")
            }
            buffer_dingtalk.append("```\n\n")
            newProguards.forEach {
                println(it)
                if (lines < 8) {
                    lines++
                    buffer_dingtalk.append("${it.toString()}\n")
                }
            }
            buffer_dingtalk.append("```\n\n")
            buffer_dingtalk.append("请开发仔细检查本次的混淆改动!")
            println("请开发仔细检查本次的混淆改动，无异议后：")
            println("手动处理托管的混淆白名单，将${whiteProguardPath}的内容替换为${newProguardPath}内容即可。")
            if (enableSendDingtalk) {
                if (lines == 8) {
                    buffer_dingtalk.append("```\n\n")
                    buffer_dingtalk.append("篇幅过长，自行执行项目内check proguard命令，或")
                    buffer_dingtalk.append("```\n\n")
                    buffer_dingtalk.append("[详情请至Jenkins找最近一次打包记录查看](http://app-jenkins.devops.guchele.cn:8080/view/Android/)")
                }
                DingDingUtils.send2DingDing(buffer_dingtalk.toString())
            }
            if (enableStopTask) throw new Exception("混淆文件有变动，打包终止！")
        } else {
            println("本次打包混淆文件无改动!")
        }
    }
}