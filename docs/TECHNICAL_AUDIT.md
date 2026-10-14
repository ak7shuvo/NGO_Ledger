# Technical Audit — NGO Loan Manager (baseline `d8a6830`)

Scope: read-only inspection of the repository at commit `d8a6830` (single commit, branch `main`, no tags).
Date: 2026-10-14. **Nothing in this audit was compiled, executed or tested** (see "Environment limits").

## 1. Environment limits (why this is an audit, not an implementation)

| Limit | Evidence | Effect |
|---|---|---|
| No Android SDK; SDK/Gradle/Maven hosts blocked | Egress proxy answered 403 to `dl.google.com`, `maven.google.com`, `services.gradle.org`, `repo.maven.apache.org` | Cannot compile, run unit tests, run lint or build an APK |
| JDK 21 installed, project targets 17 | `java -version` | Would need JDK 17 even if network were open |
| `GH_TOKEN` rejected | `gh auth status`: "The token in GH_TOKEN is invalid" | Cannot create a GitHub Release or upload an APK |
| No release signing configuration in the repo | No `signingConfigs` / `buildTypes` in Gradle files | A signed release APK needs a keystore supplied by the owner |

## 2. Baseline verified from source

- Package / application ID: `com.ngoloanmanager`; version `1.1.0` (code 2); minSdk 26, target/compile 35.
- Stack: Kotlin, Compose (BOM 2024.12.01), Material 3, Room 2.6.1 (KSP), Navigation Compose, coroutines. Wrapper pins Gradle 8.9; bytecode target JVM 17.
- 25 Kotlin files, ~3,550 lines; `MainActivity.kt` alone is 1,303 lines and holds most screens.
- Tests: 6 JVM unit-test files (38 `@Test` methods: allocator 11, financial summary 10, backup format 5, input validation 5, loan math 4, receipt format 3). No `androidTest` source set.
- `CHANGES.md` states the imported code was **never built or run** — the current `main` is itself unverified.
- CI (`.github/workflows/android-apk.yml`): runs `testDebugUnitTest` and `assembleDebug` on push to `main`, PRs and manual dispatch; debug APK only, no release job.

## 3. Financial engine — findings

Verified as sound by reading:
- Integer poisha throughout; rate handled as `BigDecimal`; `longValueExact` / `Math.addExact` fail loudly on overflow.
- `ANNUAL` and `FULL_TERM` interest, half-up rounding, remainder on the last installment — matches `FINANCIAL_RULES.md`.
- `PaymentRepository.recordPayment` / `cancelPayment`: payment row, reallocation, installment totals and audit row share one `db.withTransaction`. Cancel is guarded by an atomic `UPDATE ... WHERE cancelled = 0`; cancelled rows and their allocation rows are kept.
- `FinancialSummary`: per-loan `max(0, …)` for outstanding and advance, so one loan's advance never offsets another's balance; cancelled loans and payments excluded.

Gaps found:
1. **No portfolio-level overdue tracking.** The word "overdue" appears nowhere in `src`, and there is no overdue count/amount on the dashboard or reports. *Correction to the first revision of this audit:* the loan-detail installment list does flag a past-due unpaid installment (`dueDate < today`), originally labelled "Outstanding"; it is labelled "Overdue" on `feature/premium-ui-polish`.
2. **Loan status has only `ACTIVE` and `CANCELLED`.** No transition to a completed/paid state; "Paid" in the UI is derived per installment only.
3. **Duplicate-submission protection is unverified** for the payment and loan forms (repository has no idempotency key; receipt numbers are time + 4 random hex chars protected only by a unique index).
4. `LoanMath` overflow surfaces as `ArithmeticException`; whether the UI maps it to a friendly message was not traced.

## 4. Security — findings

| Area | Finding |
|---|---|
| Password hashing | PBKDF2WithHmacSHA256, 120,000 iterations, 256-bit output, 16-byte `SecureRandom` salt. Iteration count is below current OWASP guidance for PBKDF2-SHA256 (600,000). Changing it needs a per-record parameter/versioning so existing hashes still verify. |
| Hash comparison | Uses `==` on strings (not constant-time). Low practical risk locally; trivial to fix. |
| Session | `loggedIn` is `remember { mutableStateOf(false) }` in the composable: no auto-lock on background/timeout, and it resets on configuration change/process death. No `FLAG_SECURE`. |
| Recovery | No password-recovery path was found; a forgotten password has no reset flow. |
| Database | Plain SQLite via Room; not encrypted. `allowBackup="false"`. `exportSchema=false`, DB version 1, no migrations or migration tests. |
| Backup | Single JSON file, **plaintext** (includes national IDs, phones, addresses). The embedded SHA-256 detects corruption and casual edits but is **not authentication**: anyone can edit the payload and recompute the hash. No encryption option. |
| Restore | Strong: validate-then-replace in one Room transaction; format/schema version, count and referential/total checks; admin account excluded. Not covered: interrupted-restore recovery copy lifecycle beyond the transaction, and allowed values for `status`/`interestPeriod`. |
| Manifest | No permissions requested; only the launcher activity is exported; `FileProvider` not exported. |
| FileProvider / PDFs | Receipts in `cacheDir/receipts`, shared with a temporary read grant, cleaned after 24 h. Reasonable. |
| Logging | `Log.e("NGOLoan", …, e)` logs full stack traces; exception messages from SQLite could contain row data. |

## 5. UI / design — findings

- Montserrat (Regular/Medium/SemiBold/Bold, `.otf`) is already bundled in `res/font`. Bengali relies on the platform default fallback; no bundled Noto Sans Bengali.
- The current palette is the PETRA navy/teal/gold scheme (per `CHANGES.md`), not the palette proposed in the brief. Replacing it is a deliberate branding decision for the owner.
- A dashboard, receipts, backup UI and navigation shell exist; chart/trend widgets, overdue views and reduced-motion handling were not found.

## 6. Recommended order of work (once a build environment exists)

1. Get the baseline compiling and its 38 tests passing (CI or Android Studio). Fix whatever that reveals before any redesign.
2. Add portfolio-level overdue calculation (pure Kotlin + tests) and a completed-loan status rule.
3. Session auto-lock (lifecycle-aware), constant-time hash compare, PBKDF2 parameter versioning.
4. Backup: optional passphrase-based authenticated encryption (AES-GCM with a PBKDF2/scrypt-derived key), HMAC/AEAD tag instead of bare SHA-256, format version 2 with v1 import kept.
5. Room `exportSchema=true`, schema directory, migration tests.
6. Design-system work, split `MainActivity.kt` into per-screen files, then UI tests.
7. Release: owner-supplied keystore via GitHub Actions secrets, a tag-triggered release workflow, then publish.

## 7. What is not claimed

No test, build, lint, APK or device run was performed. No performance, accessibility or Bengali-rendering claims are made.
