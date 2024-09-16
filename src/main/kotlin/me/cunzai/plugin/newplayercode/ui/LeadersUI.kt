package me.cunzai.plugin.newplayercode.ui

import me.cunzai.plugin.newplayercode.util.monthlyInvites
import org.bukkit.entity.Player
import taboolib.library.xseries.getItemStack
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.util.getStringColored
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Chest
import taboolib.platform.util.buildItem
import taboolib.platform.util.replaceLore
import taboolib.platform.util.replaceName

object LeadersUI {

    @Config("leader_ui.yml")
    lateinit var config: Configuration

    fun open(player: Player) {
        open(player, monthlyInvites)
    }

    fun open(player: Player, leaders: List<LeaderboardEntry>) {
        player.openMenu<Chest>(config.getStringColored("title") ?: "null") {
            map(*config.getStringList("format").toTypedArray())

            set('#', config.getItemStack("split")!!) {
                isCancelled = true
            }

            set('!', config.getItemStack("back")!!) {
                isCancelled = true
                player.closeInventory()
            }

            for ((index, leaderSlot) in getSlots('$').withIndex()) {
                val entry = leaders.getOrNull(index)
                val sortIndex = index + 1
                val name = entry?.name ?: "虚位以待"
                val invited = entry?.invited ?: 0

                set(leaderSlot, buildItem(config.getItemStack("leader")!!) {
                    skullOwner = name
                }.replaceName(
                    mapOf(
                        "%index%" to sortIndex.toString(),
                        "%name%" to name
                    )
                ).replaceLore(
                    mapOf(
                        "%invited%" to invited.toString(),
                    )
                )) {
                    isCancelled = true
                }
            }

            onClick { it.isCancelled = true }
        }
    }


    data class LeaderboardEntry(
        val name: String,
        val invited: Int
    )

}