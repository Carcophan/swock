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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.carcophan.swock.model.HealthRating

object HealthRatingVisuals {
    val GreenActive = Color(0xFF16A34A)
    val GreenBg = Color(0xFFDCFCE7)
    val GreenText = Color(0xFF15803D)

    val LimeActive = Color(0xFF65A30D)
    val LimeBg = Color(0xFFECFCCB)
    val LimeText = Color(0xFF3F6212)

    val YellowActive = Color(0xFFCA8A04)
    val YellowBg = Color(0xFFFEF9C3)
    val YellowText = Color(0xFF854D0E)

    val AmberActive = Color(0xFFD97706)
    val AmberBg = Color(0xFFFEF3C7)
    val AmberText = Color(0xFF92400E)

    val OrangeActive = Color(0xFFEA580C)
    val OrangeBg = Color(0xFFFFEDD5)
    val OrangeText = Color(0xFF9A3412)

    val RedActive = Color(0xFFDC2626)
    val RedBg = Color(0xFFFEE2E2)
    val RedText = Color(0xFFB91C1C)

    fun getActiveColor(rating: HealthRating): Color = when (rating) {
        HealthRating.A -> GreenActive
        HealthRating.B -> LimeActive
        HealthRating.C -> YellowActive
        HealthRating.D -> AmberActive
        HealthRating.E -> OrangeActive
        HealthRating.F -> RedActive
    }

    fun getBgColor(rating: HealthRating): Color = when (rating) {
        HealthRating.A -> GreenBg
        HealthRating.B -> LimeBg
        HealthRating.C -> YellowBg
        HealthRating.D -> AmberBg
        HealthRating.E -> OrangeBg
        HealthRating.F -> RedBg
    }

    fun getTextColor(rating: HealthRating): Color = when (rating) {
        HealthRating.A -> GreenText
        HealthRating.B -> LimeText
        HealthRating.C -> YellowText
        HealthRating.D -> AmberText
        HealthRating.E -> OrangeText
        HealthRating.F -> RedText
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
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HealthRating.values().forEach { rating ->
                val isActive = rating == activeRating
                val color = HealthRatingVisuals.getActiveColor(rating)
                Box(
                    modifier = Modifier
                        .size(if (isActive) 8.dp else 5.dp)
                        .clip(CircleShape)
                        .background(if (isActive) color else color.copy(alpha = 0.25f))
                )
            }
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
    val activeColor = HealthRatingVisuals.getActiveColor(rating)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .clip(CircleShape)
                    .background(activeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rating.grade,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    lineHeight = 9.sp,
                    style = LocalTextStyle.current.copy(
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        ),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        )
                    )
                )
            }
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
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            HealthRating.values().forEach { rating ->
                val isSelected = rating == selectedRating
                val activeColor = HealthRatingVisuals.getActiveColor(rating)
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) activeColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    animationSpec = tween(200),
                    label = "bgColor"
                )
                val contentColor = if (isSelected) Color.White else activeColor
                val borderModifier = if (isSelected) {
                    Modifier.border(2.dp, activeColor, RoundedCornerShape(10.dp))
                } else {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .then(borderModifier)
                        .clickable { onRatingSelected(rating) },
                    shape = RoundedCornerShape(10.dp),
                    color = bgColor
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = rating.grade,
                                fontWeight = FontWeight.ExtraBold,
                                color = contentColor,
                                fontSize = 16.sp
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Info strip explaining selected rating
        val selectedActiveColor = HealthRatingVisuals.getActiveColor(selectedRating)
        val selectedBgColor = HealthRatingVisuals.getBgColor(selectedRating)
        val selectedTextColor = HealthRatingVisuals.getTextColor(selectedRating)

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = selectedBgColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(selectedActiveColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectedRating.grade,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        lineHeight = 11.sp,
                        style = LocalTextStyle.current.copy(
                            platformStyle = PlatformTextStyle(
                                includeFontPadding = false
                            ),
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            )
                        )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "${selectedRating.label} (Stufe ${selectedRating.grade})",
                        color = selectedTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedRating.description,
                        color = selectedTextColor.copy(alpha = 0.9f),
                        fontSize = 11.sp
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
                    text = "Lebensmittel-Bewertung: Stufe ${rating.grade} (${rating.label})",
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
