package com.tr.checkpacking;

import com.tr.checkpacking.utils.ProjectUtils;

import org.gradle.api.Project;

import java.util.Arrays;

public interface StopTaskConfig {

    /**
     * 终止编译的环境变量配置名称
     */
    String PROPERTY_STOP_TASK = "tr.checkpacking.stopTask";

    /**
     * 检测出问题时是否终止编译
     */
    boolean enableStopTask();

    public static boolean getStopTaskConfig(Project project, String extraPropertyName) {
        Boolean state = ProjectUtils.findProperty(project, PROPERTY_STOP_TASK, Boolean.class);
        if (state != null) {
            return state;
        }
        String[] array = ProjectUtils.findProperty(project, PROPERTY_STOP_TASK);
        if (array != null) {
            return Arrays.binarySearch(array, extraPropertyName) > -1;
        }
        return false;
    }
}
