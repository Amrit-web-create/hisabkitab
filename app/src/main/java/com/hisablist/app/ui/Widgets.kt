package com.hisablist.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hisablist.app.data.Disp

@Composable
fun CField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    number: Boolean = false,
    onDone: (() -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fieldBox(),
        singleLine = true,
        placeholder = { Text(placeholder, color = C.Grey, fontSize = 14.sp) },
        colors = fieldColors(),
        textStyle = Body,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (number) KeyboardType.Decimal else KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() })
    )
}

@Composable
fun UnitChips(selected: String, options: List<String>, lang: String, onSelect: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        options.forEach { u ->
            Pill(
                text = Disp.unit(u, lang),
                color = if (u == selected) C.Yellow else C.Paper,
                modifier = Modifier.padding(end = 6.dp),
                onClick = { onSelect(u) }
            )
        }
    }
}

@Composable
fun CDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CartoonCard(Modifier.padding(20.dp).fillMaxWidth(), color = C.Cream) {
            Column(
                Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                content = content
            )
        }
    }
}

/** Small square +/- stepper button. */
@Composable
fun StepBtn(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .clip(shape)
            .background(C.Sky)
            .border(2.dp, C.Ink, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Black, color = C.Ink)
    }
}

@Composable
fun HGap(w: Int) { Spacer(Modifier.width(w.dp)) }

@Composable
fun Ruled() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .background(C.Ink.copy(alpha = 0.12f))
            .padding(top = 1.dp)
    )
}
