package com.ngoloanmanager.receipt

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ngoloanmanager.data.*
import com.ngoloanmanager.domain.*
import com.ngoloanmanager.ui.components.GlassCard
import com.ngoloanmanager.ui.components.PremiumOutlinedButton
import com.ngoloanmanager.ui.components.PremiumPrimaryButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }
    return null
}

@Composable
fun ReceiptSection(
    db: LoanDatabase,
    loan: LoanEntity,
    borrower: BorrowerEntity?,
    installments: List<InstallmentEntity>,
    payments: List<PaymentEntity>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // A generated PDF reflects balances at generation time, so it is discarded whenever the ledger changes.
    var file by remember(loan, installments, payments) { mutableStateOf<File?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var ok by remember { mutableStateOf(true) }
    var previewPages by remember { mutableStateOf<Pair<List<Bitmap>, Int>?>(null) }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val f = file
        if (uri != null && f != null) scope.launch {
            try {
                withContext(Dispatchers.IO) { ReceiptFiles.copyTo(context, f, uri) }
                ok = true; message = "Receipt saved: ${f.name}"
            } catch (e: CancellationException) { throw e } catch (e: Exception) {
                android.util.Log.e("NGOLoan", "save receipt", e); ok = false; message = "Could not save the receipt"
            }
        }
    }

    GlassCard(elevation = 2.dp) {
        Text("Loan Receipt (PDF)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            "A4 disbursement receipt with repayment schedule and signature fields. Created on this device, no internet needed.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        PremiumPrimaryButton(
            if (file == null) "Generate Receipt" else "Regenerate Receipt",
            {
                if (borrower == null) { ok = false; message = "Member details are still loading"; return@PremiumPrimaryButton }
                busy = true; message = ""
                scope.launch {
                    try {
                        val result = withContext(Dispatchers.IO) {
                            ReceiptFiles.cleanup(context)
                            val admin = db.dao().admin()?.username ?: ""
                            val ngo = db.dao().setting("ngo_name")?.takeIf { it.isNotBlank() } ?: "NGO"
                            val paid = payments.filterNot { it.cancelled }.sumOf { it.amountPoisha }
                            val today = LocalDate.now().toString()
                            val data = ReceiptData(
                                ngoName = ngo, referenceNo = ReceiptFormat.referenceNo(loan.loanCode), generatedOn = today,
                                staffName = admin, borrowerName = borrower.name, memberCode = borrower.memberCode,
                                phone = borrower.phone, address = borrower.address,
                                nationalIdMasked = ReceiptFormat.maskId(borrower.nationalId),
                                loanCode = loan.loanCode, loanDate = loan.loanDate, firstDueDate = loan.firstDueDate,
                                principalPoisha = loan.principalPoisha, ratePercent = loan.ratePercent,
                                interestType = if (loan.interestPeriod == InterestPeriod.ANNUAL.name) "annual rate, pro-rated over the term" else "flat for the full term",
                                termMonths = loan.termMonths, interestPoisha = loan.interestPoisha,
                                totalPayablePoisha = loan.totalPayablePoisha, paidPoisha = paid,
                                outstandingPoisha = FinancialSummary.outstanding(loan.totalPayablePoisha, paid),
                                advancePoisha = FinancialSummary.advance(loan.totalPayablePoisha, paid),
                                status = loan.status,
                                installments = installments.sortedBy { it.number }.map { ReceiptInstallment(it.number, it.dueDate, it.dueAmountPoisha, it.allocatedPoisha) }
                            )
                            val out = File(ReceiptFiles.dir(context), ReceiptFormat.fileName(loan.loanCode, today))
                            ReceiptPdf.generate(context, data, out)
                            out
                        }
                        file = result; ok = true; message = "Receipt ready: ${result.name}"
                    } catch (e: CancellationException) { throw e } catch (e: Exception) {
                        android.util.Log.e("NGOLoan", "generate receipt", e); ok = false; message = "Could not create the receipt PDF"
                    } finally { busy = false }
                }
            },
            Modifier.fillMaxWidth(), loading = busy
        )
        val f = file
        if (f != null) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumOutlinedButton("Preview", {
                    scope.launch {
                        try { previewPages = withContext(Dispatchers.IO) { ReceiptFiles.renderPages(f, 900, 4) } }
                        catch (e: CancellationException) { throw e } catch (e: Exception) { ok = false; message = "Could not preview the receipt" }
                    }
                }, Modifier.weight(1f))
                PremiumOutlinedButton("Save", { saveLauncher.launch(f.name) }, Modifier.weight(1f))
                PremiumOutlinedButton("Print", {
                    val act = context.findActivity()
                    val pm = act?.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    if (pm == null) { ok = false; message = "Printing is not available" } else {
                        pm.print(f.name, PdfFilePrintAdapter(f, f.name), PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build())
                    }
                }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            PremiumPrimaryButton("Share Receipt", {
                try {
                    context.startActivity(ReceiptFiles.shareIntent(context, f))
                    ok = true; message = "Choose an app (e.g. WhatsApp) and recipient in the share sheet. Nothing is sent until you send it there."
                } catch (e: ActivityNotFoundException) { ok = false; message = "No app available to share a PDF" }
            }, Modifier.fillMaxWidth())
        }
        if (message.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
    }

    previewPages?.let { (pages, total) ->
        AlertDialog(
            onDismissRequest = { previewPages = null },
            title = { Text("Receipt preview") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pages.forEach { Image(it.asImageBitmap(), "Receipt page", Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth) }
                    if (total > pages.size) Text("Showing first ${pages.size} of $total pages. Save or print for the full document.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { previewPages = null }) { Text("Close") } }
        )
    }
}
