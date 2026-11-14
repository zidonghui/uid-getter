package com.uidgetter

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UidState {
    val uid = MutableStateFlow<String?>(null)
}
