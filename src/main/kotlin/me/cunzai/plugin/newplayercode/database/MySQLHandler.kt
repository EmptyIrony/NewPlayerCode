package me.cunzai.plugin.newplayercode.database

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import me.cunzai.plugin.newplayercode.data.PlayerData
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submitAsync
import taboolib.expansion.submitChain
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

            add("invite_time") {
                type(ColumnTypeSQL.BIGINT)
            }
        }
    }

    val claimedTable by lazy {
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

        completeTable.workspace(datasource) {
            createTable(checkExists = true)
        }.run()

        playerPlayedTimeTable.workspace(datasource) {
            createTable(checkExists = true)
        }.run()

        for (player in Bukkit.getOnlinePlayers()) {
            loadData(player)
        }
    }

    @SubscribeEvent
    fun e(e: PlayerJoinEvent) {
        val player = e.player

        loadData(player)
    }

    private fun loadData(player: Player) {
        val data = PlayerData(player.name)
        PlayerData.cache[data.name] = data

        submitChain {
            coroutineScope {
                listOf(
                    launch {
                        playerPlayedTimeTable.workspace(datasource) {
                            select {
                                where {
                                    "player_name" eq data.name
                                }
                            }
                        }.firstOrNull {
                            data.playedTime = getLong("played")
                        } ?: run {
                            playerPlayedTimeTable.insert(datasource, "player_name", "played") {
                                value(player.name, 0L)
                            }
                        }
                    },
                    launch {
                        playerCodeTable.workspace(datasource) {
                            select {
                                where {
                                    "player_name" eq data.name
                                }
                            }
                        }.firstOrNull {
                            data.code = getString("invite_code")
                        }
                    },
                    launch {
                        playerInvitesTable.workspace(datasource) {
                            select {
                                where {
                                    "invited_name" eq data.name
                                }
                            }
                        }.firstOrNull {
                            data.parent = getString("player_name")
                        }
                    },
                    launch {
                        playerInvitesTable.workspace(datasource) {
                            select {
                                where {
                                    "player_name" eq data.name
                                }
                            }
                        }.forEach {
                            data.invites[getString("invited_name")] = HashSet()
                        }
                    },
                    launch {
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
                    },
                    launch {
                        completeTable.workspace(datasource) {
                            select {
                                where {
                                    "player_name" eq data.name
                                }
                            }
                        }.forEach {
                            data.completedQuest += getString("completed")
                        }
                    }
                )
            }.forEach { it.join() }


        }
    }



}