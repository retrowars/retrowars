package com.serwylo.retrowars.games.tetris.entities

typealias Tetronimo = List<List<Boolean>>
typealias TetronimoOrientations = List<Tetronimo>

object Tetronimos {

    private const val x = true
    private const val o = false

    val I: TetronimoOrientations = listOf(

        listOf(
            listOf(o, o, x, o),
            listOf(o, o, x, o),
            listOf(o, o, x, o),
            listOf(o, o, x, o),
        ),

        listOf(
            listOf(o, o, o, o),
            listOf(o, o, o, o),
            listOf(x, x, x, x),
            listOf(o, o, o, o),
        ),

        listOf(
            listOf(o, x, o, o),
            listOf(o, x, o, o),
            listOf(o, x, o, o),
            listOf(o, x, o, o),
        ),

        listOf(
            listOf(o, o, o, o),
            listOf(x, x, x, x),
            listOf(o, o, o, o),
            listOf(o, o, o, o),
        ),

    )

    val O: TetronimoOrientations = listOf(
        listOf(
            listOf(x, x),
            listOf(x, x),
        ),
    )

    val T: TetronimoOrientations = listOf(

        // Point down / Flat up
        listOf(
            listOf(o, o, o),
            listOf(x, x, x),
            listOf(o, x, o),
        ),

        // Point left / Flat right
        listOf(
            listOf(o, x, o),
            listOf(x, x, o),
            listOf(o, x, o),
        ),

        // Point up / Flat down
        listOf(
            listOf(o, x, o),
            listOf(x, x, x),
            listOf(o, o, o),
        ),

        // Point right / Flat left
        listOf(
            listOf(o, x, o),
            listOf(o, x, x),
            listOf(o, x, o),
        ),

    )

    val S: TetronimoOrientations = listOf(

        // Point down / Flat up
        listOf(
            listOf(o, o, o),
            listOf(o, x, x),
            listOf(x, x, o),
        ),

        // Point left / Flat right
        listOf(
            listOf(x, o, o),
            listOf(x, x, o),
            listOf(o, x, o),
        ),

        // Point up / Flat down
        listOf(
            listOf(o, x, x),
            listOf(x, x, o),
            listOf(o, o, o),
        ),

        // Point right / Flat left
        listOf(
            listOf(o, x, o),
            listOf(o, x, x),
            listOf(o, o, x),
        ),

    )

    val Z: TetronimoOrientations = listOf(

        // Point down / Flat up
        listOf(
            listOf(o, o, o),
            listOf(x, x, o),
            listOf(o, x, x),
        ),

        // Point left / Flat right
        listOf(
            listOf(o, x, o),
            listOf(x, x, o),
            listOf(x, o, o),
        ),

        // Point up / Flat down
        listOf(
            listOf(x, x, o),
            listOf(o, x, x),
            listOf(o, o, o),
        ),

        // Point right / Flat left
        listOf(
            listOf(o, o, x),
            listOf(o, x, x),
            listOf(o, x, o),
        ),

    )

    val J: TetronimoOrientations = listOf(

        // Point down / Flat up
        listOf(
            listOf(o, o, o),
            listOf(x, x, x),
            listOf(o, o, x),
        ),

        // Point left / Flat right
        listOf(
            listOf(o, x, o),
            listOf(o, x, o),
            listOf(x, x, o),
        ),

        // Point up / Flat down
        listOf(
            listOf(x, o, o),
            listOf(x, x, x),
            listOf(o, o, o),
        ),

        // Point right / Flat left
        listOf(
            listOf(o, x, x),
            listOf(o, x, o),
            listOf(o, x, o),
        ),

    )

    val L: TetronimoOrientations = listOf(

        // Point down / Flat up
        listOf(
            listOf(o, o, o),
            listOf(x, x, x),
            listOf(x, o, o),
        ),

        // Point left / Flat right
        listOf(
            listOf(x, x, o),
            listOf(o, x, o),
            listOf(o, x, o),
        ),

        // Point up / Flat down
        listOf(
            listOf(o, o, x),
            listOf(x, x, x),
            listOf(o, o, o),
        ),

        // Point right / Flat left
        listOf(
            listOf(o, x, o),
            listOf(o, x, o),
            listOf(o, x, x),
        ),

    )

    private val all: List<TetronimoOrientations> = listOf(O, I, T, S, Z, J, L)

    fun random(): TetronimoOrientations = all.random()

}