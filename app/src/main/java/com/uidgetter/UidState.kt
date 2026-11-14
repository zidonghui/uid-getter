package com.uidgetter

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UidState {
    val uid = MutableStateFlow<String?>(null)

    /** XOR 加密密钥（可自行修改） */
    private const val XOR_KEY = "GameAssist2026"

    /** 异或加密字符串，返回 Base64 可读结果 */
    fun xorEncrypt(input: String): String {
        if (input.isEmpty()) return ""
        val key = XOR_KEY.toByteArray()
        val data = input.toByteArray()
        val out = ByteArray(data.size) { i ->
            (data[i].toInt() xor key[i % key.size].toInt()).toByte()
        }
        return android.util.Base64.encodeToString(out, android.util.Base64.NO_WRAP)
    }
}
