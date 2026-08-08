package org.tainorazum.multiversum.tab

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.player.TabListEntry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.event.user.UserDataRecalculateEvent
import java.util.concurrent.TimeUnit

class TabManager(
    private val plugin: Any,
    private val proxyServer: ProxyServer
) {
    private val legacySerializer = LegacyComponentSerializer.legacyAmpersand()
    private var luckPerms: LuckPerms? = null

    init {
        try {
            luckPerms = LuckPermsProvider.get()
        } catch (_: Exception) {
            // LuckPerms proxy extension not installed yet
        }
    }

    /**
     * Initializes background sync tasks and hooks into LuckPerms events on Velocity.
     */
    fun init() {
        // Subscribe to LuckPerms permission/rank recalculation events on Proxy
        luckPerms?.eventBus?.subscribe(plugin, UserDataRecalculateEvent::class.java) { event ->
            val player = proxyServer.getPlayer(event.user.uniqueId).orElse(null) ?: return@subscribe
            updatePlayerDisplayName(player)
        }

        // Periodically refresh player counts and ping latency every 3 seconds
        proxyServer.scheduler.buildTask(plugin, Runnable {
            updateAll()
        }).repeat(3L, TimeUnit.SECONDS).schedule()
    }

    /**
     * Called when a player joins the proxy network or switches sub-servers.
     */
    fun onPlayerJoin(player: Player) {
        val totalPlayers = proxyServer.allPlayers.size
        player.sendPlayerListHeaderAndFooter(buildHeader(totalPlayers), buildFooter())

        val joiningDisplayName = formatPlayerName(player)

        for (target in proxyServer.allPlayers) {
            // 1. Send all existing players to the NEW player's TabList
            if (!player.tabList.containsEntry(target.uniqueId)) {
                player.tabList.addEntry(
                    TabListEntry.builder()
                        .tabList(player.tabList)
                        .profile(target.gameProfile)
                        .displayName(formatPlayerName(target))
                        .latency(target.ping.toInt().coerceAtLeast(0))
                        .gameMode(0)
                        .build()
                )
            }

            // 2. Add NEW player to existing players' TabLists (if not already present)
            if (target.uniqueId != player.uniqueId && !target.tabList.containsEntry(player.uniqueId)) {
                target.tabList.addEntry(
                    TabListEntry.builder()
                        .tabList(target.tabList)
                        .profile(player.gameProfile)
                        .displayName(joiningDisplayName)
                        .latency(player.ping.toInt().coerceAtLeast(0))
                        .gameMode(0)
                        .build()
                )
            }
        }

        updateHeaderAndFooterForAll()
    }

    /**
     * Called when a player leaves the proxy network.
     */
    fun onPlayerQuit(player: Player) {
        val quittingUuid = player.uniqueId

        // Remove quitting player from all remaining online players' TabLists
        for (target in proxyServer.allPlayers) {
            if (target.uniqueId != quittingUuid) {
                target.tabList.removeEntry(quittingUuid)
            }
        }

        updateHeaderAndFooterForAll()
    }

    /**
     * Syncs header/footer and updates existing entries for all connected proxy players.
     */
    fun updateAll() {
        val players = proxyServer.allPlayers
        if (players.isEmpty()) return

        val header = buildHeader(players.size)
        val footer = buildFooter()

        for (player in players) {
            player.sendPlayerListHeaderAndFooter(header, footer)

            for (target in players) {
                val entry = player.tabList.getEntry(target.uniqueId).orElse(null)
                if (entry != null) {
                    entry.setDisplayName(formatPlayerName(target))
                    entry.setLatency(target.ping.toInt().coerceAtLeast(0))
                } else {
                    player.tabList.addEntry(
                        TabListEntry.builder()
                            .tabList(player.tabList)
                            .profile(target.gameProfile)
                            .displayName(formatPlayerName(target))
                            .latency(target.ping.toInt().coerceAtLeast(0))
                            .gameMode(0)
                            .build()
                    )
                }
            }
        }
    }

    private fun updateHeaderAndFooterForAll() {
        val totalCount = proxyServer.allPlayers.size
        val header = buildHeader(totalCount)
        val footer = buildFooter()

        for (player in proxyServer.allPlayers) {
            player.sendPlayerListHeaderAndFooter(header, footer)
        }
    }

    /**
     * Updates a specific player's display name dynamically across all screens (e.g. on rank change).
     */
    fun updatePlayerDisplayName(target: Player) {
        val newDisplayName = formatPlayerName(target)
        for (viewer in proxyServer.allPlayers) {
            viewer.tabList.getEntry(target.uniqueId).ifPresent { entry ->
                entry.setDisplayName(newDisplayName)
            }
        }
    }

    /**
     * Formats player name using LuckPerms prefix on Velocity.
     */
    private fun formatPlayerName(player: Player): Component {
        val lp = luckPerms ?: return Component.text(player.username, NamedTextColor.GRAY)
        val user = lp.userManager.getUser(player.uniqueId)
        val prefixRaw = user?.cachedData?.metaData?.prefix ?: ""

        val prefixComponent = if (prefixRaw.isNotBlank()) {
            legacySerializer.deserialize(prefixRaw).append(Component.text(" "))
        } else {
            Component.empty()
        }

        return prefixComponent.append(Component.text(player.username, NamedTextColor.GRAY))
    }

    private fun buildHeader(totalPlayers: Int): Component {
        val countText = if (totalPlayers == 1) "player" else "players"
        return Component.text("\n")
            .append(Component.text("knockback.gg", NamedTextColor.GREEN))
            .append(Component.text("\n\n"))
            .append(Component.text(totalPlayers, NamedTextColor.GRAY))
            .append(Component.text(" $countText", NamedTextColor.GRAY))
    }

    private fun buildFooter(): Component {
        return Component.text("\n")
            .append(Component.text("🗡", NamedTextColor.GREEN))
            .append(Component.text("\n"))
    }
}