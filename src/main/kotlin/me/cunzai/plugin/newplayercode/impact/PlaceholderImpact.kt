package me.cunzai.plugin.newplayercode.impact

import me.cunzai.plugin.newplayercode.data.PlayerData
import me.cunzai.plugin.newplayercode.database.MySQLHandler
import me.cunzai.plugin.newplayercode.util.loadMonthlyInvited
import me.cunzai.plugin.newplayercode.util.monthStartTimestamp
import me.cunzai.plugin.newplayercode.util.monthlyInvites
import me.cunzai.plugin.newplayercode.util.totalInvites
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import taboolib.platform.compat.PlaceholderExpansion

object PlaceholderImpact: PlaceholderExpansion {
    override val identifier: String
        get() = "invite"
    override val autoReload: Boolean
        get() = false
    override val enabled: Boolean
        get() = true

    override fun onPlaceholderRequest(player: OfflinePlayer?, args: String): String {
        return onPlaceholderRequest(player as? Player, args)
    }

    override fun onPlaceholderRequest(player: Player?, args: String): String {
        player ?: return "null"

        val split = args.split("_")
        val operator = split[0]

        if (operator == "code") {
            return PlayerData.cache[player.name]?.code ?: "暂无"
        }

        if (operator == "invited-total") {
            return (PlayerData.cache[player.name]?.invites?.size ?: 0).toString()
        }

        if (operator == "invited-monthly") {
            return (loadMonthlyInvited(player.name)).toString()
        }

        val cycle = split.getOrNull(1) ?: return "null"
        val index = (split.getOrNull(2)?.toIntOrNull()?.let { it - 1 }) ?: return "null"

        val leaderboardEntries = if (cycle == "total") {
            totalInvites
        } else monthlyInvites

        val entry = leaderboardEntries.getOrNull(index)

        return if (operator == "name") {
            entry?.name ?: "虚位以待"
        } else {
            entry?.invited?.toString() ?: "0"
        }
    }
}