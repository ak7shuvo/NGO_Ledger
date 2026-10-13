package com.ngoloanmanager.receipt

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import com.ngoloanmanager.R
import com.ngoloanmanager.domain.ReceiptData
import com.ngoloanmanager.domain.ReceiptFormat
import java.io.File
import java.io.FileOutputStream

/**
 * Draws an A4 (595x842 pt) receipt with android.graphics.pdf.PdfDocument - fully offline.
 * Text uses the device's system font; Bengali glyphs (and the ৳ sign) come from the device's built-in Bengali font via
 * Android's normal font fallback, so Bengali names are not turned into boxes on devices that ship Bengali support.
 */
object ReceiptPdf {
    private const val W = 595
    private const val H = 842
    private const val M = 40f
    private val NAVY = Color.parseColor("#0F172A")
    private val TEAL = Color.parseColor("#14B8A6")
    private val GOLD = Color.parseColor("#D4AF37")
    private val LIGHT = Color.parseColor("#F1F5F9")
    private val GRAY = Color.parseColor("#475569")

    private fun paint(size: Float, color: Int = NAVY, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size; this.color = color
        typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private class Writer(val doc: PdfDocument, val data: ReceiptData, val logo: Bitmap?) {
        var pageNo = 0
        lateinit var page: PdfDocument.Page
        lateinit var c: Canvas
        var y = 0f
        fun newPage(first: Boolean) {
            if (pageNo > 0) doc.finishPage(page)
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
            c = page.canvas
            y = if (first) header() else continuationHeader()
        }
        fun finish() { footer(); doc.finishPage(page) }

        private fun header(): Float {
            if (logo != null) {
                val h = 46f; val w = h * logo.width / logo.height // keep proportions
                c.drawBitmap(logo, null, RectF(M, 30f, M + w, 30f + h), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
            }
            val r = paint(9f, GRAY)
            c.drawText("Ref: ${data.referenceNo}", W - M - r.measureText("Ref: ${data.referenceNo}"), 44f, r)
            c.drawText("Issued: ${data.generatedOn}", W - M - r.measureText("Issued: ${data.generatedOn}"), 58f, r)
            c.drawRect(M, 86f, W - M, 89f, Paint().apply { color = GOLD })
            val t = paint(16f, NAVY, true)
            c.drawText(ellipsize(data.ngoName, t, W - 2 * M), M, 112f, t)
            val s = paint(11f, TEAL, true)
            c.drawText("LOAN DISBURSEMENT RECEIPT  |  ঋণ প্রদান রসিদ", M, 130f, s)
            return 150f
        }
        private fun continuationHeader(): Float {
            val t = paint(10f, GRAY, true)
            c.drawText("${data.ngoName} - ${data.referenceNo} (continued)", M, 40f, t)
            c.drawRect(M, 48f, W - M, 50f, Paint().apply { color = GOLD })
            return 70f
        }
        private fun footer() {
            val p = paint(8f, GRAY)
            c.drawText("Developed by PETRA Software Solutions", M, H - 24f, p)
            val n = "Page $pageNo"
            c.drawText(n, W - M - p.measureText(n), H - 24f, p)
        }
        fun ensure(h: Float) { if (y + h > H - 50f) { footer(); newPage(false) } }
    }

    private fun ellipsize(text: String, p: TextPaint, width: Float): String =
        TextUtils.ellipsize(text, p, width, TextUtils.TruncateAt.END).toString()

    fun generate(context: Context, data: ReceiptData, out: File) {
        val doc = PdfDocument()
        val logo = try { BitmapFactory.decodeResource(context.resources, R.drawable.petra_logo) } catch (e: Exception) { null }
        try {
            val w = Writer(doc, data, logo)
            w.newPage(true)
            section(w, "BORROWER  |  ঋণগ্রহীতা")
            kv(w, "Name", data.borrowerName); kv(w, "Member ID", data.memberCode)
            if (data.phone.isNotBlank()) kv(w, "Phone", data.phone)
            if (data.address.isNotBlank()) kv(w, "Address", data.address)
            if (data.nationalIdMasked.isNotBlank()) kv(w, "National ID", data.nationalIdMasked)
            w.y += 8f
            section(w, "LOAN DETAILS  |  ঋণের বিবরণ")
            kv(w, "Loan ID", data.loanCode); kv(w, "Issue date", data.loanDate); kv(w, "First installment", data.firstDueDate)
            kv(w, "Interest rate", "${data.ratePercent}% (${data.interestType})"); kv(w, "Term", "${data.termMonths} months")
            kv(w, "Status", data.status)
            w.y += 6f
            amountBox(w)
            w.y += 12f
            schedule(w)
            acknowledgement(w)
            w.finish()
            out.parentFile?.mkdirs()
            FileOutputStream(out).use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
    }

    private fun section(w: Writer, title: String) {
        w.ensure(30f)
        w.c.drawRect(M, w.y, W - M, w.y + 18f, Paint().apply { color = LIGHT })
        w.c.drawText(title, M + 6f, w.y + 13f, paint(9.5f, NAVY, true))
        w.y += 28f
    }

    private fun kv(w: Writer, k: String, v: String) {
        w.ensure(16f)
        w.c.drawText(k, M + 6f, w.y, paint(10f, GRAY))
        val vp = paint(10.5f, NAVY, true)
        w.c.drawText(ellipsize(v, vp, W - 2 * M - 130f), M + 130f, w.y, vp)
        w.y += 16f
    }

    private fun amountRow(w: Writer, label: String, value: String, bold: Boolean = false, color: Int = NAVY) {
        val lp = paint(if (bold) 11f else 10.5f, NAVY, bold); val vp = paint(if (bold) 12f else 10.5f, color, bold)
        w.c.drawText(label, M + 10f, w.y, lp)
        w.c.drawText(value, W - M - 10f - vp.measureText(value), w.y, vp) // right-aligned currency
        w.y += if (bold) 19f else 16f
    }

    private fun amountBox(w: Writer) {
        w.ensure(140f)
        val top = w.y - 4f
        w.y += 12f
        amountRow(w, "Principal amount disbursed", ReceiptFormat.taka(w.data.principalPoisha), bold = true)
        amountRow(w, "Interest (as recorded)", ReceiptFormat.taka(w.data.interestPoisha))
        amountRow(w, "Total payable", ReceiptFormat.taka(w.data.totalPayablePoisha), bold = true)
        amountRow(w, "Paid to date (${w.data.generatedOn})", ReceiptFormat.taka(w.data.paidPoisha))
        amountRow(w, "Outstanding balance", ReceiptFormat.taka(w.data.outstandingPoisha), bold = true, color = Color.parseColor("#0B6B63"))
        if (w.data.advancePoisha > 0) amountRow(w, "Advance (paid above total)", ReceiptFormat.taka(w.data.advancePoisha))
        w.c.drawRect(M, top, W - M, w.y, Paint().apply { color = TEAL; style = Paint.Style.STROKE; strokeWidth = 1.2f })
    }

    private fun schedule(w: Writer) {
        section(w, "REPAYMENT SCHEDULE  |  কিস্তির তালিকা")
        fun head() {
            val p = paint(9f, GRAY, true)
            w.c.drawText("#", M + 6f, w.y, p); w.c.drawText("Due date", M + 40f, w.y, p)
            val a = "Amount"; w.c.drawText(a, W - M - 150f - p.measureText(a), w.y, p)
            val b = "Paid"; w.c.drawText(b, W - M - 6f - p.measureText(b), w.y, p)
            w.c.drawLine(M, w.y + 4f, W - M, w.y + 4f, Paint().apply { color = GRAY; strokeWidth = 0.6f })
            w.y += 16f
        }
        head()
        val tp = paint(10f)
        w.data.installments.forEach { i ->
            if (w.y + 16f > H - 50f) { w.ensure(1000f); head() }
            w.c.drawText(i.number.toString(), M + 6f, w.y, tp)
            w.c.drawText(i.dueDate, M + 40f, w.y, tp)
            val a = ReceiptFormat.taka(i.amountPoisha); w.c.drawText(a, W - M - 150f - tp.measureText(a), w.y, tp)
            val b = ReceiptFormat.taka(i.paidPoisha); w.c.drawText(b, W - M - 6f - tp.measureText(b), w.y, tp)
            w.y += 15f
        }
        w.y += 10f
    }

    private fun acknowledgement(w: Writer) {
        w.ensure(190f)
        section(w, "ACKNOWLEDGEMENT  |  স্বীকারোক্তি")
        val p = paint(9f, GRAY)
        listOf(
            "I acknowledge that I have received the loan amount stated above and agree to repay it according to the schedule shown.",
            "আমি উপরে উল্লিখিত ঋণের অর্থ গ্রহণ করেছি এবং নির্ধারিত কিস্তি অনুযায়ী পরিশোধ করতে সম্মত।",
            "Terms and balances are as recorded in the organization's loan register on the issue date shown. This document is a record of disbursement."
        ).forEach { line ->
            val layout = android.text.StaticLayout.Builder.obtain(line, 0, line.length, p, (W - 2 * M - 12f).toInt()).build()
            w.ensure(layout.height + 6f)
            w.c.save(); w.c.translate(M + 6f, w.y - 8f); layout.draw(w.c); w.c.restore()
            w.y += layout.height + 4f
        }
        w.ensure(80f)
        w.y += 40f
        val line = Paint().apply { color = NAVY; strokeWidth = 0.8f }
        val half = (W - 2 * M - 40f) / 2
        w.c.drawLine(M, w.y, M + half, w.y, line)
        w.c.drawLine(W - M - half, w.y, W - M, w.y, line)
        val lp = paint(9f, GRAY)
        w.c.drawText("Borrower signature / ঋণগ্রহীতার স্বাক্ষর", M, w.y + 13f, lp)
        w.c.drawText("Authorized by / অনুমোদনকারী: ${ellipsize(w.data.staffName, lp, half - 120f)}", W - M - half, w.y + 13f, lp)
        w.c.drawText("Date: ____________", M, w.y + 30f, lp)
        w.c.drawText("Date: ____________", W - M - half, w.y + 30f, lp)
        w.y += 40f
    }
}
