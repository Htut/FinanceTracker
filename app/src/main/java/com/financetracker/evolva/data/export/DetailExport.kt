package com.financetracker.evolva.data.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.financetracker.evolva.data.model.AppCurrency
import com.financetracker.evolva.data.model.Transaction
import com.financetracker.evolva.data.model.TransactionType
import com.financetracker.evolva.data.model.TransferDirection
import com.financetracker.evolva.data.model.formatAmountNumber
import com.financetracker.evolva.data.model.formatRecordedAt
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

enum class DetailShareFormat(val label: String, val mimeType: String, val extension: String) {
    TEXT("Text", "text/plain", "txt"),
    CSV("CSV", "text/csv", "csv"),
    PDF("PDF", "application/pdf", "pdf")
}

object DetailExport {

    fun fileName(title: String, format: DetailShareFormat): String =
        "${fileBase(title)}.${format.extension}"

    fun buildBytes(
        format: DetailShareFormat,
        title: String,
        subtitle: String,
        transactions: List<Transaction>,
        currency: AppCurrency,
        summaryLines: List<String> = emptyList()
    ): ByteArray = when (format) {
        DetailShareFormat.TEXT -> buildText(title, subtitle, transactions, currency, summaryLines)
            .toByteArray(Charsets.UTF_8)
        DetailShareFormat.CSV -> buildCsv(title, subtitle, transactions, currency, summaryLines)
            .toByteArray(Charsets.UTF_8)
        DetailShareFormat.PDF -> buildPdf(title, subtitle, transactions, currency, summaryLines)
    }

    /** Opens the full Android share sheet for the chosen format. */
    fun share(
        context: Context,
        format: DetailShareFormat,
        title: String,
        subtitle: String,
        transactions: List<Transaction>,
        currency: AppCurrency,
        summaryLines: List<String> = emptyList()
    ) {
        val bytes = buildBytes(format, title, subtitle, transactions, currency, summaryLines)
        val file = writeCacheFile(context, fileName(title, format), bytes)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val subject = "$title · $subtitle"
        val previewText = buildText(title, subtitle, transactions, currency, summaryLines)

        val send = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TITLE, subject)
            putExtra(Intent.EXTRA_STREAM, uri)
            // Helps messaging apps that prefer inline text; file still attached.
            if (format == DetailShareFormat.TEXT || format == DetailShareFormat.CSV) {
                putExtra(Intent.EXTRA_TEXT, previewText)
            } else {
                putExtra(Intent.EXTRA_TEXT, subject)
            }
            clipData = ClipData.newUri(context.contentResolver, subject, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        grantUriToResolvers(context, send, uri)

        val chooser = Intent.createChooser(send, "Share via").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    }

    fun writeToStream(output: OutputStream, bytes: ByteArray) {
        output.use { it.write(bytes) }
    }

    private fun writeCacheFile(context: Context, fileName: String, bytes: ByteArray): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { it.write(bytes) }
        return file
    }

    private fun grantUriToResolvers(context: Context, intent: Intent, uri: Uri) {
        val matches = context.packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        matches.forEach { resolve ->
            context.grantUriPermission(
                resolve.activityInfo.packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    fun buildText(
        title: String,
        subtitle: String,
        transactions: List<Transaction>,
        currency: AppCurrency,
        summaryLines: List<String> = emptyList()
    ): String = buildString {
        appendLine(title)
        appendLine("Filter: $subtitle")
        appendLine("Currency: ${currency.symbol} (${currency.code})")
        appendLine("${transactions.size} transaction(s)")
        if (summaryLines.isNotEmpty()) {
            appendLine()
            summaryLines.forEach { appendLine(it) }
        }
        appendLine()
        transactions.forEach { tx ->
            appendLine(transactionLine(tx, currency))
        }
    }

    fun buildCsv(
        title: String,
        subtitle: String,
        transactions: List<Transaction>,
        currency: AppCurrency,
        summaryLines: List<String> = emptyList()
    ): String {
        val sb = StringBuilder()
        sb.appendLine(csv("Report", title))
        sb.appendLine(csv("Filter", subtitle))
        sb.appendLine(csv("Currency", "${currency.symbol} (${currency.code})"))
        sb.appendLine(csv("Count", transactions.size.toString()))
        summaryLines.forEach { line ->
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) sb.appendLine(csv(parts[0].trim(), parts[1].trim()))
            else sb.appendLine(csv("Summary", line))
        }
        sb.appendLine()
        sb.appendLine(
            listOf("Date", "Type", "Category", "Amount", "Note")
                .joinToString(",") { csvEscape(it) }
        )
        transactions.forEach { tx ->
            val amount = signedAmount(tx)
            sb.appendLine(
                listOf(
                    tx.formatRecordedAt(),
                    tx.type.name,
                    tx.category,
                    formatAmountNumber(amount, currency),
                    tx.note.orEmpty()
                ).joinToString(",") { csvEscape(it) }
            )
        }
        return sb.toString()
    }

    fun buildPdf(
        title: String,
        subtitle: String,
        transactions: List<Transaction>,
        currency: AppCurrency,
        summaryLines: List<String> = emptyList()
    ): ByteArray {
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f
        val lineHeight = 14f
        val doc = PdfDocument()
        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = 0xFF1A1A1A.toInt()
        }
        val headingPaint = Paint().apply {
            isAntiAlias = true
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = 0xFF1A1A1A.toInt()
        }
        val bodyPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            color = 0xFF1A1A1A.toInt()
        }
        val softPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            color = 0xFF6B6B6B.toInt()
        }

        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = margin

        fun newPage() {
            doc.finishPage(page)
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = margin
        }

        fun ensureSpace(needed: Float = lineHeight) {
            if (y + needed > pageHeight - margin) newPage()
        }

        canvas.drawText(title, margin, y + titlePaint.textSize, titlePaint)
        y += titlePaint.textSize + 8f
        canvas.drawText("Filter: $subtitle", margin, y + bodyPaint.textSize, softPaint)
        y += lineHeight
        canvas.drawText(
            "Currency: ${currency.symbol} (${currency.code}) · ${transactions.size} transaction(s)",
            margin,
            y + bodyPaint.textSize,
            softPaint
        )
        y += lineHeight + 6f
        summaryLines.forEach { line ->
            ensureSpace()
            canvas.drawText(line, margin, y + bodyPaint.textSize, bodyPaint)
            y += lineHeight
        }
        y += 10f

        val colDate = margin
        val colCategory = margin + 110f
        val colAmount = pageWidth - margin - 90f

        canvas.drawText("Date", colDate, y + headingPaint.textSize, headingPaint)
        canvas.drawText("Category / Note", colCategory, y + headingPaint.textSize, headingPaint)
        canvas.drawText("Amount", colAmount, y + headingPaint.textSize, headingPaint)
        y += lineHeight + 4f

        transactions.forEach { tx ->
            ensureSpace(lineHeight * 2 + 4f)
            val amountText = signedPrefix(tx) + formatAmountNumber(tx.amount, currency)
            canvas.drawText(tx.formatRecordedAt(), colDate, y + bodyPaint.textSize, bodyPaint)
            canvas.drawText(tx.category, colCategory, y + bodyPaint.textSize, bodyPaint)
            canvas.drawText(amountText, colAmount, y + bodyPaint.textSize, bodyPaint)
            y += lineHeight
            val note = tx.note?.takeIf { it.isNotBlank() }
            if (note != null) {
                ensureSpace()
                canvas.drawText(note, colCategory, y + softPaint.textSize, softPaint)
                y += lineHeight
            }
            y += 4f
        }

        doc.finishPage(page)
        val out = java.io.ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    private fun transactionLine(tx: Transaction, currency: AppCurrency): String {
        val note = tx.note?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
        return "${tx.formatRecordedAt()} · ${tx.type.name} · ${tx.category}: " +
            "${signedPrefix(tx)}${formatAmountNumber(tx.amount, currency)}$note"
    }

    private fun signedAmount(tx: Transaction): Double {
        val negative = tx.type == TransactionType.EXPENSE ||
            (tx.type == TransactionType.TRANSFER && tx.direction == TransferDirection.OUT)
        return if (negative) -kotlin.math.abs(tx.amount) else kotlin.math.abs(tx.amount)
    }

    private fun signedPrefix(tx: Transaction): String {
        return when {
            tx.type == TransactionType.EXPENSE -> "-"
            tx.type == TransactionType.TRANSFER && tx.direction == TransferDirection.OUT -> "-"
            else -> "+"
        }
    }

    private fun fileBase(title: String): String =
        title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "details" }

    private fun csv(key: String, value: String): String =
        listOf(key, value).joinToString(",") { csvEscape(it) }

    private fun csvEscape(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n')
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }
}
