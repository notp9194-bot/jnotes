package com.notp9194bot.jnotes.ui.templates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.notp9194bot.jnotes.data.model.Template
import com.notp9194bot.jnotes.domain.Templates

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesSheet(
    onDismiss: () -> Unit,
    onPickBlank: () -> Unit,
    onPickTemplate: (Template) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Choose a template", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            for (t in Templates.all) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (t.id == "blank") onPickBlank() else onPickTemplate(t) }
                        .padding(vertical = 12.dp),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                    Column {
                        Text(t.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            t.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
