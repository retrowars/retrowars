package com.serwylo.retrowars.server

import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration

object ServerLauncher {

    @JvmStatic
    fun main(arg: Array<String>) {
        val config = HeadlessApplicationConfiguration()
        HeadlessApplication(ServerApp(), config)
    }

}
