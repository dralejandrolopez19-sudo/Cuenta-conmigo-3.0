package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.graphics.withTranslation
import com.example.data.model.JournalEntry
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportHelper {

    private const val PAGE_WIDTH = 595 // A4 standard width in points
    private const val PAGE_HEIGHT = 842 // A4 standard height in points
    private const val MARGIN_LEFT = 42f
    private const val MARGIN_RIGHT = 553f
    private const val CONTENT_WIDTH = (MARGIN_RIGHT - MARGIN_LEFT).toInt() // 511 points
    private const val FOOTER_Y = 812f
    private const val MAX_CONTENT_Y = 765f

    /**
     * Generates a PDF containing the cognitive journal entries formatted cleanly for
     * sharing with health professionals, psychologists, or therapists.
     */
    fun generateJournalPdf(
        context: Context,
        patientName: String,
        entries: List<JournalEntry>,
        filterLabel: String = "Todos",
        customNote: String = ""
    ): File? {
        val document = PdfDocument()

        try {
            val pdfDir = File(context.cacheDir, "pdfs")
            if (!pdfDir.exists()) {
                pdfDir.mkdirs()
            }
            val sanitizedName = patientName.replace(Regex("[^a-zA-Z0-9_]"), "_").take(20)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val pdfFile = File(pdfDir, "diario_cognitivo_${sanitizedName}_$timestamp.pdf")

            val titlePaint = TextPaint().apply {
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 15f
                color = AndroidColor.rgb(26, 54, 93) // Navy
            }

            val subtitlePaint = TextPaint().apply {
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 10f
                color = AndroidColor.rgb(74, 85, 104) // Slate
            }

            val metaBoldPaint = TextPaint().apply {
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 9.5f
                color = AndroidColor.rgb(45, 55, 72)
            }

            val metaTextPaint = TextPaint().apply {
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 9.5f
                color = AndroidColor.rgb(74, 85, 104)
            }

            val entryTitlePaint = TextPaint().apply {
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 11.5f
                color = AndroidColor.rgb(30, 41, 59)
            }

            val bodyPaint = TextPaint().apply {
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 10f
                color = AndroidColor.rgb(51, 65, 85)
            }

            val tagPaint = TextPaint().apply {
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 8.5f
            }

            val footerPaint = TextPaint().apply {
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 8.5f
                color = AndroidColor.rgb(148, 163, 184)
            }

            val linePaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
                color = AndroidColor.rgb(226, 232, 240)
            }

            val bgPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }

            var currentPageNum = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNum).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            var currentY = 40f

            fun drawFooter(c: Canvas, pageNumber: Int) {
                val footerText = "Cuenta Conmigo • Registro Confidencial para Seguimiento de Salud Mental — Página $pageNumber"
                val width = footerPaint.measureText(footerText)
                c.drawText(footerText, (PAGE_WIDTH - width) / 2f, FOOTER_Y, footerPaint)
            }

            fun advanceToNextPage() {
                drawFooter(canvas, currentPageNum)
                document.finishPage(page)

                currentPageNum++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNum).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas

                // Minimal Header on continuation pages
                val headerText = "Cuenta Conmigo — Diario Cognitivo: $patientName (Continuación)"
                canvas.drawText(headerText, MARGIN_LEFT, 38f, subtitlePaint)
                canvas.drawLine(MARGIN_LEFT, 44f, MARGIN_RIGHT, 44f, linePaint)
                currentY = 60f
            }

            // --- PAGE 1: HEADER SECTION ---
            // Header Top Bar banner
            bgPaint.color = AndroidColor.rgb(13, 148, 136) // Teal
            canvas.drawRoundRect(RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + 6f), 3f, 3f, bgPaint)
            currentY += 18f

            canvas.drawText("CUENTA CONMIGO — CUADERNO DE BIENESTAR", MARGIN_LEFT, currentY, titlePaint)
            currentY += 16f
            canvas.drawText("Informe Clínico de Reestructuración y Registros del Diario Cognitivo", MARGIN_LEFT, currentY, subtitlePaint)
            currentY += 18f

            // Patient & Metadata Box
            val boxTop = currentY
            val boxHeight = if (customNote.isNotBlank()) 80f else 62f
            bgPaint.color = AndroidColor.rgb(248, 250, 252)
            canvas.drawRoundRect(RectF(MARGIN_LEFT, boxTop, MARGIN_RIGHT, boxTop + boxHeight), 6f, 6f, bgPaint)
            canvas.drawRoundRect(RectF(MARGIN_LEFT, boxTop, MARGIN_RIGHT, boxTop + boxHeight), 6f, 6f, linePaint)

            val metaY1 = boxTop + 18f
            canvas.drawText("Paciente: ", MARGIN_LEFT + 14f, metaY1, metaBoldPaint)
            canvas.drawText(patientName, MARGIN_LEFT + 62f, metaY1, metaTextPaint)

            val nowFormatted = SimpleDateFormat("dd 'de' MMMM, yyyy - HH:mm", Locale.forLanguageTag("es-ES")).format(Date())
            canvas.drawText("Fecha de emisión: ", MARGIN_LEFT + 270f, metaY1, metaBoldPaint)
            canvas.drawText(nowFormatted, MARGIN_LEFT + 360f, metaY1, metaTextPaint)

            val metaY2 = boxTop + 34f
            canvas.drawText("Categoría: ", MARGIN_LEFT + 14f, metaY2, metaBoldPaint)
            canvas.drawText(filterLabel, MARGIN_LEFT + 68f, metaY2, metaTextPaint)

            canvas.drawText("Total de registros: ", MARGIN_LEFT + 270f, metaY2, metaBoldPaint)
            canvas.drawText("${entries.size} entrada(s)", MARGIN_LEFT + 360f, metaY2, metaTextPaint)

            val metaY3 = boxTop + 50f
            canvas.drawText("Finalidad: Documento para acompañamiento psicoterapéutico y seguimiento médico.", MARGIN_LEFT + 14f, metaY3, subtitlePaint)

            if (customNote.isNotBlank()) {
                val noteY = boxTop + 66f
                canvas.drawText("Nota adjunta del paciente: ", MARGIN_LEFT + 14f, noteY, metaBoldPaint)
                val noteSnippet = if (customNote.length > 70) customNote.take(67) + "..." else customNote
                canvas.drawText("\"$noteSnippet\"", MARGIN_LEFT + 148f, noteY, metaTextPaint)
            }

            currentY = boxTop + boxHeight + 18f

            // Separator line before list
            canvas.drawLine(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY, linePaint)
            currentY += 16f

            // Empty state check
            if (entries.isEmpty()) {
                val emptyLayout = buildStaticLayout(
                    "No se registran notas de diario cognitivo en este periodo o filtro.",
                    bodyPaint,
                    CONTENT_WIDTH
                )
                canvas.withTranslation(MARGIN_LEFT, currentY) {
                    emptyLayout.draw(this)
                }
                currentY += emptyLayout.height + 20f
            } else {
                val dateFormat = SimpleDateFormat("dd 'de' MMMM yyyy, HH:mm", Locale.forLanguageTag("es-ES"))

                // Render each entry
                for ((index, entry) in entries.withIndex()) {
                    val entryDate = entry.createdAt?.toDate()?.let { dateFormat.format(it) } ?: "Fecha no disponible"

                    val titleText = if (entry.title.isNotBlank()) entry.title else "Sin título"
                    val contentText = if (entry.content.isNotBlank()) entry.content else "(Entrada sin descripción)"

                    val titleLayout = buildStaticLayout(titleText, entryTitlePaint, CONTENT_WIDTH - 20)
                    val bodyLayout = buildStaticLayout(contentText, bodyPaint, CONTENT_WIDTH - 20)

                    // Estimate entry block height
                    // Top tag row (16f) + spacing (6f) + title (titleLayout.height) + spacing (6f) + body (bodyLayout.height) + bottom spacing (24f)
                    val entryTotalHeight = 16f + 6f + titleLayout.height + 6f + bodyLayout.height + 22f

                    if (currentY + entryTotalHeight > MAX_CONTENT_Y) {
                        advanceToNextPage()
                    }

                    // Render Entry Box
                    val cardTop = currentY
                    val cardBottom = currentY + entryTotalHeight - 8f

                    // Subtle background for entry card
                    bgPaint.color = AndroidColor.rgb(255, 255, 255)
                    canvas.drawRoundRect(RectF(MARGIN_LEFT, cardTop, MARGIN_RIGHT, cardBottom), 6f, 6f, bgPaint)
                    linePaint.color = AndroidColor.rgb(226, 232, 240)
                    canvas.drawRoundRect(RectF(MARGIN_LEFT, cardTop, MARGIN_RIGHT, cardBottom), 6f, 6f, linePaint)

                    // Category Pill
                    val categoryColor = when (entry.category.lowercase()) {
                        "reestructuración", "reestructuracion" -> Pair(AndroidColor.rgb(254, 226, 226), AndroidColor.rgb(197, 48, 48))
                        "gratitud" -> Pair(AndroidColor.rgb(204, 251, 241), AndroidColor.rgb(13, 148, 136))
                        "reflexión", "reflexion" -> Pair(AndroidColor.rgb(224, 231, 255), AndroidColor.rgb(67, 56, 202))
                        else -> Pair(AndroidColor.rgb(241, 245, 249), AndroidColor.rgb(71, 85, 105))
                    }

                    val catText = "  ${entry.category.uppercase()}  "
                    tagPaint.color = categoryColor.second
                    val tagWidth = tagPaint.measureText(catText) + 8f

                    val pillTop = cardTop + 8f
                    val pillBottom = pillTop + 14f
                    bgPaint.color = categoryColor.first
                    canvas.drawRoundRect(RectF(MARGIN_LEFT + 10f, pillTop, MARGIN_LEFT + 10f + tagWidth, pillBottom), 4f, 4f, bgPaint)
                    canvas.drawText(catText, MARGIN_LEFT + 12f, pillTop + 10.5f, tagPaint)

                    // Date
                    canvas.drawText(entryDate, MARGIN_LEFT + 22f + tagWidth, pillTop + 10.5f, subtitlePaint)

                    // Entry number badge on right
                    val numText = "#${index + 1}"
                    val numWidth = subtitlePaint.measureText(numText)
                    canvas.drawText(numText, MARGIN_RIGHT - numWidth - 10f, pillTop + 10.5f, subtitlePaint)

                    var innerY = pillBottom + 8f

                    // Draw Title
                    canvas.withTranslation(MARGIN_LEFT + 10f, innerY) {
                        titleLayout.draw(this)
                    }
                    innerY += titleLayout.height + 6f

                    // Draw Body
                    canvas.withTranslation(MARGIN_LEFT + 10f, innerY) {
                        bodyLayout.draw(this)
                    }

                    currentY = cardBottom + 12f
                }
            }

            // Finish the last page
            drawFooter(canvas, currentPageNum)
            document.finishPage(page)

            FileOutputStream(pdfFile).use { out ->
                document.writeTo(out)
            }

            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            document.close()
        }
    }

    private fun buildStaticLayout(text: CharSequence, paint: TextPaint, width: Int): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.2f)
            .setIncludePad(false)
            .build()
    }

    /**
     * Shares the generated PDF file via Android's native share sheet (e.g. Email, WhatsApp, Drive).
     */
    fun sharePdf(context: Context, pdfFile: File, patientName: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Diario Cognitivo - $patientName")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Adjunto mi reporte de entradas del Diario Cognitivo de Cuenta Conmigo para acompañamiento en mi tratamiento y seguimiento de salud mental."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Compartir reporte con profesional de la salud")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al compartir archivo: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens the generated PDF in an installed PDF viewer app.
     */
    fun openPdf(context: Context, pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "No se encontró una aplicación para abrir PDFs.", Toast.LENGTH_SHORT).show()
        }
    }
}
