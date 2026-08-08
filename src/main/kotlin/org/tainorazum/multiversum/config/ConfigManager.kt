package org.tainorazum.multiversum.config

import java.io.File

object ConfigManager {

    /**
     * Simple utility to fetch the `redis.uri` from config.yml without heavy libraries.
     */
    fun getRedisUri(file: File): String {
        if (!file.exists()) return "redis://localhost:6379"

        var inRedisSection = false

        file.useLines { lines ->
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("#") || trimmed.isEmpty()) continue

                // Check for 'redis:' section header
                if (trimmed == "redis:") {
                    inRedisSection = true
                    continue
                }

                // Match 'uri: ...' or 'redis.uri: ...'
                if (inRedisSection && trimmed.startsWith("uri:")) {
                    return trimmed.substringAfter("uri:").trim().removeSurrounding("\"")
                }
                if (trimmed.startsWith("redis.uri:")) {
                    return trimmed.substringAfter("redis.uri:").trim().removeSurrounding("\"")
                }
            }
        }

        return "redis://localhost:6379"
    }
}