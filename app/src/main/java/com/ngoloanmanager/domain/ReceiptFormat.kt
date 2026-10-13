package com.ngoloanmanager.domain

import java.util.Locale

/** Pure helpers for the loan receipt (no Android dependencies so they are unit-testable). */
object ReceiptFormat {
    /** 123456789 poisha -> "৳ 1,234,567.89". Western digits and grouping, 2 decimals, exact (integer arithmetic). */
    fun taka(poisha: Long): String {
        val neg = poisha < 0
        val abs = if (neg) -poisha else poisha
        val whole = String.format(Locale.US, "%,d", abs / 100)
        return (if (neg) "-" else "") + "৳ " + whole + "." + String.format(Locale.US, "%02d", abs % 100)
    }

    /** Safe file name: no personal data, only the loan code and date. */
    fun fileName(loanCode: String, isoDate: String): String {
        val code = loanCode.replace(Regex("[^A-Za-z0-9._-]"), "_").take(40).ifBlank { "loan" }
        val date = isoDate.replace("-", "").filter { it.isDigit() }.take(8)
        return "Receipt_${code}_$date.pdf"
    }

    /** Keeps only the last 4 characters of an ID number, e.g. "••••6789". */
    fun maskId(id: String): String {
        val t = id.trim()
        if (t.isEmpty()) return ""
        return if (t.length <= 4) t else "••••" + t.takeLast(4)
    }

    fun referenceNo(loanCode: String): String = "LD-" + loanCode.replace(Regex("[^A-Za-z0-9-]"), "")
}

data class ReceiptInstallment(val number: Int, val dueDate: String, val amountPoisha: Long, val paidPoisha: Long)

data class ReceiptData(
    val ngoName: String,
    val referenceNo: String,
    val generatedOn: String,
    val staffName: String,
    val borrowerName: String,
    val memberCode: String,
    val phone: String,
    val address: String,
    val nationalIdMasked: String,
    val loanCode: String,
    val loanDate: String,
    val firstDueDate: String,
    val principalPoisha: Long,
    val ratePercent: String,
    val interestType: String,
    val termMonths: Int,
    val interestPoisha: Long,
    val totalPayablePoisha: Long,
    val paidPoisha: Long,
    val outstandingPoisha: Long,
    val advancePoisha: Long,
    val status: String,
    val installments: List<ReceiptInstallment>
)
