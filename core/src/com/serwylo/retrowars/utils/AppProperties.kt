package com.serwylo.retrowars.utils

import java.util.*

object AppProperties {

    val appVersionCode: Int

    init {
        val properties = Properties()
        properties.load(AppProperties::class.java.getResourceAsStream("/retrowars.properties"))
        appVersionCode = properties.getProperty("app-version-code").toInt()
    }

}