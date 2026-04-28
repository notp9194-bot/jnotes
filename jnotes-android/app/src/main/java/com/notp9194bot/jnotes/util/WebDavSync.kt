package com.notp9194bot.jnotes.util

import android.util.Base64
import java.net.HttpURLConnection
import java.net.URL

object WebDavSync {
    /** Uploads bytes via PUT with HTTP Basic auth. Returns true on 2xx. */
    fun put(url: String, user: String, pass: String, bytes: ByteArray): Boolean {
        val conn = (URL(url).openConnection() as HttpURLConnection)
        return try {
            conn.requestMethod = "PUT"
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Content-Type", "application/json")
            if (user.isNotEmpty() || pass.isNotEmpty()) {
                val token = Base64.encodeToString("$user:$pass".toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                conn.setRequestProperty("Authorization", "Basic $token")
            }
            conn.setFixedLengthStreamingMode(bytes.size)
            conn.outputStream.use { it.write(bytes) }
            conn.responseCode in 200..299
        } catch (_: Throwable) {
            false
        } finally {
            conn.disconnect()
        }
    }

    /** Returns body bytes on success, null otherwise. */
    fun get(url: String, user: String, pass: String): ByteArray? {
        val conn = (URL(url).openConnection() as HttpURLConnection)
        return try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            if (user.isNotEmpty() || pass.isNotEmpty()) {
                val token = Base64.encodeToString("$user:$pass".toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                conn.setRequestProperty("Authorization", "Basic $token")
            }
            if (conn.responseCode in 200..299) conn.inputStream.use { it.readBytes() } else null
        } catch (_: Throwable) {
            null
        } finally {
            conn.disconnect()
        }
    }
}
