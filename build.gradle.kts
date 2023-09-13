plugins {
    id("fabric-loom")
    val kotlinVersion: String by System.getProperties()
    kotlin("jvm").version(kotlinVersion)
}
base {
    val archivesBaseName: String by project
    archivesName.set(archivesBaseName)
}
val modVersion: String by project
version = modVersion
val mavenGroup: String by project
group = mavenGroup
//minecraft {}
repositories {
    maven {
        name = "CottonMC"
        setUrl("https://server.bbkr.space/artifactory/libs-release")
    }
    maven {
        name = "Ladysnake Mods"
        setUrl("https://maven.ladysnake.org/releases")
    }
    maven {
        name = "TerraformersMC Maven"
        setUrl("https://maven.terraformersmc.com/releases")
    }
    maven {
        name = "JitPack"
        setUrl("https://jitpack.io")
    }
    maven {
        setUrl("https://maven.shedaniel.me/")
    }
    maven {
        setUrl("https://maven.bai.lol")
    }
    maven("https://maven.draylar.dev/releases")

}
dependencies {
    val minecraftVersion: String by project
    minecraft("com.mojang:minecraft:$minecraftVersion")
    val yarnMappings: String by project
    mappings("net.fabricmc:yarn:$yarnMappings:v2")
    val loaderVersion: String by project
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    val fabricVersion: String by project
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")

    val fabricKotlinVersion: String by project
    modImplementation("net.fabricmc:fabric-language-kotlin:$fabricKotlinVersion")

    val libGuiVersion: String by project
    modImplementation("io.github.cottonmc:LibGui:$libGuiVersion")
    include("io.github.cottonmc:LibGui:$libGuiVersion")

    val cardinalComponentsVersion: String by project
    modImplementation("dev.onyxstudios.cardinal-components-api:cardinal-components-base:$cardinalComponentsVersion")
    include("dev.onyxstudios.cardinal-components-api:cardinal-components-base:$cardinalComponentsVersion")

    modImplementation("dev.onyxstudios.cardinal-components-api:cardinal-components-entity:$cardinalComponentsVersion")
    include("dev.onyxstudios.cardinal-components-api:cardinal-components-entity:$cardinalComponentsVersion")

    val modmenuVersion: String by project
    modImplementation("com.terraformersmc:modmenu:$modmenuVersion")

    val trinketsVersion: String by project
    modImplementation("dev.emi:trinkets:$trinketsVersion")

    val wthitVersion: String by project
    modCompileOnly("mcp.mobius.waila:wthit-api:fabric-$wthitVersion")
    modRuntimeOnly("mcp.mobius.waila:wthit:fabric-$wthitVersion")

    val omegaConfigVersion: String by project
    modImplementation("dev.draylar.omega-config:omega-config-base:$omegaConfigVersion")
    include("dev.draylar.omega-config:omega-config-base:$omegaConfigVersion")

    modImplementation("com.ezylang:EvalEx:3.0.2")
    include("com.ezylang:EvalEx:3.0.2")
}
tasks {
    val javaVersion = JavaVersion.VERSION_17
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
        options.release.set(javaVersion.toString().toInt())
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions { jvmTarget = javaVersion.toString() }
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
    }
    jar { from("LICENSE", "LICENSE_EvalEx") }
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") { expand(mutableMapOf("version" to project.version)) }
    }
    java {
        toolchain { languageVersion.set(JavaLanguageVersion.of(javaVersion.toString())) }
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        withSourcesJar()
    }
}