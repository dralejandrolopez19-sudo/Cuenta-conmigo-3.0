package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.MutedSlate
import com.example.ui.viewmodel.AppTab

@Composable
fun CuentaConmigoBottomBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF7F6))
            .navigationBarsPadding()
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        BottomNavItem(
            tab = AppTab.INICIO,
            label = "Inicio",
            filledIcon = Icons.Filled.Home,
            outlinedIcon = Icons.Outlined.Home,
            isSelected = selectedTab == AppTab.INICIO,
            onClick = { onTabSelected(AppTab.INICIO) },
            modifier = Modifier.weight(1f)
        )
        BottomNavItem(
            tab = AppTab.DIARIO,
            label = "Diario",
            filledIcon = Icons.AutoMirrored.Filled.MenuBook,
            outlinedIcon = Icons.AutoMirrored.Outlined.MenuBook,
            isSelected = selectedTab == AppTab.DIARIO,
            onClick = { onTabSelected(AppTab.DIARIO) },
            modifier = Modifier.weight(1f)
        )
        BottomNavItem(
            tab = AppTab.ANIMO,
            label = "Ánimo",
            filledIcon = Icons.Filled.Mood,
            outlinedIcon = Icons.Outlined.Mood,
            isSelected = selectedTab == AppTab.ANIMO,
            onClick = { onTabSelected(AppTab.ANIMO) },
            modifier = Modifier.weight(1f)
        )
        BottomNavItem(
            tab = AppTab.PASTILLAS,
            label = "Pastillas",
            filledIcon = Icons.Filled.Medication,
            outlinedIcon = Icons.Outlined.Medication,
            isSelected = selectedTab == AppTab.PASTILLAS,
            onClick = { onTabSelected(AppTab.PASTILLAS) },
            modifier = Modifier.weight(1f)
        )
        BottomNavItem(
            tab = AppTab.HABITOS,
            label = "Hábitos",
            filledIcon = Icons.Filled.Spa,
            outlinedIcon = Icons.Outlined.Spa,
            isSelected = selectedTab == AppTab.HABITOS,
            onClick = { onTabSelected(AppTab.HABITOS) },
            modifier = Modifier.weight(1f)
        )
        BottomNavItem(
            tab = AppTab.AGENDA,
            label = "Agenda",
            filledIcon = Icons.Filled.CalendarMonth,
            outlinedIcon = Icons.Outlined.CalendarMonth,
            isSelected = selectedTab == AppTab.AGENDA,
            onClick = { onTabSelected(AppTab.AGENDA) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BottomNavItem(
    tab: AppTab,
    label: String,
    filledIcon: ImageVector,
    outlinedIcon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val activeColor = CoralPrimary
    val inactiveColor = MutedSlate

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 2.dp, vertical = 4.dp)
            .testTag("tab_${tab.name.lowercase()}")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isSelected) CoralPrimary.copy(alpha = 0.15f) else Color.Transparent)
                .padding(horizontal = 10.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = if (isSelected) filledIcon else outlinedIcon,
                contentDescription = label,
                tint = if (isSelected) activeColor else inactiveColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) activeColor else inactiveColor,
            maxLines = 1
        )
    }
}
