import java.time.Instant

plugins {
    java
    application
}

version = "0.1.0"

val gitCommit: String = providers.exec { commandLine("git", "rev-parse", "--short", "HEAD") }
    .standardOutput.asText.get().trim()

val gitBranch: String = providers.exec { commandLine("git", "rev-parse", "--abbrev-ref", "HEAD") }
    .standardOutput.asText.get().trim()

val buildTime: String = Instant.now().toString()

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.asciidoctor:asciidoctorj:3.0.1")
    implementation("org.asciidoctor:asciidoctorj-pdf:2.3.13")
    implementation("org.asciidoctor:asciidoctorj-diagram:3.1.0")
    implementation("org.asciidoctor:asciidoctorj-diagram-plantuml:1.2025.3")
    implementation("org.asciidoctor:asciidoctorj-diagram-ditaamini:1.0.3")
    implementation("org.asciidoctor:asciidoctorj-epub3:2.2.0")

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
// Embed build metadata into version.properties at compile time
// ---------------------------------------------------------------------------
tasks.processResources {
    filesMatching("**/version.properties") {
        filter { line ->
            line
                .replace("@version@", version.toString())
                .replace("@gitCommit@", gitCommit)
                .replace("@gitBranch@", gitBranch)
                .replace("@buildTime@", buildTime)
        }
    }
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
// Distribution: tgz with fat JAR + LICENSE + README
// ---------------------------------------------------------------------------
val buildTgz by tasks.registering(Tar::class) {
    dependsOn(fatJar, tasks.test)
    archiveFileName.set("ezdoctor-${version}.tar.gz")
    destinationDirectory.set(layout.buildDirectory.dir("dist"))
    compression = Compression.GZIP

    from(fatJar.get().archiveFile) {
        rename { "ezdoctor.jar" }
    }
    from(rootDir) {
        include("LICENSE", "README.md")
    }
}
