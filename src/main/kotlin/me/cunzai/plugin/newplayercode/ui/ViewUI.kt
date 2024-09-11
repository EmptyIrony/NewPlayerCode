package me.cunzai.plugin.newplayercode.ui

import me.cunzai.plugin.newplayercode.data.PlayerData
import org.bukkit.entity.Player
import taboolib.library.xseries.getItemStack
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.util.getStringColored
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.PageableChest
import taboolib.platform.util.buildItem
import taboolib.platform.util.replaceLore
import taboolib.platform.util.replaceName

object ViewUI {

    @Config("view_ui.yml")
    lateinit var config: Configuration


    fun open(player: Player) {
        val playerData = PlayerData.cache[player.name] ?: return

        player.openMenu<PageableChest<String>>(title = config.getStringColored("title")!!) {
            val format = config.getStringList("format")
            rows(format.size)
            map(*format.toTypedArray())

            elements {
                playerData.invites.keys.toList()
            }

            onGenerate { player, element, index, slot ->
                buildItem(config.getItemStack("icon")!!) {
                    skullOwner = element
                }.replaceName("%name%", element)
                    .replaceLore("%name%", element)
            }
            slots(getSlots('#'))
            onClick { event, element ->
                event.isCancelled = true
                QuestUI.open(player, element)
            }

            setPreviousPage(getSlots('<').first()) { _, _ ->
                config.getItemStack("previous")!!
            }
            setNextPage(getSlots('>').first()) { _, _ ->
                config.getItemStack("next")!!
            }
        }
    }

}