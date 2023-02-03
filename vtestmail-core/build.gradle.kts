import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.util.*

plugins {
    `java-library`
    jacoco

    // run Sonar analysis
    id("org.sonarqube") version "3.5.0.2730"

    // get current Git branch name
    id("org.ajoberstar.grgit") version "5.0.0"

    // Gradle Versions Plugin
    // https://github.com/ben-manes/gradle-versions-plugin
    id("com.github.ben-manes.versions") version "0.44.0"

    // JarHC Gradle plugin
    id("org.jarhc") version "1.0.1"
}

// load user-specific properties -----------------------------------------------

val userPropertiesFile = file("${projectDir}/gradle.user.properties")
if (userPropertiesFile.exists()) {
    val userProperties = Properties()
    userProperties.load(userPropertiesFile.inputStream())
    userProperties.forEach {
        project.ext.set(it.key.toString(), it.value)
    }
}

// Java version check ----------------------------------------------------------

if (!JavaVersion.current().isJava11Compatible) {
    val error = "Build requires Java 11 and does not run on Java ${JavaVersion.current().majorVersion}."
    throw GradleException(error)
}

// Preconditions based on which tasks should be executed -----------------------

gradle.taskGraph.whenReady {

    // if sonar task should be executed ...
    if (gradle.taskGraph.hasTask(":sonar")) {
        // environment variable SONAR_TOKEN or property "sonar.login" must be set
        val tokenFound = project.hasProperty("sonar.login") || System.getenv("SONAR_TOKEN") != null
        if (!tokenFound) {
            val error = "Sonar: Token not found.\nPlease set property 'sonar.login' or environment variable 'SONAR_TOKEN'."
            throw GradleException(error)
        }
    }

}

// dependencies ----------------------------------------------------------------

repositories {
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.mockito:mockito-core:5.1.1")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("com.sun.mail:jakarta.mail:2.0.1")
    testImplementation("commons-net:commons-net:3.9.0")

}

// build -----------------------------------------------------------------------

java {

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }

    // automatically package source code as artifact -sources.jar
    withSourcesJar()

    // automatically package Javadoc as artifact -javadoc.jar
    withJavadocJar()
}

jacoco {
    toolVersion = "0.8.8"
}

tasks.withType<JavaCompile> {
    options.encoding = "ASCII"
}

for (version in listOf(12, 13, 14, 15, 16, 17, 18, 19, 20, 21)) {
    tasks.register<Test>("testOnJava$version") {
        group = "verification"
        description = "Runs the test suite on Java $version."
        javaLauncher.set(javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(version))
        })
    }
}

tasks.withType<Test> {

    // use JUnit 5
    useJUnitPlatform()

    // settings
    maxHeapSize = "1G"

    // configure Java Logging (JUL) for tests
    systemProperty("java.util.logging.config.file", "${projectDir}/src/test/resources/logging.properties")

    // test task output
    testLogging {
        events = mutableSetOf(
            TestLogEvent.STARTED,
            TestLogEvent.PASSED,
            TestLogEvent.FAILED,
            TestLogEvent.SKIPPED,
            TestLogEvent.STANDARD_OUT,
            TestLogEvent.STANDARD_ERROR
        )
        showStandardStreams = true
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }

}

tasks {

    jacocoTestReport {

        // run all tests first
        dependsOn(test)

        // get JaCoCo data from all test tasks
        executionData.from(
            "${buildDir}/jacoco/test.exec"
        )

        reports {

            // generate XML report (required for Sonar)
            xml.required.set(true)
            xml.outputLocation.set(file("${buildDir}/reports/jacoco/test/report.xml"))

            // generate HTML report
            html.required.set(true)

            // generate CSV report
            // csv.required.set(true)
        }
    }

    jarhcReport {
        dependsOn(jar)
        classpath.setFrom(
            jar.get().archiveFile,
            configurations.runtimeClasspath
        )
        reportFiles.setFrom(
            file("${rootDir}/docs/jarhc-report.html"),
            file("${rootDir}/docs/jarhc-report.txt")
        )
    }

    build {
        dependsOn(jarhcReport)
    }

}

sonar {
    // documentation: https://docs.sonarqube.org/latest/analysis/scan/sonarscanner-for-gradle/

    properties {

        // connection to SonarCloud
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.organization", "smarkwal")
        property("sonar.projectKey", "smarkwal_vtestmail")

        // Git branch
        property("sonar.branch.name", getGitBranchName())

        // paths to test sources and test classes
        property("sonar.tests", "${projectDir}/src/test/java")
        property("sonar.java.test.binaries", "${buildDir}/classes/java/test")

        // include test results
        property("sonar.junit.reportPaths", "${buildDir}/test-results/test")

        // include test coverage results
        property("sonar.java.coveragePlugin", "jacoco")
        property("sonar.coverage.jacoco.xmlReportPaths", "${buildDir}/reports/jacoco/test/report.xml")
    }
}

tasks.sonar {
    // run all tests and generate JaCoCo XML report
    dependsOn(
        tasks.test,
        tasks.jacocoTestReport
    )
}

// disable generation of Gradle module metadata file
tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

tasks.register("createKeystore") {
    group = "other"
    description = "Creates a keystore with an RSA keypair and self-signed certificate for localhost."
    doLast {
        val keystore = file("src/main/resources/vtestmail.pfx")
        if (keystore.exists()) keystore.delete()
        exec {
            commandLine(
                "keytool", "-genkey",
                "-alias", "localhost",
                "-dname", "CN=localhost",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-sigalg", "SHA256WithRSA",
                "-validity", "3653", // 10 years
                "-keypass", "changeit",
                "-storetype", "PKCS12",
                "-storepass", "changeit",
                "-keystore", keystore.absolutePath
            )
        }
    }
}

// helper functions ------------------------------------------------------------

fun getGitBranchName(): String {
    return grgit.branch.current().name
}
