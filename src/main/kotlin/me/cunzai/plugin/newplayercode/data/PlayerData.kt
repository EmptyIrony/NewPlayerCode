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
                data.playedTime += 1000 * 30L
                data.updatePlayedTimes()
            }
        }
    }

    var code: String? = null

    var locking = false

    var parent: String? = null

    val invites = HashMap<String, HashSet<String>>()

    val completedQuest = HashSet<String>()

    var playedTime = 0L

    fun updateParent() {
        val s = parent ?: return
        MySQLHandler.playerInvitesTable.workspace(MySQLHandler.datasource) {
            insert("player_name", "invited_name", "invite_time") {
                value(s, name, System.currentTimeMillis())
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

    fun updatePlayedTimes() {
        MySQLHandler.playerPlayedTimeTable.update(
            MySQLHandler.datasource
        ) {
            set("played", playedTime)
            where {
                "player_name" eq name
            }
        }
    }
}
