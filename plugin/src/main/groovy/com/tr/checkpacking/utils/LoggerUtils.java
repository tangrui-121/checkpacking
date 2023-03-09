package com.tr.checkpacking.utils;

import org.gradle.api.Project;
import org.gradle.api.logging.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * 基于slf4j封装的日志工具
 * <p>
 * 支持日志占位符，如："日志内容：{}，{}，{}", "参数1", "参数2", "参数3" <br>
 */
public class LoggerUtils {

    private final static String format = "{}: {}";
    private final static Logger logger = LoggerFactory.getLogger("CheckPacking");

    /**
     * 修改标准输出流日志级别，gradle默认为LogLevel.LIFECYCLE，低于该级别的日志不会输出，如info、debug等
     * <p>
     * 可以通过命令行参数修改日志级别 gradlew --info
     *
     * @param project 目标项目
     * @param level   日志级别
     * @see LogLevel
     */
    public static void captureStandardOutput(Project project, LogLevel level) {
        project.getLogging().captureStandardOutput(level);
    }

    public static void info(String tag, String msg) {
        logger.info(format, tag, msg);
    }

    public static void info(String log, @Nullable Object... args) {
        logger.info(log, args);
    }

    public static void info(String log, @Nullable Throwable e) {
        logger.info(log, e);
    }

    public static void warn(String tag, String msg) {
        logger.warn(format, tag, msg);
    }

    public static void warn(String log, @Nullable Object... args) {
        logger.warn(log, args);
    }

    public static void warn(String log, @Nullable Throwable e) {
        logger.warn(log, e);
    }

    public static void error(String tag, String msg) {
        logger.error(format, tag, msg);
    }

    public static void error(String log, @Nullable Object... args) {
        logger.error(log, args);
    }

    public static void error(String log, @Nullable Throwable e) {
        logger.error(log, e);
    }

}
