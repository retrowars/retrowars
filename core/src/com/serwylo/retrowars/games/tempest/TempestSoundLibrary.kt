package com.serwylo.retrowars.games.tempest

import com.serwylo.retrowars.audio.SoundLibrary

class TempestSoundLibrary: SoundLibrary(
    mapOf(
        "tempest_player_fire" to "tempest_player_fire.ogg",
        "tempest_enemy_flip" to "tempest_enemy_flip.ogg",
        "tempest_enemy_spawn" to "tempest_enemy_spawn.ogg",
        "tempest_enemy_hit" to "tempest_enemy_hit.ogg",
        "tempest_player_hit" to "tempest_player_hit.ogg",
        "tempest_level_warp" to "tempest_level_warp.ogg",
    )
) {

    fun playerFire() = play("tempest_player_fire")
    fun enemyFlip() = play("tempest_enemy_flip")
    fun enemySpawn() = play("tempest_enemy_spawn")
    fun enemyHit() = play("tempest_enemy_hit")
    fun playerHit() = play("tempest_player_hit")
    fun levelWarp() = play("tempest_level_warp")

}