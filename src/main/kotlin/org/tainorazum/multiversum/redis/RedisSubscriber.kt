package org.tainorazum.multiversum.redis

import com.google.gson.Gson
import com.velocitypowered.api.proxy.ProxyServer
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPubSub
import java.util.UUID
import java.util.concurrent.ForkJoinPool

data class PlayerSendPayload(
    val uuid: String,
    val targetServer: String
)

object RedisSubscriber {

    private val gson = Gson()

    fun startListening(proxy: ProxyServer, redisUri: String) {
        ForkJoinPool.commonPool().execute {
            try {
                val jedis = Jedis(redisUri)
                jedis.subscribe(object : JedisPubSub() {
                    override fun onMessage(channel: String, message: String) {
                        if (channel == "velocity_player_send") {
                            handleSendPlayer(proxy, message)
                        }
                    }
                }, "velocity_player_send")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleSendPlayer(proxy: ProxyServer, json: String) {
        try {
            val payload = gson.fromJson(json, PlayerSendPayload::class.java) ?: return

            val uuid = UUID.fromString(payload.uuid)
            val player = proxy.getPlayer(uuid).orElse(null) ?: return
            val targetServer = proxy.getServer(payload.targetServer).orElse(null) ?: return

            player.createConnectionRequest(targetServer).connect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}