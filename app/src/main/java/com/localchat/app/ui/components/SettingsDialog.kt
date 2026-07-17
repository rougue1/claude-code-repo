@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.localchat.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun SettingsDialog(
    currentHost: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var host by remember(currentHost) { mutableStateOf(currentHost) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ollama connection") },
        text = {
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                singleLine = true,
                label = { Text("Host") },
                placeholder = { Text("http://127.0.0.1:11434") },
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(host); onDismiss() }) { Text("Save & reconnect") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
