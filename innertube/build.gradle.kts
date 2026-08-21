plugins {
    kotlin("jvm")
    @Suppress("DSL_SCOPE_VIOLATION")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.encoding)
    implementation(libs.brotli)
    implementation(libs.pipepipe.extractor) {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }
    implementation(libs.brave.extractor)
    implementation(libs.re2j)
    implementation(libs.rhino)
    implementation(libs.okhttp.dnsoverhttps)
    testImplementation(libs.junit)
}
