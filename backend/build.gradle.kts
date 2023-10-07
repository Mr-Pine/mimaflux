plugins {
    id("java")
    id("antlr")
}

group = "edu.kit.kastel.formal"
version = "1.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
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

tasks.generateGrammarSource {
    maxHeapSize = "64m"
    arguments.addAll(listOf("-visitor", "-no-listener"))
}

tasks.test {
    useJUnitPlatform()
}