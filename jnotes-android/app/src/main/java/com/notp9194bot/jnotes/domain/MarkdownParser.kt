package com.notp9194bot.jnotes.domain

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

sealed interface MdBlock {
    data class Heading(val level: Int, val text: AnnotatedString) : MdBlock
    data class Paragraph(val text: AnnotatedString) : MdBlock
    data class Quote(val text: AnnotatedString) : MdBlock
    data class Code(val text: String) : MdBlock
    data class BulletList(val items: List<AnnotatedString>) : MdBlock
    data class OrderedList(val items: List<AnnotatedString>) : MdBlock
    data class TaskList(val items: List<Pair<Boolean, AnnotatedString>>) : MdBlock
    data object Divider : MdBlock
}

object MarkdownParser {

    fun parse(source: String): List<MdBlock> {
        val out = ArrayList<MdBlock>()
        val lines = source.lines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trimEnd()
            when {
                trimmed.isBlank() -> i++

                trimmed == "---" || trimmed == "***" -> {
                    out += MdBlock.Divider; i++
                }

                trimmed.startsWith("```") -> {
                    val sb = StringBuilder()
                    i++
                    while (i < lines.size && !lines[i].trimEnd().startsWith("```")) {
                        if (sb.isNotEmpty()) sb.append('\n')
                        sb.append(lines[i])
                        i++
                    }
                    if (i < lines.size) i++
                    out += MdBlock.Code(sb.toString())
                }

                trimmed.startsWith("> ") || trimmed == ">" -> {
                    val sb = StringBuilder()
                    while (i < lines.size && (lines[i].trimEnd().startsWith("> ") || lines[i].trimEnd() == ">")) {
                        if (sb.isNotEmpty()) sb.append('\n')
                        sb.append(lines[i].trimEnd().removePrefix(">").trimStart())
                        i++
                    }
                    out += MdBlock.Quote(inline(sb.toString()))
                }

                Regex("^#{1,6}\\s+").containsMatchIn(trimmed) -> {
                    val level = trimmed.takeWhile { it == '#' }.length.coerceAtMost(6)
                    val text = trimmed.removePrefix("#".repeat(level)).trim()
                    out += MdBlock.Heading(level, inline(text))
                    i++
                }

                Regex("^[-*+]\\s+\\[[ xX]]\\s+").containsMatchIn(trimmed) -> {
                    val items = ArrayList<Pair<Boolean, AnnotatedString>>()
                    while (i < lines.size && Regex("^[-*+]\\s+\\[[ xX]]\\s+").containsMatchIn(lines[i].trimEnd())) {
                        val l = lines[i].trimEnd()
                        val checked = l.contains("[x]") || l.contains("[X]")
                        val text = l.replaceFirst(Regex("^[-*+]\\s+\\[[ xX]]\\s+"), "")
                        items += checked to inline(text)
                        i++
                    }
                    out += MdBlock.TaskList(items)
                }

                Regex("^[-*+]\\s+").containsMatchIn(trimmed) -> {
                    val items = ArrayList<AnnotatedString>()
                    while (i < lines.size && Regex("^[-*+]\\s+").containsMatchIn(lines[i].trimEnd()) &&
                        !Regex("^[-*+]\\s+\\[[ xX]]\\s+").containsMatchIn(lines[i].trimEnd())
                    ) {
                        items += inline(lines[i].trimEnd().replaceFirst(Regex("^[-*+]\\s+"), ""))
                        i++
                    }
                    out += MdBlock.BulletList(items)
                }

                Regex("^\\d+\\.\\s+").containsMatchIn(trimmed) -> {
                    val items = ArrayList<AnnotatedString>()
                    while (i < lines.size && Regex("^\\d+\\.\\s+").containsMatchIn(lines[i].trimEnd())) {
                        items += inline(lines[i].trimEnd().replaceFirst(Regex("^\\d+\\.\\s+"), ""))
                        i++
                    }
                    out += MdBlock.OrderedList(items)
                }

                else -> {
                    val sb = StringBuilder(trimmed)
                    i++
                    while (i < lines.size && lines[i].trimEnd().isNotBlank() &&
                        !isBlockStart(lines[i].trimEnd())
                    ) {
                        sb.append(' ').append(lines[i].trimEnd())
                        i++
                    }
                    out += MdBlock.Paragraph(inline(sb.toString()))
                }
            }
        }
        return out
    }

    private fun isBlockStart(s: String): Boolean =
        s.startsWith("#") || s.startsWith("> ") || s.startsWith("```") ||
            Regex("^[-*+]\\s+").containsMatchIn(s) ||
            Regex("^\\d+\\.\\s+").containsMatchIn(s) ||
            s == "---" || s == "***"

    /** Inline markdown: **bold**, *italic*, `code`, [text](url), [[link]] */
    fun inline(input: String): AnnotatedString {
        val builder = AnnotatedString.Builder()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            when {
                c == '*' && i + 1 < input.length && input[i + 1] == '*' -> {
                    val end = input.indexOf("**", i + 2)
                    if (end > 0) {
                        builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        builder.append(input.substring(i + 2, end))
                        builder.pop()
                        i = end + 2
                    } else { builder.append(c); i++ }
                }
                c == '*' || c == '_' -> {
                    val end = input.indexOf(c, i + 1)
                    if (end > 0) {
                        builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        builder.append(input.substring(i + 1, end))
                        builder.pop()
                        i = end + 1
                    } else { builder.append(c); i++ }
                }
                c == '`' -> {
                    val end = input.indexOf('`', i + 1)
                    if (end > 0) {
                        builder.pushStyle(SpanStyle(fontFamily = FontFamily.Monospace))
                        builder.append(input.substring(i + 1, end))
                        builder.pop()
                        i = end + 1
                    } else { builder.append(c); i++ }
                }
                c == '[' && i + 1 < input.length && input[i + 1] == '[' -> {
                    val end = input.indexOf("]]", i + 2)
                    if (end > 0) {
                        val name = input.substring(i + 2, end)
                        builder.pushStringAnnotation("note-link", name)
                        builder.pushStyle(SpanStyle(fontWeight = FontWeight.Medium))
                        builder.append(name)
                        builder.pop()
                        builder.pop()
                        i = end + 2
                    } else { builder.append(c); i++ }
                }
                c == '[' -> {
                    val close = input.indexOf(']', i + 1)
                    if (close > 0 && close + 1 < input.length && input[close + 1] == '(') {
                        val urlEnd = input.indexOf(')', close + 2)
                        if (urlEnd > 0) {
                            val text = input.substring(i + 1, close)
                            val url = input.substring(close + 2, urlEnd)
                            builder.pushStringAnnotation("url", url)
                            builder.pushStyle(SpanStyle(fontWeight = FontWeight.Medium))
                            builder.append(text)
                            builder.pop()
                            builder.pop()
                            i = urlEnd + 1
                            continue
                        }
                    }
                    builder.append(c); i++
                }
                else -> { builder.append(c); i++ }
            }
        }
        return builder.toAnnotatedString()
    }
}
