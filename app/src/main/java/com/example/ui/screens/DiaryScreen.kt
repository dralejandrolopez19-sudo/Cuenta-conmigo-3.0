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
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.ui.components.PrivacyProtectedText
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.CoralLight
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.SoftCreamBackground
import com.example.ui.theme.TealLight
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
            // Header with Export to PDF button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Diario de Pensamientos",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText,
                        letterSpacing = (-0.4).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Registra tus pensamientos y practica la reestructuración cognitiva.",
                        fontSize = 13.sp,
                        color = MutedSlate,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                OutlinedButton(
                    onClick = { showExportDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, TealSecondary.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = TealSecondary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("export_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "Exportar a PDF",
                        modifier = Modifier.size(17.dp),
                        tint = TealSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PDF",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setJournalSearchQuery(it) },
                placeholder = {
                    Text(
                        text = "Buscar en tus notas y pensamientos...",
                        fontSize = 14.sp,
                        color = MutedSlate.copy(alpha = 0.8f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = MutedSlate
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CharcoalText.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color(0xFFCBD5E0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journal_search_field")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) CoralPrimary else Color(0xFFEDF2F7))
                            .clickable { viewModel.setJournalCategoryFilter(cat) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Color.White else CharcoalText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Content or Empty State
            if (filteredEntries.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E0),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedCategory != "Todos") {
                                "No se encontraron notas con el filtro seleccionado."
                            } else {
                                "Tu cuaderno de pensamientos está en blanco. Pulsa el botón '+' para registrar tu primera reflexión o pensamiento."
                            },
                            fontSize = 14.sp,
                            color = MutedSlate,
                            textAlign = TextAlign.Center,
                            lineHeight = 21.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
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
                        Spacer(modifier = Modifier.height(80.dp))
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
