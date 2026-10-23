package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.JournalEntry
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.TealLight
import com.example.ui.theme.TealSecondary
import com.example.util.PdfExportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ExportScope {
    ALL,
    CURRENT_FILTER
}

@Composable
fun ExportJournalDialog(
    allEntries: List<JournalEntry>,
    filteredEntries: List<JournalEntry>,
    currentFilterLabel: String,
    initialPatientName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var patientName by remember { mutableStateOf(initialPatientName) }
    var selectedScope by remember { mutableStateOf(ExportScope.ALL) }
    var customNote by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    val entriesToExport = if (selectedScope == ExportScope.ALL) allEntries else filteredEntries
    val scopeLabel = if (selectedScope == ExportScope.ALL) "Todas las entradas" else "Filtro: $currentFilterLabel"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .testTag("export_pdf_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(TealLight)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = TealSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Exportar a PDF",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalText
                            )
                            Text(
                                text = "Para profesionales de la salud",
                                fontSize = 12.sp,
                                color = MutedSlate
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MutedSlate
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info banner for therapy/clinical context
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF0FDF4))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF15803D),
                            modifier = Modifier
                                .size(18.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Genera un informe con tus reflexiones, reestructuraciones cognitivas y notas para compartirlo de forma segura con tu terapeuta o médico tratante.",
                            fontSize = 12.sp,
                            color = Color(0xFF166534),
                            lineHeight = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (allEntries.isEmpty()) {
                    Text(
                        text = "Aún no tienes entradas registradas en tu diario. Registra tu primera nota de pensamiento para poder exportarla a PDF.",
                        fontSize = 13.sp,
                        color = MutedSlate,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Entendido")
                    }
                } else {
                    // Scope Selection
                    Text(
                        text = "Seleccionar registros a exportar",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CharcoalText
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Option 1: All entries
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedScope = ExportScope.ALL }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedScope == ExportScope.ALL,
                            onClick = { selectedScope = ExportScope.ALL },
                            colors = RadioButtonDefaults.colors(selectedColor = TealSecondary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Todas las entradas (${allEntries.size} registros)",
                            fontSize = 13.sp,
                            color = CharcoalText
                        )
                    }

                    // Option 2: Current filter if applied
                    if (currentFilterLabel != "Todos") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedScope = ExportScope.CURRENT_FILTER }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedScope == ExportScope.CURRENT_FILTER,
                                onClick = { selectedScope = ExportScope.CURRENT_FILTER },
                                colors = RadioButtonDefaults.colors(selectedColor = TealSecondary)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Solo categoría '$currentFilterLabel' (${filteredEntries.size} registros)",
                                fontSize = 13.sp,
                                color = CharcoalText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Patient Name Field
                    Text(
                        text = "Nombre que figurará en el informe",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CharcoalText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = patientName,
                        onValueChange = { patientName = it },
                        singleLine = true,
                        placeholder = { Text("Ej: Tu nombre completo") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealSecondary,
                            unfocusedBorderColor = Color(0xFFCBD5E0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_patient_name_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Optional note for healthcare provider
                    Text(
                        text = "Mensaje o nota opcional para el profesional",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CharcoalText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customNote,
                        onValueChange = { customNote = it },
                        maxLines = 3,
                        placeholder = { Text("Ej: Notas para la sesión del jueves sobre manejo de ansiedad...") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealSecondary,
                            unfocusedBorderColor = Color(0xFFCBD5E0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_note_input")
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isGenerating) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = TealSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Compilando archivo PDF...",
                                fontSize = 13.sp,
                                color = MutedSlate
                            )
                        }
                    } else {
                        // Action Buttons
                        Button(
                            onClick = {
                                if (entriesToExport.isEmpty()) {
                                    Toast.makeText(context, "No hay entradas seleccionadas para exportar", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isGenerating = true
                                coroutineScope.launch {
                                    val file = withContext(Dispatchers.IO) {
                                        PdfExportHelper.generateJournalPdf(
                                            context = context,
                                            patientName = patientName.ifBlank { "Paciente" },
                                            entries = entriesToExport,
                                            filterLabel = scopeLabel,
                                            customNote = customNote.trim()
                                        )
                                    }
                                    isGenerating = false
                                    if (file != null) {
                                        PdfExportHelper.sharePdf(context, file, patientName)
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "No se pudo generar el PDF", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_share_pdf_professional")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Compartir con profesional",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                if (entriesToExport.isEmpty()) {
                                    Toast.makeText(context, "No hay entradas seleccionadas para exportar", Toast.LENGTH_SHORT).show()
                                    return@OutlinedButton
                                }
                                isGenerating = true
                                coroutineScope.launch {
                                    val file = withContext(Dispatchers.IO) {
                                        PdfExportHelper.generateJournalPdf(
                                            context = context,
                                            patientName = patientName.ifBlank { "Paciente" },
                                            entries = entriesToExport,
                                            filterLabel = scopeLabel,
                                            customNote = customNote.trim()
                                        )
                                    }
                                    isGenerating = false
                                    if (file != null) {
                                        PdfExportHelper.openPdf(context, file)
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "No se pudo generar el PDF", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_open_pdf_preview")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = TealSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ver / Abrir documento PDF",
                                fontSize = 13.sp,
                                color = CharcoalText,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
