package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JournalEntry
import com.example.ui.components.AiWellnessAssistantCard
import com.example.ui.components.PrivacyProtectedText
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.CoralLight
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleLight
import com.example.ui.theme.SoftCreamBackground
import com.example.ui.theme.TealSecondary
import com.example.ui.viewmodel.AppViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DiaryScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val entries by viewModel.journalEntries.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val searchQuery by viewModel.journalSearchQuery.collectAsState()
    val selectedCategory by viewModel.journalCategoryFilter.collectAsState()
    val isStreetModeActive by viewModel.isStreetModeActive.collectAsState()
    val isAiDisguiseEnabled by viewModel.isAiDisguiseEnabled.collectAsState()
    val aiAliases by viewModel.aiDisguiseAliases.collectAsState()
    val revealedIds by viewModel.temporarilyRevealedIds.collectAsState()
    val diaryAiState by viewModel.diaryAiReflectionState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    val categories = listOf("Todos", "Reflexión", "Gratitud", "Reestructuración", "Libre")

    val filteredEntries = entries.filter { entry ->
        val matchesCategory = selectedCategory == "Todos" || entry.category.equals(selectedCategory, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() ||
                entry.title.contains(searchQuery, ignoreCase = true) ||
                entry.content.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SoftCreamBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Cabecera Minimalista con botones compactos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Diario",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalText,
                    letterSpacing = (-0.4).sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showExportDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, TealSecondary.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = TealSecondary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("export_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Exportar a PDF",
                            modifier = Modifier.size(14.dp),
                            tint = TealSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PDF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealSecondary
                        )
                    }

                    Button(
                        onClick = { showDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("add_journal_header_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Escribir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chips de filtro compactos
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) CoralPrimary else Color.White)
                            .clickable { viewModel.setJournalCategoryFilter(cat) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else CharcoalText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            AiWellnessAssistantCard(
                title = "Reflexión Compasiva",
                subtitle = "Reestructuración amable automática de tus notas.",
                state = diaryAiState,
                accentColor = PurpleAccent,
                accentBgColor = PurpleLight,
                testTag = "diary_ai_assistant_card"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Lista de Notas o Estado Vacío Minimalista
            if (filteredEntries.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDialog = true }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp, horizontal = 20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E0),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sin notas registradas. Toca 'Escribir' para añadir tu primera reflexión.",
                            fontSize = 12.sp,
                            color = MutedSlate,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredEntries, key = { it.id }) { item ->
                        JournalEntryCard(
                            entry = item,
                            isStreetModeActive = isStreetModeActive,
                            isRevealed = revealedIds.contains("diary_${item.id}"),
                            isAiDisguiseEnabled = isAiDisguiseEnabled,
                            aiDisguiseText = aiAliases.diaryAlias,
                            onRevealClick = { viewModel.revealPrivacyItemTemporarily("diary_${item.id}") },
                            onDelete = { viewModel.deleteJournal(item.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showDialog = true },
            shape = CircleShape,
            containerColor = CoralPrimary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_journal_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Añadir pensamiento")
        }

        // Dialog
        if (showDialog) {
            NewJournalDialog(
                onDismiss = { showDialog = false },
                onSave = { title, content, cat ->
                    viewModel.saveJournal(title, content, cat)
                    showDialog = false
                }
            )
        }

        if (showExportDialog) {
            ExportJournalDialog(
                allEntries = entries,
                filteredEntries = filteredEntries,
                currentFilterLabel = selectedCategory,
                initialPatientName = profile?.displayName?.trim()?.ifBlank { null }
                    ?: Firebase.auth.currentUser?.displayName?.trim()?.ifBlank { null }
                    ?: "Dr. Oliver Hernández",
                onDismiss = { showExportDialog = false }
            )
        }
    }
}

@Composable
private fun JournalEntryCard(
    entry: JournalEntry,
    isStreetModeActive: Boolean,
    isRevealed: Boolean,
    isAiDisguiseEnabled: Boolean,
    aiDisguiseText: String,
    onRevealClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val formattedDate = remember(entry.createdAt) {
        entry.createdAt?.toDate()?.let { dateFormat.format(it) } ?: "Reciente"
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CoralLight)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isStreetModeActive && !isRevealed && isAiDisguiseEnabled) "Nota" else entry.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoralPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = MutedSlate
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar nota",
                            tint = MutedSlate.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            PrivacyProtectedText(
                realText = entry.title,
                itemId = "diary_${entry.id}",
                isStreetModeActive = isStreetModeActive,
                isRevealed = isRevealed,
                isAiDisguiseEnabled = isAiDisguiseEnabled,
                aiDisguiseText = aiDisguiseText,
                onRevealClick = { onRevealClick() },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = CharcoalText
            )

            Spacer(modifier = Modifier.height(4.dp))

            PrivacyProtectedText(
                realText = entry.content,
                itemId = "diary_${entry.id}",
                isStreetModeActive = isStreetModeActive,
                isRevealed = isRevealed,
                isAiDisguiseEnabled = isAiDisguiseEnabled,
                aiDisguiseText = "Contenido protegido en Modo Calle • Toca para previsualizar durante 5 segundos.",
                onRevealClick = { onRevealClick() },
                fontSize = 13.sp,
                color = CharcoalText.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun NewJournalDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, category: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Reflexión") }
    val categories = listOf("Reflexión", "Gratitud", "Reestructuración", "Libre")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nueva Reflexión",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = CharcoalText
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título de la reflexión") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Tipo de registro:",
                    fontSize = 12.sp,
                    color = MutedSlate
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    categories.forEach { cat ->
                        val isSelected = category == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) CoralPrimary else Color(0xFFEDF2F7))
                                .clickable { category = cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else CharcoalText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("¿Qué estás sintiendo o pensando?") },
                    placeholder = {
                        Text(
                            text = "Describe la situación, tu emoción inicial y qué perspectiva alternativa más compasiva encuentras...",
                            fontSize = 12.sp,
                            color = MutedSlate
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        onSave(title.trim(), content.trim(), category)
                    }
                },
                enabled = title.isNotBlank() && content.isNotBlank(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}
