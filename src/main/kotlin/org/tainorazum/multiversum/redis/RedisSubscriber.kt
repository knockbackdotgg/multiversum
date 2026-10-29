package org.tainorazum.multiversum.redis

import com.google.gson.Gson
import com.velocitypowered.api.proxy.ProxyServer
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPubSub
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

data class PlayerSendPayload(
    val uuid: String,
    val targetServer: String
)

object RedisSubscriber {

    private val gson = Gson()
    private val redisListenerThread = AtomicReference<Thread?>()

    fun startListening(proxy: ProxyServer, redisUri: String) {
        val existing = redisListenerThread.get()
        if (existing != null && existing.isAlive) {
            return
        }

        val thread = Thread {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    Jedis(redisUri).use { jedis ->
                        jedis.subscribe(object : JedisPubSub() {
                            override fun onMessage(channel: String, message: String) {
                                if (channel == "velocity_player_send") {
                                    handleSendPlayer(proxy, message)
                                }
                            }
                        }, "velocity_player_send")
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                } catch (e: Exception) {
                    e.printStackTrace()
                    try {
                        Thread.sleep(5000L)
                    } catch (sleepEx: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                }
            }
        }

        redisListenerThread.set(thread)
        thread.name = "multiversum-redis-subscriber"
        thread.isDaemon = true
        thread.start()
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