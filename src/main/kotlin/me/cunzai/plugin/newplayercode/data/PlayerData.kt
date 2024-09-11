package me.cunzai.plugin.newplayercode.data

import me.cunzai.plugin.newplayercode.database.MySQLHandler
import taboolib.common.platform.Schedule

data class PlayerData(val name: String) {
    companion object {
        @JvmStatic
        val cache = HashMap<String, PlayerData>()

        @Schedule(period = 20 * 30L, async = true)
        fun i() {
            for (data in cache.values) {
                data.playedTimes += 1000 * 30L
                data.updatePlayedTimes()
            }
        }
    }


    var playedTimes = 0L

    var code: String? = null

    var locking = false

    var parent: String? = null

    val invites = HashMap<String, HashSet<String>>()

    val completedQuest = HashSet<String>()

    fun updatePlayedTimes() {
        MySQLHandler.playerPlayedTimeTable.workspace(
            MySQLHandler.datasource
        ) {
            update {
                set("played", playedTimes)
                where {
                    "player_name" eq name
                }
            }
        }.run()
    }

    fun updateParent() {
        val s = parent ?: return
        MySQLHandler.playerInvitesTable.workspace(MySQLHandler.datasource) {
            insert("player_name", "invited_name") {
                value(s, name)
            }
        }.run()
    }

    fun updateCode() {
        val s = code ?: return
        MySQLHandler.playerCodeTable.workspace(MySQLHandler.datasource) {
            insert("player_name", "invite_code") {
                value(name, s)
            }
        }.run()
    }
}
