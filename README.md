# NGO Loan Manager (Android)

Offline-first Android application for single-admin NGO loan ledger management.  
**Stack:** Kotlin · Jetpack Compose · Material 3 · Room/SQLite · deterministic flat-interest engine.

## Features

- Secure admin login / logout with **PBKDF2-HMAC-SHA256** password hashing (120 000 iterations, random salt)
- Borrower / member management
- Loan application, approval, disbursement and repayment tracking
- Installment schedules, outstanding balances and overdue records
- Oldest-first payment allocation, advance balance handling and payment cancellation
- Search, filters, financial summaries and audit log
- Input validation and reliable integer-poisha financial calculations
- Premium, responsive Material 3 UI with light / dark theme support
- PETRA Software Solutions transparent logo and "Developed by PETRA Software Solutions" credit (login screen, Settings > About)
- PETRA palette: Deep Navy #0F172A, Teal #14B8A6, Gold #D4AF37, Light Gray #F1F5F9
- **Backup & Restore** (Settings) via the Android file picker, with checksum validation
- **Loan receipt PDF** per loan: preview, save, print, share (e.g. WhatsApp)

## Backup & restore

Settings > **Data Backup & Restore**.

- **Create Backup** opens the system file picker (Storage Access Framework). Choose where to save `ngo-loan-backup-<date>.json` (device storage, Drive, USB...). The app re-reads and validates the saved file and then shows the timestamp; "Last backup" is remembered.
- **Restore from File** lets you pick a backup. The file is fully validated first (format, version, SHA-256 checksum, record counts, links between members/loans/payments, loan totals vs. installments). Invalid, corrupt, edited or incompatible files are rejected with a message and nothing changes.
- Before replacing data you must confirm in a dialog that shows backup vs. current counts. The app then saves a safety copy of the current data inside its private storage (last 3 kept) and restores in a single database transaction: on any error everything rolls back.

**Included:** members (including archived), loans, installments, payments (including cancelled), payment allocations, audit log, app settings (e.g. organization name).
**Excluded:** the admin username, salt and password hash. A restore never changes who can log in, and no password data is written to the file.
**Not encrypted:** store the backup file somewhere private. Backups from a different database schema version are refused.

## Loan receipt PDF, printing and sharing

Open a loan (Loans > tap a loan) and use the **Loan Receipt (PDF)** card.

1. **Generate Receipt** creates an A4 PDF on the device (no internet). It contains the NGO name (from Settings), PETRA branding, reference `LD-<loan code>`, member name/ID/phone/address (national ID masked to the last 4 characters), loan ID, principal, interest rate and type, interest, total payable, paid to date and outstanding balance (taken from the existing ledger), the full repayment schedule, an acknowledgement text in English and Bengali, signature/date lines, and the logged-in admin as authorized staff. Amounts come from stored loan values; nothing is recalculated or invented.
2. **Preview** shows the first pages in the app. **Save** writes the PDF where you choose. **Print** opens Android's print dialog (A4). **Share Receipt** opens the Android share sheet.
3. WhatsApp: choose WhatsApp in the share sheet, pick the recipient and press send yourself. The app never sends anything automatically and cannot know whether it was delivered. If WhatsApp is not installed, pick any other app from the sheet.

Receipts are stored only temporarily in the app cache (cleared after 24 h) and shared through a FileProvider with a temporary read grant. File names contain the loan code and date, never the member's name. The PDF is regenerated (not cached) after any payment change so balances are current.
Bengali text uses the device's built-in Bengali font; verify the rendering on your devices.

## Requirements

| Tool | Version |
|------|---------|
| JDK | 17 |
| Android Gradle Plugin | 8.7.3 |
| Gradle | 8.9 (wrapper included) |
| Android SDK | compileSdk / targetSdk 35, minSdk 26 |
| Android Studio | Hedgehog or newer recommended |

## Local build

```bash
# Clone or extract the project, then:
cd NGO-APK-main   # project root (contains settings.gradle.kts)

# Make wrapper executable (Unix)
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Lint
./gradlew lintDebug
```

APK output path:

```
app/build/outputs/apk/debug/app-debug.apk
```

Open the folder in Android Studio → “Sync Project with Gradle Files” → Run on emulator or device.

## Upload to GitHub

1. Create a new empty repository on GitHub (do **not** initialize with a README).
2. From the project root:

```bash
git init
git add .
git commit -m "Initial commit: NGO Loan Manager production build"
git branch -M main
git remote add origin https://github.com/YOUR_USER/YOUR_REPO.git
git push -u origin main
```

## GitHub Actions — APK build

Workflow file: `.github/workflows/android-apk.yml`

### Automatic triggers
- Every push to the `main` branch and every pull request
- Manual run via **Actions → Build NGO Loan Manager APK → Run workflow**

### What the workflow does
1. Checks out source
2. Sets up JDK 17 (Temurin) with Gradle cache
3. Installs Android SDK platform 35 + build-tools
4. Runs `./gradlew testDebugUnitTest`, then `./gradlew assembleDebug`
5. Locates the real APK under `app/build/outputs/apk`
6. Uploads the APK as artifact **NGO-Loan-Manager-debug-apk** (30-day retention)
7. On failure, uploads build reports for diagnosis

### Download the APK
1. Open the repository on GitHub
2. Go to **Actions**
3. Select the latest successful run of “Build NGO Loan Manager APK”
4. Scroll to **Artifacts**
5. Download **NGO-Loan-Manager-debug-apk**
6. Unzip and install the `.apk` on a compatible Android device (allow “Install from unknown sources”)

### Debug vs signed release
- The workflow produces a **debug** APK (no signing secrets required). Suitable for testing and internal distribution.
- A signed **release** APK is optional and requires GitHub Secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) plus additional workflow steps. Never commit keystores or passwords.

## Project structure

```
NGO-APK-main/
├── .github/workflows/android-apk.yml
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/ngoloanmanager/
│       │   ├── MainActivity.kt          # UI + navigation + auth
│       │   ├── data/                    # Room entities, DAO, DB
│       │   ├── domain/                  # Financial math & validation
│       │   └── ui/                      # Theme & reusable components
│       └── res/
│           ├── drawable-nodpi/petra_logo.png
│           ├── font/                    # Montserrat family
│           └── values/
├── gradle/wrapper/                      # Full wrapper (jar + properties)
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
├── FINANCIAL_RULES.md
└── README.md
```

## Security notes

- Passwords are **never** stored in plaintext.
- Database is local SQLite (Room). Encryption at rest is **not** enabled in this build.
- `android:allowBackup="false"` is set.
- No network permissions; the app is fully offline.
- Single-admin model — no multi-user role system.
- Do **not** use for live production financial operations until encrypted storage, session timeout and comprehensive on-device/end-to-end tests are done and verified (see Verification status in CHANGES.md).

## Known limitations

- No encrypted database / encrypted backup
- No CSV export; PDF exists only for the per-loan disbursement receipt
- No session timeout lock
- No full dashboard reconciliation for cancelled loans + advances
- No borrower edit / archive UI beyond basic flows
- Schema is v1 only (no migrations yet)
- Unit tests cover domain math, backup format/validation and receipt formatting; Room transactions, PDF rendering and Compose UI are not instrumented-tested

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `gradle-wrapper.jar` missing | Re-download from Gradle 8.9 distribution or run `gradle wrapper --gradle-version 8.9` |
| SDK license / platform errors | Accept licenses: `yes \| sdkmanager --licenses` |
| Out-of-memory during build | Increase `org.gradle.jvmargs` in `gradle.properties` |
| APK not found in Actions | Check the “Locate generated APK” step log; ensure `assembleDebug` succeeded |
| Install fails on device | Enable “Install unknown apps” for the file manager / browser |

## Financial representation

Amounts are stored as integer **poisha** (`100 poisha = ৳1`). Interest rates are percentage strings. See `FINANCIAL_RULES.md` for the exact calculation rules.

## License / attribution

Internal NGO use. PETRA Software Solutions logo included for branding on the authentication screen.
