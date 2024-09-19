package me.cunzai.plugin.newplayercode.config

import me.cunzai.plugin.newplayercode.data.PlayerData
import me.cunzai.plugin.newplayercode.database.MySQLHandler
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.node.types.PermissionNode
import net.luckperms.api.query.QueryOptions
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.Configuration.Companion.toObject

object ConfigLoader {

    @Config("settings.yml")
    lateinit var config: Configuration

    @Config("rewards.yml")
    lateinit var rewardsConfig: Configuration

    @Config("leader_rewards.yml")
    lateinit var leaderRewardsConfig: Configuration

    var allowedUseCodeTime = -1L
    lateinit var allowedGetCodePermission: String

    val rewards = ArrayList<RewardConfig>()

    val leaderRewards = ArrayList<LeaderRewardData>()

    @Awake(LifeCycle.ENABLE)
    fun i() {
        allowedUseCodeTime = config.getLong("allowed_get_code_time")
        allowedGetCodePermission = config.getString("allowed_get_code_permission")!!

        rewards.clear()
        for (key in rewardsConfig.getKeys(false)) {
            val section = rewardsConfig.getConfigurationSection(key)!!
            val rewardConfig = section.toObject<RewardConfig>(ignoreConstructor = true)
            rewardConfig.rewardName = key
            rewards += rewardConfig
        }

        leaderRewards.clear()
        for (rewardName in leaderRewardsConfig.getKeys(false)) {
            leaderRewards += leaderRewardsConfig.getConfigurationSection(rewardName)!!.toObject<LeaderRewardData>(ignoreConstructor = true)
        }
    }


    class RewardConfig(
        var rewardName: String = "",
        val conditions: List<Condition>,
        val rewards: List<String>,
        val description: List<String>,
    )

    class Condition(
        val type: String,
        val value: String,
    ) {
        fun check(player: Player, data: PlayerData): Boolean {
            return when (type) {
                "permission" -> {
                    LuckPermsProvider.get().userManager.let {
                        it.getUser(player.uniqueId) ?: it.loadUser(player.uniqueId).get()
                    }.getInheritedGroups(QueryOptions.nonContextual())
                        .any {
                            it.name == value
                        }
                }
                "played_time" -> {
                    data.playedTime >= value.toLong()
                }
                else -> {
                    false
                }
            }
        }

        fun getValue(player: String): Any {
            return when (type) {
                "permission" -> {
                    LuckPermsProvider.get()
                        .userManager.let {
                            it.getUser(player) ?: it.loadUser(
                                it.lookupUniqueId(player).get()
                            ).get()
                        }.getInheritedGroups(QueryOptions.nonContextual())
                        .any {
                            it.name == value
                        }
                }
                "played_time" -> {
                    val playedTime = PlayerData.cache[player]?.playedTime ?: run {
                        MySQLHandler.playerPlayedTimeTable.select(MySQLHandler.datasource) {
                            where {
                                "player_name" eq player
                            }
                        }.firstOrNull {
                            getLong("played")
                        } ?: 0L
                    }
                    playedTime >= value.toLong()
                }
                else -> {
                    false
                }
            }
        }
    }

    data class LeaderRewardData(
        val rewards: List<String>,
        val desc: List<String>,
    )

}