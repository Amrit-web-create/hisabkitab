package com.hisablist.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisablist.app.data.Disp
import com.hisablist.app.data.Fmt
import com.hisablist.app.data.S
import com.hisablist.app.data.Units

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val lang = vm.settings.lang
    val close = { vm.showSettings = false }
    BackHandler(onBack = close)

    var cmd by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("kg") }

    val voice = rememberVoice(
        lang,
        onResult = { cands ->
            // Prefer a candidate the rate parser understands, show it in the box and save it.
            val ok = cands.firstOrNull { c ->
                com.hisablist.app.data.VoiceParser.parse(c).any {
                    (it.item != null || it.name.isNotEmpty()) && (it.price != null || it.lone != null || it.qty != null)
                }
            }
            cmd = ok ?: cands.first()
            vm.voiceRate(cands)
        },
        onMessage = { vm.say(it) }
    )

    Column(
        Modifier
            .fillMaxSize()
            .dotted()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Chunky(onClick = close, color = C.Paper) { Text("←", fontSize = 20.sp, fontWeight = FontWeight.Black, color = C.Ink) }
            HGap(10)
            Text("⚙️ " + S.get("settings_title", lang), style = H1)
        }
        Spacer(Modifier.height(14.dp))

        // ----- SET RATE -----
        CartoonCard(Modifier.fillMaxWidth(), color = C.Yellow) {
            Column(Modifier.padding(14.dp)) {
                Text("💰 " + S.get("set_rate", lang), style = H1)
                Text(S.get("rate_hint", lang), style = Small)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CField(
                        cmd, { cmd = it }, S.get("rate_cmd_ph", lang),
                        Modifier.weight(1f),
                        onDone = { if (vm.applyRateText(cmd)) cmd = "" }
                    )
                    HGap(8)
                    Chunky(onClick = { voice.toggle() }, color = if (voice.listening) C.Red else C.Pink, radius = 26.dp) {
                        Text(if (voice.listening) "⏹" else "🎙️", fontSize = 22.sp)
                    }
                }
                if (voice.listening) {
                    Text("🎙️ " + voice.partial.ifBlank { S.get("listening", lang) }, fontWeight = FontWeight.Bold, color = C.Ink)
                }
                Spacer(Modifier.height(8.dp))
                Chunky(onClick = { if (vm.applyRateText(cmd)) cmd = "" }, color = C.Green, modifier = Modifier.fillMaxWidth()) {
                    Text(S.get("save", lang), fontWeight = FontWeight.Black, color = C.Ink)
                }

                Spacer(Modifier.height(10.dp))
                Row {
                    CField(name, { name = it }, S.get("rate_item", lang), Modifier.weight(1.4f))
                    HGap(8)
                    CField(price, { price = it }, S.get("rate_price", lang), Modifier.weight(1f), number = true)
                }
                Spacer(Modifier.height(8.dp))
                UnitChips(unit, Units.rateUnits, lang) { unit = it }
                Spacer(Modifier.height(8.dp))
                Chunky(
                    onClick = {
                        val p = price.toDoubleOrNull()
                        if (p != null && p > 0 && name.isNotBlank() && vm.saveRateByName(name, p, unit)) {
                            vm.toast = S.fmt("rate_saved", lang, name.trim(), Fmt.num(p), Disp.unit(unit, lang))
                            name = ""
                            price = ""
                        }
                    },
                    color = C.Mint,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(S.get("save", lang), fontWeight = FontWeight.Black, color = C.Ink) }
            }
        }
        Spacer(Modifier.height(14.dp))

        // ----- SAVED RATES -----
        CartoonCard(Modifier.fillMaxWidth(), color = C.Paper) {
            Column(Modifier.padding(14.dp)) {
                Text(S.get("saved_rates", lang), style = H1)
                Spacer(Modifier.height(6.dp))
                if (vm.rates.isEmpty()) {
                    Text(S.get("no_rates", lang), style = Small)
                } else {
                    vm.rates.entries.sortedBy { Disp.keyName(it.key, lang) }.forEach { en ->
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(Disp.keyName(en.key, lang), Modifier.weight(1f), style = Body)
                            Text(
                                Fmt.money(en.value.amount) + "/" + Disp.unit(en.value.unit, lang),
                                fontWeight = FontWeight.Black,
                                color = C.Ink
                            )
                            HGap(8)
                            StepBtn("✕") { vm.deleteRate(en.key) }
                        }
                        Ruled()
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        // ----- SHOP DETAILS -----
        CartoonCard(Modifier.fillMaxWidth(), color = C.Sky) {
            Column(Modifier.padding(14.dp)) {
                Text("🏪 " + S.get("shop_details", lang), style = H1)
                Spacer(Modifier.height(8.dp))
                CField(vm.settings.shopName, { vm.updateShop(name = it) }, S.get("shop_name", lang), Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                CField(vm.settings.phone, { vm.updateShop(phone = it) }, S.get("phone", lang), Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                CField(vm.settings.upi, { vm.updateShop(upi = it) }, S.get("upi", lang), Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(14.dp))

        // ----- LANGUAGE / MODE -----
        CartoonCard(Modifier.fillMaxWidth(), color = C.Lavender) {
            Column(Modifier.padding(14.dp)) {
                Text(S.get("language", lang), style = Small)
                Row(Modifier.padding(top = 4.dp)) {
                    Pill("English", if (lang == "en") C.Yellow else C.Paper, Modifier.padding(end = 6.dp)) { vm.setLang("en") }
                    Pill("हिंदी", if (lang == "hi") C.Yellow else C.Paper) { vm.setLang("hi") }
                }
                Spacer(Modifier.height(10.dp))
                Text(S.get("mode", lang), style = Small)
                Row(Modifier.padding(top = 4.dp)) {
                    Pill(S.get("mode_home", lang), if (vm.settings.mode == "HOME") C.Yellow else C.Paper, Modifier.padding(end = 6.dp)) { vm.setMode("HOME") }
                    Pill(S.get("mode_shop", lang), if (vm.settings.mode == "SHOP") C.Yellow else C.Paper) { vm.setMode("SHOP") }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("🔒 " + S.get("offline_note", lang), style = Small)
        Spacer(Modifier.height(24.dp))
    }
}
