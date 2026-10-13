package com.ngoloanmanager.data

import com.ngoloanmanager.domain.UserFacingException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupFormatTest {
    private fun sample() = BackupFormat.Snapshot(
        borrowers = listOf(BorrowerEntity(1, "M-1", "রহিম", "017", "Sylhet", "1234567890", "2026-01-01", "", false)),
        loans = listOf(LoanEntity(1, "LN-1", 1, 100000, "12", "ANNUAL", 2, "2026-01-01", "2026-02-01", 2000, 102000, "ACTIVE", "")),
        installments = listOf(InstallmentEntity(1, 1, 1, "2026-02-01", 51000, 51000), InstallmentEntity(2, 1, 2, "2026-03-01", 51000, 0)),
        payments = listOf(PaymentEntity(1, 1, "RC-1", 51000, "2026-02-01", "", false, 5L)),
        allocations = listOf(PaymentAllocationEntity(1, 1, 51000)),
        auditLogs = listOf(AuditLogEntity(1, "LOAN_CREATED", "loan", "1", "x", 7L)),
        settings = listOf(AppSettingEntity("ngo_name", "Test NGO"))
    )

    private fun fails(block: () -> Unit) {
        try { block(); fail("expected UserFacingException") } catch (e: UserFacingException) { /* ok */ }
    }

    @Test fun roundTripKeepsEverything() {
        val parsed = BackupFormat.decode(BackupFormat.encode(sample(), "1.1.0"))
        assertEquals(sample().counts(), parsed.snapshot.counts())
        assertEquals("রহিম", parsed.snapshot.borrowers[0].name)
        assertEquals(102000L, parsed.snapshot.loans[0].totalPayablePoisha)
    }

    @Test fun backupNeverContainsAdminCredentials() {
        val text = String(BackupFormat.encode(sample(), "1.1.0"))
        assertTrue(!text.contains("passwordHash") && !text.contains("salt"))
    }

    @Test fun tamperedFileIsRejected() {
        val text = String(BackupFormat.encode(sample(), "1.1.0")).replace("Test NGO", "Evil NGO")
        fails { BackupFormat.decode(text.toByteArray()) }
    }

    @Test fun garbageAndEmptyAreRejected() {
        fails { BackupFormat.decode(ByteArray(0)) }
        fails { BackupFormat.decode("not json".toByteArray()) }
        fails { BackupFormat.decode("{\"format\":\"other\"}".toByteArray()) }
    }

    @Test fun inconsistentLedgerIsRejected() {
        val s = sample()
        fails { BackupFormat.checkIntegrity(BackupFormat.Snapshot(s.borrowers, s.loans.map { it.copy(totalPayablePoisha = 1) }, s.installments, s.payments, s.allocations, s.auditLogs, s.settings)) }
        fails { BackupFormat.checkIntegrity(BackupFormat.Snapshot(emptyList(), s.loans, s.installments, s.payments, s.allocations, s.auditLogs, s.settings)) }
    }
}
