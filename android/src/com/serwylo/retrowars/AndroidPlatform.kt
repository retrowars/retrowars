package com.serwylo.retrowars

import android.content.Context
import android.net.wifi.WifiManager
import com.badlogic.gdx.Gdx
import com.serwylo.retrowars.utils.MulticastControl
import com.serwylo.retrowars.utils.Platform
import java.net.InetAddress
import java.net.UnknownHostException


class AndroidPlatform(private val context: Context): Platform {

    companion object {
        private const val TAG = "AndroidPlatform"
    }

    private val multicastControl = AndroidMulticastControl(context)

    override fun getMulticastControl() = multicastControl

    override fun getInetAddress(): InetAddress {
        // Adapted from https://stackoverflow.com/a/23854825/2391921.
        val ip = try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val connectionInfo = wifi.connectionInfo
            val address = connectionInfo.ipAddress
            val byteAddress = byteArrayOf(
                (address and 0xff).toByte(),
                (address shr 8 and 0xff).toByte(),
                (address shr 16 and 0xff).toByte(),
                (address shr 24 and 0xff).toByte(),
            )

            InetAddress.getByAddress(byteAddress)
        } catch (e: UnknownHostException) {
            Gdx.app.error(TAG, "Unable to get IP address of device, defaulting to localhost", e)
            InetAddress.getLocalHost()
        }

        Gdx.app.log(TAG, "IP address of Android device: $ip")
        return ip
    }

}

class AndroidMulticastControl(private val context: Context): MulticastControl {

    companion object {
        private const val TAG = "AndroidMulticastControl"
    }

    private var lock: WifiManager.MulticastLock? = null

    override fun acquireLock() {
        Gdx.app.log(TAG, "Acquiring multicast lock.")
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        lock = wifi.createMulticastLock("com.serwylo.retrowars").apply {
            setReferenceCounted(true)
            acquire()
        }
    }

    override fun releaseLock() {
        Gdx.app.log(TAG, "Releasing multicast lock.")
        if (lock == null) {
            Gdx.app.error(TAG, "Expected the multicast lock to be present, but it is null. Perhaps it was already released and we are accidentally releasing it a second time?")
        }

        lock?.release()
        lock = null
    }

}