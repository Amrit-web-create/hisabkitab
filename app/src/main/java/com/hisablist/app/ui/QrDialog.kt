package com.hisablist.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisablist.app.data.Disp
import com.hisablist.app.data.Fmt
import com.hisablist.app.data.S
import com.hisablist.app.data.Share
import com.hisablist.app.data.UpiQr

@Composable
fun QrDialog(vm: AppViewModel) {
    val lang = vm.settings.lang
    val s = vm.settings
    val doc = vm.current
    val ctx = LocalContext.current
    val close = { vm.showQr = false }

    CDialog(onDismiss = close) {
        if (s.upi.isBlank()) {
            Text(S.get("qr_title", lang), style = H1)
            Spacer(Modifier.height(10.dp))
            Text(S.get("qr_need_upi", lang), style = Body)
            Spacer(Modifier.height(14.dp))
            Row {
                Chunky(onClick = close, color = C.Paper, modifier = Modifier.weight(1f)) {
                    Text(S.get("close", lang), fontWeight = FontWeight.Bold, color = C.Ink)
                }
                HGap(8)
                Chunky(
                    onClick = { vm.showQr = false; vm.showSettings = true },
                    color = C.Yellow,
                    modifier = Modifier.weight(1f)
                ) { Text(S.get("qr_go", lang), fontWeight = FontWeight.Black, color = C.Ink) }
            }
            return@CDialog
        }

        val amount = if (doc.due() > 0) doc.due() else doc.total
        val uri = remember(s.upi, s.shopName, amount) { UpiQr.uri(s.upi, s.shopName, amount) }
        val grid = remember(uri) { UpiQr.grid(uri) }

        Text(
            S.get("qr_title", lang),
            style = H1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (s.shopName.isNotBlank()) {
            Text(s.shopName, style = Small, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(6.dp))
        Text(
            Fmt.money(amount),
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            color = C.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            CartoonCard(color = Color.White) {
                Canvas(Modifier.padding(10.dp).size(230.dp).background(Color.White)) {
                    val n = grid.size
                    val cell = size.width / n
                    for (y in 0 until n) for (x in 0 until n) {
                        if (grid.at(x, y)) {
                            drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell + 0.6f, cell + 0.6f))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(s.upi, style = Small, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))

        // Item summary
        doc.entries.take(4).forEach { e ->
            val t = e.total()
            Row {
                Text(Disp.name(e, lang) + "  " + Disp.qty(e, lang), Modifier.weight(1f), style = Small)
                if (t != null) Text(Fmt.money(t), style = Small)
            }
        }
        if (doc.entries.size > 4) {
            Text("+" + (doc.entries.size - 4) + " " + S.get("more", lang), style = Small)
        }
        Spacer(Modifier.height(12.dp))

        Chunky(
            onClick = {
                val bmp = UpiQr.cardBitmap(grid, s.shopName, Fmt.money(amount), s.upi)
                Share.send(ctx, Share.billText(doc, s, lang), bmp)
            },
            color = C.Green,
            modifier = Modifier.fillMaxWidth()
        ) { Text("📤 " + S.get("share_qr", lang), fontWeight = FontWeight.Black, color = C.Ink) }
        Spacer(Modifier.height(4.dp))
        Row {
            Chunky(
                onClick = { Share.send(ctx, Share.billText(doc, s, lang), null) },
                color = C.Sky,
                modifier = Modifier.weight(1f)
            ) { Text(S.get("share_text", lang), fontWeight = FontWeight.Bold, color = C.Ink, fontSize = 13.sp) }
            HGap(8)
            Chunky(onClick = close, color = C.Paper, modifier = Modifier.weight(1f)) {
                Text(S.get("close", lang), fontWeight = FontWeight.Bold, color = C.Ink)
            }
        }
    }
}
