package com.serwylo.retrowars.utils

import com.badlogic.gdx.Gdx
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * Platform specific code, so that libgdx can run both on Desktop and Android.
 */
interface Platform {
    fun shareRetrowars()
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

/**
 * Used to be in the desktop project, but the server also wants to make use of this, so we've moved
 * to the common project to accommodate this.
 */
class DesktopPlatform: Platform {

    override fun shareRetrowars() {
        Gdx.net.openURI("https://play.google.com/store/apps/details?id=com.serwylo.retrowars")
    }

    override fun getMulticastControl() = DesktopMulticastControl()

    /**
     * Get an appropriate IP address for the current machine.
     * Iterates over all network interfaces which are up, gathering every address for each one,
     * and then prefers site local addresses first (because this is designed for local network
     * games - these are likely the only ones which will actually work), then any other address
     * (e.g. perhaps virtualised devices such as docker bridges), then loopback addresses (because
     * why would you want that for a multiplayer game?).
     */
    override fun getInetAddress(): InetAddress {
        val addresses = NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp }
            .map { it.inetAddresses.toList() }
            .flatten()
            .sortedBy {
                when {
                    it.isSiteLocalAddress -> -1
                    it.isLoopbackAddress -> 1
                    else -> 0
                }
            }

        return addresses.first() ?: InetAddress.getLocalHost()
    }

}

class DesktopMulticastControl: MulticastControl {
    override fun acquireLock() {}
    override fun releaseLock() {}
}
