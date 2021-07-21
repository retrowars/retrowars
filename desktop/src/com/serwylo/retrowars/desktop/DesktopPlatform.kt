package com.serwylo.retrowars.desktop

import com.serwylo.retrowars.utils.MulticastControl
import com.serwylo.retrowars.utils.Platform
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*

class DesktopPlatform: Platform {
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