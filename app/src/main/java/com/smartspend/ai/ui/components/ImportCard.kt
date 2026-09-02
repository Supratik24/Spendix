package com.smartspend.ai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartspend.ai.ui.model.ImportStatus

@Composable
fun ImportCard(
    status: ImportStatus,
    onEnable: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (status is ImportStatus.Complete) Icons.Rounded.CheckCircle else Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                when (status) {
                    ImportStatus.Idle -> {
                        Text("Turn on automatic tracking", fontWeight = FontWeight.Bold)
                        Text("Financial SMS stays on this device.", style = MaterialTheme.typography.bodySmall)
                    }
                    ImportStatus.Importing -> {
                        Text("Importing your transactions…", fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    is ImportStatus.Complete -> {
                        Text("Automatic tracking is ready", fontWeight = FontWeight.Bold)
                        Text(
                            buildString {
                                append("${status.found} new transaction${if (status.found == 1) "" else "s"} imported.")
                                if (status.merged > 0) append(" ${status.merged} duplicate${if (status.merged == 1) "" else "s"} merged.")
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    is ImportStatus.Failed -> {
                        Text("Automatic tracking needs attention", fontWeight = FontWeight.Bold)
                        Text(status.message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (status !is ImportStatus.Importing) {
                TextButton(onClick = onEnable) {
                    Text(
                        text = if (status is ImportStatus.Complete) "Sync" else "Enable",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
