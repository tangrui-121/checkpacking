@file:JvmName("ProjectUtils")
@file:Suppress("UNCHECKED_CAST")

package com.tr.checkpacking.utils

import com.tr.checkpacking.CommonConfig
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.internal.metaobject.DynamicObjectUtil

/**
 * 获取项目名
 */
fun Project.getProjectName(): String {
    val rootProject = this.rootProject
    return if (rootProject === this) {
        project.name
    } else {
        rootProject.name + ":" + project.name
    }
}

/**
 * 创建指定类型的Extension
 *
 * @param name    extension名称
 * @param clazz   extension类型
 * @param <T>     CommonConfig的子类
 */
fun <T : CommonConfig?> Project?.createExtension(name: String, clazz: Class<T>) {
    if (this == null) {
        LoggerUtils.warn("project is null, can not create extension. name: {}, type: {}",
            name,
            clazz)
        return
    }
    // 创建Extension实例，一般在apply plugin时调用
    extensions.create(name, clazz)
    // 然后进行Extension配置，对属性进行赋值。实际上就是调用getExtensions().configure(name, action)
    afterEvaluate {
        // 为保证参数优先级，需要在afterEvaluate中调用，否则会被build.gradle配置覆盖
        // https://docs.gradle.org/current/userguide/build_lifecycle.html
        extensions.configure(name, Action<CommonConfig> { extension ->
            //CommonConfig commonConfig = (CommonConfig) extension;
            //commonConfig.margeWithProperty(project);

            // 反射调用margeWithProperty方法，保护protected方法
            val dynamicObject = DynamicObjectUtil.asDynamicObject(extension)
            val result = dynamicObject.tryInvokeMethod("margeWithProperty", this)
            if (!result.isFound) {
                LoggerUtils.warn("can not invoke margeWithProperty method. name: {}, type: {}",
                    name,
                    clazz)
            }
        })
    }
}

/**
 * 查找指定类型的Extension
 *
 * @param clazz   Extension类型
 * @param <T>     CommonConfig的子类
 * @return T
 */
fun <T : CommonConfig?> Project?.findExtension(clazz: Class<T>): T? {
    if (this == null) {
        LoggerUtils.warn("project is null, can not find extension. type: {}", clazz)
        return null
    }
    return extensions.findByType(clazz)
}

/**
 * 查找指定类型的Extension，如果没有时则返回null
 *
 * @param name    extension名称
 * @param <T>     CommonConfig的子类
 * @return T
 */
fun <T> Project?.findExtension(name: String): T? {
    if (this == null) {
        LoggerUtils.warn("project is null, can not find extension. name: {}", name)
        return null
    }
    return extensions.findByName(name) as T
}

private const val PROPERTY_STATE_ENABLE = true.toString()
private const val PROPERTY_STATE_DISABLE = false.toString()
private const val PROPERTY_SEPARATOR = ","

/**
 * 获取字符串参数，并将字符串通过逗号分隔成数组，如果包含true或false时返回null
 *
 * @param name    参数名称
 * @return
 */
fun Project.findProperty(name: String): Array<String>? {
    val property = this.findProperty(name, String::class.java)
    if (property.isEmpty()) {
        return null
    }
    val split = property.split(PROPERTY_SEPARATOR.toRegex())
    if (split.contains(PROPERTY_STATE_DISABLE) || split.contains(PROPERTY_STATE_ENABLE))
        return null
    return split.toTypedArray()
}

inline fun <reified T> Project.findProperty(name: String, defaultValue: T): T {
    return findProperty(name, T::class.java) ?: return defaultValue
}

inline fun <reified T> Project.findProperty(name: String): T? {
    return findProperty(name, T::class.java)
}

fun <T> Project.findProperty(name: String, clazz: Class<T>): T? {
    val property = this.findProperty(name) ?: return null
    if (property is String) {
        // 可能是在Groovy/Java代码中调用的，所以需要进行兼容
        return when (clazz) {
            String::class.javaObjectType, String::class.java -> {
                property as T
            }
            Int::class.javaObjectType, Int::class.java -> {
                property.toInt() as T
            }
            Boolean::class.javaObjectType, Boolean::class.java -> {
                property.toBoolean() as T
            }
            Long::class.javaObjectType, Long::class.java -> {
                property.toLong() as T
            }
            Double::class.javaObjectType, Double::class.java -> {
                property.toDouble() as T
            }
            Float::class.javaObjectType, Float::class.java -> {
                property.toFloat() as T
            }
            else -> {
                throw IllegalArgumentException("Unsupported type: " + clazz.name)
            }
        }
    }
    return property as T
}