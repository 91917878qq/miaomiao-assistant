package com.miaomiao.assistant.util

/**
 * 应用配置常量
 * 原混淆类: T0.k
 */
object Prefs {
    const val NAME = "miao_config"

    // 服务开关
    const val SERVICE_ENABLED = "service_enabled"
    const val SELECTED_APPS = "selected_apps"
    const val FIRST_LAUNCH_DONE = "first_launch_done"

    // 规则
    const val REPLACE_RULES = "replace_rules"
    const val RULE_PRESETS = "rule_presets"
    const val ACTIVE_PRESET = "active_preset"

    // 标点 / 追加
    const val PUNCTUATION_ENABLED = "punctuation_enabled"
    const val PUNCTUATION_APPEND_TEXT = "punctuation_append_text"
    const val PUNCTUATION_CHARS = "punctuation_chars"
    const val PUNCT_ANCHOR_END = "punct_anchor_end"
    const val APPEND_ENABLED = "append_enabled"
    const val REALTIME_ENABLED = "realtime_enabled"
    const val REALTIME_TRIGGER = "realtime_trigger"
    const val BRACKET_PROTECT = "bracket_protect"
    const val RANDOM_REPLACE = "random_replace"

    // 表情
    const val EMOTICON_ENABLED = "emoticon_enabled"
    const val EMOTICON_CUSTOM = "emoticon_custom"
    const val EMOTICON_INTERVAL = "emoticon_interval"
    const val EMOTICON_SPACE_BEFORE = "emoticon_space_before"

    // 悬浮窗
    const val WECHAT_OVERLAY_ENABLED = "wechat_overlay_enabled"
    const val OVERLAY_X = "overlay_x"
    const val OVERLAY_Y = "overlay_y"
    const val OVERLAY_SIZE = "overlay_size"
    const val OVERLAY_OPACITY = "overlay_opacity"
    const val OVERLAY_AUTO_SNAP = "overlay_auto_snap"
    const val OVERLAY_APPEND_TEXT = "overlay_append_text"
    const val OVERLAY_SHOW_ICON = "overlay_show_icon"
    const val PRE_OVERLAY_TRIGGER = "pre_overlay_trigger"

    // 延迟
    const val DELAY_ENABLED = "delay_enabled"
    const val DELAY_MS = "delay_ms"

    // 外观 / 行为
    const val THEME_MODE = "theme_mode"
    const val GLASS_EFFECT_ENABLED = "glass_effect_enabled"
    const val GLASS_BLUR_LEVEL = "glass_blur_level"
    const val HIDE_RECENTS = "hide_recents"
    const val HAPTIC_ENABLED = "haptic_enabled"
    const val QQ_CAT_PAW = "qq_cat_paw"

    // 默认值
    object Defaults {
        const val PUNCTUATION_APPEND_TEXT = "喵"
        const val PUNCTUATION_CHARS = "。！？ "
        const val OVERLAY_APPEND_TEXT = "喵"
        const val OVERLAY_SIZE = 1.0f
        const val OVERLAY_OPACITY = 1.0f
        const val DELAY_MS = 300L
        const val RULE_PRESET_MAX = 30
    }

    /** 内置默认替换规则 */
    val DEFAULT_RULES = listOf(
        Rule("我", "本喵"),
        Rule("你", "主人")
    )

    /** 内置颜文字 */
    val DEFAULT_EMOTICONS = listOf(
        "^⌯𖥦⌯^ ੭ ^", "⌯'ㅅ'⌯", "=^𖥦^=", "⌯•ㅅ•⌯", "ฅ•̀∀•́ฅ",
        "ฅ ̳͒•ˑ̫• ̳͒ฅ♡", "ฅ(̳•·̫•̳ฅ)♡", "ฅ^••^ฅ", "=^•ω•^=",
        "₍^ >ヮ<^₎", "/ᐠ - ˕ -マ Ⳋ", "ฅ^•ﻌ•^ฅ", "ฅ՞•ﻌ•՞ฅ",
        "(ฅ´ω`ฅ)", "ฅ(*`ω´*)ฅ", "₍˄·͈༝·͈˄*₎◞ ̑̑", "(>^ω^<)",
        "ฅ^-﹃-^ฅ", "(=^･ᴥ･^=)", "(^ω^ฅ)", "ฅ(≧▽≦)ฅ",
        "ฅ(=´▽`=)ฅ", "(ฅ◑ω◑ฅ)", "(ฅ>ω<*ฅ)", "(=^.^=)",
        "(=ↀωↀ=)", "(=^-ω-^=)", "ฅ(*°ω°*ฅ)", "(^•ᴥ•^)",
        "( Φ ω Φ )", "ฅ( ̳• ◡ • ̳)ฅ", "o( =•ω•= )m", "~o( =∩ω∩= )m",
        "≡ω≡", "(=´ᴥ`)"
    )
}