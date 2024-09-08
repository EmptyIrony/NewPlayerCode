package me.cunzai.plugin.newplayercode.ui

import me.cunzai.plugin.newplayercode.config.ConfigLoader
import me.cunzai.plugin.newplayercode.data.PlayerData
import me.cunzai.plugin.newplayercode.database.MySQLHandler
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.expansion.submitChain
import taboolib.library.xseries.getItemStack
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.util.getStringColored
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Chest
import taboolib.platform.util.buildItem
import taboolib.platform.util.sendLang

object QuestUI {

    @Config("view_ui.yml")
    lateinit var config: Configuration

    fun open(player: Player, name: String) {
        val data = PlayerData.cache[player.name] ?: return
        val claimed = data.invites.get(name) ?: return
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

                    val buttonSlots = getSlots('#')

                    for ((index, reward) in ConfigLoader.rewards.withIndex()) {
                        if (completed.contains(reward.rewardName)) {
                            if (claimed.contains(reward.rewardName)) {
                                set(
                                    buttonSlots[index], config.getItemStack("claimed")!!
                                    .replaceDescription(reward)) {
                                    isCancelled = true
                                }
                            } else {
                                set(
                                    buttonSlots[index], config.getItemStack("completed")!!
                                        .replaceDescription(reward)) {
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
                                    .replaceDescription(reward)) {
                                isCancelled = true
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ItemStack.replaceDescription(rewardConfig: ConfigLoader.RewardConfig): ItemStack {
        return buildItem(this) {
            val index = lore.indexOf("%description%")
            if (index == -1) return@buildItem

            lore.removeAt(index)
            lore.addAll(index, rewardConfig.description)
        }
    }

}