package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleLight
import com.example.ui.viewmodel.AiFeatureUiState

/**
 * Componente minimalista de orientación contextual en segundo plano.
 * Diseñado como una barra compacta de una sola superficie (sin cajas anidadas ni ruido visual),
 * que muestra un resumen breve y permite expandir el texto completo al tocarlo.
 */
@Composable
fun AiWellnessAssistantCard(
    title: String,
    subtitle: String = "",
    state: AiFeatureUiState,
    modifier: Modifier = Modifier,
    accentColor: Color = PurpleAccent,
    accentBgColor: Color = PurpleLight,
    isWarmReadingMode: Boolean = false,
    testTag: String = "ai_assistant_card"
) {
    var expanded by remember { mutableStateOf(false) }
    val cardBg = if (isWarmReadingMode) Color(0xFFFFF8EE) else Color.White
    val primaryText = if (isWarmReadingMode) Color(0xFF3B2A1A) else CharcoalText
    val secondaryText = if (isWarmReadingMode) Color(0xFF6E563E) else MutedSlate

    val messageText = when (state) {
        is AiFeatureUiState.Success -> state.text
        is AiFeatureUiState.Loading, AiFeatureUiState.Idle -> "Sincronizando orientación del día..."
        is AiFeatureUiState.Error -> state.message
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.14f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded }
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentBgColor.copy(alpha = 0.65f))
            ) {
                Icon(
                    imageVector = Icons.Default.Spa,
                    contentDescription = subtitle.ifBlank { title },
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = messageText,
                    fontSize = 12.sp,
                    color = if (state is AiFeatureUiState.Loading || state is AiFeatureUiState.Idle) {
                        secondaryText
                    } else {
                        primaryText
                    },
                    lineHeight = 16.sp,
                    maxLines = if (expanded) 10 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("${testTag}_content")
                )
            }
        }
    }
}
