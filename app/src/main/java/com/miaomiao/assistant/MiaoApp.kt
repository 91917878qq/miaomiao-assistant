package com.miaomiao.assistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.miaomiao.assistant.util.Settings

/**
 * 应用入口
 * 对应原 MiaoApp：初始化 SharedPreferences、通知渠道、崩溃日志目录
 */
class MiaoApp : Application() {

    companion object {
        const val CHANNEL_ID = "miao_service_channel"
        const val CHANNEL_ERROR_ID = "miao_error_channel_2"
    }

    override fun onCreate() {
        super.onCreate()
        Settings.init(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_description)
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }
        val errorChannel = NotificationChannel(
            CHANNEL_ERROR_ID,
            "服务异常提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "无障碍服务异常时提醒重新开启"
            setShowBadge(true)
        }
        try {
            nm.deleteNotificationChannel("miao_error_channel")
        } catch (_: Exception) {
        }
        nm.createNotificationChannel(serviceChannel)
        nm.createNotificationChannel(errorChannel)
    }
}