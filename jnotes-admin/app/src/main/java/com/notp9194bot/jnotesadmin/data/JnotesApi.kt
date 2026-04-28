package com.notp9194bot.jnotesadmin.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class JnotesApi(private val baseUrl: String, private val adminToken: String?) {

    data class ChatMessage(
        val id: String, val from: String, val text: String, val ts: Long, val read: Boolean,
    )

    data class Thread(
        val userId: String, val name: String, val lastMessage: String,
        val lastTs: Long, val lastFrom: String?, val unread: Int,
    )

    data class Feedback(
        val id: String, val userId: String, val name: String, val text: String, val ts: Long,
    )

    suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        runCatching { request("GET", "/api/healthz", null).first in 200..299 }
            .getOrDefault(false)
    }

    suspend fun listThreads(): List<Thread> = withContext(Dispatchers.IO) {
        val (code, body) = request("GET", "/api/chat/threads", null)
        if (code !in 200..299) return@withContext emptyList()
        val arr = JSONArray(body)
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Thread(
                userId = o.optString("userId"),
                name = o.optString("name"),
                lastMessage = o.optString("lastMessage"),
                lastTs = o.optLong("lastTs"),
                lastFrom = if (o.isNull("lastFrom")) null else o.optString("lastFrom"),
                unread = o.optInt("unread"),
            )
        }
    }

    suspend fun listFeedback(): List<Feedback> = withContext(Dispatchers.IO) {
        val (code, body) = request("GET", "/api/feedback", null)
        if (code !in 200..299) return@withContext emptyList()
        val arr = JSONArray(body)
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Feedback(
                id = o.optString("id"),
                userId = o.optString("userId"),
                name = o.optString("name"),
                text = o.optString("text"),
                ts = o.optLong("ts"),
            )
        }
    }

    suspend fun getMessages(userId: String, since: Long = 0L): List<ChatMessage> =
        withContext(Dispatchers.IO) {
            val (code, body) = request("GET", "/api/chat/messages?userId=$userId&since=$since", null)
            if (code !in 200..299) return@withContext emptyList()
            val arr = JSONArray(body)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                ChatMessage(
                    id = o.optString("id"),
                    from = o.optString("from"),
                    text = o.optString("text"),
                    ts = o.optLong("ts"),
                    read = o.optBoolean("read"),
                )
            }
        }

    suspend fun reply(userId: String, text: String): ChatMessage? = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("userId", userId)
            .put("from", "admin")
            .put("text", text)
        val (code, resp) = request("POST", "/api/chat/send", body)
        if (code !in 200..299) return@withContext null
        val o = JSONObject(resp).optJSONObject("message") ?: return@withContext null
        ChatMessage(
            id = o.optString("id"),
            from = o.optString("from"),
            text = o.optString("text"),
            ts = o.optLong("ts"),
            read = o.optBoolean("read"),
        )
    }

    suspend fun markRead(userId: String): Boolean = withContext(Dispatchers.IO) {
        val body = JSONObject().put("userId", userId).put("who", "admin")
        request("POST", "/api/chat/read", body).first in 200..299
    }

    private fun request(method: String, path: String, body: JSONObject?): Pair<Int, String> {
        val url = URL(baseUrl.trimEnd('/') + path)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 8000
            readTimeout = 12000
            doInput = true
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            adminToken?.takeIf { it.isNotBlank() }?.let {
                setRequestProperty("X-Admin-Token", it)
            }
        }
        if (body != null) {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
        val text = stream?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
        return code to text
    }
}
