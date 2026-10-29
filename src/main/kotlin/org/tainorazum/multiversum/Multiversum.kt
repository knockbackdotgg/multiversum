package org.tainorazum.multiversum

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import org.slf4j.Logger
import org.tainorazum.multiversum.config.ConfigManager
import org.tainorazum.multiversum.listeners.CommandListener
import org.tainorazum.multiversum.listeners.VelocityNetworkBridge
import org.tainorazum.multiversum.redis.RedisPublisher
import org.tainorazum.multiversum.redis.RedisSubscriber
import org.tainorazum.multiversum.tab.TabManager
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Plugin(
    id = "multiversum",
    name = "multiversum",
    version = "1.0",
    authors = ["tainorazum"]
)
class Multiversum @Inject constructor(
    val proxy: ProxyServer,
    val logger: Logger
) {

    lateinit var tabManager: TabManager
        private set

    private val dataDirectory: Path = Paths.get("plugins", "multiversum")

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        val configFile = saveDefaultConfig()
        val redisUri = ConfigManager.getRedisUri(configFile)
        RedisPublisher.init(redisUri)

        // Start listening for transfer requests over Redis
        RedisSubscriber.startListening(proxy, redisUri)

        tabManager = TabManager(this, proxy)
        tabManager.init()

        proxy.eventManager.register(this, VelocityNetworkBridge(proxy, tabManager))
        proxy.eventManager.register(this, CommandListener(proxy))

        logger.info("Multiversum initialized.")
    }

    private fun saveDefaultConfig(): File {
        val folder = dataDirectory.toFile()
        if (!folder.exists()) folder.mkdirs()

        val file = File(folder, "config.yml")
        if (!file.exists()) {
            javaClass.classLoader.getResourceAsStream("config.yml")?.use { input ->
                Files.copy(input, file.toPath())
            }
        }
        return file
    }
}