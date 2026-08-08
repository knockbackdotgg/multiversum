package org.tainorazum.multiversum.redis

import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.net.URI

object RedisPublisher {

    private var pool: JedisPool? = null

    fun init(uriString: String) {
        val config = JedisPoolConfig().apply {
            maxTotal = 8
            maxIdle = 8
        }

        val uri = URI.create(uriString)
        pool = JedisPool(config, uri)
    }

    fun publish(channel: String, message: String) {
        pool?.resource?.use { jedis ->
            jedis.publish(channel, message)
        }
    }

    fun close() {
        pool?.close()
    }
}