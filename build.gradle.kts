import net.minecraftforge.gradle.userdev.UserDevExtension

val modName: String by ext.properties
val modGroup: String by ext.properties
val modVersion: String by ext.properties
val mcVersion: String by ext.properties
val mcpChannel: String by ext.properties
val mcpVersion: String by ext.properties
val forgeVersion: String by ext.properties
val pixelmonVersion: String by ext.properties
val spongeVersion: String by ext.properties

buildscript {
    repositories {
        mavenCentral()
        maven(url = "https://maven.minecraftforge.net/")
    }
    dependencies {
        classpath("net.minecraftforge.gradle:ForgeGradle:5.1.+") {
            isChanging = true
        }
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.30")
    }
}

plugins {
    java
    kotlin("jvm") version "1.5.30"
    id("net.kyori.blossom") version "1.3.0"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("org.spongepowered.gradle.plugin") version "2.0.0"
}

apply(plugin = "net.minecraftforge.gradle")

version = modVersion
group = modGroup

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8

configure<UserDevExtension> {

    mappings(mcpChannel,  mcpVersion)

}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
    ivy {
        setUrl("https://download.nodecdn.net/containers/reforged/server/release")
        metadataSources {
            artifact()
        }
        patternLayout {
            artifact("[revision]/[artifact].[ext]")
        }
    }
}

val minecraft by configurations

dependencies {
    minecraft("net.minecraftforge:forge:$forgeVersion")
    shadow("com.github.randombyte-developer.kosp:kosp:v2.2.3")
    compileOnly("pixelmon:Pixelmon-1.12.2-$pixelmonVersion-server:$pixelmonVersion")
    compileOnly("org.spongepowered:spongeapi:$spongeVersion")
}

sponge {
    apiVersion(spongeVersion)
    license("GPL-2.0")
    loader {
        name(org.spongepowered.gradle.plugin.config.PluginLoaders.JAVA_PLAIN)
        version(modVersion)
    }
    plugin("entity-particles") {
        displayName(modName)
        entrypoint("de.randombyte.entityparticles.EntityParticles")
        description("A sponge plugin")
        contributor("RandomByte") {
            description("Original Author")
        }
        contributor("Licious") {
            description("Bumped Kotlin version")
        }
        dependency("spongeapi") {
            loadOrder(org.spongepowered.plugin.metadata.model.PluginDependency.LoadOrder.AFTER)
            optional(false)
        }
    }
}

blossom {
    replaceTokenIn("src/main/kotlin/de/randombyte/entityparticles/EntityParticles.kt")
    replaceToken("@version@", modVersion)
}

tasks {

    shadowJar {
        archiveBaseName.set(modName)
        archiveClassifier.set("")
        archiveVersion.set(modVersion)
        configurations = listOf(project.configurations.shadow.get())

        relocate ("de.randombyte.kosp", "de.randombyte.entityparticles.shaded.kosp")

        exclude("module-info.class", "META-INF/maven/**", "META-INF/proguard/**", "META-INF/versions/**")
        minimize()

        finalizedBy("reobfJar")
    }

    artifacts {
        archives(shadowJar)
        shadow(shadowJar)
    }

}
