import org.ajoberstar.grgit.Grgit

plugins {
    id("java")
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.github.hierynomus.license") version "0.16.1"
    id("org.ajoberstar.grgit") version "5.2.0"
}

group = "edu.kit.kastel.formal"
version = "1.2.0"
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

tasks.shadowJar {
    dependsOn(versionFile)
    dependencies {
        exclude(dependency("org.antlr:antlr4:.*"))
        exclude(dependency("com.ibm.icu:.*:.*"))
        exclude(dependency("org.antlr:ST4:.*"))
    }
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

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("com.beust:jcommander:1.82")
    implementation("org.kordamp.ikonli:ikonli-swing:12.3.1")
    implementation("org.kordamp.ikonli:ikonli-codicons-pack:12.3.1")

    implementation(project(":backend"))
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
