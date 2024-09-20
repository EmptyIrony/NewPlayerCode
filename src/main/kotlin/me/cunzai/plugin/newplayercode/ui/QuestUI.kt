package me.cunzai.plugin.newplayercode.ui

import me.cunzai.plugin.newplayercode.config.ConfigLoader
import me.cunzai.plugin.newplayercode.data.PlayerData
import me.cunzai.plugin.newplayercode.database.MySQLHandler
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.function.console
import taboolib.common.platform.function.submit
import taboolib.expansion.submitChain
import taboolib.library.xseries.getItemStack
import taboolib.module.chat.colored
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.util.getStringColored
import taboolib.module.lang.asLangText
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Chest
import taboolib.platform.util.buildItem
import taboolib.platform.util.replaceLore
import taboolib.platform.util.replaceName
import taboolib.platform.util.sendLang
import kotlin.math.max

object QuestUI {

    @Config("quest_ui.yml")
    lateinit var config: Configuration

    fun open(player: Player, name: String) {
        val data = PlayerData.cache[player.name] ?: return
        val claimed = data.invites[name] ?: return
        submitChain {
            val (completed, conditionCache) = async {
                val map = ConfigLoader.rewards.associate {
                    it.rewardName to it.conditions.map { it.getValue(name) }
                }

                MySQLHandler.completeTable.workspace(MySQLHandler.datasource) {
                    select {
                        where {
                            "player_name" eq name
                        }
                    }
                }.map { getString("completed") }
                    .toHashSet() to map
            }

            sync {
                player.openMenu<Chest>(config.getStringColored("title")!!.replace("%name%", name)) {
                    val format = config.getStringList("format")
                    rows(format.size)
                    map(*format.toTypedArray())

                    val buttonSlots = getSlots('#')

                    set('!', ViewUI.config.getItemStack("back")!!) {
                        isCancelled = true
                        player.closeInventory()
                        submit(delay = 1L) {
                            for (command in config.getStringList("back.commands")) {
                                Bukkit.dispatchCommand(
                                    Bukkit.getConsoleSender(),
                                    command.replace("%player%", player.name),
                                )
                            }
                        }
                    }

                    for ((index, reward) in ConfigLoader.rewards.withIndex()) {
                        val cacheList = conditionCache[reward.rewardName] ?: emptyList()

                        if (completed.contains(reward.rewardName)) {
                            if (claimed.contains(reward.rewardName)) {
                                set(
                                    buttonSlots[index], config.getItemStack("claimed")!!
                                    .replaceDescription(reward, cacheList)) {
                                    isCancelled = true
                                }
                            } else {
                                set(
                                    buttonSlots[index], config.getItemStack("completed")!!
                                        .replaceDescription(reward, cacheList)) {
                                    isCancelled = true
                                    val success = claimed.add(reward.rewardName)
                                    if (!success) return@set

                                    player.closeInventory()

                                    submitChain {
                                        async {
                                            MySQLHandler.claimedTable.workspace(MySQLHandler.datasource) {
                                                insert("player_name", "invited_name", "claimed") {
                                                    value(player.name, name, reward.rewardName)
                                                }
                                            }.run()
                                        }
                                        sync {
                                            for (command in reward.rewards) {
                                                Bukkit.dispatchCommand(
                                                    Bukkit.getConsoleSender(),
                                                    command.replace("%player%", player.name)
                                                )
                                            }

                                            player.sendLang("claim_success")
                                            open(player, name)
                                        }
                                    }
                                }
                            }
                        } else {
                            set(
                                buttonSlots[index], config.getItemStack("incomplete")!!
                                    .replaceDescription(reward, cacheList)) {
                                isCancelled = true
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ItemStack.replaceDescription(rewardConfig: ConfigLoader.RewardConfig, cacheList: List<Any> = emptyList()): ItemStack {
        val map = HashMap<String, String>()

        map["%name%"] = rewardConfig.rewardName
        for ((index, condition) in rewardConfig.conditions.withIndex()) {
            val value = cacheList.getOrNull(index)
            map["%condition_${index + 1}%"] = if (value is Long) {
                (max(condition.value.toLongOrNull() ?: 0L, value) / 60 / 60 / 1000L).toString()
            } else {
                console().asLangText((value as? Boolean ?: false).toString())
            }
        }

        return buildItem(this) {
            val index = lore.indexOf("%description%")
            if (index == -1) return@buildItem

            lore.removeAt(index)
            lore.addAll(index, rewardConfig.description.colored())
        }.replaceName(map)
            .replaceLore(map)
    }

}