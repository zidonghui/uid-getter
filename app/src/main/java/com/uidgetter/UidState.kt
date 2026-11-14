package com.uidgetter

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UidState {
    val uid = MutableStateFlow<String?>(null)

    /** 调试：最近拦截到的任意响应 URL */
    val lastUrl = MutableStateFlow<String?>(null)

    /** 调试：最近一次匹配 checkLogin 的原始响应体 */
    val lastRawBody = MutableStateFlow<String?>(null)
}
