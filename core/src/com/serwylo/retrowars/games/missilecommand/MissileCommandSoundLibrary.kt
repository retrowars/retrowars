package com.serwylo.retrowars.games.missilecommand

import com.serwylo.retrowars.audio.SoundLibrary

class MissileCommandSoundLibrary: SoundLibrary(
    mapOf(
        "missilecommand_fire_enemy" to "missilecommand_fire_enemy.ogg",
        "missilecommand_fire_player" to "missilecommand_fire_player.ogg",
        "missilecommand_fire_no_ammunition" to "missilecommand_fire_no_ammunition.ogg",
        "missilecommand_hit_city" to "missilecommand_hit_city.ogg",
        "missilecommand_hit_missile" to "missilecommand_hit_missile.ogg",
        "missilecommand_hit_nothing" to "missilecommand_hit_nothing.ogg",

        // Combination of explosionCrunch 3 + 4 from Kenney overlayed without any other effects..
        "missilecommand_all_cities_hit" to "missilecommand_all_cities_hit.ogg",
    )
) {

    fun fireEnemy() = play("missilecommand_fire_enemy")
    fun firePlayer() = play("missilecommand_fire_player")
    fun fireNoAmmunition() = play("missilecommand_fire_no_ammunition")
    fun hitCity() = play("missilecommand_hit_city")
    fun hitMissile() = play("missilecommand_hit_missile")
    fun hitNothing() = play("missilecommand_hit_nothing")
    fun allCitiesHit() = play("missilecommand_all_cities_hit")

}