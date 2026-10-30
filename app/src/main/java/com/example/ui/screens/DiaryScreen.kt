package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.JournalEntry
import com.example.ui.components.AiWellnessAssistantCard
import com.example.ui.components.WarmReadingHeaderToggle
import com.example.ui.components.WarmReadingPalette
import com.example.ui.components.WarmReadingScreenOverlay
import com.example.ui.components.WarmReadingStatusBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val journalCategories = listOf(
    "Todos",
    "Reflexión",
    "Gratitud",
    "Desahogo",
    "Logro",
    "Terapia"
)

@Composable
fun DiaryScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val entries by viewModel.journalEntries.collectAsStateWithLifecycle()
    val searchQuery by viewModel.journalSearchQuery.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.journalCategoryFilter.collectAsStateWithLifecycle()
    val showNewDialog by viewModel.showNewJournalDialog.collectAsStateWithLifecycle()
    val isStreetModeActive by viewModel.isStreetModeActive.collectAsStateWithLifecycle()
    val isAiDisguiseEnabled by viewModel.isAiDisguiseEnabled.collectAsStateWithLifecycle()
    val aiDisguiseAliases by viewModel.aiDisguiseAliases.collectAsStateWithLifecycle()
    val revealedIds by viewModel.temporarilyRevealedIds.collectAsStateWithLifecycle()
    val diaryAiState by viewModel.diaryAiReflectionState.collectAsStateWithLifecycle()

    // Modo de lectura cálido y brillo reducido
    val isReadingModeActive by viewModel.isReadingModeActive.collectAsStateWithLifecycle()
    val isReadingModeAutoByTime by viewModel.isReadingModeAutoByTime.collectAsStateWithLifecycle()
    val readingStartHour by viewModel.readingAutoStartHour.collectAsStateWithLifecycle()
    val readingEndHour by viewModel.readingAutoEndHour.collectAsStateWithLifecycle()
    val readingDimLevel by viewModel.readingDimLevel.collectAsStateWithLifecycle()
    val readingWarmthLevel by viewModel.readingWarmthLevel.collectAsStateWithLifecycle()

    var selectedEntryForDetail by remember { mutableStateOf<JournalEntry?>(null) }

    val filteredEntries = remember(entries, searchQuery, categoryFilter) {
        entries.filter { entry ->
            val matchesCategory = categoryFilter == "Todos" || entry.category.equals(categoryFilter, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                entry.title.contains(searchQuery, ignoreCase = true) ||
                entry.content.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    val screenBgColor = if (isReadingModeActive) {
        WarmReadingPalette.PaperBackground
    } else {
        SoftCreamBackground
    }
    val primaryTextColor = if (isReadingModeActive) {
        WarmReadingPalette.InkPrimary
    } else {
        CharcoalText
    }
    val secondaryTextColor = if (isReadingModeActive) {
        WarmReadingPalette.InkSecondary
    } else {
        MutedSlate
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBgColor)
            .testTag(if (isReadingModeActive) "diary_screen_warm_reading" else "diary_screen_normal")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Encabezado con acceso rápido al Modo de Lectura Cálido
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Diario",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor,
                        modifier = Modifier.testTag("diary_header_title")
                    )
                    Text(
                        text = "${entries.size} notas guardadas",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WarmReadingHeaderToggle(
                        isReadingModeActive = isReadingModeActive,
                        isAutoByTime = isReadingModeAutoByTime,
                        onToggleReadingMode = { viewModel.toggleReadingMode() },
                        onOpenSettings = { viewModel.openReadingModeDialog() },
                        tagPrefix = "diary_reading_mode"
                    )

                    Button(
                        onClick = { viewModel.showNewJournalDialog.value = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isReadingModeActive) WarmReadingPalette.AmberAccent else PurpleAccent
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("new_journal_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Escribir", fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }

            // Barra de estado del Modo Lectura Cálido cuando está activo
            WarmReadingStatusBar(
                isReadingModeActive = isReadingModeActive,
                isAutoByTime = isReadingModeAutoByTime,
                startHour = readingStartHour,
                endHour = readingEndHour,
                dimLevel = readingDimLevel,
                warmthLevel = readingWarmthLevel,
                onOpenSettings = { viewModel.openReadingModeDialog() },
                modifier = Modifier.padding(top = 8.dp),
                tagPrefix = "diary_reading_mode"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Buscador compacto adaptado al Modo Lectura
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setJournalSearchQuery(it) },
                placeholder = {
                    Text(
                        "Buscar en mi diario...",
                        color = secondaryTextColor.copy(alpha = 0.75f)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = if (isReadingModeActive) WarmReadingPalette.AmberAccent else MutedSlate
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setJournalSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = secondaryTextColor)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = if (isReadingModeActive) WarmReadingPalette.CardBackground else Color.White,
                    unfocusedContainerColor = if (isReadingModeActive) WarmReadingPalette.CardBackground else Color.White,
                    focusedBorderColor = if (isReadingModeActive) WarmReadingPalette.AmberAccent else PurpleAccent,
                    unfocusedBorderColor = if (isReadingModeActive) WarmReadingPalette.BorderColor else SoftBorder,
                    focusedTextColor = primaryTextColor,
                    unfocusedTextColor = primaryTextColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journal_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filtros simples en fila
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                journalCategories.forEach { category ->
                    val selected = categoryFilter == category
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setJournalCategoryFilter(category) },
                        label = {
                            Text(
                                category,
                                color = when {
                                    isReadingModeActive && selected -> WarmReadingPalette.InkPrimary
                                    isReadingModeActive -> WarmReadingPalette.InkSecondary
                                    else -> Color.Unspecified
                                }
                            )
                        },
                        colors = if (isReadingModeActive) {
                            FilterChipDefaults.filterChipColors(
                                containerColor = WarmReadingPalette.CardBackground,
                                selectedContainerColor = WarmReadingPalette.AmberSoft
                            )
                        } else {
                            FilterChipDefaults.filterChipColors()
                        },
                        modifier = Modifier.testTag("journal_filter_$category")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Reflexión contextual integrada en segundo plano
            AiWellnessAssistantCard(
                title = "Reflexión del diario",
                subtitle = "Síntesis de tus notas recientes",
                state = diaryAiState,
                accentColor = if (isReadingModeActive) WarmReadingPalette.AmberAccent else PurpleAccent,
                accentBgColor = if (isReadingModeActive) WarmReadingPalette.AmberSoft else PurpleLight,
                isWarmReadingMode = isReadingModeActive,
                testTag = "diary_ai_card"
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = secondaryTextColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "Aún no hay notas en esta categoría",
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryTextColor
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredEntries, key = { it.id }) { entry ->
                        val isEntryRevealed = !isStreetModeActive || revealedIds.contains("diary_${entry.id}")
                        JournalEntryCard(
                            entry = entry,
                            isMasked = !isEntryRevealed,
                            isReadingModeActive = isReadingModeActive,
                            disguisedTitle = if (isAiDisguiseEnabled) aiDisguiseAliases.diaryAlias else "Nota personal protegida",
                            onRevealTemporarily = { viewModel.revealPrivacyItemTemporarily("diary_${entry.id}") },
                            onClick = { selectedEntryForDetail = entry },
                            onDelete = { viewModel.deleteJournal(entry.id) }
                        )
                    }
                }
            }
        }

        // Filtro cálido ámbar y reducción de brillo sobre el Diario
        WarmReadingScreenOverlay(
            isReadingModeActive = isReadingModeActive,
            dimLevel = readingDimLevel,
            warmthLevel = readingWarmthLevel
        )
    }

    if (showNewDialog) {
        NewJournalEntryDialog(
            isReadingModeActive = isReadingModeActive,
            onDismiss = { viewModel.showNewJournalDialog.value = false },
            onSave = { title, content, category ->
                viewModel.saveJournal(title, content, category)
                viewModel.showNewJournalDialog.value = false
            }
        )
    }

    selectedEntryForDetail?.let { entry ->
        JournalDetailDialog(
            entry = entry,
            isReadingModeActive = isReadingModeActive,
            onDismiss = { selectedEntryForDetail = null },
            onDelete = {
                viewModel.deleteJournal(entry.id)
                selectedEntryForDetail = null
            }
        )
    }
}

@Composable
private fun JournalEntryCard(
    entry: JournalEntry,
    isMasked: Boolean = false,
    isReadingModeActive: Boolean = false,
    disguisedTitle: String = "Nota personal protegida",
    onRevealTemporarily: () -> Unit = {},
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(entry.createdAt) {
        val dateObj = entry.createdAt?.toDate() ?: Date()
        SimpleDateFormat("d MMM yyyy • HH:mm", Locale.forLanguageTag("es-ES")).format(dateObj)
    }

    val cardBgColor = if (isReadingModeActive) {
        WarmReadingPalette.CardBackground
    } else {
        Color.White
    }
    val titleColor = if (isReadingModeActive) {
        WarmReadingPalette.InkPrimary
    } else {
        CharcoalText
    }
    val bodyColor = if (isReadingModeActive) {
        WarmReadingPalette.InkSecondary
    } else {
        MutedSlate
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = if (isReadingModeActive) BorderStroke(1.dp, WarmReadingPalette.BorderColor) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isReadingModeActive) 0.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isMasked) onRevealTemporarily() else onClick()
            }
            .testTag("journal_card_${entry.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isReadingModeActive) WarmReadingPalette.AmberAccent.copy(alpha = 0.15f)
                                else PurpleLight
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isMasked) "Privado" else entry.category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isReadingModeActive) WarmReadingPalette.AmberAccent else PurpleAccent
                        )
                    }
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = bodyColor
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("delete_journal_${entry.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Eliminar",
                        tint = bodyColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = if (isMasked) disguisedTitle else entry.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )

            Text(
                text = if (isMasked) "Toca para revelar durante 5 segundos" else entry.content,
                style = MaterialTheme.typography.bodyMedium,
                color = bodyColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun JournalDetailDialog(
    entry: JournalEntry,
    isReadingModeActive: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(entry.createdAt) {
        val dateObj = entry.createdAt?.toDate() ?: Date()
        SimpleDateFormat("EEEE d 'de' MMMM yyyy, HH:mm", Locale.forLanguageTag("es-ES")).format(dateObj)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isReadingModeActive) WarmReadingPalette.CardBackground else Color.White,
            border = if (isReadingModeActive) BorderStroke(1.dp, WarmReadingPalette.BorderColor) else null,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (isReadingModeActive) WarmReadingPalette.AmberAccent.copy(alpha = 0.16f) else PurpleLight,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = entry.category,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isReadingModeActive) WarmReadingPalette.AmberAccent else PurpleAccent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = if (isReadingModeActive) WarmReadingPalette.InkSecondary else MutedSlate
                        )
                    }
                }

                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isReadingModeActive) WarmReadingPalette.InkPrimary else CharcoalText
                )

                Text(
                    text = dateStr.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isReadingModeActive) WarmReadingPalette.InkSecondary else MutedSlate
                )

                HorizontalDivider(
                    color = if (isReadingModeActive) WarmReadingPalette.BorderColor else SoftBorder
                )

                Text(
                    text = entry.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isReadingModeActive) WarmReadingPalette.InkPrimary else CharcoalText
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = CoralDark)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Eliminar")
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isReadingModeActive) WarmReadingPalette.AmberAccent else PurpleAccent
                        )
                    ) {
                        Text("Cerrar", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun NewJournalEntryDialog(
    isReadingModeActive: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Reflexión") }

    val categories = remember { journalCategories.filter { it != "Todos" } }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isReadingModeActive) WarmReadingPalette.CardBackground else Color.White,
            border = if (isReadingModeActive) BorderStroke(1.dp, WarmReadingPalette.BorderColor) else null,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Nueva nota en el diario",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isReadingModeActive) WarmReadingPalette.InkPrimary else CharcoalText
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            modifier = Modifier.testTag("new_journal_cat_$cat")
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_title_input")
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Escribe cómo te sientes hoy...") },
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_content_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank() && content.isNotBlank()) {
                                onSave(title.trim(), content.trim(), category)
                            }
                        },
                        enabled = title.isNotBlank() && content.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isReadingModeActive) WarmReadingPalette.AmberAccent else PurpleAccent
                        ),
                        modifier = Modifier.testTag("save_journal_button")
                    ) {
                        Text("Guardar", color = Color.White)
                    }
                }
            }
        }
    }
}
