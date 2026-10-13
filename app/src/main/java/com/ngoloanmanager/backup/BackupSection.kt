package com.ngoloanmanager.backup

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ngoloanmanager.data.BackupFormat
import com.ngoloanmanager.data.BackupManager
import com.ngoloanmanager.data.LoanDatabase
import com.ngoloanmanager.domain.UserFacingException
import com.ngoloanmanager.ui.components.GlassCard
import com.ngoloanmanager.ui.components.PremiumOutlinedButton
import com.ngoloanmanager.ui.components.PremiumPrimaryButton
import com.ngoloanmanager.ui.theme.DialogShape
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

private fun readLimited(input: InputStream, max: Int): ByteArray {
    val out = ByteArrayOutputStream()
    val buf = ByteArray(16 * 1024)
    while (true) {
        val n = input.read(buf)
        if (n < 0) break
        out.write(buf, 0, n)
        if (out.size() > max) throw UserFacingException("The selected file is too large to be a valid backup")
    }
    return out.toByteArray()
}

private fun versionName(context: Context): String =
    try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0" } catch (e: Exception) { "1.0.0" }

@Composable
fun BackupSection(db: LoanDatabase) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember(db) { BackupManager(db) }
    val prefs = remember { context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE) }
    var lastBackup by remember { mutableStateOf(prefs.getString("last_backup", null)) }
    var lastRestore by remember { mutableStateOf(prefs.getString("last_restore", null)) }
    var message by remember { mutableStateOf("") }
    var ok by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<BackupFormat.Parsed?>(null) }
    var currentCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    fun fail(tag: String, e: Throwable, fallback: String) {
        if (e !is UserFacingException) android.util.Log.e("NGOLoan", tag, e)
        ok = false; message = if (e is UserFacingException) (e.message ?: fallback) else fallback
    }

    val createLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true; message = ""
        scope.launch {
            try {
                val (bytes, snap) = manager.createBackup(versionName(context))
                withContext(Dispatchers.IO) {
                    val out = context.contentResolver.openOutputStream(uri, "wt") ?: throw UserFacingException("Cannot write to the chosen location")
                    out.use { it.write(bytes); it.flush() }
                    // Read the file back and validate it, so "success" means the saved file is really restorable.
                    val back = context.contentResolver.openInputStream(uri)?.use { readLimited(it, BackupFormat.MAX_BYTES) }
                        ?: throw UserFacingException("Backup was written but could not be read back")
                    BackupFormat.decode(back)
                }
                val stamp = LocalDateTime.now().format(STAMP)
                prefs.edit().putString("last_backup", stamp).apply()
                lastBackup = stamp
                ok = true
                message = "Backup saved and verified at $stamp (${snap.loans.size} loans, ${snap.borrowers.size} members, ${snap.payments.size} payments)"
            } catch (e: CancellationException) { throw e } catch (e: Exception) { fail("backup", e, "Backup failed. Nothing was changed; try another location.") }
            finally { busy = false }
        }
    }

    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true; message = ""
        scope.launch {
            try {
                val parsed = withContext(Dispatchers.IO) {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { readLimited(it, BackupFormat.MAX_BYTES) }
                        ?: throw UserFacingException("Cannot open the selected file")
                    BackupFormat.decode(bytes)
                }
                currentCounts = manager.currentCounts()
                pending = parsed
            } catch (e: CancellationException) { throw e } catch (e: Exception) { fail("restore-validate", e, "The selected file could not be read as a backup") }
            finally { busy = false }
        }
    }

    GlassCard {
        Text("Data Backup & Restore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Included: members (incl. archived), loans, installments, payments (incl. cancelled), allocations, audit log, organization settings.\n" +
                "Not included: the admin username/password - a restore never changes your login.\n" +
                "The file is not encrypted: keep it somewhere private.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text("Last backup: ${lastBackup ?: "never"}", style = MaterialTheme.typography.bodySmall)
        Text("Last restore: ${lastRestore ?: "never"}", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        PremiumPrimaryButton("Create Backup", { createLauncher.launch("ngo-loan-backup-${LocalDateTime.now().format(FILE_STAMP)}.json") }, Modifier.fillMaxWidth(), loading = busy)
        Spacer(Modifier.height(8.dp))
        PremiumOutlinedButton("Restore from File", { openLauncher.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth(), enabled = !busy)
        if (message.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
    }

    pending?.let { p ->
        val s = p.snapshot
        AlertDialog(
            onDismissRequest = { if (!busy) pending = null },
            shape = DialogShape,
            title = { Text("Replace all current data?") },
            text = {
                Text(
                    "Backup created: ${p.createdAt.take(16).replace('T', ' ')} UTC\n" +
                        "Backup contains: ${s.borrowers.size} members, ${s.loans.size} loans, ${s.payments.size} payments.\n" +
                        "Currently on this device: ${currentCounts["borrowers"] ?: 0} members, ${currentCounts["loans"] ?: 0} loans, ${currentCounts["payments"] ?: 0} payments.\n\n" +
                        "Everything currently stored will be replaced. A safety copy of the current data is saved inside the app first. This cannot be undone from the screen."
                )
            },
            confirmButton = {
                PremiumPrimaryButton("Replace Data", {
                    busy = true
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                val (bytes, _) = manager.createBackup(versionName(context))
                                val dir = File(context.filesDir, "backups").apply { mkdirs() }
                                File(dir, "pre-restore-${LocalDateTime.now().format(FILE_STAMP)}.json").writeBytes(bytes)
                                dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(3)?.forEach { it.delete() }
                            }
                            manager.restore(p)
                            val stamp = LocalDateTime.now().format(STAMP)
                            prefs.edit().putString("last_restore", stamp).apply()
                            lastRestore = stamp
                            ok = true; message = "Restore completed at $stamp"
                        } catch (e: CancellationException) { throw e } catch (e: Exception) {
                            fail("restore", e, "Restore failed. Your existing data was not changed.")
                        } finally { busy = false; pending = null }
                    }
                }, loading = busy)
            },
            dismissButton = { TextButton(onClick = { pending = null }, enabled = !busy) { Text("Cancel") } }
        )
    }
}
