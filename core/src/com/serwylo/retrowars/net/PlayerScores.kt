package com.serwylo.retrowars.net

import com.badlogic.gdx.Gdx

class PlayerScores {

    companion object {
        private const val SCORE_BREAKPOINT_SIZE = 40000L
    }

    private val scores = mutableMapOf<Player, Long>()

    /**
     * For each player, record the next score for which we will send an event to other players.
     * Once we notice their score go over this threshold, we'll bump it to the next breakpoint.
     */
    private val scoreBreakpoints = mutableMapOf<Player, Long>()

    fun getScoreFor(player: Player) = scores[player] ?: 0

    fun incrementScore(player: Player, scoreToAdd: Long) {

    }

    fun clear() {
        scores.clear()
    }

    fun advanceBreakpoints(player: Player): Int {
        val currentScore = getScoreFor(player)
        var counter = 0
        do {
            val breakpoint = getNextScoreBreakpointFor(player)
            scoreBreakpoints[player] = breakpoint + SCORE_BREAKPOINT_SIZE
            counter ++
        } while (breakpoint + SCORE_BREAKPOINT_SIZE < currentScore)

        return counter
    }

    private fun getNextScoreBreakpointFor(player: Player): Long {
        val breakpoint = scoreBreakpoints[player] ?: 0L
        if (breakpoint == 0L) {
            scoreBreakpoints[player] = SCORE_BREAKPOINT_SIZE
            return SCORE_BREAKPOINT_SIZE
        }

        return breakpoint
    }

    /**
     * Useful for the current player updating their own score before broadcasting to other players.
     * No need for us to send a handicap even to ourselves under these circumstances, and the server
     * will be responsible for sending these events to other clients.
     */
    fun updateScoreIgnoreBreakpoints(player: Player, score: Long) {
        scores[player] = score
    }
}