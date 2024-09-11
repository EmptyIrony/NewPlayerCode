package me.cunzai.plugin.newplayercode.database

import com.google.gson.Gson
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

    private val gson = Gson()

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
            val message: CrossServerMessage = gson.fromJson(message, CrossServerMessage::class.java)
            Bukkit.getPlayerExact(message.playerName)?.apply {
                sendMessage(message.message)
            }

            if (message.isInvited) {
                PlayerData.cache[message.playerName]?.invites?.putIfAbsent(message.invitedPlayer, HashSet())
            }
        }
    }

    fun crossServerMessage(sendTo: String, invitedPlayer: String, isInvited: Boolean, message: String) {
        connector.connection().publish(
            "code_cross_server_message",
            gson.toJson(CrossServerMessage(sendTo, isInvited, invitedPlayer, message))
        )
    }

    class CrossServerMessage(
        val playerName: String,
        val isInvited: Boolean,
        val invitedPlayer: String,
        val message: String
    )

}