package com.montanhajr.petclicker

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

private const val CLICKS_UNTIL_AD = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    selectedSound: Int,
    showInterstitialAd: () -> Unit
) {
    val context = LocalContext.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val imageColor = if (isPressed) Color.Red else Color.Gray

    val soundPool = remember {
        SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .build()
    }

    var soundId by remember { mutableStateOf<Int?>(null) }
    var soundLoaded by remember { mutableStateOf(false) }
    
    // Contador de cliques para o anúncio intersticial
    var clickCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedSound) {
        soundId?.let { soundPool.unload(it) }
        soundLoaded = false
        val newSoundId = soundPool.load(context, selectedSound, 1)
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0 && sampleId == newSoundId) {
                soundId = sampleId
                soundLoaded = true
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { soundPool.release() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    IconButton(
                        onClick = { navController.navigate(AppDestinations.SETTINGS_SCREEN) },
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_icon_content_description),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.dogclicker),
                contentDescription = stringResource(R.string.dog_clicker_image_content_description),
                colorFilter = ColorFilter.tint(imageColor),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(600.dp)
                    .alpha(0.5f)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        if (soundLoaded && soundId != null) {
                            soundPool.play(soundId!!, 1f, 1f, 1, 0, 1f)
                        }
                        
                        // Lógica do contador de cliques
                        clickCount++
                        if (clickCount >= CLICKS_UNTIL_AD) {
                            showInterstitialAd()
                            clickCount = 0
                        }
                    }
            )

            // Mensagem de aviso sobre o anúncio (aparece quando faltam 5 ou menos cliques)
            val clicksRemaining = CLICKS_UNTIL_AD - clickCount
            if (clicksRemaining <= 5) {
                Text(
                    text = pluralStringResource(R.plurals.ad_announcement, clicksRemaining, clicksRemaining),
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                // Espaço vazio para manter o layout estável
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
