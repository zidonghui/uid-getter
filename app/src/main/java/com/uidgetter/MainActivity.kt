package com.uidgetter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.sankokomi.wirebare.kernel.common.IImportantEventListener
import top.sankokomi.wirebare.kernel.common.IProxyStatusListener
import top.sankokomi.wirebare.kernel.common.ImportantEvent
import top.sankokomi.wirebare.kernel.common.ProxyStatus
import top.sankokomi.wirebare.kernel.common.VpnPrepareActivity
import top.sankokomi.wirebare.kernel.common.WireBare

class MainActivity : VpnPrepareActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WireBare.addVpnProxyStatusListener(vpnStatusListener)
        WireBare.addImportantEventListener(eventListener)
        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }

    override fun onPrepareSuccess() {
        startWireBare()
    }

    private fun startWireBare() {
        WireBare.startProxy {
            mtu = 1400
            tcpProxyServerCount = 5
            ipv4ProxyAddress = "10.1.10.1" to 32
            enableIPv6 = false
            addRoutes("0.0.0.0" to 0)
            addAsyncHttpInterceptor(listOf(UidInterceptor.Factory()))
        }
    }

    private val vpnStatusListener = object : IProxyStatusListener {
        override fun onVpnStatusChanged(oldStatus: ProxyStatus, newStatus: ProxyStatus): Boolean {
            proxyStatusState.value = newStatus
            return false
        }
    }

    private val eventListener = object : IImportantEventListener {
        override fun onPost(event: ImportantEvent) {}
    }

    override fun onDestroy() {
        WireBare.removeVpnProxyStatusListener(vpnStatusListener)
        WireBare.removeImportantEventListener(eventListener)
        super.onDestroy()
    }

    @Composable
    private fun MainScreen() {
        val context = LocalContext.current
        val uid by UidState.uid.collectAsState()
        val status by proxyStatusState.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = "游戏助手",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(20.dp)
                ) {
                    Text(
                        text = "启动助手",
                        modifier = Modifier.weight(1f),
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = status == ProxyStatus.ACTIVE || status == ProxyStatus.STARTING,
                        onCheckedChange = { on ->
                            if (on) prepareProxy() else WireBare.stopProxy()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "获取到的UID",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val encrypted = uid?.let { UidState.xorEncrypt(it) }
                        Text(
                            text = encrypted ?: "等待捕获…",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (uid != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val currentUid = uid
                            if (currentUid.isNullOrBlank()) {
                                Toast.makeText(context, "暂无可复制的 UID", Toast.LENGTH_SHORT).show()
                            } else {
                                val encrypted = UidState.xorEncrypt(currentUid)
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("UID", encrypted))
                                Toast.makeText(context, "已复制加密 UID", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("复制")
                    }
                }
            }
        }
    }

    companion object {
        private val proxyStatusState = kotlinx.coroutines.flow.MutableStateFlow(ProxyStatus.DEAD)

        init {
            // 桥接 VPN 状态到 Compose
            // 实际状态通过 vpnStatusListener 更新
        }
    }
}
