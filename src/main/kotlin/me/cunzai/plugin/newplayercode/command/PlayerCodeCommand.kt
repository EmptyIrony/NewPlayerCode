package me.cunzai.plugin.newplayercode.command

import me.cunzai.plugin.newplayercode.config.ConfigLoader
import me.cunzai.plugin.newplayercode.data.PlayerData
import me.cunzai.plugin.newplayercode.database.MySQLHandler
import me.cunzai.plugin.newplayercode.database.RedisHandler
import me.cunzai.plugin.newplayercode.util.genCode
import org.bukkit.entity.Player
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.console
import taboolib.common.platform.function.submitAsync
import taboolib.module.chat.Components
import taboolib.module.chat.colored
import taboolib.module.lang.asLangText
import taboolib.platform.util.sendLang

@CommandHeader("code", permissionDefault = PermissionDefault.TRUE)
object PlayerCodeCommand {

    @CommandBody
    val show = subCommand {
        execute<Player> { sender, _, _ ->
            val data = PlayerData.cache[sender.name] ?: run {
                sender.sendMessage("&c数据加载中..".colored())
                return@execute
            }

            if (!sender.hasPermission(ConfigLoader.allowedGetCodePermission)) {
                sender.sendLang("no_gen_code_permission")
                return@execute
            }

            if (data.locking) return@execute

            val dataCode = data.code
            if (dataCode == null) {
                submitAsync {
                    data.locking = true
                    sender.sendLang("generating_code")
                    val code = genCode(data)
                    data.code = code
                    data.updateCode()
                    data.locking = false

                    Components.text(console().asLangText("code_view"))
                        .clickCopyToClipboard(code)
                        .sendTo(adaptPlayer(sender))
                }
            } else {
                Components.text(console().asLangText("code_view"))
                    .clickCopyToClipboard(dataCode)
                    .sendTo(adaptPlayer(sender))
            }
        }
    }

    @CommandBody
    val apply = subCommand {
        dynamic("邀请码") {
            execute<Player> { sender, _, argument ->
                val data = PlayerData.cache[sender.name] ?: run {
                    sender.sendMessage("&c数据加载中..".colored())
                    return@execute
                }

                if (ConfigLoader.allowedUseCodeTime > data.playedTimes) {
                    sender.sendLang("no_played_time")
                    return@execute
                }

                val oldParent = data.parent
                if (oldParent != null) {
                    sender.sendLang("already_exist_parent", oldParent)
                    return@execute
                }

                if (data.locking) return@execute

                data.locking = true
                submitAsync {
                    val success = MySQLHandler.playerCodeTable.workspace(MySQLHandler.datasource) {
                        select {
                            where {
                                "invite_code" eq data.code
                            }
                        }
                    }.firstOrNull {
                        val playerName = getString("player_name")

                        data.parent = playerName
                        data.updateParent()
                        data.locking = false

                        RedisHandler.crossServerMessage(playerName, data.name, true, console().asLangText("invited", playerName))
                        sender.sendLang("success_use_code", playerName)

                        true
                    } ?: false

                    if (!success) {
                        sender.sendLang("no_code_found")
                    }
                }

            }
        }
    }

}