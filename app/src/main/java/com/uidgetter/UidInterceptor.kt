package com.uidgetter

import top.sankokomi.wirebare.kernel.interceptor.http.HttpSession
import top.sankokomi.wirebare.kernel.interceptor.http.async.AsyncHttpIndexedInterceptor
import top.sankokomi.wirebare.kernel.interceptor.http.async.AsyncHttpInterceptChain
import top.sankokomi.wirebare.kernel.interceptor.http.async.AsyncHttpInterceptor
import top.sankokomi.wirebare.kernel.interceptor.http.async.AsyncHttpInterceptorFactory
import top.sankokomi.wirebare.kernel.util.uncompressGzip
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

class UidInterceptor : AsyncHttpIndexedInterceptor() {

    private val bodyBuffers = ConcurrentHashMap<Long, ByteArrayOutputStream>()

    class Factory : AsyncHttpInterceptorFactory {
        override fun create(): AsyncHttpInterceptor = UidInterceptor()
    }

    override suspend fun onRequest(
        chain: AsyncHttpInterceptChain,
        buffer: ByteBuffer,
        session: HttpSession,
        index: Int
    ) {
        super.onRequest(chain, buffer, session, index)
    }

    override suspend fun onRequestFinished(
        chain: AsyncHttpInterceptChain,
        session: HttpSession,
        index: Int
    ) {
        super.onRequestFinished(chain, session, index)
    }

    override suspend fun onResponse(
        chain: AsyncHttpInterceptChain,
        buffer: ByteBuffer,
        session: HttpSession,
        index: Int
    ) {
        if (index == 0) {
            val url = session.response.url.orEmpty()
            if (url.contains("checkLogin", ignoreCase = true)) {
                val bos = ByteArrayOutputStream()
                bodyBuffers[session.response.sequence] = bos
                val dup = buffer.duplicate()
                val arr = ByteArray(dup.remaining())
                dup.get(arr)
                // 跳过响应头（\r\n\r\n 之后才是 body）
                val headerEnd = findHeaderEnd(arr)
                if (headerEnd >= 0) {
                    bos.write(arr, headerEnd, arr.size - headerEnd)
                }
            }
        } else {
            bodyBuffers[session.response.sequence]?.let { bos ->
                val dup = buffer.duplicate()
                val arr = ByteArray(dup.remaining())
                dup.get(arr)
                bos.write(arr)
            }
        }
        super.onResponse(chain, buffer, session, index)
    }

    override suspend fun onResponseFinished(
        chain: AsyncHttpInterceptChain,
        session: HttpSession,
        index: Int
    ) {
        bodyBuffers.remove(session.response.sequence)?.let { bos ->
            runCatching {
                var body = bos.toByteArray()
                body = dechunk(body)
                // gzip 解压
                val encoding = session.response.contentEncoding.orEmpty().lowercase()
                if (encoding.contains("gzip")) {
                    body = runCatching { body.uncompressGzip() }.getOrDefault(body)
                }
                val text = body.toString(Charsets.UTF_8)
                val root = JSONObject(text)
                val uid = root.optJSONObject("data")?.optString("uid").orEmpty()
                if (uid.isNotBlank()) {
                    UidState.uid.value = uid
                }
            }
        }
        }
        super.onResponseFinished(chain, session, index)
    }

    private fun findHeaderEnd(data: ByteArray): Int {
        for (i in 0..data.size - 4) {
            if (data[i] == '\r'.code.toByte() && data[i + 1] == '\n'.code.toByte() &&
                data[i + 2] == '\r'.code.toByte() && data[i + 3] == '\n'.code.toByte()
            ) {
                return i + 4
            }
        }
        return -1
    }

    private fun dechunk(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        var pos = 0
        while (pos < data.size) {
            var lineEnd = -1
            var i = pos
            while (i < data.size - 1) {
                if (data[i] == '\r'.code.toByte() && data[i + 1] == '\n'.code.toByte()) {
                    lineEnd = i
                    break
                }
                i++
            }
            if (lineEnd < 0) break
            val sizeLine = String(data, pos, lineEnd - pos, Charsets.US_ASCII)
            val sizeStr = sizeLine.substringBefore(';').trim()
            val chunkSize = sizeStr.toIntOrNull(16) ?: return data
            if (chunkSize == 0) break
            val dataStart = lineEnd + 2
            val dataEnd = dataStart + chunkSize
            if (dataEnd > data.size) break
            out.write(data, dataStart, chunkSize)
            pos = dataEnd + 2
            if (pos > data.size) break
        }
        val result = out.toByteArray()
        return if (result.isEmpty()) data else result
    }
}
