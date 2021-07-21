package com.serwylo.retrowars.utils

import java.net.InetAddress

/**
 * Platform specific code, so that libgdx can run both on Desktop and Android.
 */
interface Platform {
    fun getMulticastControl(): MulticastControl
    fun getInetAddress(): InetAddress
}

/**
 * Android requires the WifiManager to acquire a multicast lock in order for JmDNS discovery to work.
 * Desktop doesn't need to do anything specific.
 */
interface MulticastControl {
    fun acquireLock()
    fun releaseLock()
}