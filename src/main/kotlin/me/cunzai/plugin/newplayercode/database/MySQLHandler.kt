package me.cunzai.plugin.newplayercode.database

import me.cunzai.plugin.newplayercode.data.PlayerData
import org.bukkit.event.player.PlayerJoinEvent
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submitAsync
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.database.ColumnOptionSQL
import taboolib.module.database.ColumnTypeSQL
import taboolib.module.database.Table
import taboolib.module.database.getHost
import java.util.HashSet

object MySQLHandler {

    @Config("database.yml")
    lateinit var config: Configuration

    private val host by lazy {
        config.getHost("mysql")
    }

    val datasource by lazy {
        host.createDataSource()
    }

    val playerCodeTable by lazy {
        Table("player_invite_code", host) {
            add {
                id()
            }

            add("player_name") {
                type(ColumnTypeSQL.VARCHAR, 64) {
                    options(ColumnOptionSQL.KEY)
                }
            }

            add("invite_code") {
                type(ColumnTypeSQL.VARCHAR, 64) {
                    options(ColumnOptionSQL.KEY)
                }
            }
        }
    }

    val playerInvitesTable by lazy {
        Table("player_invited", host) {
            add {
                id()
            }

            add("player_name") {
                type(ColumnTypeSQL.VARCHAR, 64) {
                    options(ColumnOptionSQL.KEY)
                }
            }

            add("invited_name") {
                type(ColumnTypeSQL.VARCHAR, 64) {
                    options(ColumnOptionSQL.KEY)
                }
            }
        }
    }

    private val claimedTable by lazy {
        Table("invite_claimed_data", host) {
            add {
                id()
            }

            add("player_name") {
                type(ColumnTypeSQL.VARCHAR, 64) {
                    options(ColumnOptionSQL.KEY)
                }
            }

            add("invited_name") {
                type(ColumnTypeSQL.VARCHAR, 64)
            }

            add("claimed") {
                type(ColumnTypeSQL.VARCHAR, 64) {
                    options(ColumnOptionSQL.KEY)
                }
            }
        }
    }

    val playerPlayedTimeTable by lazy {
        Table("player_played_data", host) {
            add {
                id()
            }

            add ("player_name"){
                type(ColumnTypeSQL.VARCHAR, 64) {
                    options(ColumnOptionSQL.KEY)
                }
            }

            add("played") {
                type(ColumnTypeSQL.BIGINT)
            }
        }
    }

    val completeTable by lazy {
        Table("complete_quest_data", host) {
            add {
                id()
            }

            add ("player_name"){
                type(ColumnTypeSQL.VARCHAR, 64) {
                    options(ColumnOptionSQL.KEY)
                }
            }

            add("completed") {
                type(ColumnTypeSQL.VARCHAR, 64)
            }
        }
    }

    @Awake(LifeCycle.ENABLE)
    fun i() {
        playerInvitesTable.workspace(datasource) {
            createTable(checkExists = true)
        }.run()

        claimedTable.workspace(datasource) {
            createTable(checkExists = true)
        }.run()

        playerCodeTable.workspace(datasource) {
            createTable(checkExists = true)
        }.run()

        playerPlayedTimeTable.workspace(datasource) {
            createTable(checkExists = true)
        }.run()

        completeTable.workspace(datasource) {
            createTable(checkExists = true)
        }.run()
    }

    @SubscribeEvent
    fun e(e: PlayerJoinEvent) {
        val player = e.player

        submitAsync {
            val data = PlayerData(player.name)
            PlayerData.cache[data.name] = data

            playerPlayedTimeTable.workspace(datasource) {
                select {
                    where {
                        "player_name" eq data.name
                    }
                }
            }.firstOrNull {
                data.playedTimes = getLong("played")
            }

            playerCodeTable.workspace(datasource) {
                select {
                    where {
                        "player_name" eq data.name
                    }
                }
            }.firstOrNull {
                data.code = getString("invite_code")
            }

            playerInvitesTable.workspace(datasource) {
                select {
                    where {
                        "invited_name" eq data.name
                    }
                }
            }.firstOrNull {
                data.parent = getString("player_name")
            }

            playerInvitesTable.workspace(datasource) {
                select {
                    where {
                        "player_name" eq data.name
                    }
                }
            }.forEach {
                data.invites[getString("invited_name")] = HashSet()
            }

            claimedTable.workspace(datasource) {
                select {
                    where {
                        "player_name" eq data.name
                    }
                }
            }.forEach {
                data.invites.getOrPut(getString("invited_name")) {
                    HashSet()
                } += getString("claimed")
            }
        }
    }



}