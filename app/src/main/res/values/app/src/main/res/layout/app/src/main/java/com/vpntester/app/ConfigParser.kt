package com.vpntester.app

import android.util.Base64
import java.net.URI
import java.net.URLDecoder

data class VpnConfig(
    val raw: String,
    val protocol: String,   // "vless" или "ss"
    val name: String,
    val host: String,
    val port: Int
) {
    var latencyMs: Long = -1   // -1 = ещё не проверено, -2 = недоступен
    var status: String = "Не проверено"
}

object ConfigParser {

    fun parseSubscriptionBody(body: String): List<VpnConfig> {
        val decoded = tryBase64Decode(body.trim()) ?: body
        val lines = decoded.split("\n", "\r\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val result = mutableListOf<VpnConfig>()
        for (line in lines) {
            val cfg = when {
                line.startsWith("vless://") -> parseVless(line)
                line.startsWith("ss://") -> parseShadowsocks(line)
                else -> null
            }
            if (cfg != null) result.add(cfg)
        }
        return result
    }

    private fun tryBase64Decode(s: String): String? {
        return try {
            val cleaned = s.replace("\n", "").replace("\r", "")
            String(Base64.decode(cleaned, Base64.DEFAULT), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseVless(link: String): VpnConfig? {
        return try {
            val uri = URI(link)
            val host = uri.host ?: return null
            val port = if (uri.port != -1) uri.port else 443
            val name = uri.fragment?.let { URLDecoder.decode(it, "UTF-8") } ?: host
            VpnConfig(raw = link, protocol = "vless", name = name, host = host, port = port)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseShadowsocks(link: String): VpnConfig? {
        return try {
            val withoutScheme = link.removePrefix("ss://")
            val hashIndex = withoutScheme.indexOf('#')
            val namePart = if (hashIndex != -1) withoutScheme.substring(hashIndex + 1) else null
            val body = if (hashIndex != -1) withoutScheme.substring(0, hashIndex) else withoutScheme

            val atIndex = body.lastIndexOf('@')
            val host: String
            val port: Int

            if (atIndex != -1) {
                val hostPort = body.substring(atIndex + 1)
                val parts = hostPort.split(":")
                host = parts[0]
                port = parts.getOrNull(1)?.toIntOrNull() ?: 8388
            } else {
                val decoded = tryBase64Decode(body) ?: return null
                val at = decoded.lastIndexOf('@')
                if (at == -1) return null
                val hostPort = decoded.substring(at + 1)
                val parts = hostPort.split(":")
                host = parts[0]
                port = parts.getOrNull(1)?.toIntOrNull() ?: 8388
            }

            val name = namePart?.let { URLDecoder.decode(it, "UTF-8") } ?: host
            VpnConfig(raw = link, protocol = "ss", name = name, host = host, port = port)
        } catch (e: Exception) {
            null
        }
    }
}
