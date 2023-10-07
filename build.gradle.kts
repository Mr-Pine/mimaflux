import org.ajoberstar.grgit.Grgit

plugins {
    id("java")
    id("application")
    id("antlr")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.github.hierynomus.license") version "0.16.1"
    id("org.ajoberstar.grgit") version "5.2.0"
}

group = "edu.kit.kastel.formal"
version = "1.1.0"
val grgit: Grgit = Grgit.open(mapOf("currentDir" to project.rootDir))

val versionFile by tasks.registering {
    val resourcesDir = sourceSets.main.get().output.resourcesDir!!
    outputs.dir(resourcesDir)
    doFirst {
        resourcesDir.mkdirs()
        File(resourcesDir, "VERSION").writeText("$version (${grgit.head().abbreviatedId})")
        println( "$resourcesDir/VERSION created.")
    }
}

tasks.compileJava {
    dependsOn(versionFile)
}

repositories {
    mavenCentral()
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "edu.kit.kastel.formal.mimaflux.MimaFlux"
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

application {
    mainClass = "edu.kit.kastel.formal.mimaflux.MimaFlux"
}

tasks.shadowJar {
    dependsOn(versionFile)
    dependencies {
        exclude(dependency("org.antlr:antlr4:.*"))
        exclude(dependency("com.ibm.icu:.*:.*"))
        exclude(dependency("org.antlr:ST4:.*"))
    }
}

//configurations {
//    implementation {
//        extendsFrom = extendsFrom.findAll { it != configurations.antlr }
//    }
//}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")

    // Runtime only needs antlr4-runtime, not the whole antlr4.
    implementation("org.antlr:antlr4-runtime:4.13.1")

    // Customise antlr4 version, for buildscript
    antlr("org.antlr:antlr4:4.13.1")

    implementation("com.beust:jcommander:1.82")
    implementation("org.kordamp.ikonli:ikonli-swing:12.3.1")
    implementation("org.kordamp.ikonli:ikonli-codicons-pack:12.3.1")
}

tasks.generateGrammarSource {
    maxHeapSize = "64m"
    arguments.addAll(listOf("-visitor", "-no-listener"))
}

tasks.test {
    useJUnitPlatform()
}

license {
    header = file("HEADER")
    include("**/*.java")
    include("**/*.g4")
    mapping(
        mapOf(
            "java" to "SLASHSTAR_STYLE",
            "g4" to "SLASHSTAR_STYLE"
        )
    )
}
