plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.16"
    id("com.diffplug.spotless") version "7.0.4"
    id("com.gradleup.shadow") version "8.3.6"
    id("io.freefair.lombok") version "9.2.0"
}

group = "fr.hyping.hypingauctions"
version = "1.7.0"

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io")
    mavenCentral()
    maven("https://repo.tcoded.com/releases")
    maven("https://repo.opencollab.dev/main/")
}

dependencies {
    paperweight.foliaDevBundle("1.21.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.5")

    compileOnly(files("libs/hypingmenus-1.7.23.jar"))
    compileOnly(files("libs/oraxen-1.189.3.jar"))
    compileOnly("org.mongodb:mongodb-driver-sync:5.1.0")
    compileOnly(files("libs/hypingcounters-2.7.0.jar"))
    compileOnly("com.tcoded.hyping:BedrockUtilAPI:1.0.0")
    compileOnly(files("libs/hypingitems-1.17.27.jar"))
    compileOnly(files("libs/HypingCore-2.20.7.jar"))
    
    implementation(files("libs/signgui-2.5.3.jar"))
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.0")
    implementation("org.mongodb:mongodb-driver-sync:5.1.0")
    implementation("fr.mrmicky:FastInv:3.1.1")
    compileOnly("org.geysermc.floodgate:api:2.2.4-SNAPSHOT")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks {
    assemble {
        dependsOn(clean)
    }

    processResources {
        val props = mapOf("version" to version)

        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    jar {
        archiveClassifier.set("raw")
        manifest {
            attributes["paperweight-mappings-namespace"] = "mojang"
        }
    }

    shadowJar {
        archiveClassifier.set("")
        relocate("com.mongodb", "fr.hyping.hypingauctions.libs.mongo")
    }

    build {
        dependsOn(shadowJar)
    }
}
