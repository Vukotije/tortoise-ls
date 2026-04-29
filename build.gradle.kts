plugins {
    kotlin("jvm") version "2.3.20"
    application
}

group = "dev.tortoise"
version = "0.1.0-SNAPSHOT"

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("dev.tortoise.server.bootstrap.MainKt")
}

dependencies {
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:1.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.3")
}

tasks.test {
    useJUnitPlatform()
}
