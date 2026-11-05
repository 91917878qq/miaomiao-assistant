package com.miaomiao.assistant.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.miaomiao.assistant.MiaoApp
import com.miaomiao.assistant.R
import com.miaomiao.assistant.util.Settings
import com.miaomiao.assistant.util.TextProcessor

/**
 * 悬浮窗服务
 * 对应原 MiaoOverlayService：可拖拽气泡 + 手动处理面板
 */
class MiaoOverlayService : Service() {

    companion object {
        const val NOTIF_ID = 3002
    }

    private var wm: WindowManager? = null
    private var bubble: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var panel: View? = null
    private var panelParams: WindowManager.LayoutParams? = null
    private var editText: EditText? = null
    private val baseSizeDp = 52

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
        buildBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return if (Settings.wechatOverlayEnabled) START_STICKY else {
            stopSelf()
            START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            bubble?.let { wm?.removeView(it) }
        } catch (_: Exception) {
        }
        try {
            panel?.let { wm?.removeView(it) }
        } catch (_: Exception) {
        }
        bubble = null
        panel = null
        editText = null
    }

    private fun startAsForeground() {
        val notif = Notification.Builder(this, MiaoApp.CHANNEL_ID)
            .setContentTitle("喵喵助手悬浮窗")
            .setContentText("手动处理运行中")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notif, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()

    private fun overlayType(): Int = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

    private fun buildBubble() {
        val size = (Settings.overlaySize * dp(baseSizeDp.toFloat())).toInt()
        val params = WindowManager.LayoutParams(
            size, size,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (Settings.overlayX >= 0) Settings.overlayX else dp(16f)
            y = if (Settings.overlayY >= 0) Settings.overlayY else dp(120f)
        }
        bubbleParams = params

        val view = if (Settings.overlayShowIcon) {
            createIconBubbleView()
        } else {
            createTextBubbleView(Settings.overlayAppendText.ifEmpty { "喵" })
        }
        view.alpha = Settings.overlayOpacity
        bubble = view
        attachBubbleTouch(view, params)
        try {
            wm?.addView(view, params)
        } catch (_: Exception) {
            stopSelf()
        }
    }

    private fun createTextBubbleView(text: String): TextView {
        return TextView(this).apply {
            setText(text)
            setTextColor(Color.WHITE)
            setTextSize(if (text.codePointCount(0, text.length) <= 1) 17f else 11f)
            gravity = Gravity.CENTER
            background = bubbleBg()
        }
    }

    private fun createIconBubbleView(): View {
        return TextView(this).apply {
            text = "🐱"
            setTextSize(24f)
            gravity = Gravity.CENTER
            background = bubbleBg()
        }
    }

    private fun bubbleBg(): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColors(intArrayOf(Color.parseColor("#6E7681"), Color.parseColor("#4B5563")))
        orientation = GradientDrawable.Orientation.TL_BR
    }

    private fun attachBubbleTouch(view: View, params: WindowManager.LayoutParams) {
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragging = false
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!dragging && (kotlin.math.abs(dx) > dp(6f) || kotlin.math.abs(dy) > dp(6f))) {
                        dragging = true
                    }
                    if (dragging) {
                        params.x = startX + dx.toInt()
                        params.y = startY + dy.toInt()
                        try {
                            wm?.updateViewLayout(view, params)
                        } catch (_: Exception) {
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (dragging) {
                        Settings.overlayX = params.x
                        Settings.overlayY = params.y
                        snapToEdge()
                    } else {
                        expandPanel()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun snapToEdge() {
        val params = bubbleParams ?: return
        val width = resources.displayMetrics.widthPixels
        val margin = dp(8f)
        val target = if (params.x + params.width / 2 >= width / 2) {
            (width - params.width - margin).coerceAtLeast(margin)
        } else {
            margin
        }
        if (target != params.x) {
            params.x = target
            try {
                wm?.updateViewLayout(bubble, params)
            } catch (_: Exception) {
            }
            Settings.overlayX = target
        }
    }

    private fun expandPanel() {
        if (panel?.parent != null) return
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12f), dp(8f), dp(12f), dp(10f))
            background = panelBg()
            elevation = dp(8f).toFloat()
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = "手动处理"
            setTextColor(Color.parseColor("#1F2937"))
            setTextSize(14f)
        }
        title.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        header.addView(title)
        val closeBtn = smallButton("关闭")
        val foldBtn = smallButton("收起")
        header.addView(foldBtn)
        header.addView(closeBtn)
        container.addView(header)

        val input = EditText(this).apply {
            hint = "输入或粘贴需要处理的文本"
            setTextColor(Color.parseColor("#1F2937"))
            setHintTextColor(Color.parseColor("#9CA3AF"))
            setTextSize(14f)
            gravity = Gravity.TOP or Gravity.START
            setPadding(dp(10f), dp(7f), dp(10f), dp(7f))
            background = inputBg()
        }
        input.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8f) }
        container.addView(input)
        this.editText = input

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        actions.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8f) }
        val doBtn = actionButton("处理")
        val copyBtn = actionButton("复制")
        doBtn.layoutParams = LinearLayout.LayoutParams(0, dp(30f), 1f).apply { marginEnd = dp(6f) }
        copyBtn.layoutParams = LinearLayout.LayoutParams(0, dp(30f), 1f).apply { marginStart = dp(6f) }
        actions.addView(doBtn)
        actions.addView(copyBtn)
        container.addView(actions)

        doBtn.setOnClickListener { handleProcess() }
        copyBtn.setOnClickListener { handleCopy() }
        foldBtn.setOnClickListener { collapsePanel() }
        closeBtn.setOnClickListener {
            collapsePanel()
            Settings.wechatOverlayEnabled = false
            stopSelf()
        }

        val params = WindowManager.LayoutParams(
            dp(216f),
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bubbleParams?.x ?: dp(16f)
            y = bubbleParams?.y ?: dp(104f)
        }
        panelParams = params
        panel = container
        try {
            wm?.addView(container, params)
            bubble?.visibility = View.INVISIBLE
        } catch (_: Exception) {
        }
    }

    private fun collapsePanel() {
        try {
            panel?.let { wm?.removeView(it) }
        } catch (_: Exception) {
        }
        panel = null
        editText = null
        bubble?.visibility = View.VISIBLE
    }

    private fun handleProcess() {
        val text = editText?.text?.toString() ?: ""
        if (text.isEmpty()) {
            toast("请先输入文本")
            return
        }
        val foreground = MiaoAccessibilityService.instance?.currentForeground ?: ""
        val isQQ = foreground == "com.tencent.mobileqq"
        val result = TextProcessor.process(text, isQQ)
        editText?.setText(result)
        editText?.setSelection(result.length)
    }

    private fun handleCopy() {
        val text = editText?.text?.toString() ?: ""
        if (text.isEmpty()) {
            toast("暂无可复制的内容")
            return
        }
        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("miao", text))
        toast("已复制到剪贴板")
    }

    private fun actionButton(text: String): Button = Button(this).apply {
        setText(text)
        setAllCaps(false)
        setTextSize(13f)
        setTextColor(Color.WHITE)
        background = GradientDrawable().apply {
            cornerRadius = dp(14f).toFloat()
            setColors(intArrayOf(Color.parseColor("#6E7681"), Color.parseColor("#4B5563")))
            orientation = GradientDrawable.Orientation.TL_BR
        }
    }

    private fun smallButton(text: String): Button = Button(this).apply {
        setText(text)
        setAllCaps(false)
        setTextSize(12f)
        setTextColor(Color.parseColor("#4B5563"))
        background = GradientDrawable().apply {
            cornerRadius = dp(12f).toFloat()
            setColor(Color.parseColor("#EFF1F4"))
        }
    }

    private fun panelBg(): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(20f).toFloat()
        setColor(Color.parseColor("#F7F8FA"))
        setStroke(dp(1f), Color.parseColor("#E5E7EB"))
    }

    private fun inputBg(): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(14f).toFloat()
        setColor(Color.WHITE)
        setStroke(dp(1f), Color.parseColor("#E5E7EB"))
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}