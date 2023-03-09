package com.tr.checkpacking.checksnapshot;

import com.tr.checkpacking.utils.DingDingUtils;
import com.tr.checkpacking.utils.LoggerUtils;
import com.tr.checkpacking.utils.ProjectUtils;

import org.gradle.BuildResult;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ModuleVersionSelector;
import org.gradle.api.logging.LogLevel;

import java.util.HashSet;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 快照依赖类型检查插件
 *
 * @author shhu
 */
@SuppressWarnings("unused")
public class CheckSnapshotPlugin implements Plugin<Project> {

    private static final String TAG = "CheckSnapshotPlugin";

    @Nullable
    private HashSet<String> dependencySet;
    @Nullable
    private CheckSnapshotExtension checkSnapshotExtension;
    @Nullable
    private String projectName;

    @SuppressWarnings("deprecation")
    @Override
    public void apply(@Nonnull Project project) {
        LoggerUtils.captureStandardOutput(project, LogLevel.INFO);

        projectName = ProjectUtils.getProjectName(project);

        // 配置插件配置
        ProjectUtils.createExtension(project, CheckSnapshotExtension.EXTENSION_NAME, CheckSnapshotExtension.class);

        // 获取插件配置
        checkSnapshotExtension = ProjectUtils.findExtension(project, CheckSnapshotExtension.EXTENSION_NAME);
        if (checkSnapshotExtension != null && !checkSnapshotExtension.pluginState) {
            // 关闭插件时直接跳过
            LoggerUtils.info(TAG, "插件被禁用");
            return;
        }

        dependencySet = new HashSet<>();
        // 收集所有依赖项
        project.getConfigurations().all(configuration -> {
            // 配置依赖策略
            configuration.resolutionStrategy(strategy -> {
                // 遍历所有依赖
                strategy.eachDependency(dependency -> {
                    ModuleVersionSelector dependencyTarget = dependency.getTarget();
                    // set去重
                    dependencySet.add(dependencyTarget.toString());
                });
            });
        });
        // 对编译流程没有强依赖，可以在buildFinished后直接检查
        //noinspection deprecation
        project.getGradle().buildFinished(this::buildFinished);
    }

    private void buildFinished(BuildResult buildResult) {
        if (buildResult != null && buildResult.getFailure() != null) {
            // 编译失败时忽略
            return;
        }

        if (dependencySet == null || dependencySet.isEmpty()) {
            // 依赖项为空时忽略
            LoggerUtils.info(TAG, "dependencySet is empty");
            return;
        }

        // 正则匹配快照版本号
        Pattern regex = Pattern.compile("^[\\w-.]+:[\\w-]+:[\\d.]+-([\\d.-]|SNAPSHOT)+$");
        HashSet<String> snapshotSet = new HashSet<>();
        for (String dependency : dependencySet) {
            if (regex.matcher(dependency).matches()) {
                snapshotSet.add(dependency);
                LoggerUtils.warn(TAG, "存在快照依赖：" + dependency);
            }
        }
        if (snapshotSet.isEmpty()) {
            // 没有快照依赖
            LoggerUtils.info(TAG, "没有发现快照依赖");
            return;
        }

        if (checkSnapshotExtension != null && !checkSnapshotExtension.enableSendDingTalk) {
            // 钉钉消息被禁用
            LoggerUtils.info(TAG, "钉钉消息被禁用");
            return;
        }

        LoggerUtils.info(TAG, "发送钉钉消息");
        // 发送钉钉消息
        StringBuilder sb = new StringBuilder();
        sb.append("# Project: ")
                .append(projectName)
                .append("：\n\n")
                .append("存在快照依赖：\n\n");
        for (String s : snapshotSet) {
            sb.append("* ").append(s).append("\n\n");
        }
        DingDingUtils.send2DingDing(sb.toString());
    }
}
