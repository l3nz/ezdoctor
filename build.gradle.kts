plugins {
    java
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.asciidoctor:asciidoctorj:3.0.1")
    implementation("org.asciidoctor:asciidoctorj-pdf:2.3.13")
    implementation("org.asciidoctor:asciidoctorj-diagram:3.1.0")
    implementation("org.asciidoctor:asciidoctorj-diagram-plantuml:1.2025.3")
    implementation("org.asciidoctor:asciidoctorj-diagram-ditaamini:1.0.3")

    implementation("info.picocli:picocli:4.7.6")
    testImplementation("junit:junit:4.13.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

application {
    mainClass.set("com.github.l3nz.ezdoctor.EzdoctorMain")
}

// ---------------------------------------------------------------------------
// Fat-JAR: bundle all dependencies into a single JAR
// ---------------------------------------------------------------------------
val fatJar by tasks.registering(Jar::class) {
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    })
}

// ---------------------------------------------------------------------------
// Standalone directory: minimal JRE (jlink) + fat JAR + launcher script
// ---------------------------------------------------------------------------
val standaloneDir = layout.buildDirectory.dir("standalone/ezdoctor")

val jlinkJre by tasks.registering(Exec::class) {
    val jreDir = standaloneDir.get().dir("jre").asFile

    val javaHome = javaToolchains
        .launcherFor(java.toolchain)
        .get()
        .metadata
        .installationPath
        .asFile
        .absolutePath

    val jlinkBin = "$javaHome/bin/jlink"

    doFirst { delete(jreDir) }

    commandLine(
        jlinkBin,
        "--add-modules", "java.base,java.desktop,java.logging,java.management,java.naming,java.scripting,java.sql,java.xml,jdk.unsupported",
        "--strip-debug",
        "--no-man-pages",
        "--no-header-files",
        "--compress", "zip-6",
        "--output", jreDir.absolutePath
    )
}

val buildStandalone by tasks.registering(Copy::class) {
    dependsOn(fatJar, jlinkJre)

    // Copy fat JAR into lib/
    from(fatJar.get().archiveFile) {
        into("lib")
    }
    into(standaloneDir)

    // Create launcher scripts after the copy
    doLast {
        val dir = standaloneDir.get().asFile

        // exec "${'$'}DIR/jre/bin/java"

        // Unix launcher
        val sh = File(dir, "ezdoctor")
        sh.writeText("""
            |#!/usr/bin/env bash
            |DIR="${'$'}(cd "${'$'}(dirname "${'$'}0")" && pwd)"
            |exec "java" \
            |  --add-opens=java.base/java.lang=ALL-UNNAMED \
            |  --add-opens=java.base/java.io=ALL-UNNAMED \
            |  --add-opens=java.base/java.nio=ALL-UNNAMED \
            |  --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
            |  --add-opens=java.base/java.util=ALL-UNNAMED \
            |  --enable-native-access=ALL-UNNAMED \
            |  --sun-misc-unsafe-memory-access=allow \
            |  -Xmx512m \
            |  -jar "${'$'}DIR/lib/${fatJar.get().archiveFileName.get()}" \
            |  "${'$'}@"
        """.trimMargin() + "\n")
        sh.setExecutable(true)

        // Windows launcher
        val bat = File(dir, "ezdoctor.bat")
        bat.writeText("""
            |@echo off
            |set DIR=%~dp0
            |"%DIR%jre\bin\java.exe" ^
            |  --add-opens=java.base/java.lang=ALL-UNNAMED ^
            |  --add-opens=java.base/java.io=ALL-UNNAMED ^
            |  --add-opens=java.base/java.nio=ALL-UNNAMED ^
            |  --add-opens=java.base/sun.nio.ch=ALL-UNNAMED ^
            |  --add-opens=java.base/java.util=ALL-UNNAMED ^
            |  --enable-native-access=ALL-UNNAMED ^
            |  --sun-misc-unsafe-memory-access=allow ^
            |  -Xmx512m ^
            |  -jar "%DIR%lib\${fatJar.get().archiveFileName.get()}" ^
            |  %*
        """.trimMargin() + "\r\n")
    }
}
