# Summary of changes - NGO-APK-PETRA-Final

## Audit findings
- Backup/restore did not exist: Settings only showed a "not available" notice.
- No receipt/PDF/share/FileProvider code existed. Palette was emerald (not PETRA). Footer credit missing.
- Existing business logic (LoanMath, PaymentAllocator, transactions for loans/payments, PBKDF2 auth) was reviewed and left unchanged.
- Room `exportSchema = true` without a schema directory (build warning) -> set to false.
- Workflow duplicated Gradle caching and never ran tests -> now runs `testDebugUnitTest` then `assembleDebug`, also on pull requests.

## Changes
1. Palette applied app-wide (Navy primary, Teal buttons, Gold highlights, Light Gray background; dark theme navy-based); status/navigation bar colors updated.
2. "Developed by PETRA Software Solutions" on the login screen and Settings > About; logo file untouched.
3. Backup/restore (`data/BackupManager.kt`, `backup/BackupSection.kt`): SAF file picker, checksum + integrity validation, read-back verification, timestamps, confirmation dialog, safety copy, single-transaction restore, admin credentials excluded.
4. Loan receipt PDF (`receipt/ReceiptPdf.kt`, `domain/ReceiptFormat.kt`): offline A4 PDF via `PdfDocument`, multi-page schedule, right-aligned currency, signatures, Bengali/English text.
5. Preview (PdfRenderer), save (SAF), print (PrintManager), share (FileProvider + chooser, temporary read grant). No automation of WhatsApp.
6. Tests added: `BackupFormatTest`, `ReceiptFormatTest`. Version 1.1.0 (code 2). README updated.

## Verification status (honest)
- NOT built or run here: the delivery environment had no Android SDK and could not download one, so `assembleDebug`, unit tests, lint and on-device behaviour (SAF, printing, WhatsApp sharing, Bengali rendering) have **not** been executed. All new code was written carefully but is unverified until GitHub Actions or Android Studio compiles it.
- Expect to check on first build: compile errors in new files, the unit tests, and PDF layout on a real device.
