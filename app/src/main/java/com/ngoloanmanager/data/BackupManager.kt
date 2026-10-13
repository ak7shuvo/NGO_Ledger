package com.ngoloanmanager.data

import androidx.room.withTransaction
import com.ngoloanmanager.domain.UserFacingException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.security.MessageDigest
import java.time.Instant

/**
 * Local backup / restore of ledger data as one JSON file.
 *
 * INCLUDED: borrowers (archived too), loans, installments, payments (cancelled too), payment allocations,
 * audit log, app settings (e.g. organization name).
 * EXCLUDED: the admin account (username, salt, password hash) - a restore never changes who can log in.
 *
 * File layout: a small JSON envelope whose "payload" is the data serialized as a STRING; "sha256" is the hash of
 * exactly that string, so any edit/truncation/corruption is detected before anything is touched.
 */
object BackupFormat {
    const val FORMAT = "ngo-loan-manager-backup"
    const val FORMAT_VERSION = 1
    const val SCHEMA_VERSION = 1 // must match LoanDatabase version
    const val MAX_BYTES = 50 * 1024 * 1024

    class Snapshot(
        val borrowers: List<BorrowerEntity>,
        val loans: List<LoanEntity>,
        val installments: List<InstallmentEntity>,
        val payments: List<PaymentEntity>,
        val allocations: List<PaymentAllocationEntity>,
        val auditLogs: List<AuditLogEntity>,
        val settings: List<AppSettingEntity>
    ) {
        fun counts() = mapOf(
            "borrowers" to borrowers.size, "loans" to loans.size, "installments" to installments.size,
            "payments" to payments.size, "allocations" to allocations.size,
            "auditLogs" to auditLogs.size, "settings" to settings.size
        )
    }

    class Parsed(val createdAt: String, val appVersion: String, val snapshot: Snapshot)

    fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    fun encode(snapshot: Snapshot, appVersion: String, createdAt: String = Instant.now().toString()): ByteArray {
        val payload = JSONObject().apply {
            put("borrowers", JSONArray().also { a -> snapshot.borrowers.forEach { a.put(JSONObject().put("id", it.id).put("memberCode", it.memberCode).put("name", it.name).put("phone", it.phone).put("address", it.address).put("nationalId", it.nationalId).put("registeredAt", it.registeredAt).put("notes", it.notes).put("archived", it.archived)) } })
            put("loans", JSONArray().also { a -> snapshot.loans.forEach { a.put(JSONObject().put("id", it.id).put("loanCode", it.loanCode).put("borrowerId", it.borrowerId).put("principalPoisha", it.principalPoisha).put("ratePercent", it.ratePercent).put("interestPeriod", it.interestPeriod).put("termMonths", it.termMonths).put("loanDate", it.loanDate).put("firstDueDate", it.firstDueDate).put("interestPoisha", it.interestPoisha).put("totalPayablePoisha", it.totalPayablePoisha).put("status", it.status).put("notes", it.notes)) } })
            put("installments", JSONArray().also { a -> snapshot.installments.forEach { a.put(JSONObject().put("id", it.id).put("loanId", it.loanId).put("number", it.number).put("dueDate", it.dueDate).put("dueAmountPoisha", it.dueAmountPoisha).put("allocatedPoisha", it.allocatedPoisha)) } })
            put("payments", JSONArray().also { a -> snapshot.payments.forEach { a.put(JSONObject().put("id", it.id).put("loanId", it.loanId).put("receiptNo", it.receiptNo).put("amountPoisha", it.amountPoisha).put("paymentDate", it.paymentDate).put("notes", it.notes).put("cancelled", it.cancelled).put("createdAt", it.createdAt)) } })
            put("allocations", JSONArray().also { a -> snapshot.allocations.forEach { a.put(JSONObject().put("paymentId", it.paymentId).put("installmentId", it.installmentId).put("amountPoisha", it.amountPoisha)) } })
            put("auditLogs", JSONArray().also { a -> snapshot.auditLogs.forEach { a.put(JSONObject().put("id", it.id).put("action", it.action).put("recordType", it.recordType).put("recordId", it.recordId).put("details", it.details).put("timestamp", it.timestamp)) } })
            put("settings", JSONArray().also { a -> snapshot.settings.forEach { a.put(JSONObject().put("key", it.key).put("value", it.value)) } })
        }.toString()
        val counts = JSONObject().also { c -> snapshot.counts().forEach { (k, v) -> c.put(k, v) } }
        return JSONObject()
            .put("format", FORMAT).put("formatVersion", FORMAT_VERSION).put("schemaVersion", SCHEMA_VERSION)
            .put("appVersion", appVersion).put("createdAt", createdAt)
            .put("counts", counts).put("sha256", sha256(payload)).put("payload", payload)
            .toString(2).toByteArray(Charsets.UTF_8)
    }

    /** Validates everything; throws UserFacingException with a clear message and never touches the database. */
    fun decode(bytes: ByteArray): Parsed {
        if (bytes.isEmpty()) throw UserFacingException("The selected file is empty")
        if (bytes.size > MAX_BYTES) throw UserFacingException("The selected file is too large to be a valid backup")
        try {
            val env = JSONObject(String(bytes, Charsets.UTF_8))
            if (env.optString("format") != FORMAT) throw UserFacingException("This is not an NGO Loan Manager backup file")
            val fv = env.optInt("formatVersion", -1)
            if (fv != FORMAT_VERSION) throw UserFacingException("Incompatible backup format version ($fv). This app supports version $FORMAT_VERSION")
            val sv = env.optInt("schemaVersion", -1)
            if (sv != SCHEMA_VERSION) throw UserFacingException("Incompatible database version in backup ($sv). This app supports version $SCHEMA_VERSION")
            val payload = env.getString("payload")
            if (sha256(payload) != env.optString("sha256")) throw UserFacingException("The backup file is corrupted or has been modified (checksum mismatch)")
            val p = JSONObject(payload)
            val snap = Snapshot(
                borrowers = p.getJSONArray("borrowers").map { o -> BorrowerEntity(o.getLong("id"), o.getString("memberCode"), o.getString("name"), o.getString("phone"), o.getString("address"), o.getString("nationalId"), o.getString("registeredAt"), o.getString("notes"), o.getBoolean("archived")) },
                loans = p.getJSONArray("loans").map { o -> LoanEntity(o.getLong("id"), o.getString("loanCode"), o.getLong("borrowerId"), o.getLong("principalPoisha"), o.getString("ratePercent"), o.getString("interestPeriod"), o.getInt("termMonths"), o.getString("loanDate"), o.getString("firstDueDate"), o.getLong("interestPoisha"), o.getLong("totalPayablePoisha"), o.getString("status"), o.getString("notes")) },
                installments = p.getJSONArray("installments").map { o -> InstallmentEntity(o.getLong("id"), o.getLong("loanId"), o.getInt("number"), o.getString("dueDate"), o.getLong("dueAmountPoisha"), o.getLong("allocatedPoisha")) },
                payments = p.getJSONArray("payments").map { o -> PaymentEntity(o.getLong("id"), o.getLong("loanId"), o.getString("receiptNo"), o.getLong("amountPoisha"), o.getString("paymentDate"), o.getString("notes"), o.getBoolean("cancelled"), o.getLong("createdAt")) },
                allocations = p.getJSONArray("allocations").map { o -> PaymentAllocationEntity(o.getLong("paymentId"), o.getLong("installmentId"), o.getLong("amountPoisha")) },
                auditLogs = p.getJSONArray("auditLogs").map { o -> AuditLogEntity(o.getLong("id"), o.getString("action"), o.getString("recordType"), o.getString("recordId"), o.getString("details"), o.getLong("timestamp")) },
                settings = p.getJSONArray("settings").map { o -> AppSettingEntity(o.getString("key"), o.getString("value")) }
            )
            val declared = env.getJSONObject("counts")
            snap.counts().forEach { (k, v) -> if (declared.optInt(k, -1) != v) throw UserFacingException("The backup file is incomplete (record count mismatch for $k)") }
            checkIntegrity(snap)
            return Parsed(env.optString("createdAt"), env.optString("appVersion"), snap)
        } catch (e: UserFacingException) {
            throw e
        } catch (e: JSONException) {
            throw UserFacingException("The selected file is not a valid backup (unreadable or missing data)")
        } catch (e: RuntimeException) {
            throw UserFacingException("The selected file is not a valid backup")
        }
    }

    /** Referential and financial sanity checks so a restore can never produce an inconsistent ledger. */
    fun checkIntegrity(s: Snapshot) {
        fun bad(msg: String): Nothing = throw UserFacingException("Backup failed integrity check: $msg")
        val borrowerIds = s.borrowers.map { it.id }.toSet()
        val loanIds = s.loans.map { it.id }.toSet()
        val instById = s.installments.associateBy { it.id }
        val paymentIds = s.payments.map { it.id }.toSet()
        if (borrowerIds.size != s.borrowers.size || loanIds.size != s.loans.size || instById.size != s.installments.size || paymentIds.size != s.payments.size) bad("duplicate IDs")
        if (s.borrowers.map { it.memberCode }.toSet().size != s.borrowers.size) bad("duplicate member codes")
        if (s.loans.map { it.loanCode }.toSet().size != s.loans.size) bad("duplicate loan codes")
        if (s.payments.map { it.receiptNo }.toSet().size != s.payments.size) bad("duplicate receipt numbers")
        if (s.loans.any { it.borrowerId !in borrowerIds }) bad("a loan refers to a missing member")
        if (s.installments.any { it.loanId !in loanIds }) bad("an installment refers to a missing loan")
        if (s.payments.any { it.loanId !in loanIds }) bad("a payment refers to a missing loan")
        if (s.allocations.any { it.paymentId !in paymentIds || it.installmentId !in instById }) bad("an allocation refers to a missing record")
        if (s.allocations.map { it.paymentId to it.installmentId }.toSet().size != s.allocations.size) bad("duplicate allocations")
        if (s.loans.any { it.principalPoisha < 0 || it.interestPoisha < 0 || it.totalPayablePoisha != it.principalPoisha + it.interestPoisha }) bad("loan totals do not add up")
        if (s.payments.any { it.amountPoisha <= 0 }) bad("a payment has a non-positive amount")
        if (s.installments.any { it.dueAmountPoisha < 0 || it.allocatedPoisha < 0 || it.allocatedPoisha > it.dueAmountPoisha }) bad("an installment has invalid amounts")
        val sums = s.installments.groupBy { it.loanId }.mapValues { (_, v) -> v.sumOf { it.dueAmountPoisha } }
        if (s.loans.any { (sums[it.id] ?: 0L) != it.totalPayablePoisha }) bad("installments do not add up to the loan total")
        if (s.settings.map { it.key }.toSet().size != s.settings.size) bad("duplicate settings")
    }

    private inline fun <T> JSONArray.map(f: (JSONObject) -> T): List<T> = List(length()) { f(getJSONObject(it)) }
}

class BackupManager(private val db: LoanDatabase) {
    private val dao get() = db.dao()

    /** Consistent snapshot (single transaction) encoded to bytes. */
    suspend fun createBackup(appVersion: String): Pair<ByteArray, BackupFormat.Snapshot> {
        val snap = db.withTransaction {
            BackupFormat.Snapshot(dao.exportBorrowers(), dao.exportLoans(), dao.exportInstallments(), dao.exportPayments(), dao.exportAllocations(), dao.exportAuditLogs(), dao.exportSettings())
        }
        return BackupFormat.encode(snap, appVersion) to snap
    }

    suspend fun currentCounts(): Map<String, Int> = db.withTransaction {
        BackupFormat.Snapshot(dao.exportBorrowers(), dao.exportLoans(), dao.exportInstallments(), dao.exportPayments(), dao.exportAllocations(), dao.exportAuditLogs(), dao.exportSettings()).counts()
    }

    /** Replaces all ledger data in ONE transaction; any failure rolls back and leaves existing data untouched. Admin account is kept. */
    suspend fun restore(parsed: BackupFormat.Parsed) {
        val s = parsed.snapshot
        db.withTransaction {
            dao.wipeAllocations(); dao.wipePayments(); dao.wipeInstallments(); dao.wipeLoans(); dao.wipeBorrowers(); dao.wipeAuditLogs(); dao.wipeSettings()
            dao.restoreBorrowers(s.borrowers); dao.restoreLoans(s.loans); dao.restoreInstallments(s.installments)
            dao.restorePayments(s.payments); dao.restoreAllocations(s.allocations); dao.restoreAuditLogs(s.auditLogs); dao.restoreSettings(s.settings)
            dao.audit(AuditLogEntity(action = "BACKUP_RESTORED", recordType = "backup", recordId = "-", details = "Restored backup created ${parsed.createdAt} (${s.loans.size} loans, ${s.payments.size} payments)"))
        }
    }
}
