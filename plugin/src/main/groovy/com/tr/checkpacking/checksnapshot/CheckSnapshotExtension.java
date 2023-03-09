package com.tr.checkpacking.checksnapshot;

import com.tr.checkpacking.CommonConfig;
import com.tr.checkpacking.utils.ProjectUtils;

import org.gradle.api.Project;

import java.util.Arrays;

import javax.annotation.Nonnull;

/**
 * 快照依赖检查配置
 */
public class CheckSnapshotExtension extends CommonConfig {

    public static final String EXTENSION_NAME = "checkSnapshot";

    @Override
    protected void margeWithProperty(@Nonnull Project project) {
        super.margeWithProperty(project);

        String[] array = ProjectUtils.findProperty(project, PROPERTY_PLUGIN_STATE);
        if (array != null) {
            pluginState = Arrays.binarySearch(array, EXTENSION_NAME) > -1;
        }

        array = ProjectUtils.findProperty(project, PROPERTY_SEND_DING_TALK);
        if (array != null) {
            enableSendDingTalk = Arrays.binarySearch(array, EXTENSION_NAME) > -1;
        }
    }
}
