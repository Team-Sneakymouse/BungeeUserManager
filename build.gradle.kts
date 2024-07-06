plugins {
    kotlin("jvm") version "1.8.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://repo.md-5.net/content/repositories/releases/")
}

dependencies {
    implementation(kotlin("stdlib"))
    compileOnly("net.md-5:bungeecord-api:1.20-R0.1")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

sourceSets {
    main {
        java.srcDir("src/main/kotlin")
        resources.srcDir("src/resources")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.shadowJar {
    archiveClassifier.set("")
    from(sourceSets.main.get().output)
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
