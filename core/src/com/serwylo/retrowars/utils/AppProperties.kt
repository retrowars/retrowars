package com.serwylo.retrowars.utils

import java.util.*

object AppProperties {

    val appVersionCode: Int
    val appVersionName: String

    init {
        val properties = Properties()
        properties.load(AppProperties::class.java.getResourceAsStream("/retrowars.properties"))
        appVersionCode = properties.getProperty("app-version-code").toInt()
        appVersionName = properties.getProperty("app-version-name")
    }

}