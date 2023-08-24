import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val nflowExplorer: Configuration by configurations.creating
val logbackVersion: String by project
val nflowVersion: String by project

fun DependencyHandler.springBoot(name: String) = create(
        group = "org.springframework.boot",
        name = "spring-boot-starter-$name"
)

fun DependencyHandler.logback(name: String) = create(
        group = "ch.qos.logback",
        name = name,
        version = logbackVersion
)

fun DependencyHandler.nflow(name: String) = create(
        group = "io.nflow",
        name = name,
        version = nflowVersion
)

plugins {
    base
    kotlin("jvm") version "1.6.0"
    id("org.jetbrains.kotlin.plugin.spring") version "1.6.21"
    id("org.springframework.boot") version "2.4.2"
    id("io.spring.dependency-management") version "1.0.8.RELEASE"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://plugins.gradle.org/m2/") }
}

group = "nflow-kotlin"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation(springBoot("jdbc"))
    implementation(springBoot("web"))
    implementation(logback("logback-classic"))
    implementation(nflow("nflow-rest-api-spring-web"))

    runtimeOnly("com.h2database:h2:2.2.220")

    testImplementation(kotlin("test"))
    testImplementation(springBoot("test"))
    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.4.2")

    nflowExplorer(
            group = "io.nflow",
            name = "nflow-explorer",
            version = nflowVersion,
            ext = "tar.gz"
    )
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")

task<Copy>("resolveNflowExplorer") {
    from(tarTree(resources.gzip(configurations["nflowExplorer"].singleFile)))
    destinationDir = file("$buildDir/resources/main/static/explorer")
}

tasks {
    withType<ProcessResources> {
        dependsOn(get("resolveNflowExplorer"))
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    withType<Test> {
        testLogging { events("passed", "skipped", "failed") }
        useJUnitPlatform()
    }
}
