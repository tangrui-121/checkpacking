## 打包检查插件



#### 作用（检查输出并可发送至钉钉）

- 依赖检查

  ```
  com.tr.activity 依赖变动如下
  
  D：com.tr.adv-filter:adv-filter:1.1.4  
  A：com.tr.adv-filter:advFilter:1.2.0-SNAPSHOT  
     加途径：  
     项目  
  U：com.github.bzcoder:blockcanarycompat-analyzer，由0.0.3改为0.0.1  该sdk降级了，请谨慎处理！！！！  
     影响范围：  
     com.github.bzcoder:blockcanarycompat-android:0.0.1
  U：com.tr.lint:lint，由1.0.0改为1.0.0-20230308.071626-7，对其他依赖没有影响，放心使用。  
     该sdk大版本相同，本次正式/快照版本切换，请谨慎处理！！！！
  U：com.squareup.curtains:curtains，由0.0.5-20220725.054533-9改为0.0.6-20221102.021704-1，对其他依赖没有影响，放心使用。
  U:androidx.collection:collection，由1.1.5改为1.1.0 
      影响范围： 
      androidx.databinding:databinding-runtime:4.1.3 
      androidx.viewpager2:viewpager2:1.0.0 
      androidx.fragment:fragment:1.3.6 
      ...
  ```

- 混淆检查

  ```
  混淆文件有变动
  包名：com.tr.activity
  本次打包删除混淆：
  -keep class com.huawei.updatesdk.fileprovider.UpdateSdkFileProvider { (); }
  -keep class com.huawei.updatesdk.service.otaupdate.AppUpdateActivity { (); }
  本次打包新增混淆：
  -keep class com.xiaomi.**{*;}
  -keep class com.huawei.** {*;}
  -keep class com.meizu.**{*;}
  -keep class org.apache.thrift.** {*;}
  -keep public class **.R$*{
     public static final int *;
  }
  请开发仔细检查本次的混淆改动!
  ```

- 权限检查

  ```
  apk存在多余权限
  包名：com.tr.activity
  [android.permission.POST_NOTIFICATIONS, 
  com.hihonor.push.permission.READ_PUSH_NOTIFICATION_INFO, 
  com.hihonor.android.launcher.permission.CHANGE_BADGE]
  ```

- 快照检查

  - 用正则检查快照依赖



#### 发布

- maven地址、账户密码、版本号等等在gradle.properties中，自行修改
- 钉钉token在DingDingUtils中，自行修改
- 发布线上maven请使用： gradlew publishReleasePublicationToMavenRepository
- 发布本地maven请使用： gradlew publishReleasePublicationToMavenLocal
- 因为该项目为单项目多插件，貌似官方不太支持这种方式，也有可能是我的build.gradle配置不够完善，直接使用publish的话，maven上文件夹会变得混乱。



#### 使用

- 添加插件依赖

  ```
  dependencies {
      classpath 'com.tr.checkpacking:plugin:1.0.0'
      自行修改为gradle.properties中的配置
  }
  ```

- 混淆检查

  ```
  在混淆文件中加入
  # 生成所有的混淆配置文件
  -printconfiguration ./build/full-r8-config.txt
  ```

  ```
  app/build.gradle
  
  apply plugin: 'com.tr.checkProguard'
  
  checkProguard {
      pluginState = true                                              // 是否打开检查，默认开启，下同  
      enableSendDingTalk = true                                       // 失败时时候通知钉钉群，默认开启，下同 
      whiteProguardPath = "${project.projectDir}/proguard_white.txt"  // 本地混淆白名单目录
      newProguardPath = "${project.buildDir}/full-r8-config.txt"      // 本地生成的混淆文件目录 
  }
  ```

  命令行： 不希望出现问题而终止打包的话(如测试打包时)：

  ```
  gradlew checkReleaseProguard -Ptr.checkpacking.stopTask=false
  ```

- 依赖检查

  ```
  apply plugin: 'com.tr.checkDependencies'
  
  checkDependencies {
    whiteDependenciesName = "${project.projectDir}/dependencies_white.txt"  // 依赖白名单
     mainProjectName = ["check:app:unspecified","check_pipeline:app:unspecified",
                         "check:app1:unspecified", "check_pipeline:app1:unspecified"]                          
                         // 各lib在gradle编译里面对应的字符串，用来标识自身，防止在使用jenkins打包工具时项目名字不一样导致无效输出，可不配置
  }
  ```

- 权限检查

  ```
  apply plugin: 'com.tr.checkPermissions'
  
  checkPermissions { 
      whitePermissionsName = "permissions_white.txt"  // 本地权限白名单文件名，默认为permissions_white.txt
  }
  ```

  命令行： 不希望出现问题而终止打包的话(如测试打包时)：

  ```
  gradlew checkReleasePermissions -Ptr.checkpacking.stopTask=false
  ```

- 快照检查

  ```
  apply plugin: 'com.tr.checkSnapshot'
  ```

当然了，除了命令行，还提供了task，在主项目的tasks文件夹下，可以看到依赖变更检查、白名单更新等等group

我们在检查完毕，确认无误之后，可以调用白名单的task来更新本地白名单。



## 参数配置

**参数类型:** 参数支持在`build.gradle中配置`、`命令行参数`或`两者都支持`，当两者都支持时，遵循优先级策略

**优先级:** `命令行参数` > `项目级别gradle.properties` > `全局gradle.properties` > `build.gradle配置`

### build.gradle配置

可以单独配置每个插件的参数，如果参数多处配置时优先级最低

| 参数                   | 数据类型 | 默认值                | 支持的插件                | 说明                   |
| ---------------------- | -------- | --------------------- | ------------------------- | ---------------------- |
| `pluginState`          | boolean  | true                  | `all`                     | 是否启用插件           |
| `enableSendDingTalk`   | boolean  | true                  | `all`                     | 是否将结果发送到钉钉   |
| `whitePermissionsName` | String   | permissions_white.txt | `com.tr.checkPermissions` | 本地权限白名单文件路径 |
| `whiteProguardPath`    | String   | null                  | `com.tr.checkProguard`    | 本地白名单混淆文件路径 |
| `newProguardPath`      | String   | null                  | `com.tr.checkProguard`    | 新生成的混淆文件路径   |

### 命令行参数或gradle变量

主要用于对一些插件的参数进行复写，优先级较高

| 参数                           | 对应的build.gradle配置参数 | 支持内容                                                     | 支持的插件                                        | 说明                     |
| ------------------------------ | -------------------------- | ------------------------------------------------------------ | ------------------------------------------------- | ------------------------ |
| `tr.checkpacking.pluginState`  | `pluginState`              | `true`、`false`、(`checkProguard`、`checkPermissions`、`checkSnapshot`、`checkDependencies`) | `all`                                             | 检测出问题时是否中断编译 |
| `tr.checkpacking.sendDingTalk` | `enableSendDingtalk`       | `true`、`false`、(`checkProguard`、`checkPermissions`、`checkSnapshot`、`checkDependencies`) | `all`                                             | 是否将结果发送到钉钉     |
| `tr.checkpacking.stopTask`     | `--`                       | `true`、`false`、(`checkProguard`、`checkPermissions`)       | `com.tr.checkPermissions`、`com.tr.checkProguard` | 检测出问题时是否中断编译 |

**备注:**

* 使用命令行参数时需要使用如下格式`-P{name}=value`，如：`-Ptr.checkpacking.stopTask=false`。
* 变量内容除了true或false以外时，支持配置多个，使用英文逗号分隔，
  如：`tr.checkpacking.sendDingTalk=checkProguard,checkPermissions,checkSnapshot,checkDependencies`

### 系统环境变量

用于修改一些不经常变动的配置，例如：钉钉机器人配置

| 参数                                | 类型   | 默认值                 | 说明             |
| ----------------------------------- | ------ | ---------------------- | ---------------- |
| `CHECKPACKING_DINGTALK_ACCESSTOKEN` | String | 钉钉机器人access token |
| `CHECKPACKING_DINGTALK_KEYWORD`     | String | 打包检查结果：         | 钉钉机器人关键字 |