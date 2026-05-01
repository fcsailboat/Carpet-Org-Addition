plugins {
    kotlin("jvm") version "2.3.20"
    id("com.gradleup.shadow") version "8.3.0"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.6.0.202603022253-r")
    @Suppress("all")
    implementation("ch.qos.logback:logback-classic:1.5.13")
    implementation("org.apache.httpcomponents:httpclient:4.5.14")
    @Suppress("all")
    implementation("org.apache.httpcomponents.client5:httpclient5-fluent:5.4")
    implementation("com.formdev:flatlaf:3.7.1")
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("PublisherKt")
}

tasks.shadowJar {
    archiveBaseName.set("Publisher")
    archiveClassifier.set("")
    archiveVersion.set("1.0.0")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir.parentFile
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
