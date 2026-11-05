package com.miaomiao.assistant.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.miaomiao.assistant.util.Prefs
import com.miaomiao.assistant.util.Settings
import com.miaomiao.assistant.util.TextProcessor
import kotlin.math.max

/**
 * 无障碍文本改写服务
 * 对应原始 MiaoAccessibilityService：
 *  - 监听指定前台应用的输入框事件
 *  - 读取文本 → 经文本管线改写 → ACTION_SET_TEXT 写回
 */
class MiaoAccessibilityService : AccessibilityService() {

    companion object {
        const val TAG = "MiaoAccessibility"
        const val WECHAT_PKG = "com.tencent.mm"
        private const val SYSTEM_UI = "com.android.systemui"
        private const val NOTIF_ID = 81001
        private const val NOTIF_ERROR_ID = 81002
        private const val SETTLE_MS = 120L
        private const val DELETE_GUARD_MS = 700L
        private const val PROCESS_GUARD_MS = 1500L
        private const val MAX_DEPTH = 18
        private const val MAX_VISITED = 500
        private const val ACTION_SET_TEXT = 0x200000 // 2097152
        private const val ACTION_ARGUMENT_SET_TEXT = "ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE"
        private const val ACTION_CURSOR = 0x20000 // 131072
        private const val ACTION_ARG_SELECTION_START = "ACTION_ARGUMENT_SELECTION_START_INT"
        private const val ACTION_ARG_SELECTION_END = "ACTION_ARGUMENT_SELECTION_END_INT"

        @Volatile
        var instance: MiaoAccessibilityService? = null
            private set

        @Volatile
        var isRunning: Boolean = false
            private set

        private val WECHAT_EDIT_IDS = listOf(
            "com.tencent.mm:id/chatting_content_et",
            "com.tencent.mm:id/alk",
            "com.tencent.mm:id/alj",
            "com.tencent.mm:id/y5"
        )
        private val SEND_ID_KEYWORDS = listOf("send", "btn_send", "chatting_send_btn", "emoji_send_btn", "anv")
        private val SEND_TEXTS = listOf("发送", "Send", "SEND")
    }

    private data class PkgState(
        var lastWritten: String = "",
        var guardUntil: Long = 0L,
        var lastRaw: String = "",
        var initialized: Boolean = false,
        var lastEcho: String = ""
    )

    private val pkgStates = HashMap<String, PkgState>()
    private var isProcessing = false
    private var guardRunnable: Runnable? = null
    private var lastEventSource: AccessibilityNodeInfo? = null
    var currentForeground = ""
        private set
    private var processedCount = 0
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val scheduled = HashMap<String, Runnable>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        instance = this
        try {
            serviceInfo?.let { info ->
                info.flags = info.flags or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                setServiceInfo(info)
            }
        } catch (_: Exception) {
        }
        updateNotification()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        try {
            if (!Settings.serviceEnabled) return
            val pkg = event.packageName?.toString() ?: return
            when (event.eventType) {
                AccessibilityEvent.TYPE_VIEW_CLICKED -> handleClickEvent(event, pkg)
                AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                    if (!isImeOrSystem(pkg)) currentForeground = pkg
                    if (pkg == WECHAT_PKG) scheduleProcess(resolveTargetPkg(pkg))
                }
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                    if (!isImeOrSystem(pkg)) currentForeground = pkg
                    if (pkg == WECHAT_PKG) scheduleProcess(resolveTargetPkg(pkg))
                }
                AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                    lastEventSource = safeSource(event)
                    scheduleProcess(resolveTargetPkg(pkg))
                }
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    if (!isImeOrSystem(pkg)) {
                        if (currentForeground != pkg) resetPkgState(resolveTargetPkg(pkg))
                        currentForeground = pkg
                        updateNotification()
                    }
                }
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    val target = resolveTargetPkg(pkg)
                    if (pkg == WECHAT_PKG || target == WECHAT_PKG) {
                        lastEventSource = safeSource(event)
                        scheduleProcess(WECHAT_PKG)
                    }
                }
            }
        } catch (t: Throwable) {
            // 事件处理必须安全失败
        }
    }

    override fun onInterrupt() {
        cancelScheduled()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        instance = null
        handler.removeCallbacksAndMessages(null)
        pkgStates.clear()
        lastEventSource = null
    }

    override fun onLowMemory() {
        pkgStates.clear()
        cancelScheduled()
    }

    // ---------- 调度 ----------

    private fun scheduleProcess(pkg: String) {
        if (pkg.isEmpty()) return
        val apps = Settings.selectedApps()
        if (apps.isEmpty() || pkg !in apps) return
        scheduled.remove(pkg)?.let { handler.removeCallbacks(it) }
        val delay = if (Settings.delayEnabled) Settings.delayMs else SETTLE_MS
        val r = Runnable { scheduled.remove(pkg); doProcess(pkg) }
        scheduled[pkg] = r
        handler.postDelayed(r, delay)
    }

    private fun flushScheduled(pkg: String) {
        scheduled.remove(pkg)?.let {
            handler.removeCallbacks(it)
            doProcess(pkg)
        }
    }

    private fun cancelScheduled() {
        scheduled.values.forEach { handler.removeCallbacks(it) }
        scheduled.clear()
    }

    private fun resetPkgState(pkg: String) {
        if (pkg.isEmpty()) return
        pkgStates.remove(pkg)
        cancelScheduled()
    }

    private fun resolveTargetPkg(pkg: String): String {
        if (isImeOrSystem(pkg)) return currentForeground
        if (pkg != currentForeground) {
            currentForeground = pkg
            updateNotification()
        }
        return pkg
    }

    private fun isImeOrSystem(pkg: String): Boolean =
        pkg == packageName || pkg == SYSTEM_UI

    private fun safeSource(event: AccessibilityEvent): AccessibilityNodeInfo? = try {
        event.source
    } catch (_: Exception) {
        null
    }

    // ---------- 点击/发送检测 ----------

    private fun handleClickEvent(event: AccessibilityEvent, pkg: String) {
        val source = safeSource(event) ?: return
        try {
            val viewId = source.viewIdResourceName ?: ""
            val text = source.text?.toString() ?: ""
            val desc = source.contentDescription?.toString() ?: ""
            val combined = "$text $desc $viewId"
            if (SEND_ID_KEYWORDS.any { viewId.contains(it, true) } ||
                SEND_TEXTS.any { combined.contains(it) }
            ) {
                flushScheduled(pkg)
                resetPkgState(pkg)
            }
        } catch (_: Exception) {
        } finally {
            try {
                source.recycle()
            } catch (_: Exception) {
            }
        }
    }

    // ---------- 核心处理 ----------

    private fun doProcess(pkg: String) {
        if (isProcessing) return
        val st = pkgStates.getOrPut(pkg) { PkgState() }
        val now = System.currentTimeMillis()
        if (now < st.guardUntil) return

        val input = findInputNode(lastEventSource, pkg == WECHAT_PKG) ?: return
        try {
            var text = input.text?.toString() ?: return
            if (text.isEmpty()) {
                // 尝试刷新
                try {
                    input.refresh()
                    text = input.text?.toString() ?: return
                } catch (_: Exception) {
                    return
                }
            }
            if (text.isEmpty()) {
                st.lastWritten = ""
                st.lastRaw = ""
                st.lastEcho = ""
                return
            }

            // 密码框跳过
            if (input.isPassword) return

            // 回声/重复检测
            if (text == st.lastEcho && text == st.lastWritten) return
            st.lastEcho = text
            if (text == st.lastWritten) return

            // 删除防抖
            if (st.lastWritten.isNotEmpty() && text.length < st.lastWritten.length &&
                st.lastWritten.startsWith(text)
            ) {
                st.guardUntil = now + DELETE_GUARD_MS
                st.lastWritten = text
                return
            }

            val processed = TextProcessor.process(text, pkg == WECHAT_PKG)
            if (processed == text) {
                st.lastWritten = text
                return
            }
            writeText(pkg, input, processed)
        } catch (_: Exception) {
        } finally {
            try {
                input.recycle()
            } catch (_: Exception) {
            }
        }
    }

    private fun writeText(pkg: String, node: AccessibilityNodeInfo, text: String) {
        isProcessing = true
        armProcessingGuard()
        pkgStates[pkg]?.lastWritten = text
        processedCount++
        updateNotification()

        if (!setTextSafe(node, text)) {
            handler.postDelayed({
                try {
                    node.refresh()
                } catch (_: Exception) {
                }
                if (!setTextSafe(node, text)) {
                    val alt = findInputNode(lastEventSource, pkg == WECHAT_PKG)
                    if (alt != null) {
                        setTextSafe(alt, text)
                        setCursorToEnd(alt, text.length)
                        try {
                            alt.recycle()
                        } catch (_: Exception) {
                        }
                    }
                } else {
                    setCursorToEnd(node, text.length)
                }
                finishProcessing()
            }, 50L)
        } else {
            setCursorToEnd(node, text.length)
            handler.postDelayed({ finishProcessing() }, 40L)
        }
    }

    private fun armProcessingGuard() {
        guardRunnable?.let { handler.removeCallbacks(it) }
        guardRunnable = Runnable { isProcessing = false }
        handler.postDelayed(guardRunnable!!, PROCESS_GUARD_MS)
    }

    private fun finishProcessing() {
        isProcessing = false
        guardRunnable?.let { handler.removeCallbacks(it) }
        guardRunnable = null
    }

    private fun setTextSafe(node: AccessibilityNodeInfo, text: String): Boolean = try {
        val bundle = Bundle()
        bundle.putCharSequence(ACTION_ARGUMENT_SET_TEXT, text)
        node.performAction(ACTION_SET_TEXT, bundle)
    } catch (_: Exception) {
        false
    }

    private fun setCursorToEnd(node: AccessibilityNodeInfo, len: Int) {
        try {
            val b = Bundle()
            b.putInt(ACTION_ARG_SELECTION_START, len)
            b.putInt(ACTION_ARG_SELECTION_END, len)
            node.performAction(ACTION_CURSOR, b)
        } catch (_: Exception) {
        }
    }

    // ---------- 输入框查找 ----------

    private fun isEditTextClass(name: CharSequence?): Boolean {
        val s = name?.toString() ?: return false
        return s.contains("EditText", true) ||
            s.contains("TextInputEditText", true) ||
            s.contains("MMEditText", true)
    }

    private fun canSetText(node: AccessibilityNodeInfo): Boolean = try {
        if (node.isPassword) false
        else (node.actions and ACTION_SET_TEXT) != 0
    } catch (_: Exception) {
        false
    }

    private fun findWeChatById(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root == null) return null
        for (id in WECHAT_EDIT_IDS) {
            try {
                root.findAccessibilityNodeInfosByViewId(id)?.forEach { n ->
                    if (!n.isPassword && (n.isEditable || isEditTextClass(n.className) || canSetText(n))) {
                        return AccessibilityNodeInfo.obtain(n)
                    }
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun findInputNode(root: AccessibilityNodeInfo?, wechat: Boolean): AccessibilityNodeInfo? {
        if (wechat && root != null) {
            findWeChatById(root)?.let { return it }
        }
        val roots = collectRoots()
        var best: AccessibilityNodeInfo? = null
        var bestScore = -1
        for (r in roots) {
            val found = findBestEditable(r, true, 0, intArrayOf(0))
            if (found != null && found.ordinal > bestScore) {
                bestScore = found.ordinal
                best?.let { try { it.recycle() } catch (_: Exception) {} }
                best = found.node
            } else {
                found?.node?.let { try { it.recycle() } catch (_: Exception) {} }
            }
            try {
                r.recycle()
            } catch (_: Exception) {
            }
        }
        return best
    }

    private data class BestNode(val node: AccessibilityNodeInfo?, val ordinal: Int)

    private fun findBestEditable(
        node: AccessibilityNodeInfo?,
        allowEditText: Boolean,
        depth: Int,
        visited: IntArray
    ): BestNode? {
        if (node == null || depth > MAX_DEPTH) return null
        if (visited[0]++ > MAX_VISITED) return null
        var best = BestNode(null, -1)
        if (isEditableCandidate(node, allowEditText)) {
            var score = if (node.isFocused) 100 else 0
            try {
                if (node.isEditable) score += 40
                if (isEditTextClass(node.className)) score += 20
                if (!node.text.isNullOrEmpty()) score += 10
                if (node.isVisibleToUser) score += 5
            } catch (_: Exception) {
            }
            best = BestNode(AccessibilityNodeInfo.obtain(node), score)
        }
        val childCount = try {
            node.childCount
        } catch (_: Exception) {
            0
        }
        for (i in 0 until childCount) {
            val child = try {
                node.getChild(i)
            } catch (_: Exception) {
                null
            } ?: continue
            val sub = findBestEditable(child, allowEditText, depth + 1, visited)
            try {
                child.recycle()
            } catch (_: Exception) {
            }
            if (sub != null && sub.ordinal > best.ordinal) best = sub
        }
        return best
    }

    private fun isEditableCandidate(node: AccessibilityNodeInfo, allowEditText: Boolean): Boolean {
        if (node == null) return false
        return try {
            if (node.isPassword) return false
            if (canSetText(node)) return true
            allowEditText && (node.isEditable || isEditTextClass(node.className))
        } catch (_: Exception) {
            false
        }
    }

    private fun collectRoots(): List<AccessibilityNodeInfo> {
        val list = ArrayList<AccessibilityNodeInfo>()
        try {
            rootInActiveWindow?.let { list.add(it) }
        } catch (_: Exception) {
        }
        try {
            windows.forEach { w ->
                try {
                    val root = w.root
                    if (root != null && root !in list) list.add(root)
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }
        return list
    }

    // ---------- 通知 ----------

    private fun updateNotification() {
        try {
            val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            val text = if (currentForeground.isNotEmpty() &&
                Settings.selectedApps().contains(currentForeground)
            ) {
                "正在监听：$currentForeground，已处理 $processedCount 次"
            } else {
                "正在后台运行"
            }
            nm.notify(NOTIF_ID, buildNotification(text))
        } catch (_: Exception) {
        }
    }

    private fun buildNotification(text: String): android.app.Notification =
        android.app.Notification.Builder(this, "miao_service_channel")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("喵喵助手运行中")
            .setContentText(text)
            .setOngoing(true)
            .setShowWhen(false)
            .setCategory(android.app.Notification.CATEGORY_SERVICE)
            .setContentIntent(
                android.app.PendingIntent.getActivity(
                    this, 0,
                    Intent(this, com.miaomiao.assistant.MainActivity::class.java),
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
}