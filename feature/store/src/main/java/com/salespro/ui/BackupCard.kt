package com.salespro.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.salespro.core.R
import com.salespro.settings.AppSettings
import com.salespro.utils.BackupManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** More > Backup & Restore: save all data to a file the user chooses, or restore from one. */
@Composable
fun BackupCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var lastBackupAt by remember { mutableStateOf(AppSettings.lastBackupAt) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                try {
                    BackupManager.exportBackup(context, uri)
                    lastBackupAt = AppSettings.lastBackupAt
                    toast(context.getString(R.string.backup_done))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    toast(context.getString(R.string.backup_failed, e.message ?: ""))
                } finally {
                    busy = false
                }
            }
        }
    }

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> pendingRestore = uri }

    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.backup_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(stringResource(R.string.backup_desc), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (lastBackupAt > 0L) {
                    stringResource(
                        R.string.backup_last,
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(lastBackupAt))
                    )
                } else {
                    stringResource(R.string.backup_never)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (lastBackupAt > 0L) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
            )

            if (busy) {
                Text(stringResource(R.string.backup_working), style = MaterialTheme.typography.bodyMedium)
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }

            Button(
                onClick = { createLauncher.launch(BackupManager.suggestedFileName()) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Backup, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.backup_create))
            }
            OutlinedButton(
                onClick = { openLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Restore, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.backup_restore))
            }
        }
    }

    pendingRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text(stringResource(R.string.restore_confirm_title)) },
            text = { Text(stringResource(R.string.restore_confirm_text)) },
            confirmButton = {
                Button(onClick = {
                    pendingRestore = null
                    busy = true
                    scope.launch {
                        try {
                            BackupManager.restoreBackup(context, uri)
                            BackupManager.restartApp(context)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: BackupManager.InvalidBackupException) {
                            toast(context.getString(R.string.restore_invalid))
                        } catch (e: BackupManager.NewerBackupException) {
                            toast(context.getString(R.string.restore_newer))
                        } catch (e: Exception) {
                            toast(context.getString(R.string.restore_failed, e.message ?: ""))
                        } finally {
                            busy = false
                        }
                    }
                }) {
                    Text(stringResource(R.string.restore_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
