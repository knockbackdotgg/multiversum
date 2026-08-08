package org.tainorazum.multiversum.listeners

import com.google.gson.Gson
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.proxy.ProxyServer
import org.tainorazum.multiversum.redis.RedisPublisher
import org.tainorazum.multiversum.tab.TabManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class VelocityNetworkBridge(
    private val proxy: ProxyServer,
    private val tabManager: TabManager
) {

    private val gson = Gson()
    private val playerServerMap = ConcurrentHashMap<UUID, String>()

    @Subscribe
    fun onServerConnected(event: ServerConnectedEvent) {
        val player = event.player
        val previousServer = event.previousServer.orElse(null)?.serverInfo?.name
        val currentServer = event.server.serverInfo.name

        playerServerMap[player.uniqueId] = currentServer

        val isNetworkJoin = previousServer == null

        val eventData = mapOf(
            "uuid" to player.uniqueId.toString(),
            "currentServer" to currentServer,
            "previousServer" to (previousServer ?: ""),
            "isNetworkJoin" to isNetworkJoin
        )

        // 1. Publish Redis Event for Paper backend sync
        RedisPublisher.publish("velocity_player_switch", gson.toJson(eventData))

        // 2. Handle Tab List appropriately
        if (isNetworkJoin) {
            // Full setup: player just joined Velocity proxy
            tabManager.onPlayerJoin(player)
        } else {
            // Server hop: player is already in Tab; just update their profile/display name
            tabManager.updatePlayerDisplayName(player)
        }
    }

    @Subscribe
    fun onDisconnect(event: DisconnectEvent) {
        val player = event.player
        val lastServer = playerServerMap.remove(player.uniqueId) ?: ""

        val eventData = mapOf(
            "uuid" to player.uniqueId.toString(),
            "lastServer" to lastServer,
            "isNetworkQuit" to true
        )

        RedisPublisher.publish("velocity_player_disconnect", gson.toJson(eventData))

        // Clean up player from everyone's Tab list
        tabManager.onPlayerQuit(player)
    }
}