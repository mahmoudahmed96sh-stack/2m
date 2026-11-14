package com.example.receipt

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.model.RepairTicket
import com.example.data.model.SaleTransaction
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface ReceiptPayload {
  data class SaleReceipt(val sale: SaleTransaction) : ReceiptPayload

  data class RepairReceipt(val ticket: RepairTicket) : ReceiptPayload
}

object ReceiptGenerator {
  private val egpFormatter: NumberFormat
    get() = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }

  fun formatEgp(amount: Double): String = "${egpFormatter.format(amount)} ج.م"

  fun renderReceiptBitmap(context: Context, payload: ReceiptPayload): Bitmap {
    val width = 1080
    val padding = 64f
    val innerWidth = (width - padding * 2).toInt()
    val cairoTypeface =
      try {
        ResourcesCompat.getFont(context, R.font.cairo) ?: Typeface.DEFAULT
      } catch (_: Exception) {
        Typeface.DEFAULT
      }

    val goldColor = Color.parseColor("#D4AF37")
    val brightGold = Color.parseColor("#FFD700")
    val matteBlack = Color.parseColor("#0F0F0F")
    val cardBg = Color.parseColor("#181818")
    val ivory = Color.parseColor("#F5F2EB")
    val muted = Color.parseColor("#A8A298")

    val rows = buildLineRows(payload)
    val estimatedHeight = 980 + (rows.size * 92)
    val bitmap = Bitmap.createBitmap(width, estimatedHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Matte black luxury background
    canvas.drawColor(matteBlack)

    // Outer metallic gold border
    val borderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = goldColor
        style = Paint.Style.STROKE
        strokeWidth = 4f
      }
    canvas.drawRoundRect(RectF(24f, 24f, width - 24f, estimatedHeight - 24f), 28f, 28f, borderPaint)

    // Logo centered at top
    val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.img_store_logo)
    var y = 64f
    if (logoBitmap != null) {
      val logoSize = 180
      val left = (width - logoSize) / 2f
      canvas.drawBitmap(
        logoBitmap,
        null,
        RectF(left, y, left + logoSize, y + logoSize),
        Paint(Paint.ANTI_ALIAS_FLAG),
      )
      y += logoSize + 20f
    }

    val titlePaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = brightGold
        textSize = 46f
        typeface = Typeface.create(cairoTypeface, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
      }
    canvas.drawText("2m Store", width / 2f, y + 40f, titlePaint)
    y += 58f

    val subtitleText =
      when (payload) {
        is ReceiptPayload.SaleReceipt -> "فاتورة مبيعات إلكترونية"
        is ReceiptPayload.RepairReceipt -> "إيصال استلام ومتابعة صيانة"
      }
    val subPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ivory
        textSize = 32f
        typeface = cairoTypeface
        textAlign = Paint.Align.CENTER
      }
    canvas.drawText(subtitleText, width / 2f, y + 32f, subPaint)
    y += 64f

    // Gold divider
    val linePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = goldColor
        strokeWidth = 2f
      }
    canvas.drawLine(padding, y, width - padding, y, linePaint)
    y += 32f

    // Metadata block
    val dateStr =
      SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.US).format(
        when (payload) {
          is ReceiptPayload.SaleReceipt -> payload.sale.createdAt?.toDate() ?: Date()
          is ReceiptPayload.RepairReceipt -> payload.ticket.createdAt?.toDate() ?: Date()
        }
      )

    val metaLines =
      when (payload) {
        is ReceiptPayload.SaleReceipt ->
          listOf(
            "رقم الفاتورة: ${payload.sale.invoiceNumber}",
            "التاريخ: $dateStr",
            "الموظف المسؤول: ${payload.sale.staffName}",
            if (payload.sale.customerName.isNotBlank()) "العميل: ${payload.sale.customerName}" else null,
            if (payload.sale.customerPhone.isNotBlank()) "الهاتف: ${payload.sale.customerPhone}" else null,
            "طريقة الدفع: ${payload.sale.paymentMethodEnum.arabicLabel}",
          ).filterNotNull()
        is ReceiptPayload.RepairReceipt ->
          listOf(
            "رقم التذكرة: ${payload.ticket.ticketNumber}",
            "التاريخ: $dateStr",
            "العميل: ${payload.ticket.customerName}",
            "الهاتف: ${payload.ticket.customerPhone}",
            "الجهاز: ${payload.ticket.deviceModel}",
            "الفني المسؤول: ${payload.ticket.assignedTechName}",
            "الحالة الحالية: ${payload.ticket.statusEnum.arabicLabel}",
          )
      }

    val metaTextPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ivory
        textSize = 28f
        typeface = cairoTypeface
      }
    metaLines.forEach { line ->
      y = drawRtlTextBlock(canvas, line, metaTextPaint, padding, y, innerWidth)
      y += 10f
    }

    y += 20f
    // Card container for items/details
    val cardTop = y
    val cardHeight = (rows.size * 76f) + 56f
    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBg }
    canvas.drawRoundRect(
      RectF(padding, cardTop, width - padding, cardTop + cardHeight),
      20f,
      20f,
      cardPaint,
    )
    canvas.drawRoundRect(
      RectF(padding, cardTop, width - padding, cardTop + cardHeight),
      20f,
      20f,
      borderPaint.apply { strokeWidth = 2f },
    )

    y = cardTop + 32f
    val rowTitlePaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ivory
        textSize = 28f
        typeface = Typeface.create(cairoTypeface, Typeface.BOLD)
      }
    val rowPricePaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = brightGold
        textSize = 28f
        typeface = Typeface.create(cairoTypeface, Typeface.BOLD)
      }

    rows.forEach { (label, value) ->
      canvas.drawText(value, padding + 24f, y + 30f, rowPricePaint)
      drawRtlTextBlock(canvas, label, rowTitlePaint, padding + 24f, y, innerWidth - 260)
      y += 72f
    }

    y = cardTop + cardHeight + 36f
    canvas.drawLine(padding, y, width - padding, y, linePaint)
    y += 34f

    // Totals block
    val totalPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = brightGold
        textSize = 38f
        typeface = Typeface.create(cairoTypeface, Typeface.BOLD)
      }
    val secondaryTotalPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = muted
        textSize = 28f
        typeface = cairoTypeface
      }

    when (payload) {
      is ReceiptPayload.SaleReceipt -> {
        val sale = payload.sale
        y = drawRtlTextBlock(canvas, "الإجمالي: ${formatEgp(sale.totalAmountEgp)}", totalPaint, padding, y, innerWidth)
        y += 14f
        y = drawRtlTextBlock(canvas, "المدفوع: ${formatEgp(sale.paidAmountEgp)}", secondaryTotalPaint, padding, y, innerWidth)
        if (sale.remainingDebtEgp > 0.01) {
          y += 10f
          y = drawRtlTextBlock(canvas, "المتبقي (آجل): ${formatEgp(sale.remainingDebtEgp)}", secondaryTotalPaint, padding, y, innerWidth)
        }
      }
      is ReceiptPayload.RepairReceipt -> {
        val ticket = payload.ticket
        y = drawRtlTextBlock(canvas, "التكلفة المقدرة: ${formatEgp(ticket.estimatedCostEgp)}", totalPaint, padding, y, innerWidth)
        y += 14f
        y = drawRtlTextBlock(canvas, "المدفوع مقدماً: ${formatEgp(ticket.paidAmountEgp)}", secondaryTotalPaint, padding, y, innerWidth)
        if (ticket.remainingAmountEgp > 0.01) {
          y += 10f
          y = drawRtlTextBlock(canvas, "المتبقي عند الاستلام: ${formatEgp(ticket.remainingAmountEgp)}", secondaryTotalPaint, padding, y, innerWidth)
        }
      }
    }

    y += 36f
    val footerPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = muted
        textSize = 24f
        typeface = cairoTypeface
        textAlign = Paint.Align.CENTER
      }
    canvas.drawText("شكراً لتعاملكم مع 2m Store", width / 2f, y + 24f, footerPaint)

    return bitmap
  }

  private fun buildLineRows(payload: ReceiptPayload): List<Pair<String, String>> =
    when (payload) {
      is ReceiptPayload.SaleReceipt ->
        payload.sale.items.map { item ->
          "${item.name} ×${item.qty}" to formatEgp(item.lineTotalEgp)
        }
      is ReceiptPayload.RepairReceipt ->
        listOf(
          "العطل: ${payload.ticket.issueDescription.take(45)}" to formatEgp(payload.ticket.estimatedCostEgp)
        )
    }

  private fun drawRtlTextBlock(
    canvas: Canvas,
    text: String,
    paint: TextPaint,
    x: Float,
    y: Float,
    width: Int,
  ): Float {
    val layout =
      StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(100))
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(0f, 1.1f)
        .build()
    canvas.save()
    canvas.translate(x, y)
    layout.draw(canvas)
    canvas.restore()
    return y + layout.height
  }

  fun saveReceiptPngToCache(context: Context, bitmap: Bitmap, filePrefix: String): Uri {
    val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
    val outFile = File(dir, "${filePrefix}_${System.currentTimeMillis()}.png")
    FileOutputStream(outFile).use { fos ->
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
    }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
  }

  fun buildWhatsAppText(payload: ReceiptPayload): String =
    when (payload) {
      is ReceiptPayload.SaleReceipt -> {
        val s = payload.sale
        buildString {
          appendLine("*2m Store - فاتورة مبيعات*")
          appendLine("رقم الفاتورة: ${s.invoiceNumber}")
          appendLine("الأصناف: ${s.itemsSummary}")
          appendLine("الإجمالي: ${formatEgp(s.totalAmountEgp)}")
          appendLine("المدفوع: ${formatEgp(s.paidAmountEgp)}")
          if (s.remainingDebtEgp > 0.01) {
            appendLine("المتبقي: ${formatEgp(s.remainingDebtEgp)}")
          }
          append("شكراً لتعاملكم معنا.")
        }
      }
      is ReceiptPayload.RepairReceipt -> {
        val t = payload.ticket
        buildString {
          appendLine("*2m Store - متابعة صيانة*")
          appendLine("رقم التذكرة: ${t.ticketNumber}")
          appendLine("الجهاز: ${t.deviceModel}")
          appendLine("الحالة: ${t.statusEnum.arabicLabel}")
          appendLine("التكلفة: ${formatEgp(t.estimatedCostEgp)}")
          if (t.remainingAmountEgp > 0.01) {
            appendLine("المتبقي: ${formatEgp(t.remainingAmountEgp)}")
          }
        }
      }
    }

  fun shareReceiptViaWhatsApp(context: Context, payload: ReceiptPayload) {
    val bitmap = renderReceiptBitmap(context, payload)
    val prefix =
      when (payload) {
        is ReceiptPayload.SaleReceipt -> payload.sale.invoiceNumber
        is ReceiptPayload.RepairReceipt -> payload.ticket.ticketNumber
      }
    val uri = saveReceiptPngToCache(context, bitmap, prefix)
    val text = buildWhatsAppText(payload)

    val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

    val pm = context.packageManager
    val hasWhatsApp =
      listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
        try {
          pm.getPackageInfo(pkg, 0)
          true
        } catch (_: Exception) {
          false
        }
      }

    if (hasWhatsApp != null) {
      intent.setPackage(hasWhatsApp)
      context.startActivity(intent)
    } else {
      context.startActivity(Intent.createChooser(intent, "مشاركة الفاتورة"))
    }
  }

  fun printReceipt(context: Context, payload: ReceiptPayload) {
    val bitmap = renderReceiptBitmap(context, payload)
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
    val jobName =
      when (payload) {
        is ReceiptPayload.SaleReceipt -> "2mStore_${payload.sale.invoiceNumber}"
        is ReceiptPayload.RepairReceipt -> "2mStore_${payload.ticket.ticketNumber}"
      }

    printManager.print(
      jobName,
      object : PrintDocumentAdapter() {
        override fun onLayout(
          oldAttributes: PrintAttributes?,
          newAttributes: PrintAttributes?,
          cancellationSignal: CancellationSignal?,
          callback: LayoutResultCallback?,
          extras: Bundle?,
        ) {
          if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
          }
          val info =
            PrintDocumentInfo.Builder("$jobName.pdf")
              .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
              .setPageCount(1)
              .build()
          callback?.onLayoutFinished(info, true)
        }

        override fun onWrite(
          pages: Array<out PageRange>?,
          destination: ParcelFileDescriptor?,
          cancellationSignal: CancellationSignal?,
          callback: WriteResultCallback?,
        ) {
          if (destination == null) {
            callback?.onWriteFailed("Missing destination")
            return
          }
          val pdf = PdfDocument()
          val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
          val page = pdf.startPage(pageInfo)
          val canvas = page.canvas
          val scale = minOf(555f / bitmap.width, 802f / bitmap.height)
          val destW = bitmap.width * scale
          val destH = bitmap.height * scale
          val left = (595f - destW) / 2f
          val top = 20f
          canvas.drawBitmap(
            bitmap,
            Rect(0, 0, bitmap.width, bitmap.height),
            RectF(left, top, left + destW, top + destH),
            Paint(Paint.ANTI_ALIAS_FLAG),
          )
          pdf.finishPage(page)
          try {
            FileOutputStream(destination.fileDescriptor).use { out -> pdf.writeTo(out) }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
          } catch (e: Exception) {
            callback?.onWriteFailed(e.message)
          } finally {
            pdf.close()
          }
        }
      },
      PrintAttributes.Builder().build(),
    )
  }
}
