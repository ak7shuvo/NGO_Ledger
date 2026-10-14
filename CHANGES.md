# Premium UI polish (`feature/premium-ui-polish`)

**Verification status: NOT compiled or run locally.** The authoring environment had no Android SDK and could not reach
Google/Gradle/Maven hosts. Edits were syntax-checked with a Kotlin parser and reviewed by hand, but type/symbol errors,
unit tests and on-device behaviour are unverified until GitHub Actions (or Android Studio) builds this branch.
The baseline it builds on was itself never compiled (see the section below).

## Visual
- Palette moved to midnight navy / deep indigo / electric blue / teal / lavender on an off-white background, with matching dark theme.
  Existing constant names kept. Brand teal/gold are too light as text on white, so light mode uses darker `accentTeal()` / `accentGold()` / `accentSuccess()` helpers.
- Dashboard hero is now a blue-indigo-teal gradient with light pools, shows the existing *Total Outstanding (Including Interest)* figure, and the
  Payable/Advance figures sit in a card beneath it. No metric definitions changed.
- Money in the hero eases to new values; the final text always equals the stored amount, screen readers get the final amount, and
  animation is skipped when system animations are off.
- Cards: dark-theme-aware borders and indigo-tinted shadows. Buttons: spring press feedback. Text fields: filled surface.
- Login: gradient backdrop with soft glows, scrollable/IME-aware card, show/hide password, busy state on the button.
- Page switches crossfade. Member/Loan metric cards use vector icons.

## Confirmed bugs fixed (UI/workflow only)
- System Back on member/loan detail exited the app; it now returns to the previous screen (and to Home from other tabs).
- Login and password change ran PBKDF2 on the main thread and could be double-submitted; now off-main with a busy lock. Password hashes compared in constant time.
- Settings: saving the org name or changing the password had no error handling (an exception would crash); now handled, atomic, with
  success/error colouring. The top bar now refreshes after the org name changes, and an empty name is rejected.
- No field had a keyboard type: amount/rate now use decimal, term numeric, phone phone, passwords password keyboards.
- New Member dialog could clip on small screens; now scrollable.
- Garbled copy fixed ("first Member Add", "Loan Create", "Cancel" for a cancelled payment, etc.). Past-due installments read "Overdue".

## Not done (out of scope or unverifiable here)
Parallax, detail-screen transitions, bottom sheets, charts, bundled Noto Sans Bengali (Bengali still uses the system fallback),
reduced-motion beyond the system animation scale. Serious unrelated findings (plaintext backups, session auto-lock, PBKDF2 iteration count,
no overdue dashboard metric) are documented in `docs/TECHNICAL_AUDIT.md` and deliberately not changed here.

---

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
