package com.ngoloanmanager.receipt

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/** Receipts live only in the app cache (temporary); sharing uses FileProvider with a temporary read grant. */
object ReceiptFiles {
    fun dir(context: Context) = File(context.cacheDir, "receipts")

    /** Removes receipts older than 24 hours so personal data is not left lying around. */
    fun cleanup(context: Context) {
        val limit = System.currentTimeMillis() - 24L * 60 * 60 * 1000
        dir(context).listFiles()?.forEach { if (it.lastModified() < limit) it.delete() }
    }

    fun uri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)

    /** Native share sheet; WhatsApp appears there when installed, otherwise any other PDF-capable app can be used. */
    fun shareIntent(context: Context, file: File): Intent {
        val uri = uri(context, file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Loan receipt")
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Share receipt").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun copyTo(context: Context, file: File, target: Uri) {
        val out = context.contentResolver.openOutputStream(target, "wt") ?: error("Cannot open destination")
        out.use { o -> FileInputStream(file).use { it.copyTo(o) }; o.flush() }
    }

    /** Renders up to [maxPages] pages for the in-app preview. */
    fun renderPages(file: File, widthPx: Int, maxPages: Int): Pair<List<Bitmap>, Int> {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { r ->
                val bitmaps = ArrayList<Bitmap>()
                for (i in 0 until minOf(r.pageCount, maxPages)) {
                    r.openPage(i).use { p ->
                        val h = (widthPx.toFloat() * p.height / p.width).toInt()
                        val bmp = Bitmap.createBitmap(widthPx, h, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmaps.add(bmp)
                    }
                }
                return bitmaps to r.pageCount
            }
        }
    }
}

/** Feeds an already generated PDF to Android's print framework. */
class PdfFilePrintAdapter(private val file: File, private val jobName: String) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?, newAttributes: PrintAttributes, cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback, extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) { callback.onLayoutCancelled(); return }
        callback.onLayoutFinished(
            PrintDocumentInfo.Builder(jobName).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN).build(),
            true
        )
    }

    override fun onWrite(
        pages: Array<out PageRange>, destination: ParcelFileDescriptor, cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        try {
            FileInputStream(file).use { input -> FileOutputStream(destination.fileDescriptor).use { input.copyTo(it) } }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed("Could not print the receipt")
        }
    }
}
