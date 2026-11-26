package com.hisablist.app.data

import java.util.Locale

enum class Cmd { ADD, REMOVE, SET }

/**
 * One parsed phrase, e.g. "2 kilo allo" -> item=Potato, qty=2, unit=kg.
 * [lone] is a bare number with no unit ("sugar 45"); caller decides if it is qty or price.
 */
data class Parsed(
    val cmd: Cmd,
    val item: ItemDef?,
    val name: String,
    val qty: Double?,
    val unit: String?,
    val price: Double?,
    val lone: Double?
)

/** 100% offline rule-based parser for Hinglish / Hindi / English kirana phrases. */
object VoiceParser {

    fun parse(text: String): List<Parsed> {
        val toks = Text.tokens(text)
        val segs = ArrayList<List<String>>()
        var cur = ArrayList<String>()
        for (t in toks) {
            if (t in Lex.separators) {
                if (cur.isNotEmpty()) { segs.add(cur); cur = ArrayList() }
            } else cur.add(t)
        }
        if (cur.isNotEmpty()) segs.add(cur)
        return segs.mapNotNull { parseSeg(it) }
    }

    private fun parseSeg(raw: List<String>): Parsed? {
        var cmd = Cmd.ADD
        val t = ArrayList<String>()
        var i = 0
        while (i < raw.size) {
            val w = raw[i]
            if (w in Lex.removeWords) {
                cmd = Cmd.REMOVE
                i++
                if (i < raw.size && raw[i] in Lex.afterCommand) i++
                continue
            }
            if (w in Lex.editWords) {
                if (cmd != Cmd.REMOVE) cmd = Cmd.SET
                i++
                if (i < raw.size && raw[i] in Lex.afterCommand) i++
                continue
            }
            t.add(w)
            i++
        }

        var item: ItemDef? = null
        var qty: Double? = null
        var unit: String? = null
        var price: Double? = null
        var lone: Double? = null
        val name = ArrayList<String>()

        var k = 0
        while (k < t.size) {
            val w = t[k]
            if (item == null) {
                val m = Dict.match(t, k)
                if (m != null) { item = m.first; k += m.second; continue }
            }
            val n = Lex.number(w)
            if (n != null) {
                var v = n
                var j = k + 1
                if (j < t.size && t[j] in Lex.hundred && v < 10) { v *= 100; j++ }
                val nx = t.getOrNull(j)
                val nu = if (nx != null) Lex.units[nx] else null
                when {
                    nx != null && nx in Lex.rupee -> { price = v; k = j + 1 }
                    nu != null -> {
                        if (nu == "paav") { qty = v * 0.25; unit = "kg" } else { qty = v; unit = nu }
                        k = j + 1
                    }
                    else -> { if (lone == null) lone = v; k = j }
                }
                continue
            }
            val u = Lex.units[w]
            if (u != null) {
                if (u == "paav") { if (qty == null) qty = 0.25; unit = "kg" } else if (unit == null) unit = u
                k++
                continue
            }
            if (w in Lex.rupee || w in Lex.filler || w == ",") { k++; continue }
            if (item == null) {
                val f = Dict.fuzzy(w)
                if (f != null) { item = f; k++; continue }
            }
            name.add(w)
            k++
        }

        val custom = if (item == null) name.joinToString(" ").replaceFirstChar { it.uppercase() } else ""
        if (item == null && custom.isEmpty() && qty == null && price == null && lone == null) return null
        return Parsed(cmd, item, custom, qty, unit, price, lone)
    }

    /** "allo 40 rupaye kilo" -> (40.0, "kg"); "40" -> (40.0, null). */
    fun rateFromText(s: String): Pair<Double?, String?> {
        val p = parse(s).firstOrNull { it.price != null || it.lone != null || it.qty != null }
            ?: return Pair(null, null)
        return Pair(p.price ?: p.lone ?: p.qty, p.unit)
    }

    fun keyOf(p: Parsed): String = p.item?.key ?: ("c:" + p.name.lowercase(Locale.ROOT))
}

/** Turns "paanch plus teen", "10 into 4", "100 ghata 25" into "5+3", "10×4", "100-25". */
object VoiceMath {
    private val ops: Map<String, String> = buildMap {
        fun o(sym: String, words: String) { for (w in words.split(",")) put(w.trim(), sym) }
        o("+", "+,plus,add,jod,jodo,jor,aur,जोड़,जोड़ो,प्लस,और")
        o("-", "-,minus,ghata,ghatao,kam,less,subtract,घटा,घटाओ,माइनस,कम")
        o("×", "×,*,x,into,times,multiply,multiplied,guna,gunaa,gun,गुना,गुणा,इनटू")
        o("÷", "÷,/,divide,divided,by,bhaag,bhag,भाग")
        o("%", "%,percent,pratishat,प्रतिशत,परसेंट")
    }

    fun toExpression(text: String): String? {
        var s = text.lowercase(Locale.ROOT)
        for (c in listOf("+", "-", "*", "/", "×", "÷", "%")) s = s.replace(c, " $c ")
        val parts = s.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val sb = StringBuilder()
        var sawNumber = false
        var i = 0
        while (i < parts.size) {
            val w = parts[i]
            val op = ops[w]
            val n = Lex.number(w)
            if (n != null) {
                var v = n
                if (i + 1 < parts.size && parts[i + 1] in Lex.hundred && v < 10) { v *= 100; i++ }
                if (sb.isNotEmpty() && sb.last().let { it.isDigit() || it == '.' }) sb.append("+")
                sb.append(Fmt.num(v))
                sawNumber = true
            } else if (op != null) {
                if (op == "%") {
                    if (sb.isNotEmpty() && sb.last().isDigit()) sb.append("%")
                } else if (sb.isNotEmpty()) {
                    if (sb.last() in "+-×÷") sb.setLength(sb.length - 1)
                    sb.append(op)
                } else if (op == "-") sb.append("-")
            }
            i++
        }
        return if (sawNumber) sb.toString() else null
    }
}

/** Small safe expression evaluator for the calculator tab. */
object Calc {
    fun eval(expr: String): Double? {
        var s = expr.replace("×", "*").replace("÷", "/").replace("−", "-").replace(" ", "")
        while (s.isNotEmpty() && s.last() in "+-*/") s = s.dropLast(1)
        if (s.isEmpty()) return null
        return try {
            val p = P(s)
            val v = p.expr()
            if (p.pos != s.length || v.isNaN() || v.isInfinite()) null else v
        } catch (e: Exception) {
            null
        }
    }

    private class P(val s: String) {
        var pos = 0

        fun expr(): Double {
            var v = term()
            while (pos < s.length && (s[pos] == '+' || s[pos] == '-')) {
                val op = s[pos++]
                val r = term()
                v = if (op == '+') v + r else v - r
            }
            return v
        }

        fun term(): Double {
            var v = factor()
            while (pos < s.length && (s[pos] == '*' || s[pos] == '/')) {
                val op = s[pos++]
                val r = factor()
                if (op == '/' && r == 0.0) throw ArithmeticException("div0")
                v = if (op == '*') v * r else v / r
            }
            return v
        }

        fun factor(): Double {
            var neg = false
            while (pos < s.length && (s[pos] == '-' || s[pos] == '+')) {
                if (s[pos] == '-') neg = !neg
                pos++
            }
            val st = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            if (st == pos) throw IllegalArgumentException("num")
            var v = s.substring(st, pos).toDouble()
            while (pos < s.length && s[pos] == '%') { v /= 100.0; pos++ }
            return if (neg) -v else v
        }
    }
}
