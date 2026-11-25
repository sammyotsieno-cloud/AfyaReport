package ke.co.afyareport.ui.report

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import ke.co.afyareport.domain.Report740
import ke.co.afyareport.domain.RowKind

/** Writes the 740 as an A4 PDF through a file the user picks. */
object ReportPdf {
    private const val W = 595
    private const val H = 842
    private const val MARGIN = 36f

    fun write(context: Context, uri: Uri, title: String, period: String, report: Report740): Boolean {
        val doc = PdfDocument()
        return try {
            val text = Paint().apply { isAntiAlias = true; textSize = 9.5f; color = Color.BLACK }
            val bold = Paint(text).apply { isFakeBoldText = true }
            val head = Paint(bold).apply { textSize = 12f }
            val right = Paint(text).apply { textAlign = Paint.Align.RIGHT }
            val rightBold = Paint(bold).apply { textAlign = Paint.Align.RIGHT }
            val shade = Paint().apply { color = Color.rgb(228, 225, 244) }
            val xMale = W - MARGIN - 100f
            val xFemale = W - MARGIN - 50f
            val xTotal = W - MARGIN

            var pageNo = 0
            var page: PdfDocument.Page? = null
            var y = 0f

            fun startPage() {
                page?.let { doc.finishPage(it) }
                pageNo += 1
                val p = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
                page = p
                val c = p.canvas
                y = MARGIN + 8f
                c.drawText(title, MARGIN, y, head)
                y += 14f
                c.drawText(period, MARGIN, y, text)
                y += 18f
                c.drawText("Male", xMale, y, rightBold)
                c.drawText("Female", xFemale, y, rightBold)
                c.drawText("Total", xTotal, y, rightBold)
                y += 6f
                c.drawLine(MARGIN, y, W - MARGIN, y, text)
                y += 14f
            }

            startPage()
            report.rows.forEach { r ->
                if (y > H - MARGIN - 14f) startPage()
                val c = page!!.canvas
                when (r.kind) {
                    RowKind.SECTION -> {
                        c.drawRect(MARGIN, y - 10f, W - MARGIN, y + 4f, shade)
                        c.drawText(r.label, MARGIN + 4f, y, bold)
                    }
                    RowKind.SUBHEAD -> c.drawText(r.label, MARGIN + 4f, y, bold)
                    else -> {
                        val indent = if (r.kind == RowKind.BAND) 18f else 4f
                        val maxW = xMale - 40f - MARGIN - indent
                        var label = r.label
                        val n = text.breakText(label, true, maxW, null)
                        if (n < label.length) label = label.substring(0, maxOf(0, n - 1)) + "..."
                        c.drawText(label, MARGIN + indent, y, text)
                        c.drawText(r.male, xMale, y, right)
                        c.drawText(r.female, xFemale, y, right)
                        c.drawText(r.total, xTotal, y, right)
                    }
                }
                y += 15f
            }
            page?.let { doc.finishPage(it) }
            context.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) } != null
        } catch (e: Exception) {
            false
        } finally {
            doc.close()
        }
    }
}
