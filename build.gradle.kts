import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

defaultTasks("clean", "build")

allprojects {
    group = "com.vaadin.unified-test-mcp"
    version = "1.0-SNAPSHOT"

    repositories {
        // the Unified API and Dramafinder built locally, gradle/libs.versions.toml; nothing else from ~/.m2
        mavenLocal {
            content { includeVersionByRegex("org\\.vaadin\\.addons", "unified-test-.*|dramafinder", ".*-shadow") }
        }
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    dependencies {
        "testImplementation"(rootProject.libs.junit)
        "testRuntimeOnly"(rootProject.libs.junit.launcher)
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        // --release, not a toolchain: builds on any installed JDK 21+, never downloads one
        options.release = 21
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events = setOf(TestLogEvent.FAILED)
            exceptionFormat = TestExceptionFormat.FULL
            showCauses = true
        }
    }
}
