package com.notp9194bot.jnotes.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.notp9194bot.jnotes.data.model.Note
import com.notp9194bot.jnotes.data.model.NoteType

/**
 * Renders a note to a single (paginated) A4 PDF file at the given content-resolver URI.
 *
 * Every exported page automatically gets a low-visibility "jnotes" watermark:
 *   1. A large diagonal watermark across the page center.
 *   2. A small footer line at the bottom of every page.
 */
object PdfExport {
    private const val PAGE_WIDTH = 595   // A4 in PostScript points (1pt = 1/72")
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36
    private const val WATERMARK_TEXT = "jnotes"
    private const val WATERMARK_FOOTER = "Exported from jnotes"

    fun export(context: Context, note: Note, target: Uri): Boolean {
        val doc = PdfDocument()
        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint().apply {
            isAntiAlias = true
            textSize = 12f
            typeface = Typeface.DEFAULT
        }
        val metaPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.DEFAULT
            color = 0xFF666666.toInt()
        }

        val text = when (note.type) {
            NoteType.CHECKLIST -> note.items.joinToString("\n") { (if (it.checked) "[x] " else "[ ] ") + it.text }
            NoteType.TEXT -> note.body
        }

        val maxWidth = PAGE_WIDTH - 2 * MARGIN
        val lines = wrap(text, bodyPaint, maxWidth.toFloat())

        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        drawWatermark(canvas)
        var y = MARGIN.toFloat() + 18f
        canvas.drawText(note.title.ifBlank { "Untitled" }, MARGIN.toFloat(), y, titlePaint)
        y += 14f
        canvas.drawText(DateUtils.formatFull(note.updatedAt), MARGIN.toFloat(), y, metaPaint)
        y += 18f

        val lineHeight = bodyPaint.fontSpacing
        for (l in lines) {
            if (y + lineHeight > PAGE_HEIGHT - MARGIN - 12) {
                doc.finishPage(page)
                pageNumber++
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                drawWatermark(canvas)
                y = MARGIN.toFloat() + 12f
            }
            canvas.drawText(l, MARGIN.toFloat(), y, bodyPaint)
            y += lineHeight
        }
        doc.finishPage(page)

        return try {
            context.contentResolver.openOutputStream(target)?.use { doc.writeTo(it) }
            true
        } catch (_: Throwable) {
            false
        } finally {
            doc.close()
        }
    }

    /** Draws the low-visibility "jnotes" diagonal watermark and a tiny footer tag on the page. */
    private fun drawWatermark(canvas: android.graphics.Canvas) {
        // Big diagonal watermark across the page center.
        val diagPaint = Paint().apply {
            isAntiAlias = true
            textSize = 96f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            color = Color.argb(28, 120, 120, 120) // very low opacity grey
            textAlign = Paint.Align.CENTER
        }
        canvas.save()
        canvas.rotate(-30f, PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f)
        canvas.drawText(WATERMARK_TEXT, PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f, diagPaint)
        canvas.restore()

        // Subtle footer on every page.
        val footerPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8f
            typeface = Typeface.DEFAULT
            color = Color.argb(110, 120, 120, 120)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            WATERMARK_FOOTER,
            PAGE_WIDTH / 2f,
            (PAGE_HEIGHT - 14).toFloat(),
            footerPaint,
        )
    }

    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        val out = mutableListOf<String>()
        for (raw in text.split('\n')) {
            if (raw.isEmpty()) { out += ""; continue }
            val words = raw.split(' ')
            val cur = StringBuilder()
            for (w in words) {
                val candidate = if (cur.isEmpty()) w else "$cur $w"
                if (paint.measureText(candidate) <= maxWidth) {
                    cur.clear(); cur.append(candidate)
                } else {
                    if (cur.isNotEmpty()) out += cur.toString()
                    cur.clear(); cur.append(w)
                }
            }
            if (cur.isNotEmpty()) out += cur.toString()
        }
        return out
    }
}
