package com.montanhajr.petclicker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    isDarkTheme: Boolean,
    selectedSound: Int,
    onThemeChange: (Boolean) -> Unit,
    onSoundSelected: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_button_content_description)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.choose_sound_title), style = MaterialTheme.typography.titleMedium)

            val sounds = listOf(
                Triple(stringResource(R.string.sound_option_one_card_title), Icons.Filled.MusicNote, R.raw.clicker1),
                Triple(stringResource(R.string.sound_option_two_card_title), Icons.Filled.MusicNote, R.raw.clicker2),
                Triple(stringResource(R.string.sound_option_three_card_title), Icons.Filled.MusicNote, R.raw.clicker3)
            )

            sounds.forEach { (title, icon, soundRes) ->
                SoundOptionCard(
                    title = title,
                    icon = icon,
                    isSelected = selectedSound == soundRes
                ) {
                    onSoundSelected(soundRes)
                }
            }

            HorizontalDivider(Modifier.padding(horizontal = 48.dp, vertical = 24.dp))

            Text(stringResource(R.string.appearance_title), style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.dark_mode_switch))
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = onThemeChange
                )
            }
        }
    }
}


@Composable
fun SoundOptionCard(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = MaterialTheme.shapes.large

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .then(
                if (isSelected) Modifier.border(
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    shape
                ) else Modifier
            )
            .clip(shape)
            .clickable { onClick() },
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Filled.Check else icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
            Text(
                text = title,
                style = if (isSelected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
