package com.serwylo.retrowars.games.asteroids.entities

import com.badlogic.gdx.math.Vector2

interface HasBoundingSphere {
    fun getPosition(): Vector2
    fun getRadius(): Float
}
