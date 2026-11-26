package com.hisablist.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisablist.app.data.Disp
import com.hisablist.app.data.Doc
import com.hisablist.app.data.Fmt
import com.hisablist.app.data.S
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private fun startOfToday(): Long {
    val c = Calendar.getInstance()
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

@Composable
fun HistoryScreen(vm: AppViewModel) {
    val lang = vm.settings.lang
    var q by remember { mutableStateOf("") }
    var delId by remember { mutableStateOf<Long?>(null) }
    val fmt = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US) }

    val filtered = vm.docs.filter { d ->
        q.isBlank() || d.title.contains(q, ignoreCase = true) ||
            d.entries.any { en -> Disp.name(en, lang).contains(q, ignoreCase = true) }
    }.sortedByDescending { it.createdAt }

    val start = startOfToday()
    val today = vm.docs.filter { it.createdAt >= start && it.mode == "SHOP" }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        CField(q, { q = it }, S.get("hist_search", lang), Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))

        if (today.isNotEmpty()) {
            val total = today.sumOf { it.total }
            val due = today.sumOf { it.due() }
            CartoonCard(Modifier.fillMaxWidth(), color = C.Yellow) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(S.get("hist_today", lang) + " · " + today.size + " " + S.get("bills", lang), style = Small)
                        Text(Fmt.money(total), fontSize = 24.sp, fontWeight = FontWeight.Black, color = C.Ink)
                    }
                    if (due > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(S.get("due", lang), style = Small)
                            Text(Fmt.money(due), fontSize = 18.sp, fontWeight = FontWeight.Black, color = C.Red)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "🕘\n" + S.get("hist_empty", lang),
                    textAlign = TextAlign.Center,
                    color = C.Grey,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { d ->
                    DocCard(d, lang, fmt.format(java.util.Date(d.createdAt)), vm) { delId = d.id }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }

    val del = delId
    if (del != null) {
        CDialog(onDismiss = { delId = null }) {
            Text(S.get("delete_q", lang), style = H1)
            Spacer(Modifier.height(12.dp))
            Row {
                Chunky(onClick = { delId = null }, color = C.Paper, modifier = Modifier.weight(1f)) {
                    Text(S.get("cancel", lang), fontWeight = FontWeight.Bold, color = C.Ink)
                }
                HGap(8)
                Chunky(onClick = { vm.deleteDoc(del); delId = null }, color = C.Red, modifier = Modifier.weight(1f)) {
                    Text(S.get("delete", lang), fontWeight = FontWeight.Black, color = C.Ink)
                }
            }
        }
    }
}

@Composable
private fun DocCard(d: Doc, lang: String, date: String, vm: AppViewModel, onDelete: () -> Unit) {
    val shop = d.mode == "SHOP"
    val tint: Color = if (shop) C.Mint else C.Lavender
    CartoonCard(Modifier.fillMaxWidth(), color = tint) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    (if (shop) "🏪 " else "🏠 ") + d.title.ifBlank { S.get("untitled", lang) },
                    Modifier.weight(1f),
                    style = Body,
                    maxLines = 1
                )
                if (shop) {
                    val key = when (d.status) {
                        "PAID" -> "st_paid"
                        "UDHAR" -> "st_udhar"
                        "PARTIAL" -> "st_partial"
                        else -> "st_open"
                    }
                    Pill(S.get(key, lang), if (d.status == "PAID") C.Green else if (d.status == "OPEN") C.Paper else C.Peach)
                }
            }
            Text(date, style = Small)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (shop) {
                    Text(Fmt.money(d.total), fontSize = 22.sp, fontWeight = FontWeight.Black, color = C.Ink)
                    HGap(8)
                    if (d.due() > 0) Text(S.get("due", lang) + " " + Fmt.money(d.due()), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = C.Red)
                }
                Spacer(Modifier.weight(1f))
                Text(d.entries.size.toString() + " " + S.get("items", lang), style = Small)
            }
            Spacer(Modifier.height(8.dp))
            Row {
                Chunky(onClick = { vm.openDoc(d.id) }, color = C.Yellow, modifier = Modifier.weight(1f)) {
                    Text(S.get("open", lang), fontWeight = FontWeight.Bold, color = C.Ink, fontSize = 13.sp)
                }
                HGap(6)
                Chunky(onClick = { vm.duplicateDoc(d.id) }, color = C.Sky, modifier = Modifier.weight(1f)) {
                    Text("⧉ " + S.get("repeat", lang), fontWeight = FontWeight.Bold, color = C.Ink, fontSize = 13.sp)
                }
                HGap(6)
                Chunky(onClick = onDelete, color = C.Red) {
                    Text("🗑", fontSize = 14.sp)
                }
            }
        }
    }
}
