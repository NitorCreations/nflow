import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val nflowExplorer: Configuration by configurations.creating
val logbackVersion: String by project
val nflowVersion: String by project

plugins {
    base
    kotlin("jvm") version "2.3.20"
    id("org.jetbrains.kotlin.plugin.spring") version "2.3.20"
    id("org.springframework.boot") version "4.0.6"
}

group = "nflow-kotlin"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_25
}

repositories {
    mavenCentral()
    maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }
    maven { url = uri("https://plugins.gradle.org/m2/") }
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.6"))
    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.6"))
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("ch.qos.logback:logback-classic")
    implementation("io.nflow:nflow-rest-api-spring-web:$nflowVersion")

    runtimeOnly("com.h2database:h2:2.4.240")

    testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.4.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    nflowExplorer("io.nflow:nflow-explorer:$nflowVersion@tar.gz")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
    sourceSets["main"].kotlin.srcDirs("src")
    sourceSets["test"].kotlin.srcDirs("test")
}

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")

tasks.register<Copy>("resolveNflowExplorer") {
    from(tarTree(resources.gzip(configurations["nflowExplorer"].singleFile)))
    into(layout.buildDirectory.dir("resources/main/static/explorer"))
}

tasks {
    withType<ProcessResources> {
        dependsOn(named("resolveNflowExplorer"))
    }

    withType<Test> {
        testLogging { events("passed", "skipped", "failed") }
        useJUnitPlatform()
    }
}
