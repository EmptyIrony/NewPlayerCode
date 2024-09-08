package me.cunzai.plugin.newplayercode.ui

import me.cunzai.plugin.newplayercode.config.ConfigLoader
import me.cunzai.plugin.newplayercode.data.PlayerData
import me.cunzai.plugin.newplayercode.database.MySQLHandler
import org.bukkit.entity.Player
import taboolib.expansion.submitChain
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.util.getStringColored
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Chest

object QuestUI {

    @Config("view_ui.yml")
    lateinit var config: Configuration

    fun open(player: Player, name: String) {
        val claimed = PlayerData.cache[player.name]?.invites?.get(name) ?: return
        submitChain {
            val completed = async {
                MySQLHandler.completeTable.workspace(MySQLHandler.datasource) {
                    select {
                        where {
                            "player_name" eq name
                        }
                    }
                }.map { getString("completed") }
                    .toHashSet()
            }

            sync {
                player.openMenu<Chest>(config.getStringColored("title")!!.replace("%name%", name)) {
                    val format = config.getStringList("format")
                    rows(format.size)
                    map(*format.toTypedArray())
                    for (reward in ConfigLoader.rewards) {

                    }
                }
            }
        }
    }

}