package me.cunzai.plugin.newplayercode.util

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import me.cunzai.plugin.newplayercode.data.PlayerData
import me.cunzai.plugin.newplayercode.database.MySQLHandler
import me.cunzai.plugin.newplayercode.ui.LeadersUI.LeaderboardEntry
import taboolib.common.platform.Schedule
import taboolib.expansion.AsyncDispatcher
import taboolib.expansion.submitChain
import taboolib.module.database.Order
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random

val monthStartTimestamp: Long
    get() {
        val currentDate = LocalDate.now()
        val firstDayOfMonth = currentDate.withDayOfMonth(1)
        return firstDayOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

val totalInvites = ArrayList<LeaderboardEntry>()
val monthlyInvites = ArrayList<LeaderboardEntry>()

@Schedule(period = 30 * 20L)
fun refreshLeaderboards() {
    submitChain {
        val (total, monthly) = coroutineScope {
            async { loadLeaders(false) }.await() to
            async { loadLeaders(true) }.await()
        }
        sync {
            totalInvites.clear()
            totalInvites += total

            monthlyInvites.clear()
            monthlyInvites += monthly
        }
    }
}

suspend fun loadLeaders(isMonth: Boolean): List<LeaderboardEntry> = withContext(AsyncDispatcher) {
    MySQLHandler.playerInvitesTable.select(MySQLHandler.datasource) {
        rows("player_name", "COUNT(*) as invited")
        if (isMonth) {
            where {
                ("invite_time" gte monthStartTimestamp)
            }
        }
        groupBy("player_name")
        orderBy("invited", Order.Type.DESC)
        limit(10)
    }.map {
        LeaderboardEntry(getString("player_name"), getInt("invited"))
    }
}

fun Long.millisToRoundedTime(): String {
    var millis = this
    millis += 1L
    val seconds = millis / 1000L
    val minutes = seconds / 60L
    val hours = minutes / 60L
    val days = hours / 24L
    return if (days > 0) {
        days.toString() + " 天 " + (hours - 24 * days) + " 小时"
    } else if (hours > 0) {
        hours.toString() + " 小时 " + (minutes - 60 * hours) + " 分钟"
    } else if (minutes > 0) {
        minutes.toString() + " 分钟 " + (seconds - 60 * minutes) + " 秒"
    } else {
        "$seconds 秒"
    }
}

fun genCode(data: PlayerData): String {
    while (true) {
        val code = genCode()
        val exist = MySQLHandler.playerCodeTable.workspace(MySQLHandler.datasource) {
            select {
                where {
                    "invite_code" eq code
                }
            }
        }.firstOrNull { true } ?: false
        if (!exist) {
            return code
        }
    }
}

private fun genCode(): String {
    // 定义一个包含大小写字母和数字的字符集
    val chars = ('a'..'z') + ('0'..'9')
    val sb = StringBuilder(6) // 初始化StringBuilder来构建最终的字符串，初始容量设置为6

    // 使用Random实例生成6个随机索引，并从字符集中选择字符
    repeat(6) {
        // Random.nextInt(chars.length) 生成一个[0, chars.length)之间的随机数，即索引
        val randomChar = chars[Random.nextInt(chars.size)]
        sb.append(randomChar) // 将选中的字符添加到StringBuilder中
    }

    return sb.toString() // 将StringBuilder转换为String并返回
}