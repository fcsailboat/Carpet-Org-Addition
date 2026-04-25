plugins {
    kotlin("jvm") version "2.3.20"
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
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}