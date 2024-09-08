package me.cunzai.plugin.newplayercode.tracker

import me.cunzai.plugin.newplayercode.config.ConfigLoader
import me.cunzai.plugin.newplayercode.data.PlayerData
import me.cunzai.plugin.newplayercode.database.MySQLHandler
import me.cunzai.plugin.newplayercode.database.RedisHandler
import taboolib.common.platform.Schedule
import taboolib.common.platform.function.console
import taboolib.module.lang.asLangText
import taboolib.platform.util.onlinePlayers
import taboolib.platform.util.sendLang

object QuestTracker {


    @Schedule(period = 5 * 20L, async = true)
    fun i() {
        for ((data, player) in onlinePlayers.mapNotNull { (PlayerData.cache[it.name] ?: return@mapNotNull null) to it }) {
            for (reward in ConfigLoader.rewards) {
                if (data.completedQuest.contains(reward.rewardName)) continue
                val notComplete = reward.conditions.any {
                    !it.check(player, data)
                }
                if (!notComplete) continue

                data.completedQuest += reward.rewardName
                MySQLHandler.completeTable.workspace(MySQLHandler.datasource) {
                    insert("player_name", "completed") {
                        value(player.name, reward.rewardName)
                    }
                }.run()

                player.sendLang("complete_quest", reward.rewardName)
                RedisHandler.crossServerMessage(
                    data.parent ?: continue,
                    player.name, false,
                    console().asLangText(
                        "new_player_complete_quest",
                        player.name, reward.rewardName
                    )
                )
            }
        }
    }

}