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
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisablist.app.data.Disp
import com.hisablist.app.data.Entry
import com.hisablist.app.data.Fmt
import com.hisablist.app.data.S
import com.hisablist.app.data.Share
import com.hisablist.app.data.Units
import com.hisablist.app.data.VoiceParser

@Composable
fun ListScreen(vm: AppViewModel) {
    val lang = vm.settings.lang
    val shop = vm.settings.mode == "SHOP"
    val doc = vm.current
    val ctx = LocalContext.current

    var editId by remember { mutableStateOf<Long?>(null) }
    var partialAsk by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var title by remember(doc.id) { mutableStateOf(doc.title) }

    val voice = rememberVoice(lang, onResult = { vm.voiceList(it) }, onMessage = { vm.say(it) })

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        // Mode + New
        Row(verticalAlignment = Alignment.CenterVertically) {
            Chunky(
                onClick = { vm.setMode("HOME") },
                color = if (!shop) C.Mint else C.Paper,
                padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) { Text(S.get("mode_home", lang), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = C.Ink) }
            HGap(6)
            Chunky(
                onClick = { vm.setMode("SHOP") },
                color = if (shop) C.Mint else C.Paper,
                padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) { Text(S.get("mode_shop", lang), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = C.Ink) }
            Spacer(Modifier.weight(1f))
            Chunky(
                onClick = { vm.newDoc() },
                color = C.Lavender,
                padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) { Text("＋ " + S.get("new", lang), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = C.Ink) }
        }
        Spacer(Modifier.height(6.dp))
        CField(
            value = title,
            onChange = { title = it; vm.setTitle(it) },
            placeholder = S.get(if (shop) "title_ph_shop" else "title_ph_home", lang),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))

        // Notebook
        CartoonCard(Modifier.weight(1f).fillMaxWidth(), color = C.Paper) {
            if (doc.entries.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "📒\n" + S.get("empty_list", lang),
                        textAlign = TextAlign.Center,
                        color = C.Grey,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(doc.entries, key = { it.id }) { e ->
                        EntryRow(e, lang, shop, vm) { editId = e.id }
                        Ruled()
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // Total / status bar
        if (shop) {
            CartoonCard(Modifier.fillMaxWidth(), color = C.Mint) {
                Column(Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(S.get("total", lang), style = Small)
                            Text(Fmt.money(doc.total), fontSize = 28.sp, fontWeight = FontWeight.Black, color = C.Ink)
                            if (doc.due() > 0) {
                                Text(S.get("due", lang) + ": " + Fmt.money(doc.due()), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.Red)
                            }
                        }
                        Chunky(onClick = { vm.showQr = true }, color = C.Yellow) {
                            Text("QR", fontWeight = FontWeight.Black, fontSize = 16.sp, color = C.Ink)
                        }
                        HGap(6)
                        Chunky(onClick = { Share.send(ctx, Share.billText(doc, vm.settings, lang), null) }, color = C.Sky) {
                            Text("📤", fontSize = 16.sp)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row {
                        listOf(
                            "OPEN" to "st_open", "PAID" to "st_paid", "UDHAR" to "st_udhar", "PARTIAL" to "st_partial"
                        ).forEach { (st, k) ->
                            Pill(
                                text = S.get(k, lang),
                                color = if (doc.status == st) C.Yellow else C.Paper,
                                modifier = Modifier.padding(end = 6.dp),
                                onClick = { if (st == "PARTIAL") partialAsk = true else vm.setStatus(st, 0.0) }
                            )
                        }
                    }
                }
            }
        } else {
            val done = doc.entries.count { it.checked }
            CartoonCard(Modifier.fillMaxWidth(), color = C.Mint) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        S.fmt("items_count", lang, doc.entries.size, done),
                        Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        color = C.Ink
                    )
                    Chunky(onClick = { Share.send(ctx, Share.billText(doc, vm.settings, lang), null) }, color = C.Sky) {
                        Text("📤 " + S.get("share", lang), fontWeight = FontWeight.Bold, color = C.Ink, fontSize = 14.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))

        if (voice.listening) {
            Text(
                "🎙️ " + (voice.partial.ifBlank { S.get("listening", lang) }),
                color = C.Ink,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Input row: text + add + big mic
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
            CField(
                value = text,
                onChange = { text = it },
                placeholder = S.get("input_ph", lang),
                modifier = Modifier.weight(1f),
                onDone = { if (text.isNotBlank()) { vm.addText(text); text = "" } }
            )
            HGap(8)
            Chunky(
                onClick = { if (text.isNotBlank()) { vm.addText(text); text = "" } },
                color = C.Green,
                padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 14.dp)
            ) { Text("＋", fontSize = 22.sp, fontWeight = FontWeight.Black, color = C.Ink) }
            HGap(8)
            Chunky(
                onClick = { voice.toggle() },
                color = if (voice.listening) C.Red else C.Pink,
                radius = 30.dp,
                padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 14.dp)
            ) { Text(if (voice.listening) "⏹" else "🎙️", fontSize = 24.sp) }
        }
    }

    // Dialogs
    val ask = vm.priceAsk
    if (ask != null) {
        val e = doc.entries.firstOrNull { it.id == ask }
        if (e != null) PriceDialog(e, lang, vm) else vm.priceAsk = null
    }
    val eid = editId
    if (eid != null) {
        val e = doc.entries.firstOrNull { it.id == eid }
        if (e != null) EditDialog(e, lang, shop, vm) { editId = null } else editId = null
    }
    if (partialAsk) PartialDialog(lang, doc.total, vm) { partialAsk = false }
}

@Composable
private fun EntryRow(e: Entry, lang: String, shop: Boolean, vm: AppViewModel, onEdit: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CartoonCheck(e.checked) { vm.toggle(e.id) }
        HGap(10)
        Column(Modifier.weight(1f).clickable(onClick = onEdit)) {
            Text(
                Disp.name(e, lang),
                style = Body,
                textDecoration = if (e.checked) TextDecoration.LineThrough else TextDecoration.None
            )
            val sub = if (e.fixed != null) Disp.qty(e, lang)
            else if (shop && e.rate != null) Disp.qty(e, lang) + " × " + Fmt.money(e.rate) + "/" + Disp.unit(e.rateUnit, lang)
            else Disp.qty(e, lang)
            Text(sub, style = Small)
        }
        if (shop) {
            val t = e.total()
            if (t != null) {
                Text(Fmt.money(t), fontWeight = FontWeight.Black, fontSize = 16.sp, color = C.Ink)
            } else {
                Pill(S.get("set_price", lang), C.Yellow, onClick = { vm.priceAsk = e.id })
            }
            HGap(8)
        }
        if (e.fixed == null) {
            StepBtn("−") { vm.bump(e.id, -1) }
            HGap(4)
            StepBtn("+") { vm.bump(e.id, 1) }
        } else {
            StepBtn("✕") { vm.removeEntry(e.id) }
        }
    }
}

@Composable
private fun PriceDialog(e: Entry, lang: String, vm: AppViewModel) {
    var rate by remember(e.id) { mutableStateOf("") }
    var unit by remember(e.id) { mutableStateOf(Units.defaultRateUnit(e.unit)) }
    val voice = rememberVoice(
        lang,
        onResult = { cands ->
            for (c in cands) {
                val r = VoiceParser.rateFromText(c)
                val v = r.first
                if (v != null) {
                    rate = Fmt.num(v)
                    r.second?.let { unit = Units.defaultRateUnit(it) }
                    break
                }
            }
        },
        onMessage = { vm.say(it) }
    )
    CDialog(onDismiss = { vm.priceAsk = null }) {
        Text(S.fmt("price_for", lang, Disp.name(e, lang)), style = H1)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CField(
                value = rate,
                onChange = { rate = it },
                placeholder = S.get("rate_label", lang),
                number = true,
                modifier = Modifier.weight(1f)
            )
            HGap(8)
            Chunky(onClick = { voice.toggle() }, color = if (voice.listening) C.Red else C.Pink, radius = 26.dp) {
                Text(if (voice.listening) "⏹" else "🎙️", fontSize = 22.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        UnitChips(unit, Units.rateUnits, lang) { unit = it }
        Spacer(Modifier.height(12.dp))
        Row {
            Chunky(onClick = { vm.priceAsk = null }, color = C.Paper, modifier = Modifier.weight(1f)) {
                Text(S.get("skip", lang), fontWeight = FontWeight.Bold, color = C.Ink)
            }
            HGap(8)
            Chunky(
                onClick = {
                    val v = rate.toDoubleOrNull()
                    if (v != null && v > 0) { vm.setEntryRate(e.id, v, unit); vm.priceAsk = null }
                },
                color = C.Green,
                modifier = Modifier.weight(1f)
            ) { Text(S.get("save", lang), fontWeight = FontWeight.Black, color = C.Ink) }
        }
    }
}

@Composable
private fun EditDialog(e: Entry, lang: String, shop: Boolean, vm: AppViewModel, onClose: () -> Unit) {
    var qty by remember(e.id) { mutableStateOf(Fmt.num(e.qty)) }
    var unit by remember(e.id) { mutableStateOf(e.unit) }
    var rate by remember(e.id) { mutableStateOf(e.rate?.let { Fmt.num(it) } ?: "") }
    var rateUnit by remember(e.id) { mutableStateOf(e.rateUnit) }

    CDialog(onDismiss = onClose) {
        Text(Disp.name(e, lang), style = H1)
        Spacer(Modifier.height(10.dp))
        if (e.fixed == null) {
            Text(S.get("quantity", lang), style = Small)
            CField(qty, { qty = it }, S.get("quantity", lang), Modifier.fillMaxWidth(), number = true)
            Spacer(Modifier.height(6.dp))
            UnitChips(unit, Units.all, lang) { unit = it }
            if (shop) {
                Spacer(Modifier.height(10.dp))
                Text(S.get("rate_label", lang), style = Small)
                CField(rate, { rate = it }, S.get("rate_label", lang), Modifier.fillMaxWidth(), number = true)
                Spacer(Modifier.height(6.dp))
                UnitChips(rateUnit, Units.rateUnits, lang) { rateUnit = it }
            }
            Spacer(Modifier.height(14.dp))
        }
        Row {
            Chunky(onClick = { vm.removeEntry(e.id); onClose() }, color = C.Red, modifier = Modifier.weight(1f)) {
                Text(S.get("delete", lang), fontWeight = FontWeight.Bold, color = C.Ink)
            }
            if (e.fixed == null) {
                HGap(8)
                Chunky(
                    onClick = {
                        vm.editEntry(e.id, qty.toDoubleOrNull() ?: e.qty, unit, rate.toDoubleOrNull(), rateUnit)
                        onClose()
                    },
                    color = C.Green,
                    modifier = Modifier.weight(1f)
                ) { Text(S.get("save", lang), fontWeight = FontWeight.Black, color = C.Ink) }
            }
        }
    }
}

@Composable
private fun PartialDialog(lang: String, total: Double, vm: AppViewModel, onClose: () -> Unit) {
    var amt by remember { mutableStateOf("") }
    CDialog(onDismiss = onClose) {
        Text(S.get("partial_q", lang), style = H1)
        Text(S.get("total", lang) + ": " + Fmt.money(total), style = Small)
        Spacer(Modifier.height(10.dp))
        CField(amt, { amt = it }, "₹", Modifier.fillMaxWidth(), number = true)
        Spacer(Modifier.height(12.dp))
        Row {
            Chunky(onClick = onClose, color = C.Paper, modifier = Modifier.weight(1f)) {
                Text(S.get("cancel", lang), fontWeight = FontWeight.Bold, color = C.Ink)
            }
            HGap(8)
            Chunky(
                onClick = {
                    val v = amt.toDoubleOrNull()
                    if (v != null && v >= 0) { vm.setStatus("PARTIAL", v); onClose() }
                },
                color = C.Green,
                modifier = Modifier.weight(1f)
            ) { Text(S.get("save", lang), fontWeight = FontWeight.Black, color = C.Ink) }
        }
    }
}
