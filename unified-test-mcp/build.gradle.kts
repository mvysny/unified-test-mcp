plugins {
    application
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.tinymcpserver)
    implementation(libs.unified.test.playwright) {
        // there for an app under test running in the same JVM; ours runs elsewhere
        exclude(group = "com.vaadin", module = "vaadin-dev")
    }
    // the Unified API declares it `provided`; it needs the component classes (Button.class), runs none of them
    implementation(libs.vaadin.core)

    // the system test boots the test app in the test JVM
    testImplementation(project(":test-app"))
    testImplementation(libs.vaadin.boot)
}

tasks.jar {
    // the released jar is the shadow jar; this one steps aside from its file name
    archiveClassifier = "plain"
    manifest {
        attributes("Implementation-Version" to project.version)
    }
}

// the released artifact: unified-test-mcp-<version>.jar, every dependency inside, run with `java -jar`
tasks.shadowJar {
    archiveClassifier = ""
    // Shadow's default, EXCLUDE, drops a duplicate path before mergeServiceFiles() sees it
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()
    // INCLUDE would otherwise leave several entries of one name; this keeps every dependency's text in one
    listOf("META-INF/LICENSE", "META-INF/LICENSE.md", "META-INF/LICENSE-notice.md", "META-INF/NOTICE")
        .forEach { append(it) }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

// the jar is the only distribution; no zip or tar of start scripts
listOf("distZip", "distTar", "shadowDistZip", "shadowDistTar").forEach {
    tasks.named(it) { enabled = false }
}

application {
    mainClass = "com.vaadin.unifiedtestmcp.Main"
}
