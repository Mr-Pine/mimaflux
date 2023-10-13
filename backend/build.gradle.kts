plugins {
    id("java-library")
    id("antlr")
    id("maven-publish")
}

group = "edu.kit.kastel.formal"
version = "1.2.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    withSourcesJar()
    withJavadocJar()
}

tasks.named("sourcesJar") {
    dependsOn(tasks.generateGrammarSource)
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    // Runtime only needs antlr4-runtime, not the whole antlr4.
    implementation("org.antlr:antlr4-runtime:4.13.1")

    // Customise antlr4 version, for buildscript
    antlr("org.antlr:antlr4:4.13.1")
}

publishing {
    publications {
        val mimafluxCapacitor by creating(MavenPublication::class.java) {
            from(components["java"])
            artifactId = "mimaflux-capacitor"
        }
    }
}

tasks.generateGrammarSource {
    maxHeapSize = "64m"
    arguments.addAll(listOf("-visitor", "-no-listener"))
}

tasks.test {
    useJUnitPlatform()
}
