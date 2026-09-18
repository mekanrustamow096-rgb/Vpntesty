package com.vpntester.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object ConfigTester {

    private const val TIMEOUT_MS = 4000

    suspend fun test(config: VpnConfig): VpnConfig = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(config.host, config.port), TIMEOUT_MS)
                val elapsed = System.currentTimeMillis() - start
                config.latencyMs = elapsed
                config.status = "Доступен"
            }
        } catch (e: Exception) {
            config.latencyMs = -2
            config.status = "Недоступен"
        }
        config
    }
}
