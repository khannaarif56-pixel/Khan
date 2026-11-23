package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DeepBronzeGold
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.HudSurface
import com.example.ui.theme.HudSurfaceBorder
import com.example.ui.theme.HudSurfaceElevated
import com.example.ui.theme.MoltenAmber
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RadiantChampagne
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextPrimaryGoldWhite
import com.example.ui.theme.TextSecondaryMuted

@Composable
fun JarvisHudBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    ObsidianBlack,
                    DeepNavy,
                    ObsidianBlack
                )
            )
        )
    ) {
        content()
    }
}

@Composable
fun JarvisTopHudBar(
    title: String,
    subtitle: String? = null,
    ownerDisplayName: String = "aarif khann",
    primaryLanguage: String,
    onToggleLanguage: () -> Unit,
    onBackClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("hud_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate Back",
                        tint = SolarGold
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = SolarGold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Bilingual Hindi / English instant toggle
            Surface(
                shape = CircleShape,
                color = HudSurfaceElevated,
                border = BorderStroke(1.dp, HudSurfaceBorder),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onToggleLanguage)
                    .testTag("language_toggle_pill")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Translate,
                        contentDescription = "Switch Language",
                        tint = SolarGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (primaryLanguage == "hi") "हिं" else "EN",
                        style = MaterialTheme.typography.labelMedium,
                        color = RadiantChampagne,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Small golden "aarif khann" text in the upper-right corner
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = ownerDisplayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = SolarGold,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("owner_badge_text")
                )
                Text(
                    text = "OWNER HUD",
                    style = MaterialTheme.typography.labelSmall,
                    color = MoltenAmber.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
fun HolographicCard(
    modifier: Modifier = Modifier,
    highlightBorder: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderColor = if (highlightBorder) SolarGold.copy(alpha = 0.78f) else HudSurfaceBorder
    val cardModifier = if (onClick != null) {
        modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    } else {
        modifier
    }
    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = HudSurface.copy(alpha = 0.9f)
        ),
        border = BorderStroke(if (highlightBorder) 1.4.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun HudSectionTitle(
    title: String,
    icon: ImageVector? = null,
    trailingText: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SolarGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = RadiantChampagne,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (!trailingText.isNullOrBlank()) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelSmall,
                color = SolarGold
            )
        }
    }
}

@Composable
fun GoldPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SolarGold,
            contentColor = ObsidianBlack,
            disabledContainerColor = DeepBronzeGold.copy(alpha = 0.5f),
            disabledContentColor = TextSecondaryMuted
        )
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GoldOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (enabled) SolarGold else DeepBronzeGold),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = RadiantChampagne
        )
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StatusPill(
    label: String,
    tint: Color = SolarGold,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.16f))
            .border(1.dp, tint.copy(alpha = 0.5f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
