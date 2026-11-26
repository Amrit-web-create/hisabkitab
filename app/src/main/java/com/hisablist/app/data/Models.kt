package com.hisablist.app.data

import java.util.Locale

/** Unit helpers. Weight/volume/count groups let us price "500 g" against a "per kg" rate. */
object Units {
    val all = listOf("kg", "g", "l", "ml", "pc", "packet", "dozen")
    val rateUnits = listOf("kg", "l", "pc", "packet", "dozen")

    fun factor(u: String): Double = when (u) {
        "g", "ml" -> 0.001
        "dozen" -> 12.0
        else -> 1.0
    }

    fun group(u: String): String = when (u) {
        "kg", "g" -> "w"
        "l", "ml" -> "v"
        else -> "c"
    }

    fun defaultRateUnit(u: String): String = when (u) {
        "g" -> "kg"
        "ml" -> "l"
        else -> u
    }
}

object Fmt {
    fun num(d: Double): String {
        if (d == Math.floor(d) && Math.abs(d) < 1e12) return d.toLong().toString()
        return String.format(Locale.US, "%.2f", d).trimEnd('0').trimEnd('.')
    }

    fun money(d: Double): String = "₹" + num(d)
}

data class Entry(
    val id: Long,
    val key: String,          // dictionary key, "c:<name>" for custom, "calc" for calculator lines
    val name: String,         // fallback display name (custom items)
    val qty: Double,
    val unit: String,
    val rate: Double? = null, // price per rateUnit
    val rateUnit: String = "kg",
    val fixed: Double? = null,
    val checked: Boolean = false
) {
    fun total(): Double? {
        if (fixed != null) return fixed
        val r = rate ?: return null
        return if (Units.group(unit) == Units.group(rateUnit)) {
            qty * Units.factor(unit) / Units.factor(rateUnit) * r
        } else {
            qty * r
        }
    }
}

data class Doc(
    val id: Long,
    val title: String,
    val createdAt: Long,
    val entries: List<Entry>,
    val status: String = "OPEN",   // OPEN, PAID, UDHAR, PARTIAL
    val paid: Double = 0.0,
    val mode: String = "HOME"      // HOME, SHOP
) {
    val total: Double get() = entries.sumOf { it.total() ?: 0.0 }

    fun due(): Double = when (status) {
        "UDHAR" -> total
        "PARTIAL" -> (total - paid).coerceAtLeast(0.0)
        else -> 0.0
    }
}

data class Rate(val amount: Double, val unit: String)

data class Settings(
    val shopName: String = "",
    val phone: String = "",
    val upi: String = "",
    val lang: String = "en",
    val mode: String = "HOME"
)

/** Display helpers (language aware). */
object Disp {
    fun name(e: Entry, lang: String): String {
        val d = Dict.find(e.key)
        return if (d != null) (if (lang == "hi") d.hi else d.en) else e.name
    }

    fun keyName(key: String, lang: String): String {
        val d = Dict.find(key)
        if (d != null) return if (lang == "hi") d.hi else d.en
        return key.removePrefix("c:").replaceFirstChar { it.uppercase() }
    }

    fun unit(u: String, lang: String): String = S.get("u_$u", lang)

    fun qty(e: Entry, lang: String): String = Fmt.num(e.qty) + " " + unit(e.unit, lang)
}
