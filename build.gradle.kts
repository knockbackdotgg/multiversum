plugins {
    kotlin("jvm") version "2.4.20-Beta2"
    id("com.gradleup.shadow") version "9.6.1"
    id("xyz.jpenilla.run-velocity") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")

    // redis
    implementation("redis.clients:jedis:5.1.0")

    // luckperms
    compileOnly("net.luckperms:api:5.5")

    // ProtocolLib for packet-level listeners (compileOnly; server should provide the plugin)
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")
}

kotlin {
    jvmToolchain(21)
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    runVelocity {
        // Configure the Velocity version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        velocityVersion("3.5.0-SNAPSHOT")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("velocity-plugin.json") {
            expand(props)
        }
    }
}
