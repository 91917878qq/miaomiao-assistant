package com.miaomiao.assistant.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * 配置访问封装
 * 所有读写走 SharedPreferences "miao_config"
 */
object Settings {
    lateinit var prefs: SharedPreferences
        private set

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
    }

    // ---- 服务 ----
    var serviceEnabled: Boolean
        get() = prefs.getBoolean(Prefs.SERVICE_ENABLED, false)
        set(v) = prefs.edit().putBoolean(Prefs.SERVICE_ENABLED, v).apply()

    fun selectedApps(): Set<String> =
        prefs.getStringSet(Prefs.SELECTED_APPS, emptySet()) ?: emptySet()

    fun setSelectedApps(apps: Set<String>) =
        prefs.edit().putStringSet(Prefs.SELECTED_APPS, apps).apply()

    var firstLaunchDone: Boolean
        get() = prefs.getBoolean(Prefs.FIRST_LAUNCH_DONE, false)
        set(v) = prefs.edit().putBoolean(Prefs.FIRST_LAUNCH_DONE, v).apply()

    // ---- 规则 ----
    fun loadRules(): List<Rule> {
        val raw = prefs.getString(Prefs.REPLACE_RULES, null) ?: return Prefs.DEFAULT_RULES
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val from = o.optString("from")
                if (from.isNotEmpty()) Rule(from, o.optString("to")) else null
            }.ifEmpty { Prefs.DEFAULT_RULES }
        } catch (_: Exception) {
            Prefs.DEFAULT_RULES
        }
    }

    fun saveRules(rules: List<Rule>) {
        val arr = JSONArray()
        rules.forEach { arr.put(JSONObject().put("from", it.from).put("to", it.to)) }
        prefs.edit().putString(Prefs.REPLACE_RULES, arr.toString()).apply()
    }

    fun loadPresets(): List<RulePreset> {
        val raw = prefs.getString(Prefs.RULE_PRESETS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val rulesArr = o.optJSONArray("rules") ?: JSONArray()
                val rules = (0 until rulesArr.length()).mapNotNull { j ->
                    val ro = rulesArr.getJSONObject(j)
                    val from = ro.optString("from")
                    if (from.isNotEmpty()) Rule(from, ro.optString("to")) else null
                }
                RulePreset(o.optString("name"), rules)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun savePresets(presets: List<RulePreset>) {
        val arr = JSONArray()
        presets.forEach { p ->
            val rulesArr = JSONArray()
            p.rules.forEach { rulesArr.put(JSONObject().put("from", it.from).put("to", it.to)) }
            arr.put(JSONObject().put("name", p.name).put("rules", rulesArr))
        }
        prefs.edit().putString(Prefs.RULE_PRESETS, arr.toString()).apply()
    }

    var activePreset: String
        get() = prefs.getString(Prefs.ACTIVE_PRESET, "") ?: ""
        set(v) = prefs.edit().putString(Prefs.ACTIVE_PRESET, v).apply()

    // ---- 标点 / 追加 ----
    var punctuationEnabled: Boolean
        get() = prefs.getBoolean(Prefs.PUNCTUATION_ENABLED, false)
        set(v) = prefs.edit().putBoolean(Prefs.PUNCTUATION_ENABLED, v).apply()

    var punctuationAppendText: String
        get() = prefs.getString(Prefs.PUNCTUATION_APPEND_TEXT, Prefs.Defaults.PUNCTUATION_APPEND_TEXT)!!
        set(v) = prefs.edit().putString(Prefs.PUNCTUATION_APPEND_TEXT, v).apply()

    var punctuationChars: String
        get() = prefs.getString(Prefs.PUNCTUATION_CHARS, Prefs.Defaults.PUNCTUATION_CHARS)!!
        set(v) = prefs.edit().putString(Prefs.PUNCTUATION_CHARS, v).apply()

    var punctAnchorEnd: Boolean
        get() = prefs.getBoolean(Prefs.PUNCT_ANCHOR_END, true)
        set(v) = prefs.edit().putBoolean(Prefs.PUNCT_ANCHOR_END, v).apply()

    var appendEnabled: Boolean
        get() = prefs.getBoolean(Prefs.APPEND_ENABLED, true)
        set(v) = prefs.edit().putBoolean(Prefs.APPEND_ENABLED, v).apply()

    var realtimeEnabled: Boolean
        get() = prefs.getBoolean(Prefs.REALTIME_ENABLED, true)
        set(v) = prefs.edit().putBoolean(Prefs.REALTIME_ENABLED, v).apply()

    var realtimeTrigger: Boolean
        get() = prefs.getBoolean(Prefs.REALTIME_TRIGGER, false)
        set(v) = prefs.edit().putBoolean(Prefs.REALTIME_TRIGGER, v).apply()

    var bracketProtect: Boolean
        get() = prefs.getBoolean(Prefs.BRACKET_PROTECT, true)
        set(v) = prefs.edit().putBoolean(Prefs.BRACKET_PROTECT, v).apply()

    var randomReplace: Boolean
        get() = prefs.getBoolean(Prefs.RANDOM_REPLACE, false)
        set(v) = prefs.edit().putBoolean(Prefs.RANDOM_REPLACE, v).apply()

    // ---- 表情 ----
    var emoticonEnabled: Boolean
        get() = prefs.getBoolean(Prefs.EMOTICON_ENABLED, false)
        set(v) = prefs.edit().putBoolean(Prefs.EMOTICON_ENABLED, v).apply()

    var emoticonCustom: String
        get() = prefs.getString(Prefs.EMOTICON_CUSTOM, "") ?: ""
        set(v) = prefs.edit().putString(Prefs.EMOTICON_CUSTOM, v).apply()

    var emoticonInterval: Int
        get() = maxOf(1, prefs.getInt(Prefs.EMOTICON_INTERVAL, 3))
        set(v) = prefs.edit().putInt(Prefs.EMOTICON_INTERVAL, maxOf(1, v)).apply()

    var emoticonSpaceBefore: Boolean
        get() = prefs.getBoolean(Prefs.EMOTICON_SPACE_BEFORE, true)
        set(v) = prefs.edit().putBoolean(Prefs.EMOTICON_SPACE_BEFORE, v).apply()

    fun emoticonList(): List<String> {
        val custom = emoticonCustom.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        return custom.ifEmpty { Prefs.DEFAULT_EMOTICONS }
    }

    // ---- 悬浮窗 ----
    var wechatOverlayEnabled: Boolean
        get() = prefs.getBoolean(Prefs.WECHAT_OVERLAY_ENABLED, false)
        set(v) = prefs.edit().putBoolean(Prefs.WECHAT_OVERLAY_ENABLED, v).apply()

    var overlayX: Int
        get() = prefs.getInt(Prefs.OVERLAY_X, -1)
        set(v) = prefs.edit().putInt(Prefs.OVERLAY_X, v).apply()

    var overlayY: Int
        get() = prefs.getInt(Prefs.OVERLAY_Y, -1)
        set(v) = prefs.edit().putInt(Prefs.OVERLAY_Y, v).apply()

    var overlaySize: Float
        get() = prefs.getFloat(Prefs.OVERLAY_SIZE, Prefs.Defaults.OVERLAY_SIZE)
        set(v) = prefs.edit().putFloat(Prefs.OVERLAY_SIZE, v.coerceIn(0.8f, 1.6f)).apply()

    var overlayOpacity: Float
        get() = prefs.getFloat(Prefs.OVERLAY_OPACITY, Prefs.Defaults.OVERLAY_OPACITY)
        set(v) = prefs.edit().putFloat(Prefs.OVERLAY_OPACITY, v.coerceIn(0.3f, 1.0f)).apply()

    var overlayAutoSnap: Boolean
        get() = prefs.getBoolean(Prefs.OVERLAY_AUTO_SNAP, false)
        set(v) = prefs.edit().putBoolean(Prefs.OVERLAY_AUTO_SNAP, v).apply()

    var overlayAppendText: String
        get() {
            val v = prefs.getString(Prefs.OVERLAY_APPEND_TEXT, Prefs.Defaults.OVERLAY_APPEND_TEXT)!!
            return v.ifEmpty { Prefs.Defaults.OVERLAY_APPEND_TEXT }
        }
        set(v) = if (v.isNotEmpty()) prefs.edit().putString(Prefs.OVERLAY_APPEND_TEXT, v).apply() else Unit

    var overlayShowIcon: Boolean
        get() = prefs.getBoolean(Prefs.OVERLAY_SHOW_ICON, false)
        set(v) = prefs.edit().putBoolean(Prefs.OVERLAY_SHOW_ICON, v).apply()

    var preOverlayTrigger: String
        get() = prefs.getString(Prefs.PRE_OVERLAY_TRIGGER, "none") ?: "none"
        set(v) = prefs.edit().putString(Prefs.PRE_OVERLAY_TRIGGER, v).apply()

    // ---- 延迟 ----
    var delayEnabled: Boolean
        get() = prefs.getBoolean(Prefs.DELAY_ENABLED, false)
        set(v) = prefs.edit().putBoolean(Prefs.DELAY_ENABLED, v).apply()

    var delayMs: Long
        get() = prefs.getLong(Prefs.DELAY_MS, Prefs.Defaults.DELAY_MS)
        set(v) = prefs.edit().putLong(Prefs.DELAY_MS, v).apply()

    // ---- 外观 / 行为 ----
    var themeMode: String
        get() = prefs.getString(Prefs.THEME_MODE, "system") ?: "system"
        set(v) = prefs.edit().putString(Prefs.THEME_MODE, v).apply()

    var glassEffectEnabled: Boolean
        get() = prefs.getBoolean(Prefs.GLASS_EFFECT_ENABLED, true)
        set(v) = prefs.edit().putBoolean(Prefs.GLASS_EFFECT_ENABLED, v).apply()

    var glassBlurLevel: Int
        get() = prefs.getInt(Prefs.GLASS_BLUR_LEVEL, 1).coerceIn(0, 2)
        set(v) = prefs.edit().putInt(Prefs.GLASS_BLUR_LEVEL, v.coerceIn(0, 2)).apply()

    var hideRecents: Boolean
        get() = prefs.getBoolean(Prefs.HIDE_RECENTS, false)
        set(v) = prefs.edit().putBoolean(Prefs.HIDE_RECENTS, v).apply()

    var hapticEnabled: Boolean
        get() = prefs.getBoolean(Prefs.HAPTIC_ENABLED, false)
        set(v) = prefs.edit().putBoolean(Prefs.HAPTIC_ENABLED, v).apply()

    var qqCatPaw: Boolean
        get() = prefs.getBoolean(Prefs.QQ_CAT_PAW, false)
        set(v) = prefs.edit().putBoolean(Prefs.QQ_CAT_PAW, v).apply()

    // ---- 权限检查 ----
    fun canDrawOverlays(context: Context): Boolean =
        android.provider.Settings.canDrawOverlays(context)
}

/** 规则预设（对应 RulePreset） */
data class RulePreset(
    val name: String,
    val rules: List<Rule>
)