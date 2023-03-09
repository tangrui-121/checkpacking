import org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal
import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream
import java.util.regex.Pattern

plugins {
    id("java-gradle-plugin")
    id("groovy")
    id("java")
    id("maven-publish")
    id("org.jetbrains.kotlin.jvm") version "1.5.31"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.spockframework:spock-core:2.0-groovy-3.0")
    implementation("com.squareup.okhttp3:okhttp:3.12.4")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

gradlePlugin {

    plugins {
        create("checkProguard") {
            id = "com.tr.checkProguard"
            implementationClass = "com.tr.checkpacking.checkproguard.CheckProguardPlugin"
        }
        create("checkPermissions") {
            id = "com.tr.checkPermissions"
            implementationClass = "com.tr.checkpacking.checkpermissions.CheckPermissionsPlugin"
        }
        create("checkSnapshot") {
            id = "com.tr.checkSnapshot"
            implementationClass = "com.tr.checkpacking.checksnapshot.CheckSnapshotPlugin"
        }
        create("checkDependencies") {
            id = "com.tr.checkDependencies"
            implementationClass = "com.tr.checkpacking.checkdependencies.CheckDependenciesPlugin"
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    sourceSets {
        getByName("main").kotlin.srcDirs("src/main/groovy")
    }
}

afterEvaluate {
    publishing {
        repositories {
            maven {
                val url = if (property("VERSION").toString().endsWith("SNAPSHOT"))
                    property("REPO_URL_SNAPSHOT").toString() else property("REPO_URL_RELEASE").toString()
                setUrl(url)
                isAllowInsecureProtocol = true
                credentials {
                    username = property("USERNAME").toString()
                    password = property("PASSWORD").toString()
                }
            }
        }

        publications {
            create<MavenPublication>("release") {
                from(components.findByName("java"))
                artifact(tasks.findByName("sourceJar"))
                groupId = property("GROUP_ID").toString()
                artifactId = property("ARTIFACT_ID").toString()
                version = property("VERSION").toString()
            }
        }
    }
}

afterEvaluate {
    // org.gradle.api.publish.maven.plugins.MavenPublishPlugin
    val publishing = extensions.getByName("publishing") as PublishingExtension
    val mavenPublications: NamedDomainObjectSet<MavenPublicationInternal> =
        publishing.publications.withType(MavenPublicationInternal::class.java)
    val repositories: NamedDomainObjectList<MavenArtifactRepository> =
        publishing.repositories.withType(MavenArtifactRepository::class.java)
    mavenPublications.all {
        val publicationName = this.name.capitalized()
        repositories.all {
            val repositoryName = this.name.capitalized()
            val publishTaskName =
                "publish${publicationName}PublicationTo${repositoryName}Repository"
            val taskProvider = tasks.named<PublishToMavenRepository>(publishTaskName)
            taskProvider.configure {
                val groupId = this.publication.groupId
                val artifactId = this.publication.artifactId
                val version = this.publication.version
                val url = this.repository.url

                fun printDependency(
                    url: String,
                    group: String,
                    artifact: String,
                    version: String,
                ) {
                    println("""
                        repositories {
                            maven {
                                url = "$url"
                            }
                        }
                        
                        dependencies {
                            implementation "$group:$artifact:$version"
                        }
                        """.trimIndent())
                }

                fun printDefaultDependency(
                    group: String,
                    artifact: String,
                    version: String,
                    message: String,
                ) {
                    println("""
                        // $message
                        dependencies {
                            implementation "$group:$artifact:$version"
                        }
                        """.trimIndent())
                }

                // http://app-gitlab.devops.guchele.cn:8081/repository/maven-releases/
                val pattern =
                    Pattern.compile("^https?://([\\da-z\\.-]+)\\.([a-z\\.]{2,6}):?\\d*/repository/([\\w-\\.]+)/?\$")
                val matcher = pattern.matcher(url.toString())
                // 正则匹配仓库名称
                if (!matcher.matches()) {
                    printDefaultDependency(groupId,
                        artifactId,
                        version,
                        "maven url is not supported, print default dependency")
                    return@configure
                }
                val urlRepositoryName = matcher.group(3) ?: "maven-public"
                val searchUrl = "${url.scheme}://${url.host}${
                    if (url.port == -1) "" else ":" + url.port
                }/service/rest/v1/search?sort=version&direction=desc&repository=${
                    urlRepositoryName
                }&maven.groupId=${groupId}&maven.artifactId=${artifactId}"
                this.doLast {
                    val output = ByteArrayOutputStream()
                    val result = exec {
                        commandLine("curl", "-X", "GET", searchUrl)
                        standardOutput = output
                    }
                    var element: Map<String, Any?>? = null
                    if (result.exitValue == 0) {
                        val jsonSlurper = groovy.json.JsonSlurper()
                        try {
                            val jsonObj = jsonSlurper.parse(output.toByteArray())
                            val map = jsonObj as Map<String, List<Any>?>
                            val first = map["items"]?.getOrNull(0)
                            element = first as Map<String, Any?>
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    if (element == null) {
                        printDefaultDependency(groupId,
                            artifactId,
                            version,
                            "maven result parse error, print default dependency")
                        return@doLast
                    }
                    val group = element["group"].toString()
                    val name = element["name"].toString()
                    val v = element["version"].toString()
                    printDependency(url.toString(), group, name, v)
                }
            }
        }
    }
}

tasks.create<Jar>("sourceJar") {
    from(sourceSets.main.get().allSource)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    archiveClassifier.set("sources")
}

// https://docs.gradle.org/6.1/release-notes.html#defining-compilation-order-between-groovy,-scala-and-java
tasks.compileGroovy {
    val compileKotlin = tasks.named("compileKotlin", KotlinCompile::class).get()
    classpath += files(compileKotlin.destinationDirectory)
}
