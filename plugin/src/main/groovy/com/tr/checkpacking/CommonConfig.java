package com.tr.checkpacking;

import com.tr.checkpacking.utils.ProjectUtils;

import org.gradle.api.Project;

import javax.annotation.Nonnull;

/**
 * 插件公共属性
 */
public class CommonConfig {

    public static final String PROPERTY_PLUGIN_STATE = "tr.checkpacking.pluginState";

    public static final String PROPERTY_SEND_DING_TALK = "tr.checkpacking.sendDingTalk";

    /**
     * 插件开关
     * 所有插件默认全开启
     */
    public boolean pluginState = true;

    /**
     * 是否通知钉钉群
     * 所有插件默认全开启
     * 各端开发可在开发期间设为false，在提交代码测试Jenkins打包时设为true
     */
    public boolean enableSendDingTalk = true;

    /**
     * 将命令行参数或环境变量配置 和 build.gradle中的配置合并
     * 默认支持了
     *
     * @param project
     * @see ProjectUtils#createExtension(Project, String, Class)
     */
    protected void margeWithProperty(@Nonnull Project project) {
        Boolean state = ProjectUtils.findProperty(project, PROPERTY_PLUGIN_STATE, Boolean.class);
        if (state != null) {
            this.pluginState = state;
        }

        state = ProjectUtils.findProperty(project, PROPERTY_SEND_DING_TALK, Boolean.class);
        if (state != null) {
            this.enableSendDingTalk = state;
        }
        // 如需其他类型请自行扩展
        // 子类属性请在子类中扩展
    }

}
