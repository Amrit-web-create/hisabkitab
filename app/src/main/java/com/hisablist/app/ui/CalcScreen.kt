package com.hisablist.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisablist.app.data.Calc
import com.hisablist.app.data.Fmt
import com.hisablist.app.data.S
import com.hisablist.app.data.VoiceMath

@Composable
fun CalcScreen(vm: AppViewModel) {
    val lang = vm.settings.lang
    val ctx = LocalContext.current
    var expr by remember { mutableStateOf("") }
    val result = Calc.eval(expr)

    val speaker = remember { Speaker(ctx) }
    DisposableEffect(Unit) { onDispose { speaker.shutdown() } }

    val voice = rememberVoice(
        lang,
        onResult = { cands ->
            for (c in cands) {
                val e = VoiceMath.toExpression(c)
                if (e != null) { expr = e; break }
            }
        },
        onMessage = { vm.say(it) }
    )

    fun press(k: String) {
        when (k) {
            "C" -> expr = ""
            "⌫" -> expr = expr.dropLast(1)
            "=" -> if (result != null) expr = Fmt.num(result)
            "+", "-", "×", "÷" -> {
                if (expr.isEmpty()) {
                    if (k == "-") expr = "-"
                } else {
                    val last = expr.last()
                    expr = if (last in "+-×÷") expr.dropLast(1) + k else expr + k
                }
            }
            else -> expr += k
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        CartoonCard(Modifier.fillMaxWidth(), color = C.Sky) {
            Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.End) {
                Text(
                    expr.ifEmpty { "0" },
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = C.Ink,
                    maxLines = 2,
                    textAlign = TextAlign.End
                )
                Text(
                    if (result != null) "= " + Fmt.num(result) else " ",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.Ink.copy(alpha = 0.7f)
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        if (voice.listening) {
            Text("🎙️ " + voice.partial.ifBlank { S.get("listening", lang) }, fontWeight = FontWeight.Bold, color = C.Ink)
        }

        Row {
            Chunky(
                onClick = { voice.toggle() },
                color = if (voice.listening) C.Red else C.Pink,
                modifier = Modifier.weight(1f)
            ) { Text(if (voice.listening) "⏹" else "🎙️", fontSize = 22.sp) }
            HGap(8)
            Chunky(
                onClick = {
                    val r = result
                    if (r != null) speaker.speak(S.fmt("calc_say", lang, Fmt.num(r)), lang)
                },
                color = C.Lavender,
                modifier = Modifier.weight(2f)
            ) { Text(S.get("calc_read", lang), fontWeight = FontWeight.Bold, color = C.Ink) }
        }
        Spacer(Modifier.height(4.dp))

        val rows = listOf(
            listOf("C", "⌫", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", "00", ".", "=")
        )
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth().weight(1f)) {
                row.forEach { k ->
                    val col: Color = when (k) {
                        "=" -> C.Green
                        "C", "⌫" -> C.Peach
                        "+", "-", "×", "÷", "%" -> C.Yellow
                        else -> C.Paper
                    }
                    Chunky(
                        onClick = { press(k) },
                        color = col,
                        modifier = Modifier.weight(1f).fillMaxSize().padding(horizontal = 3.dp),
                        padding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text(if (k == "-") "−" else k, fontSize = 24.sp, fontWeight = FontWeight.Black, color = C.Ink)
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Chunky(
            onClick = {
                val r = result
                if (r != null && r != 0.0) vm.sendToBill(r)
            },
            color = C.Mint,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
        ) { Text(S.get("calc_send", lang), fontWeight = FontWeight.Black, color = C.Ink, fontSize = 16.sp) }
    }
}
