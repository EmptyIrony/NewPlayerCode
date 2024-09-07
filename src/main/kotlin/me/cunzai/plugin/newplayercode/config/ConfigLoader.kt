package me.cunzai.plugin.newplayercode.config

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

    var allowedUseCodeTime = -1L
    lateinit var allowedGetCodePermission: String

    val rewards = ArrayList<RewardConfig>()

    @Awake(LifeCycle.ENABLE)
    fun i() {
        allowedUseCodeTime = config.getLong("allowed_get_code_time")
        allowedGetCodePermission = config.getString("allowed_get_code_permission")!!

        rewards.clear()
        for (key in rewardsConfig.getKeys(false)) {
            val section = rewardsConfig.getConfigurationSection(key)!!
            rewards += section.toObject<RewardConfig>(ignoreConstructor = true)
        }
    }


    class RewardConfig(
        val conditions: List<Condition>,
        val rewards: List<String>
    )

    class Condition(
        val type: String,
        val value: String,
    )

}