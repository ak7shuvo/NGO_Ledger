package com.ngoloanmanager.ui.components

import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ngoloanmanager.ui.theme.*

/** True when the user turned system animations off (Developer options / accessibility "remove animations"). */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        try {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Money text that eases toward a new value. The animation only affects the interim frames:
 * the final text is always exactly [format] of [poisha], and screen readers always get the final amount.
 */
@Composable
fun AnimatedMoneyText(
    poisha: Long,
    format: (Long) -> String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    val reduced = rememberReducedMotion()
    var shown by remember { mutableLongStateOf(poisha) }
    LaunchedEffect(poisha, reduced) {
        val from = shown
        if (reduced || from == poisha) {
            shown = poisha
        } else {
            animate(0f, 1f, animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)) { f, _ ->
                shown = if (f >= 1f) poisha else from + ((poisha - from).toDouble() * f).toLong()
            }
            shown = poisha
        }
    }
    Text(
        text = format(shown),
        style = style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.semantics { contentDescription = format(poisha) }
    )
}

/** Soft elevated card with subtle depth — restrained 3D, not toy-like */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 2.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = CardShape
    val dark = isDarkSurface()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cardScale"
    )
    Surface(
        modifier = modifier
            .scale(scale)
            .shadow(elevation, shape, clip = false, ambientColor = BrandIndigo.copy(alpha = 0.10f), spotColor = BrandIndigo.copy(alpha = 0.16f))
            .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick) else Modifier),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            Modifier
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (dark) 0.8f else 0.55f)
                        )
                    ),
                    shape
                )
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface.copy(alpha = 0.985f))
                    )
                )
                .padding(16.dp)
                .animateContentSize(),
            content = content
        )
    }
}

/** Premium metric summary card for dashboard */
@Composable
fun MetricCard(
    title: String,
    value: String,
    symbol: String = "৳",
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    icon: ImageVector? = null
) {
    GlassCard(modifier = modifier, elevation = 3.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(accent.copy(alpha = 0.18f), accent.copy(alpha = 0.06f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                } else {
                    Text(
                        symbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    value,
                    style = MoneyStyleSmall,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Hero gradient header card. [body] is an optional slot below the subtitle (e.g. a headline amount);
 * [trailing] stays the last parameter so existing trailing-lambda call sites are unchanged.
 */
@Composable
fun HeroCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    body: (@Composable ColumnScope.() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(24.dp)
    val reduced = rememberReducedMotion()
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    // One short entrance only (no infinite animation): glows drift into place and fade in.
    val settle by animateFloatAsState(
        targetValue = if (started || reduced) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "heroSettle"
    )
    Box(
        modifier
            .fillMaxWidth()
            .shadow(12.dp, shape, clip = false, ambientColor = BrandIndigo.copy(0.30f), spotColor = BrandNavy.copy(0.34f))
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0.00f to BrandNavy,
                        0.50f to BrandIndigo,
                        0.84f to Color(0xFF3B63D6),
                        1.00f to Color(0xFF1FA59A)
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
    ) {
        // Soft light pools for depth; decorative and non-interactive.
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset((28f + (1f - settle) * 40f).dp.roundToPx(), (-38f - (1f - settle) * 30f).dp.roundToPx()) }
                .graphicsLayer { alpha = settle }
                .size(164.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.02f), Color.Transparent)
                    )
                )
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset { IntOffset((-56f - (1f - settle) * 24f).dp.roundToPx(), (64f + (1f - settle) * 24f).dp.roundToPx()) }
                .graphicsLayer { alpha = settle }
                .size(190.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(BrandLavender.copy(alpha = 0.26f), BrandLavender.copy(alpha = 0.02f), Color.Transparent)
                    )
                )
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(76.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.02f), Color.Transparent)
                    )
                )
        )
        Row(
            Modifier
                .fillMaxWidth()
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.06f))), shape)
                .padding(22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.90f))
                if (body != null) {
                    Spacer(Modifier.height(14.dp))
                    body()
                }
            }
            trailing?.invoke()
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null, action: @Composable (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        action?.invoke()
    }
}

@Composable
fun EmptyStateView(message: String, icon: String = "◎") {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatusChip(text: String, active: Boolean = true) {
    Surface(
        shape = PillShape,
        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PremiumPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "btn"
    )
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .scale(scale)
            .height(52.dp)
            .shadow(if (enabled && !loading) 5.dp else 0.dp, ButtonShape, clip = false, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.26f)),
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandTeal,
            contentColor = BrandNavy,
            disabledContainerColor = BrandTeal.copy(alpha = 0.4f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 0.dp, focusedElevation = 4.dp, hoveredElevation = 4.dp),
        interactionSource = interaction
    ) {
        if (loading) {
            CircularProgressIndicator(
                Modifier.size(20.dp),
                color = BrandNavy,
                strokeWidth = 2.dp
            )
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun PremiumOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = ButtonShape,
        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        trailingIcon = trailingIcon,
        modifier = modifier.fillMaxWidth(),
        shape = FieldShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun ListRowCard(
    title: String,
    subtitle: String,
    meta: String? = null,
    status: String? = null,
    statusActive: Boolean = true,
    onClick: () -> Unit,
    leading: @Composable (() -> Unit)? = null
) {
    GlassCard(onClick = onClick, elevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (meta != null) {
                    Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            if (status != null) {
                Spacer(Modifier.width(8.dp))
                StatusChip(status, statusActive)
            }
        }
    }
}

@Composable
fun QuickActionChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = PillShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}
