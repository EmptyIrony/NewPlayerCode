package me.cunzai.plugin.newplayercode.util

import me.cunzai.plugin.newplayercode.data.PlayerData
import me.cunzai.plugin.newplayercode.database.MySQLHandler
import kotlin.random.Random

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