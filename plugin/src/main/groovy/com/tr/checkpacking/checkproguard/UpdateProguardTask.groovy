package com.tr.checkpacking.checkproguard

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

class UpdateProguardTask extends DefaultTask {

    public String newProguardPath
    public String whiteProguardPath

    @Input
    String getNewProguardPath() {
        return newProguardPath
    }

    @Input
    String getWhiteProguardPath() {
        return whiteProguardPath
    }

    @TaskAction
    def action() {
        def isOver = copy(newProguardPath, whiteProguardPath)
        if (isOver) {
            println("已更新混淆白名单")
        } else {
            println("混淆白名单更新失败")
        }
    }

    static def copy(String srcPath, String destPath) {
        def f = new File(srcPath)
        def d = new File(destPath)
        if (!f.exists()) {
            println("新的全量混淆文件尚未生成")
            return
        }
        if (!d.exists()) {
            d.createNewFile()
        }
        f.withReader {
            reader ->
                def lines = reader.readLines()
                d.withWriter { writer ->
                    lines.each {
                        writer.append(it + "\r\n")
                    }
                }

        }
        return true
    }
}