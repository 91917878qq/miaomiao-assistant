package com.miaomiao.assistant

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.miaomiao.assistant.service.MiaoAccessibilityService
import com.miaomiao.assistant.service.MiaoOverlayService
import com.miaomiao.assistant.util.Settings

/**
 * 主界面
 * 对应原 MainActivity：设置开关、一键检测、关于
 * 使用 Kyant0/AndroidLiquidGlass 库实现液态玻璃背景/导航栏/卡片
 */
class MainActivity : ComponentActivity() {

    private var serviceEnabled by mutableStateOf(false)
    private var overlayEnabled by mutableStateOf(false)
    private var firstLaunch by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        serviceEnabled = Settings.serviceEnabled
        overlayEnabled = Settings.wechatOverlayEnabled
        firstLaunch = !Settings.firstLaunchDone

        setContent {
            MaterialTheme {
                LiquidGlassBackground {
                    MainScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        serviceEnabled = Settings.serviceEnabled
        overlayEnabled = Settings.wechatOverlayEnabled
    }

    @Composable
    private fun LiquidGlassBackground(content: @Composable () -> Unit) {
        val backdrop = rememberCanvasBackdrop {
            // 渐变背景（玻璃底图）
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF8EC5FC),
                        Color(0xFFE0C3FC),
                        Color(0xFFB8C6DB)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
            )
            // 点缀色块（让玻璃折射可见）
            drawCircle(Color(0x55FFFFFF), radius = size.minDimension * 0.3f, center = Offset(size.width * 0.2f, size.height * 0.25f))
            drawCircle(Color(0x33FFFFFF), radius = size.minDimension * 0.2f, center = Offset(size.width * 0.8f, size.height * 0.6f))
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(0.dp) },
                    effects = {
                        lens(refractionHeight = 18f, refractionAmount = 1.5f)
                    }
                )
        ) {
            content()
        }
    }

    @Composable
    private fun MainScreen() {
        var selectedTab by remember { mutableStateOf(0) }
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                GlassNavigationBar(selectedTab) { selectedTab = it }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> HomeTab()
                    1 -> ToolsTab()
                    2 -> AboutTab()
                }
            }
        }
    }

    @Composable
    private fun GlassNavigationBar(selectedTab: Int, onSelect: (Int) -> Unit) {
        val backdrop = rememberCanvasBackdrop {
            drawRect(Color(0x66FFFFFF))
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(24.dp) },
                    effects = {
                        lens(refractionHeight = 14f, refractionAmount = 1.2f, depthEffect = true)
                    }
                ),
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.3f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GlassTab("🏠", "首页", selectedTab == 0) { onSelect(0) }
                GlassTab("🛠", "工具", selectedTab == 1) { onSelect(1) }
                GlassTab("ℹ", "关于", selectedTab == 2) { onSelect(2) }
            }
        }
    }

    @Composable
    private fun GlassTab(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            color = if (selected) Color.White.copy(alpha = 0.4f) else Color.Transparent
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(icon)
                Text(label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    @Composable
    private fun GlassCard(title: String, content: @Composable ColumnScope.() -> Unit) {
        val backdrop = rememberCanvasBackdrop {
            drawRect(Color(0x59FFFFFF))
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(20.dp) },
                    effects = {
                        lens(refractionHeight = 10f, refractionAmount = 1.0f, depthEffect = true)
                    }
                ),
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.25f)
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }

    @Composable
    private fun HomeTab() {
        Text("喵喵助手", style = MaterialTheme.typography.headlineMedium)
        Text("完成两项权限即可使用", style = MaterialTheme.typography.bodyMedium)

        GlassCard("服务开关") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("无障碍服务", style = MaterialTheme.typography.titleMedium)
                    Text("在指定应用中自动改写文本", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = serviceEnabled,
                    onCheckedChange = {
                        serviceEnabled = it
                        Settings.serviceEnabled = it
                        if (it) openAccessibilitySettings()
                    }
                )
            }
            if (serviceEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("悬浮窗", style = MaterialTheme.typography.titleMedium)
                        Text("手动处理面板", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = overlayEnabled,
                        onCheckedChange = {
                            overlayEnabled = it
                            Settings.wechatOverlayEnabled = it
                            startOverlay()
                        }
                    )
                }
            }
        }

        if (firstLaunch) {
            GlassCard("使用提示") {
                Text("• 在最近任务里长按或下滑本应用卡片，锁定后台。")
                Text("• 把工具页的后台保活设置都打开，运行更稳。")
                Text("• 重启手机后，需要重新开启无障碍服务。")
                Text("• 配置完选项后建议先使用一键检测，以保证配置无误。")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    firstLaunch = false
                    Settings.firstLaunchDone = true
                }) { Text("知道了") }
            }
        }
    }

    @Composable
    private fun ToolsTab() {
        Text("工具", style = MaterialTheme.typography.headlineMedium)
        GlassCard("一键检测") {
            Text("将读取本机系统权限、已选应用与版本信息，逐项判断功能状态。", style = MaterialTheme.typography.bodySmall)
            Text("全程离线，不联网、不上传任何信息，约需 1～3 秒。", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { runDiagnostics() }) { Text("开始检测") }
        }
    }

    @Composable
    private fun AboutTab() {
        Text("关于", style = MaterialTheme.typography.headlineMedium)
        GlassCard("关于本应用") {
            Text("喵喵助手 3.0.0 · 91917878qq", style = MaterialTheme.typography.titleMedium)
            Text("开发者：91917878qq", style = MaterialTheme.typography.bodySmall)
            Text("反馈邮箱：1490964435@qq.com", style = MaterialTheme.typography.bodySmall)
            Text("赞助支持：给作者添点动力", style = MaterialTheme.typography.bodySmall)
            Text("本软件通过 Android 无障碍服务实现文本改写功能，所有处理均在本地完成，不收集、不上传用户任何输入内容。", style = MaterialTheme.typography.bodySmall)
        }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun startOverlay() {
        if (overlayEnabled && Settings.wechatOverlayEnabled) {
            val intent = Intent(this, MiaoOverlayService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            stopService(Intent(this, MiaoOverlayService::class.java))
        }
    }

    private fun runDiagnostics() {
        val overlayOk = Settings.canDrawOverlays(this)
        val msg = buildString {
            appendLine("检测结果：")
            appendLine("无障碍服务：${if (MiaoAccessibilityService.isRunning) "正常" else "未开启或已异常关闭"}")
            appendLine("悬浮窗权限：${if (overlayOk) "正常" else "未开启"}")
            appendLine("已选应用：${Settings.selectedApps().size} 个")
            appendLine("版本：3.0.0 (51)")
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("检测结果")
            .setMessage(msg)
            .setPositiveButton("确定", null)
            .show()
    }
}