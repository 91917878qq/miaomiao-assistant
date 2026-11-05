package com.miaomiao.assistant.util

/**
 * 替换规则数据类
 * 对应原版 Rule（from -> to 文本替换）
 */
data class Rule(
    val from: String,
    val to: String
) {
    override fun toString(): String = "Rule(from=$from, to=$to)"
}