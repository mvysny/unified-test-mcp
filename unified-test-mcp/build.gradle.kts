plugins {
    application
}

dependencies {
    implementation(libs.tinymcpserver)
    implementation(libs.unified.test.playwright) {
        // there for an app under test running in the same JVM; ours runs elsewhere
        exclude(group = "com.vaadin", module = "vaadin-dev")
    }
    // the Unified API declares it `provided`; it needs the component classes (Button.class), runs none of them
    implementation(libs.vaadin.core)
}

application {
    mainClass = "com.vaadin.unifiedtestmcp.Main"
}
