package me.cunzai.plugin.newplayercode.database

import me.cunzai.plugin.newplayercode.data.PlayerData
import org.bukkit.Bukkit
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.expansion.AlkaidRedis
import taboolib.expansion.SingleRedisConnector
import taboolib.expansion.fromConfig
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import java.util.HashSet

object RedisHandler {

    @Config(value = "database.yml")
    lateinit var config: Configuration

    private val connector: SingleRedisConnector by lazy {
        AlkaidRedis.create()
            .fromConfig(config.getConfigurationSection("redis")!!)
            .connect()
    }


    @Awake(LifeCycle.ENABLE)
    fun i() {
        connector.connection().subscribe(
            "code_cross_server_message"
        ) {
            val message: CrossServerMessage = get(ignoreConstructor = true)
            Bukkit.getPlayerExact(message.playerName)?.apply {
                sendMessage(message.message)
            }

            PlayerData.cache[message.playerName]?.invites?.putIfAbsent(message.invitedPlayer, HashSet())
        }
    }

    fun crossServerMessage(sendTo: String, invitedPlayer: String, message: String) {
        connector.connection().publish("code_cross_server_message", CrossServerMessage(sendTo, invitedPlayer, message))
    }

    class CrossServerMessage(
        val playerName: String,
        val invitedPlayer: String,
        val message: String
    )

}