package me.cunzai.plugin.newplayercode.command

import me.cunzai.plugin.newplayercode.config.ConfigLoader
import me.cunzai.plugin.newplayercode.data.PlayerData
import me.cunzai.plugin.newplayercode.database.MySQLHandler
import me.cunzai.plugin.newplayercode.database.RedisHandler
import me.cunzai.plugin.newplayercode.ui.LeadersUI
import me.cunzai.plugin.newplayercode.ui.QuestUI
import me.cunzai.plugin.newplayercode.ui.ViewUI
import me.cunzai.plugin.newplayercode.util.genCode
import me.cunzai.plugin.newplayercode.util.monthlyInvites
import me.cunzai.plugin.newplayercode.util.totalInvites
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.command.*
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.console
import taboolib.common.platform.function.submitAsync
import taboolib.expansion.createHelper
import taboolib.module.chat.Components
import taboolib.module.chat.colored
import taboolib.module.lang.asLangText
import taboolib.platform.util.sendLang

@CommandHeader("code", permissionDefault = PermissionDefault.TRUE)
object PlayerCodeCommand {

    @CommandBody
    val look = subCommand {
        execute<Player> { sender, _, _ ->
            ViewUI.open(sender)
        }
    }

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

                    Components.text(console().asLangText("code_view", code))
                        .clickCopyToClipboard(code)
                        .sendTo(adaptPlayer(sender))
                }
            } else {
                Components.text(console().asLangText("code_view", dataCode))
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

                if (ConfigLoader.allowedUseCodeTime > (data.playedTime)) {
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
                                "invite_code" eq argument
                            }
                        }
                    }.firstOrNull {
                        val playerName = getString("player_name")

                        if (playerName == sender.name) {
                            sender.sendLang("cant_bound_self")
                            data.locking = false
                            return@firstOrNull true
                        }

                        data.parent = playerName
                        data.updateParent()
                        data.locking = false

                        RedisHandler.crossServerMessage(playerName, data.name, true, console().asLangText("invited", sender.name))
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

    @CommandBody(permissionDefault = PermissionDefault.TRUE)
    val leader = subCommand {
        execute<Player> { sender, _, _ ->
            LeadersUI.open(sender)
        }

        dynamic("type") {
            execute<Player> { sender, _, argument ->
                LeadersUI.open(sender, if (argument == "total") totalInvites else monthlyInvites, argument == "total")
            }
        }
    }

    @CommandBody(permission = "code.admin")
    val sendLeaderboardRewards = subCommand {
        execute<CommandSender> { sender, _, _ ->
            for ((index, leader) in monthlyInvites.withIndex()) {
                ConfigLoader.leaderRewards.getOrNull(index)?.rewards?.forEach { reward ->
                    Bukkit.dispatchCommand(
                        Bukkit.getConsoleSender(),
                        reward.replace("%player%", leader.name)
                    )
                }
            }

            sender.sendMessage("&a奖励发放完成".colored())
        }
    }

    @CommandBody
    val reload = subCommand {
        execute<CommandSender> { sender, _, argument ->
            ConfigLoader.config.reload()
            ConfigLoader.rewardsConfig.reload()
            ConfigLoader.i()

            QuestUI.config.reload()
            ViewUI.config.reload()

            sender.sendMessage("ok")
        }
    }

    @CommandBody
    val main = mainCommand {
        createHelper(checkPermissions = true)
    }

}