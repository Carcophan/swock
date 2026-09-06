package de.carcophan.swock.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.carcophan.swock.model.HealthRating

object HealthRatingVisuals {
    val GreenActive = Color(0xFF16A34A)
    val GreenBg = Color(0xFFDCFCE7)
    val GreenText = Color(0xFF15803D)

    val YellowActive = Color(0xFFD97706)
    val YellowBg = Color(0xFFFEF3C7)
    val YellowText = Color(0xFFB45309)

    val RedActive = Color(0xFFDC2626)
    val RedBg = Color(0xFFFEE2E2)
    val RedText = Color(0xFFB91C1C)

    fun getActiveColor(rating: HealthRating): Color = when (rating) {
        HealthRating.GREEN -> GreenActive
        HealthRating.YELLOW -> YellowActive
        HealthRating.RED -> RedActive
    }

    fun getBgColor(rating: HealthRating): Color = when (rating) {
        HealthRating.GREEN -> GreenBg
        HealthRating.YELLOW -> YellowBg
        HealthRating.RED -> RedBg
    }

    fun getTextColor(rating: HealthRating): Color = when (rating) {
        HealthRating.GREEN -> GreenText
        HealthRating.YELLOW -> YellowText
        HealthRating.RED -> RedText
    }
}

@Composable
fun HealthTrafficLightMiniPill(
    activeRating: HealthRating,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E293B),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Green Dot
            val isGreen = activeRating == HealthRating.GREEN
            Box(
                modifier = Modifier
                    .size(if (isGreen) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (isGreen) HealthRatingVisuals.GreenActive else HealthRatingVisuals.GreenActive.copy(alpha = 0.25f))
            )
            // Yellow Dot
            val isYellow = activeRating == HealthRating.YELLOW
            Box(
                modifier = Modifier
                    .size(if (isYellow) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (isYellow) HealthRatingVisuals.YellowActive else HealthRatingVisuals.YellowActive.copy(alpha = 0.25f))
            )
            // Red Dot
            val isRed = activeRating == HealthRating.RED
            Box(
                modifier = Modifier
                    .size(if (isRed) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (isRed) HealthRatingVisuals.RedActive else HealthRatingVisuals.RedActive.copy(alpha = 0.25f))
            )
        }
    }
}

@Composable
fun HealthRatingBadge(
    rating: HealthRating,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    val bgColor = HealthRatingVisuals.getBgColor(rating)
    val textColor = HealthRatingVisuals.getTextColor(rating)
    val dotColor = HealthRatingVisuals.getActiveColor(rating)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            if (showLabel) {
                Spacer(modifier = Modifier.width(4.5.dp))
                Text(
                    text = rating.label,
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun HealthTrafficLightSelector(
    selectedRating: HealthRating,
    onRatingSelected: (HealthRating) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HealthRating.values().forEach { rating ->
            val isSelected = rating == selectedRating
            val activeColor = HealthRatingVisuals.getActiveColor(rating)
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) HealthRatingVisuals.getBgColor(rating) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                animationSpec = tween(200),
                label = "bgColor"
            )
            val borderModifier = if (isSelected) {
                Modifier.border(1.5.dp, activeColor, RoundedCornerShape(12.dp))
            } else {
                Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .then(borderModifier)
                    .clickable { onRatingSelected(rating) },
                shape = RoundedCornerShape(12.dp),
                color = bgColor
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) activeColor else activeColor.copy(alpha = 0.35f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = rating.label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) HealthRatingVisuals.getTextColor(rating) else MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun HealthExplanationBox(
    rating: HealthRating,
    reason: String,
    modifier: Modifier = Modifier
) {
    val bgColor = HealthRatingVisuals.getBgColor(rating)
    val activeColor = HealthRatingVisuals.getActiveColor(rating)
    val textColor = HealthRatingVisuals.getTextColor(rating)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor.copy(alpha = 0.85f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(activeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = activeColor,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "Ampel-Bewertung: ${rating.label}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                if (reason.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}
