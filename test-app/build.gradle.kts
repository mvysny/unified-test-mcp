plugins {
    application
    alias(libs.plugins.vaadin)
}

dependencies {
    implementation(libs.vaadin.core)
    if (!vaadin.effective.productionMode.get()) {
        implementation(libs.vaadin.dev)
    }
    implementation(libs.vaadin.boot)
    implementation(libs.jetbrains.annotations)
    implementation(libs.slf4j.simple)

    testImplementation(libs.karibu.testing)
}

application {
    mainClass = "com.vaadin.unifiedtestmcp.testapp.Main"
}
