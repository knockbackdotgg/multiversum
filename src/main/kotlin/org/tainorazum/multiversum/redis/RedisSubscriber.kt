package org.tainorazum.multiversum.redis

import com.google.gson.Gson
import com.velocitypowered.api.proxy.ProxyServer
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPubSub
import java.util.UUID
import java.util.concurrent.ForkJoinPool

object RedisSubscriber {

    private val gson = Gson()

    fun startListening(proxy: ProxyServer, redisUri: String) {
        ForkJoinPool.commonPool().execute {
            try {
                val jedis = Jedis(redisUri)
                jedis.subscribe(object : JedisPubSub() {
                    override fun onMessage(channel: String, message: String) {
                        if (channel == "velocity_send_player") {
                            handleSendPlayer(proxy, message)
                        }
                    }
                }, "velocity_send_player")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleSendPlayer(proxy: ProxyServer, json: String) {
        try {
            val data = gson.fromJson(json, Map::class.java)
            val uuidString = data["uuid"] as? String ?: return
            val targetServerName = data["targetServer"] as? String ?: return

            val uuid = UUID.fromString(uuidString)
            val player = proxy.getPlayer(uuid).orElse(null) ?: return
            val targetServer = proxy.getServer(targetServerName).orElse(null) ?: return

            player.createConnectionRequest(targetServer).connect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}