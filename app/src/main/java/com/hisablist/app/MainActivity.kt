package com.hisablist.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hisablist.app.data.S
import com.hisablist.app.ui.AppViewModel
import com.hisablist.app.ui.C
import com.hisablist.app.ui.CalcScreen
import com.hisablist.app.ui.Chunky
import com.hisablist.app.ui.H1
import com.hisablist.app.ui.HistoryScreen
import com.hisablist.app.ui.HisabTheme
import com.hisablist.app.ui.ListScreen
import com.hisablist.app.ui.QrDialog
import com.hisablist.app.ui.SettingsScreen
import com.hisablist.app.ui.dotted
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HisabTheme {
                val vm: AppViewModel = viewModel()
                AppRoot(vm)
            }
        }
    }
}

@Composable
private fun AppRoot(vm: AppViewModel) {
    val lang = vm.settings.lang

    LaunchedEffect(vm.toast) {
        if (vm.toast != null) {
            delay(2200)
            vm.toast = null
        }
    }

    Box(Modifier.fillMaxSize().background(C.Cream).dotted()) {
        if (vm.showSettings) {
            SettingsScreen(vm)
        } else {
            Column(Modifier.fillMaxSize()) {
                TopBar(vm)
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (vm.tab) {
                        0 -> ListScreen(vm)
                        1 -> CalcScreen(vm)
                        else -> HistoryScreen(vm)
                    }
                }
                BottomNav(vm)
            }
        }

        if (vm.showQr) QrDialog(vm)

        val msg = vm.toast
        if (msg != null) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 96.dp, start = 24.dp, end = 24.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(C.Ink)
                    .clickable { vm.toast = null }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(msg, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun TopBar(vm: AppViewModel) {
    val lang = vm.settings.lang
    Row(
        Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🧾 " + S.get("app_title", lang), style = H1, modifier = Modifier.weight(1f), maxLines = 1)
        LangSwitch(lang) { vm.setLang(it) }
        Box(Modifier.padding(start = 8.dp)) {
            Chunky(
                onClick = { vm.showSettings = true },
                color = C.Yellow,
                padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) { Text("⚙️", fontSize = 18.sp) }
        }
    }
}

@Composable
private fun LangSwitch(lang: String, onPick: (String) -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(Modifier.clip(shape).border(2.5.dp, C.Ink, shape)) {
        listOf("en" to "EN", "hi" to "हिंदी").forEach { (code, label) ->
            Box(
                Modifier
                    .background(if (lang == code) C.Yellow else C.Paper)
                    .clickable { onPick(code) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, fontWeight = FontWeight.Black, fontSize = 13.sp, color = C.Ink)
            }
        }
    }
}

@Composable
private fun BottomNav(vm: AppViewModel) {
    val lang = vm.settings.lang
    val tabs = listOf(
        Triple(0, "📒", "tab_list"),
        Triple(1, "🧮", "tab_calc"),
        Triple(2, "🕘", "tab_hist")
    )
    Row(
        Modifier
            .fillMaxWidth()
            .background(C.Paper)
            .border(2.5.dp, C.Ink)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { (i, icon, key) ->
            val sel = vm.tab == i
            val shape = RoundedCornerShape(16.dp)
            Column(
                Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(if (sel) C.Yellow else C.Paper)
                    .border(2.5.dp, if (sel) C.Ink else androidx.compose.ui.graphics.Color.Transparent, shape)
                    .clickable { vm.tab = i }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(icon, fontSize = 22.sp)
                Text(S.get(key, lang), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = C.Ink, maxLines = 1)
            }
        }
    }
}
