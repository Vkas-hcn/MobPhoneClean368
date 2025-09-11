package com.carefree.and.joyous.utils

import java.text.DecimalFormat

/**
 * 文件大小格式化工具类
 * 统一使用1000进制标准
 */
object FileSizeUtils {
    
    /**
     * 格式化文件大小，返回带单位的字符串
     *
     * @param bytes 字节数
     * @return 格式化后的文件大小字符串
     */
    fun formatFileSize(bytes: Long): String {
        val result = formatFileSizeWithUnit(bytes)
        return "${result.first} ${result.second}"
    }
    
    /**
     * 格式化文件大小，返回数值和单位的Pair
     *
     * @param bytes 字节数
     * @return Pair(数值, 单位)
     */
    fun formatFileSizeWithUnit(bytes: Long): Pair<String, String> {
        return when {
            bytes >= 1000L * 1000L * 1000L -> {
                val gb = bytes.toDouble() / (1000L * 1000L * 1000L)
                val formatted = if (gb < 10.0) {
                    DecimalFormat("#.#").format(gb)
                } else {
                    DecimalFormat("#").format(gb)
                }
                Pair(formatted, "GB")
            }
            
            bytes >= 1000L * 1000L -> {
                val mb = bytes.toDouble() / (1000L * 1000L)
                val formatted = DecimalFormat("#").format(mb)
                Pair(formatted, "MB")
            }
            
            bytes >= 1000L -> {
                val kb = bytes.toDouble() / 1000L
                val formatted = DecimalFormat("#").format(kb)
                Pair(formatted, "KB")
            }
            
            else -> {
                Pair("$bytes", "B")
            }
        }
    }
}