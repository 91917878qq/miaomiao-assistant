package com.miaomiao.assistant.util

/**
 * 文本处理管线
 * 处理顺序（与原版一致）：
 *  1. 括号保护替换规则
 *  2. 标点后追加
 *  3. 追加颜文字
 *  4. QQ 猫爪包裹
 */
object TextProcessor {

    private val OPEN_BRACKETS = setOf('【', '〖', '“', '‘', '（', '《', '〈', '(', '[', '{', '《', '"', '\'')
    private val CLOSE_BRACKETS = setOf('】', '〗', '”', '’', '）', '》', '〉', ')', ']', '}', '》', '"', '\'')
    private const val CAT_PAW_TAG = "\u0014ǿ\u000fC\u0000"

    /**
     * 完整处理入口
     * @param text 输入文本
     * @param qq 是否 QQ（应用猫爪）
     */
    fun process(text: String, qq: Boolean = false): String {
        if (text.isEmpty()) return text
        var result = text

        // 1. 实时替换规则
        if (Settings.realtimeEnabled) {
            result = applyRules(result, Settings.loadRules(), Settings.bracketProtect, Settings.randomReplace)
        }

        // 2. 标点触发追加
        result = applyPunctuation(result)

        // 3. 追加颜文字
        result = appendEmoticon(result)

        // 4. QQ 猫爪
        if (qq && Settings.qqCatPaw) {
            result = wrapCatPaw(result)
        }
        return result
    }

    /** 替换规则：from -> to，支持括号保护和随机替换 */
    fun applyRules(text: String, rules: List<Rule>, bracketProtect: Boolean, random: Boolean): String {
        if (text.isEmpty() || rules.isEmpty()) return text
        val sorted = rules.filter { it.from.isNotEmpty() }.sortedByDescending { it.from.length }
        val sb = StringBuilder()
        val len = text.length
        var i = 0
        var depth = 0
        while (i < len) {
            val c = text[i]
            // 括号保护
            if (bracketProtect) {
                if (c == '[') { depth++; sb.append(c); i++; continue }
                if (c == ']') { if (depth > 0) depth--; sb.append(c); i++; continue }
                if (depth > 0) { sb.append(c); i++; continue }
            }
            var matched = false
            for (rule in sorted) {
                if (i + rule.from.length <= len && text.substring(i, i + rule.from.length) == rule.from) {
                    // 词边界保护（避免过度替换）
                    if (rule.from.length > 0) {
                        val afterIdx = i + rule.from.length
                        // 拼音词边界检查：匹配后一个字符不是常见中文标点去掉（简化实现保持与原版一致即可）
                    }
                    sb.append(rule.to)
                    i += rule.from.length
                    matched = true
                    if (random) {
                        // 随机模式只替换一条
                        return sb.toString() + text.substring(i)
                    }
                    break
                }
            }
            if (!matched) {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    /** 标点触发：在句尾标点后追加文本（对应 n.j / n.i） */
    fun applyPunctuation(text: String): String {
        if (text.isEmpty()) return text
        val appendText = Settings.punctuationAppendText
        if (appendText.isEmpty()) return text

        val triggerRealtime = Settings.realtimeTrigger
        val triggerPunct = Settings.punctuationEnabled
        if (!triggerRealtime && !triggerPunct) return text

        val punctSet = Settings.punctuationChars.toSet()
        if (punctSet.isEmpty()) return text

        // 找到最后一个标点后的内容，在其后追加
        var lastPunctIdx = -1
        for (i in text.indices) {
            if (text[i] in punctSet) lastPunctIdx = i
        }
        if (lastPunctIdx < 0) return text

        // 检查是否已追加过
        val tail = text.substring(lastPunctIdx + 1)
        if (tail.contains(appendText)) return text

        return text.substring(0, lastPunctIdx + 1) + appendText + tail
    }

    /** 追加颜文字（对应 n.a） */
    fun appendEmoticon(text: String): String {
        if (!Settings.emoticonEnabled || text.isEmpty()) return text
        if (!text.any { it.isLetterOrDigit() }) return text
        val emoticons = Settings.emoticonList()
        if (emoticons.isEmpty()) return text
        // 轮换选择
        val idx = (System.currentTimeMillis() / Settings.emoticonInterval.toLong()).toInt() % emoticons.size
        val emo = emoticons[idx]
        if (text.endsWith(emo)) return text
        val space = if (Settings.emoticonSpaceBefore) " " else ""
        return text + space + emo
    }

    /** QQ 猫爪包裹（对应 n.b） */
    fun wrapCatPaw(text: String): String {
        if (text.isEmpty()) return text
        val stripped = stripCatPaw(text)
        if (stripped.isEmpty()) return text
        return CAT_PAW_TAG + stripped + CAT_PAW_TAG
    }

    private fun stripCatPaw(text: String): String {
        var t = text
        while (t.startsWith(CAT_PAW_TAG)) t = t.removePrefix(CAT_PAW_TAG)
        while (t.endsWith(CAT_PAW_TAG)) t = t.removeSuffix(CAT_PAW_TAG)
        return t
    }

    /** 去除已有的颜文字后缀（对应 n.g） */
    fun stripEmoticon(text: String): String {
        if (!Settings.emoticonEnabled || text.isEmpty()) return text
        for (emo in Settings.emoticonList()) {
            if (text.endsWith(" $emo")) return text.removeSuffix(" $emo")
            if (text.endsWith(emo)) return text.removeSuffix(emo)
        }
        return text
    }

    /** 检测文本是否包含 CJK 字符 */
    fun containsCjk(text: String): Boolean {
        for (c in text) {
            val code = c.code
            if ((code in 0x4E00..0x9FFF) || (code in 0x3400..0x4DBF) ||
                (code in 0x3040..0x30FF) || (code in 0xAC00..0xD7AF)
            ) {
                return true
            }
        }
        return false
    }

    /** 检测是否已追加（文本包含 appendText） */
    fun alreadyAppended(text: String): Boolean {
        val appendText = Settings.punctuationAppendText
        return appendText.isNotEmpty() && text.contains(appendText)
    }
}