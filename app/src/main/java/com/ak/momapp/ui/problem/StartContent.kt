package com.ak.momapp.ui.problem

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ak.momapp.i18n.LocalStrings
import com.ak.momapp.ui.icons.AppIcons
import com.ak.momapp.ui.icons.StartMark
import com.ak.momapp.ui.theme.LocalSkin
import com.ak.momapp.ui.theme.UiSkin

/** The halo behind an unsolved day's trophy. Gold in every palette. */
private val TrophyGold = Color(0xFFF5B301)

/**
 * The trophy's own colour, dark enough to hold against a gold disc.
 *
 * This can be a fixed ink again because the halo below is now close to
 * opaque. When the halo was a 10-40% wash it took its character from
 * whatever was behind it, so a fixed ink worked on the light palettes
 * and vanished on the dark ones; a solid gold disc looks the same in
 * both, and dark-on-gold is legible against either.
 */
private val TrophyInk = Color(0xFF4A2E00)

/**
 * The daily challenge lives behind the little trophy. A golden pulse
 * says "today's is waiting"; once done it rests, greyed out, until
 * tomorrow. Shared by the Start screen and the problem screen.
 */
@Composable
internal fun TrophyButton(challengeDone: Boolean, onClick: () -> Unit) {
    val strings = LocalStrings.current
    // Sanctioned skin branch, same reason as the start mark: Modern draws
    // its trophy so it takes the palette, Legacy keeps the emoji it shipped.
    val drawn = LocalSkin.current == UiSkin.MODERN
    IconButton(onClick = onClick) {
        if (challengeDone) {
            // Done for today: it rests rather than disappearing. Still
            // legible, because it is also how she gets back in to re-read
            // a chain she has already finished.
            TrophyMark(
                drawn = drawn,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .alpha(0.6f)
                    .semantics { contentDescription = strings.challengeTitle },
            )
        } else {
            val pulse = rememberInfiniteTransition(label = "trophy")
            val scale by pulse.animateFloat(
                initialValue = 1f,
                targetValue = 1.14f,
                animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
                label = "trophyScale",
            )
            // A solid gold disc that breathes, rather than a faint wash
            // that pulses. At 10-40% the halo barely registered against a
            // warm background and the trophy inside it read as grey.
            val glow by pulse.animateFloat(
                initialValue = 0.72f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
                label = "trophyGlow",
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(TrophyGold.copy(alpha = glow)),
            ) {
                TrophyMark(
                    drawn = drawn,
                    tint = TrophyInk,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .semantics { contentDescription = strings.challengeTitle },
                )
            }
        }
    }
}

/**
 * The trophy itself, drawn or set, so the pulsing and resting states
 * cannot drift apart by being written out twice.
 */
@Composable
private fun TrophyMark(drawn: Boolean, tint: Color, modifier: Modifier = Modifier) {
    if (drawn) {
        Icon(
            imageVector = AppIcons.Trophy,
            contentDescription = null,
            tint = tint,
            modifier = modifier.size(22.dp),
        )
    } else {
        Text(text = "🏆", modifier = modifier)
    }
}

/**
 * What greets her when she opens the app on her own: the usual top-row
 * buttons and one big Start. No problem until she asks for one.
 *
 * The day's tally used to sit under the greeting and no longer does. It
 * is one of the figures on the stats screen, which is where a number she
 * might want to go and look at belongs; here it was only ever something
 * else to read before pressing Start.
 */
@Composable
internal fun StartContent(
    challengeDone: Boolean,
    onStart: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenChallenge: () -> Unit,
    onOpenPractice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Scaffold(modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Flexible on the left, fixed icons on the right: the gear
                // keeps its place at any system font size. The day's tally
                // moved down under the greeting, where it has room to wrap.
                Spacer(Modifier.weight(1f))
                TrophyButton(challengeDone = challengeDone, onClick = onOpenChallenge)
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = strings.settingsIconDescription,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Sanctioned skin branch: this is a picture, not a token.
                // Legacy's abacus is an emoji drawn by the system font, so
                // it ignores the palette and differs on every phone; there
                // is no colour token that turns one into the other.
                if (LocalSkin.current == UiSkin.MODERN) {
                    StartMark()
                } else {
                    Text(text = "🧮", style = MaterialTheme.typography.displayLarge)
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = strings.readyLine,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                ) {
                    Text(strings.startButton, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(12.dp))
                // The quieter second option: drill one type instead of
                // taking the usual mixed break.
                FilledTonalButton(
                    onClick = onOpenPractice,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) {
                    Text(strings.practiceButton, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
