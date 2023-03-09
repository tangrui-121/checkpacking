package com.tr.checkpacking.checkproguard

import com.tr.checkpacking.CommonConfig
import com.tr.checkpacking.StopTaskConfig
import com.tr.checkpacking.utils.ProjectUtils
import org.gradle.api.Project

import javax.annotation.Nonnull

class ProguardExtension extends CommonConfig implements StopTaskConfig {

    public static final String EXTENSION_NAME = "checkProguard"

    /**
     * 混淆文件源文件路径
     */
    String newProguardPath

    /**
     * 混淆文件白名单路径
     */
    String whiteProguardPath

    private boolean enableStopTask = false

    @Override
    protected void margeWithProperty(@Nonnull Project project) {
        super.margeWithProperty(project)
        def array = ProjectUtils.findProperty(project, PROPERTY_PLUGIN_STATE)
        if (array != null) {
            pluginState = array.contains(EXTENSION_NAME)
        }

        array = ProjectUtils.findProperty(project, PROPERTY_SEND_DING_TALK)
        if (array != null) {
            enableSendDingTalk = array.contains(EXTENSION_NAME)
        }

        enableStopTask = getStopTaskConfig(project, EXTENSION_NAME)
    }

    @Override
    boolean enableStopTask() {
        return enableStopTask
    }
}
