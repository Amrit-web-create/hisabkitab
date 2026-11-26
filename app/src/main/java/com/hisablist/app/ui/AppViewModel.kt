package com.hisablist.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.hisablist.app.data.Cmd
import com.hisablist.app.data.Doc
import com.hisablist.app.data.Entry
import com.hisablist.app.data.Fmt
import com.hisablist.app.data.Parsed
import com.hisablist.app.data.Rate
import com.hisablist.app.data.S
import com.hisablist.app.data.Settings
import com.hisablist.app.data.Store
import com.hisablist.app.data.VoiceParser
import com.hisablist.app.data.Disp
import java.util.Locale

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val store = Store(app)

    var settings by mutableStateOf(store.loadSettings())
        private set
    var rates by mutableStateOf(store.loadRates())
        private set
    var docs by mutableStateOf(store.loadDocs())
        private set
    var current by mutableStateOf(initialDoc())
        private set

    var tab by mutableStateOf(0)
    var showSettings by mutableStateOf(false)
    var showQr by mutableStateOf(false)
    var priceAsk by mutableStateOf<Long?>(null)
    var toast by mutableStateOf<String?>(null)
    var calcExpr by mutableStateOf("")

    /** Whoever taps a mic button sets this first; the single VoiceHandle forwards results here. */
    var voiceSink: (List<String>) -> Unit = {}

    private var seq = System.currentTimeMillis()
    private fun nextId(): Long { seq += 1; return seq }

    private fun blankDoc() = Doc(nextId(), "", System.currentTimeMillis(), emptyList(), mode = settings.mode)

    private fun initialDoc(): Doc {
        val id = store.loadCurrentId()
        return docs.firstOrNull { it.id == id } ?: Doc(
            System.currentTimeMillis(), "", System.currentTimeMillis(), emptyList(), mode = settings.mode
        )
    }

    // ---------- helpers ----------
    fun say(key: String) { toast = S.get(key, settings.lang) }
    private fun sayRaw(text: String) { toast = text }

    private fun String.cap() = replaceFirstChar { it.uppercase() }

    private fun commit(d: Doc) {
        current = d
        store.saveCurrentId(d.id)
        val keep = d.entries.isNotEmpty() || d.title.isNotBlank()
        docs = when {
            !keep -> docs.filter { it.id != d.id }
            docs.any { it.id == d.id } -> docs.map { if (it.id == d.id) d else it }
            else -> listOf(d) + docs
        }
        store.saveDocs(docs)
    }

    private fun mutate(f: (Doc) -> Doc) = commit(f(current))

    // ---------- settings ----------
    fun setLang(l: String) {
        settings = settings.copy(lang = l)
        store.saveSettings(settings)
    }

    fun setMode(m: String) {
        settings = settings.copy(mode = m)
        store.saveSettings(settings)
        mutate { it.copy(mode = m) }
    }

    fun updateShop(name: String? = null, phone: String? = null, upi: String? = null) {
        settings = settings.copy(
            shopName = name ?: settings.shopName,
            phone = phone ?: settings.phone,
            upi = upi ?: settings.upi
        )
        store.saveSettings(settings)
    }

    // ---------- rates ----------
    private fun putRate(key: String, amount: Double, unit: String) {
        rates = rates + (key to Rate(amount, unit))
        store.saveRates(rates)
    }

    /** Save a rate and back-fill unpriced lines of the open bill that use the same item. */
    fun setRateFor(key: String, amount: Double, unit: String) {
        putRate(key, amount, unit)
        mutate { d ->
            d.copy(entries = d.entries.map {
                if (it.key == key && it.rate == null && it.fixed == null) it.copy(rate = amount, rateUnit = unit) else it
            })
        }
    }

    fun deleteRate(key: String) {
        rates = rates - key
        store.saveRates(rates)
    }

    fun saveRateByName(name: String, amount: Double, unit: String): Boolean {
        val p = VoiceParser.parse(name).firstOrNull { it.item != null || it.name.isNotEmpty() } ?: return false
        setRateFor(VoiceParser.keyOf(p), amount, unit)
        return true
    }

    /** "allo 40 rupaye kilo" / "sugar 45". Returns true if a rate was saved. */
    fun applyRateText(text: String): Boolean {
        val p = VoiceParser.parse(text).firstOrNull { it.item != null || it.name.isNotEmpty() }
        val price = p?.let { it.price ?: it.lone ?: it.qty }
        if (p == null || price == null) { say("rate_bad"); return false }
        val unit = Units_default(p)
        val key = VoiceParser.keyOf(p)
        setRateFor(key, price, unit)
        val lang = settings.lang
        sayRaw(S.fmt("rate_saved", lang, Disp.keyName(key, lang), Fmt.num(price), Disp.unit(unit, lang)))
        return true
    }

    private fun Units_default(p: Parsed): String =
        com.hisablist.app.data.Units.defaultRateUnit(p.unit ?: p.item?.unit ?: "kg")

    fun voiceRate(cands: List<String>) {
        val text = cands.firstOrNull { c -> VoiceParser.parse(c).any { (it.item != null || it.name.isNotEmpty()) && (it.price != null || it.lone != null || it.qty != null) } }
            ?: cands.firstOrNull() ?: return
        applyRateText(text)
    }

    // ---------- list / bill ----------
    fun voiceList(cands: List<String>) {
        val text = cands.firstOrNull { c -> VoiceParser.parse(c).any { it.item != null } }
            ?: cands.firstOrNull() ?: return
        addText(text)
    }

    fun addText(text: String) {
        val parsed = VoiceParser.parse(text).filter { it.item != null || it.name.isNotEmpty() }
        if (parsed.isEmpty()) { say("nothing_heard"); return }
        val shop = settings.mode == "SHOP"
        val lang = settings.lang
        var d = current
        var ask: Long? = null
        val names = ArrayList<String>()
        for (p in parsed) {
            val key = VoiceParser.keyOf(p)
            names.add(Disp.keyName(key, lang))
            when (p.cmd) {
                Cmd.REMOVE -> d = d.copy(entries = d.entries.filter { it.key != key })
                Cmd.SET -> {
                    val q = p.qty ?: p.lone
                    if (q != null && d.entries.any { it.key == key }) {
                        d = d.copy(entries = d.entries.map {
                            if (it.key == key) it.copy(qty = q, unit = p.unit ?: it.unit) else it
                        })
                    } else {
                        val r = addEntry(d, p, key, shop)
                        d = r.first
                        if (r.second != null) ask = r.second
                    }
                }
                Cmd.ADD -> {
                    val r = addEntry(d, p, key, shop)
                    d = r.first
                    if (r.second != null) ask = r.second
                }
            }
        }
        commit(d)
        if (ask != null) priceAsk = ask
        val removed = parsed.all { it.cmd == Cmd.REMOVE }
        sayRaw((if (removed) "✓ " + S.get("removed", lang) else "✓ " + S.get("added", lang)) + ": " + names.joinToString(", "))
    }

    private fun addEntry(d: Doc, p: Parsed, key: String, shop: Boolean): Pair<Doc, Long?> {
        val def = p.item
        val unit = p.unit ?: def?.unit ?: "pc"
        val qty = p.qty ?: p.lone ?: 1.0
        var rate: Double? = null
        var rateUnit = unit
        if (p.price != null) {
            rate = p.price
            putRate(key, p.price, unit)
        } else {
            val saved = rates[key]
            if (saved != null) { rate = saved.amount; rateUnit = saved.unit }
        }
        val existing = d.entries.firstOrNull { it.key == key && it.fixed == null && it.unit == unit }
        val entry: Entry
        val list: List<Entry>
        if (existing != null) {
            entry = existing.copy(
                qty = existing.qty + qty,
                rate = existing.rate ?: rate,
                rateUnit = if (existing.rate != null) existing.rateUnit else rateUnit
            )
            list = d.entries.map { if (it.id == existing.id) entry else it }
        } else {
            entry = Entry(nextId(), key, def?.en ?: p.name.cap(), qty, unit, rate, rateUnit)
            list = d.entries + entry
        }
        return Pair(d.copy(entries = list), if (shop && entry.rate == null) entry.id else null)
    }

    fun toggle(id: Long) = mutate { d -> d.copy(entries = d.entries.map { if (it.id == id) it.copy(checked = !it.checked) else it }) }

    fun bump(id: Long, dir: Int) {
        val d = current
        val e = d.entries.firstOrNull { it.id == id } ?: return
        val step = when (e.unit) { "kg", "l" -> 0.5; "g", "ml" -> 50.0; else -> 1.0 }
        val q = e.qty + dir * step
        commit(
            if (q <= 0.0) d.copy(entries = d.entries.filter { it.id != id })
            else d.copy(entries = d.entries.map { if (it.id == id) it.copy(qty = q) else it })
        )
    }

    fun removeEntry(id: Long) = mutate { d -> d.copy(entries = d.entries.filter { it.id != id }) }

    fun editEntry(id: Long, qty: Double, unit: String, rate: Double?, rateUnit: String) {
        val e = current.entries.firstOrNull { it.id == id } ?: return
        if (rate != null) putRate(e.key, rate, rateUnit)
        mutate { d ->
            d.copy(entries = d.entries.map {
                if (it.id == id) it.copy(qty = if (qty > 0) qty else it.qty, unit = unit, rate = rate, rateUnit = rateUnit) else it
            })
        }
    }

    /** Answer to "Price for Potato?" prompt. */
    fun setEntryRate(id: Long, amount: Double, unit: String) {
        val e = current.entries.firstOrNull { it.id == id } ?: return
        setRateFor(e.key, amount, unit)
        mutate { d ->
            d.copy(entries = d.entries.map { if (it.id == id) it.copy(rate = amount, rateUnit = unit) else it })
        }
    }

    fun sendToBill(amount: Double) {
        val e = Entry(nextId(), "calc", S.get("calc_item", settings.lang), 1.0, "pc", fixed = amount)
        mutate { it.copy(entries = it.entries + e) }
        say("calc_sent")
    }

    // ---------- documents ----------
    fun newDoc() {
        commit(current) // make sure the open one is stored if it has content
        current = blankDoc()
        store.saveCurrentId(current.id)
        tab = 0
    }

    fun openDoc(id: Long) {
        val d = docs.firstOrNull { it.id == id } ?: return
        current = d
        store.saveCurrentId(d.id)
        tab = 0
    }

    /** Instant repeat bill: copy of an old bill/list with fresh date, unpaid, unchecked. */
    fun duplicateDoc(id: Long) {
        val d = docs.firstOrNull { it.id == id } ?: return
        val copy = d.copy(
            id = nextId(),
            createdAt = System.currentTimeMillis(),
            status = "OPEN",
            paid = 0.0,
            entries = d.entries.map { it.copy(id = nextId(), checked = false) }
        )
        commit(copy)
        tab = 0
    }

    fun deleteDoc(id: Long) {
        docs = docs.filter { it.id != id }
        store.saveDocs(docs)
        if (current.id == id) {
            current = blankDoc()
            store.saveCurrentId(current.id)
        }
    }

    fun setTitle(t: String) = mutate { it.copy(title = t) }

    fun setStatus(status: String, paid: Double) = mutate { it.copy(status = status, paid = paid) }
}
