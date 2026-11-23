package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.device.CoreVoiceState
import com.example.ui.theme.DeepBronzeGold
import com.example.ui.theme.HoloCyan
import com.example.ui.theme.MoltenAmber
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RadiantChampagne
import com.example.ui.theme.SolarGold
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GoldenHolographicCore(
    coreState: CoreVoiceState,
    amplitude: Float,
    modifier: Modifier = Modifier,
    coreSize: Dp = 236.dp,
    onCoreTap: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_core_rings")

    val outerRotationState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_ring"
    )
    val midRotationState = infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mid_ring"
    )
    val innerRotationState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_ring"
    )
    val pulseState = infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    val speedFactor = when (coreState) {
        CoreVoiceState.IDLE -> 1f
        CoreVoiceState.LISTENING -> 1.8f
        CoreVoiceState.THINKING -> 2.4f
        CoreVoiceState.SPEAKING -> 1.5f
    }
    val smoothAmpState = animateFloatAsState(
        targetValue = when (coreState) {
            CoreVoiceState.LISTENING -> amplitude.coerceIn(0.25f, 1f)
            CoreVoiceState.SPEAKING -> 0.72f
            CoreVoiceState.THINKING -> 0.45f
            CoreVoiceState.IDLE -> 0.12f
        },
        label = "smooth_amplitude"
    )

    val accentTint = when (coreState) {
        CoreVoiceState.LISTENING -> RadiantChampagne
        CoreVoiceState.THINKING -> HoloCyan
        CoreVoiceState.SPEAKING -> SolarGold
        CoreVoiceState.IDLE -> SolarGold
    }

    val statusText = when (coreState) {
        CoreVoiceState.IDLE -> "STANDBY • TAP CORE"
        CoreVoiceState.LISTENING -> "LISTENING..."
        CoreVoiceState.THINKING -> "THINKING..."
        CoreVoiceState.SPEAKING -> "SPEAKING..."
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(coreSize)
                .clip(CircleShape)
                .clickable(onClick = onCoreTap)
                .testTag("jarvis_holographic_core_button")
                .semantics { contentDescription = "JARVIS Golden AI Core, status $statusText" }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val outerRotation = outerRotationState.value
                val midRotation = midRotationState.value
                val innerRotation = innerRotationState.value
                val pulse = pulseState.value
                val baseAmp = smoothAmpState.value
                val smoothAmp = if (coreState == CoreVoiceState.SPEAKING) {
                    (0.55f + (pulse - 0.92f) * 2.2f).coerceIn(0.2f, 1f)
                } else {
                    baseAmp
                }

                val center = Offset(size.width / 2f, size.height / 2f)
                val maxR = size.minDimension / 2f

                // Outer golden radial holographic aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentTint.copy(alpha = 0.28f + smoothAmp * 0.2f),
                            MoltenAmber.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxR
                    ),
                    radius = maxR,
                    center = center
                )

                // Ring 1: Outer HUD degree ticks & arc segments
                rotate(degrees = (outerRotation * speedFactor) % 360f, pivot = center) {
                    val r1 = maxR * 0.92f
                    val arcRect = Size(r1 * 2f, r1 * 2f)
                    val topLeft = Offset(center.x - r1, center.y - r1)
                    for (segment in 0 until 4) {
                        drawArc(
                            color = SolarGold.copy(alpha = 0.65f),
                            startAngle = segment * 90f + 10f,
                            sweepAngle = 62f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcRect,
                            style = Stroke(width = 3.2f, cap = StrokeCap.Round)
                        )
                    }
                    // Telemetry degree notches
                    for (deg in 0 until 360 step 15) {
                        val rad = deg * PI / 180.0
                        val tickStart = r1 * 0.91f
                        val tickEnd = if (deg % 45 == 0) r1 * 0.99f else r1 * 0.95f
                        drawLine(
                            color = RadiantChampagne.copy(alpha = if (deg % 45 == 0) 0.8f else 0.35f),
                            start = Offset(
                                center.x + (cos(rad) * tickStart).toFloat(),
                                center.y + (sin(rad) * tickStart).toFloat()
                            ),
                            end = Offset(
                                center.x + (cos(rad) * tickEnd).toFloat(),
                                center.y + (sin(rad) * tickEnd).toFloat()
                            ),
                            strokeWidth = if (deg % 45 == 0) 2.6f else 1.3f
                        )
                    }
                }

                // Ring 2: Counter-rotating orbital ring
                rotate(degrees = (midRotation * speedFactor) % 360f, pivot = center) {
                    val r2 = maxR * 0.75f
                    val arcRect = Size(r2 * 2f, r2 * 2f)
                    val topLeft = Offset(center.x - r2, center.y - r2)
                    drawCircle(
                        color = DeepBronzeGold.copy(alpha = 0.45f),
                        radius = r2,
                        center = center,
                        style = Stroke(width = 1.5f)
                    )
                    for (i in 0 until 3) {
                        drawArc(
                            color = accentTint.copy(alpha = 0.85f),
                            startAngle = i * 120f + 12f,
                            sweepAngle = 72f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcRect,
                            style = Stroke(width = 5f, cap = StrokeCap.Round)
                        )
                    }
                }

                // Ring 3: Fast inner reactor ring with luminous nodes
                rotate(degrees = (innerRotation * speedFactor) % 360f, pivot = center) {
                    val r3 = maxR * 0.57f
                    val arcRect = Size(r3 * 2f, r3 * 2f)
                    val topLeft = Offset(center.x - r3, center.y - r3)
                    drawArc(
                        color = RadiantChampagne.copy(alpha = 0.9f),
                        startAngle = 0f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcRect,
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = MoltenAmber.copy(alpha = 0.8f),
                        startAngle = 180f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcRect,
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                    )
                    for (nodeDeg in listOf(0, 120, 240)) {
                        val rad = nodeDeg * PI / 180.0
                        drawCircle(
                            color = RadiantChampagne,
                            radius = 5f,
                            center = Offset(
                                center.x + (cos(rad) * r3).toFloat(),
                                center.y + (sin(rad) * r3).toFloat()
                            )
                        )
                    }
                }

                // Central glowing golden AI core sphere
                val coreRadius = maxR * (0.37f + smoothAmp * 0.06f) * pulse
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            RadiantChampagne,
                            SolarGold,
                            MoltenAmber.copy(alpha = 0.85f),
                            ObsidianBlack.copy(alpha = 0.95f)
                        ),
                        center = center,
                        radius = coreRadius
                    ),
                    radius = coreRadius,
                    center = center
                )
            }

            // Center icon overlay inside the core
            Icon(
                imageVector = when (coreState) {
                    CoreVoiceState.IDLE -> Icons.Filled.Mic
                    CoreVoiceState.LISTENING -> Icons.Filled.GraphicEq
                    CoreVoiceState.THINKING -> Icons.Filled.Psychology
                    CoreVoiceState.SPEAKING -> Icons.Filled.VolumeUp
                },
                contentDescription = null,
                tint = ObsidianBlack,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Animated golden voice-wave equalizer bar strip
        GoldenVoiceWaveBar(
            coreState = coreState,
            amplitudeProvider = { smoothAmpState.value },
            modifier = Modifier
                .fillMaxWidth(0.76f)
                .height(28.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Listening / Thinking / Speaking telemetry pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .clip(CircleShape)
                .background(accentTint.copy(alpha = 0.14f))
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(accentTint)
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = accentTint,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun GoldenVoiceWaveBar(
    coreState: CoreVoiceState,
    amplitude: Float = 0.2f,
    amplitudeProvider: (() -> Float)? = null,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "voice_wave_bars")
    val phaseState = transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Canvas(modifier = modifier) {
        val phase = phaseState.value
        val currentAmplitude = amplitudeProvider?.invoke() ?: amplitude
        val barCount = 29
        val spacing = size.width / barCount
        val centerIdx = barCount / 2f
        val activeMultiplier = when (coreState) {
            CoreVoiceState.IDLE -> 0.18f
            CoreVoiceState.LISTENING -> (0.45f + currentAmplitude * 0.55f).coerceIn(0.3f, 1f)
            CoreVoiceState.THINKING -> 0.55f
            CoreVoiceState.SPEAKING -> 0.8f
        }
        val barColor = if (coreState == CoreVoiceState.THINKING) HoloCyan else SolarGold

        for (i in 0 until barCount) {
            val distFromCenter = 1f - (kotlin.math.abs(i - centerIdx) / centerIdx)
            val wave = ((sin(phase + i * 0.55f) + 1f) * 0.5f)
            val heightFrac = (0.14f + distFromCenter * wave * activeMultiplier).coerceIn(0.12f, 1f)
            val barH = size.height * heightFrac
            val x = spacing * i + spacing * 0.5f
            val yTop = (size.height - barH) / 2f
            drawLine(
                color = barColor.copy(alpha = 0.35f + distFromCenter * 0.65f),
                start = Offset(x, yTop),
                end = Offset(x, yTop + barH),
                strokeWidth = (spacing * 0.42f).coerceAtLeast(2.5f),
                cap = StrokeCap.Round
            )
        }
    }
}
