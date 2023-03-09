package com.tr.checkpacking.checkpermissions

import com.tr.checkpacking.CommonConfig
import com.tr.checkpacking.StopTaskConfig
import com.tr.checkpacking.utils.ProjectUtils
import org.gradle.api.Project

import javax.annotation.Nonnull

class PermissionsExtension extends CommonConfig implements StopTaskConfig {

    public static final String EXTENSION_NAME = "checkPermissions"

    /**
     * 本地权限白名单文件名
     */
    String whitePermissionsName = "permissions_white.txt"

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
