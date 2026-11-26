package com.hisablist.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object C {
    val Ink = Color(0xFF18181B)
    val Cream = Color(0xFFFFF8E7)
    val Paper = Color(0xFFFFFDF5)
    val Yellow = Color(0xFFFFD84D)
    val Mint = Color(0xFFA8F0C6)
    val Green = Color(0xFF3DBE7A)
    val Pink = Color(0xFFFFB8D1)
    val Sky = Color(0xFFA9D8FF)
    val Lavender = Color(0xFFD8C8FF)
    val Peach = Color(0xFFFFCDA8)
    val Red = Color(0xFFFF6B6B)
    val Grey = Color(0xFF71717A)
    val Dot = Color(0x14000000)
}

private val scheme = lightColorScheme(
    primary = C.Ink,
    onPrimary = Color.White,
    background = C.Cream,
    surface = C.Paper,
    onSurface = C.Ink
)

@Composable
fun HisabTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}

val Border = 2.5.dp

fun Modifier.dotted(): Modifier = this.drawBehind {
    val step = 28.dp.toPx()
    var y = step / 2
    var row = 0
    while (y < size.height) {
        var x = if (row % 2 == 0) step / 2 else step
        while (x < size.width) {
            drawCircle(C.Dot, radius = 2.dp.toPx(), center = Offset(x, y))
            x += step
        }
        y += step
        row++
    }
}

/** Thick-outlined pastel card. */
@Composable
fun CartoonCard(
    modifier: Modifier = Modifier,
    color: Color = C.Paper,
    radius: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(radius)
    var m = modifier
        .drawBehind {
            drawRoundRect(C.Ink, topLeft = Offset(0f, 4.dp.toPx()), size = size, cornerRadius = CornerRadius(radius.toPx()))
        }
        .clip(shape)
        .background(color)
        .border(Border, C.Ink, shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Box(m) { content() }
}

/** Chunky 3D-press button with bottom shadow. */
@Composable
fun Chunky(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = C.Yellow,
    radius: Dp = 16.dp,
    padding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val shape = RoundedCornerShape(radius)
    val shadow = if (pressed) 1.dp else 4.dp
    Box(modifier.padding(bottom = 4.dp), propagateMinConstraints = true) {
        Box(
            Modifier
                .offset(y = if (pressed) 3.dp else 0.dp)
                .drawBehind {
                    drawRoundRect(
                        C.Ink,
                        topLeft = Offset(0f, shadow.toPx()),
                        size = size,
                        cornerRadius = CornerRadius(radius.toPx())
                    )
                }
                .clip(shape)
                .background(color)
                .border(Border, C.Ink, shape)
                .clickable(interactionSource = src, indication = null, onClick = onClick)
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, content = content)
        }
    }
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(50)
    var m = modifier.clip(shape).background(color).border(2.dp, C.Ink, shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Box(m.padding(horizontal = 10.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = C.Ink)
    }
}

/** Tiny dotted checkbox that "pops" when ticked. */
@Composable
fun CartoonCheck(checked: Boolean, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier
            .clip(shape)
            .background(if (checked) C.Green else Color.White)
            .border(Border, C.Ink, shape)
            .clickable(onClick = onToggle)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.padding(0.dp)) {
            Text(if (checked) "✓" else " ", fontSize = 20.sp, fontWeight = FontWeight.Black, color = C.Ink, modifier = Modifier.padding(horizontal = 6.dp))
        }
    }
}

val H1 = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Black, color = C.Ink)
val Body = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
val Small = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = C.Grey)

@Composable
fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    cursorColor = C.Ink,
    focusedTextColor = C.Ink,
    unfocusedTextColor = C.Ink
)

/** Rounded, thick-outlined text-field container for BasicTextField-like usage via TextField. */
fun Modifier.fieldBox(): Modifier {
    val shape = RoundedCornerShape(14.dp)
    return this.clip(shape).border(Border, C.Ink, shape)
}
