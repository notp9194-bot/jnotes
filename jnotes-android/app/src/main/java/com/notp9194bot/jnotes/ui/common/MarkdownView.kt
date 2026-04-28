package com.notp9194bot.jnotes.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notp9194bot.jnotes.domain.MarkdownParser
import com.notp9194bot.jnotes.domain.MdBlock

@Composable
fun MarkdownView(
    source: String,
    onLinkedTitleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(source) { MarkdownParser.parse(source) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (block in blocks) {
            when (block) {
                is MdBlock.Heading -> {
                    val style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineMedium
                        2 -> MaterialTheme.typography.headlineSmall
                        3 -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.titleMedium
                    }
                    InlineText(text = block.text, onLinkedTitleClick = onLinkedTitleClick, style = style)
                }
                is MdBlock.Paragraph -> {
                    InlineText(
                        text = block.text,
                        onLinkedTitleClick = onLinkedTitleClick,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                is MdBlock.Quote -> {
                    Row {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        )
                        InlineText(
                            text = block.text,
                            onLinkedTitleClick = onLinkedTitleClick,
                            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
                is MdBlock.Code -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                    ) {
                        Text(
                            text = block.text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                        )
                    }
                }
                is MdBlock.BulletList -> {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (item in block.items) {
                            Row { Text("•   "); InlineText(item, onLinkedTitleClick) }
                        }
                    }
                }
                is MdBlock.OrderedList -> {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        block.items.forEachIndexed { i, item ->
                            Row { Text("${i + 1}.  "); InlineText(item, onLinkedTitleClick) }
                        }
                    }
                }
                is MdBlock.TaskList -> {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        for ((checked, item) in block.items) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Box(modifier = Modifier.width(8.dp))
                                InlineText(item, onLinkedTitleClick)
                            }
                        }
                    }
                }
                MdBlock.Divider -> HorizontalDivider()
            }
        }
    }
}

@Composable
private fun InlineText(
    text: AnnotatedString,
    onLinkedTitleClick: (String) -> Unit,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    ClickableText(
        text = text,
        style = style.copy(color = MaterialTheme.colorScheme.onSurface),
        modifier = modifier,
        onClick = { offset ->
            text.getStringAnnotations("note-link", offset, offset).firstOrNull()?.let {
                onLinkedTitleClick(it.item); return@ClickableText
            }
            text.getStringAnnotations("url", offset, offset).firstOrNull()?.let {
                runCatching { uriHandler.openUri(it.item) }
            }
        },
    )
}
