package com.localchat.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private sealed class MdSegment {
    data class Text(val text: String) : MdSegment()
    data class Code(val language: String, val code: String) : MdSegment()
}

private val codeFenceRegex = Regex("```([a-zA-Z0-9_+-]*)\\n?([\\s\\S]*?)```")
private val inlineCodeRegex = Regex("`([^`\\n]+)`")

private fun parseMarkdownSegments(input: String): List<MdSegment> {
    val segments = mutableListOf<MdSegment>()
    var lastIndex = 0
    for (match in codeFenceRegex.findAll(input)) {
        if (match.range.first > lastIndex) {
            segments += MdSegment.Text(input.substring(lastIndex, match.range.first))
        }
        segments += MdSegment.Code(match.groupValues[1], match.groupValues[2].trimEnd('\n'))
        lastIndex = match.range.last + 1
    }
    if (lastIndex < input.length) segments += MdSegment.Text(input.substring(lastIndex))
    return segments
}

private fun renderInline(text: String): AnnotatedString = buildAnnotatedString {
    var lastEnd = 0
    for (m in inlineCodeRegex.findAll(text)) {
        append(text.substring(lastEnd, m.range.first))
        withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0xFF262B36))) {
            append(m.groupValues[1])
        }
        lastEnd = m.range.last + 1
    }
    append(text.substring(lastEnd))
}

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val segments = remember(text) { parseMarkdownSegments(text) }
    Column(modifier = modifier) {
        segments.forEach { segment ->
            when (segment) {
                is MdSegment.Code -> CodeBlock(segment.language, segment.code)
                is MdSegment.Text -> if (segment.text.isNotBlank() || segments.size == 1) {
                    Text(
                        text = renderInline(segment.text),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeBlock(language: String, code: String) {
    val clipboard = LocalClipboardManager.current
    Surface(
        color = Color(0xFF12151B),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Box {
            Column(modifier = Modifier.padding(top = 30.dp, start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = Color(0xFFD7DCE5),
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                )
            }
            if (language.isNotBlank()) {
                Text(
                    text = language,
                    fontSize = 11.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 12.dp, top = 6.dp),
                )
            }
            IconButton(
                onClick = { clipboard.setText(AnnotatedString(code)) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp),
            ) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = "Copy code",
                    tint = Color(0xFF9098AA),
                    modifier = Modifier.padding(2.dp),
                )
            }
        }
    }
}
