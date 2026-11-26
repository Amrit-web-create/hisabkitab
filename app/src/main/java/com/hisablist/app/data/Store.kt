package com.hisablist.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Offline persistence: everything lives in SharedPreferences as JSON. */
class Store(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("hisab_list", Context.MODE_PRIVATE)

    fun loadSettings(): Settings {
        val j = sp.getString("settings", null) ?: return Settings()
        return try {
            val o = JSONObject(j)
            Settings(
                shopName = o.optString("shop"),
                phone = o.optString("phone"),
                upi = o.optString("upi"),
                lang = o.optString("lang", "en"),
                mode = o.optString("mode", "HOME")
            )
        } catch (e: Exception) {
            Settings()
        }
    }

    fun saveSettings(s: Settings) {
        val o = JSONObject()
            .put("shop", s.shopName).put("phone", s.phone).put("upi", s.upi)
            .put("lang", s.lang).put("mode", s.mode)
        sp.edit().putString("settings", o.toString()).apply()
    }

    fun loadRates(): Map<String, Rate> {
        val j = sp.getString("rates", null) ?: return emptyMap()
        return try {
            val o = JSONObject(j)
            val m = LinkedHashMap<String, Rate>()
            for (k in o.keys()) {
                val r = o.getJSONObject(k)
                m[k] = Rate(r.getDouble("a"), r.getString("u"))
            }
            m
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun saveRates(m: Map<String, Rate>) {
        val o = JSONObject()
        for ((k, r) in m) o.put(k, JSONObject().put("a", r.amount).put("u", r.unit))
        sp.edit().putString("rates", o.toString()).apply()
    }

    fun loadDocs(): List<Doc> {
        val j = sp.getString("docs", null) ?: return emptyList()
        return try {
            val a = JSONArray(j)
            (0 until a.length()).map { docFrom(a.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveDocs(list: List<Doc>) {
        val a = JSONArray()
        list.sortedByDescending { it.createdAt }.take(500).forEach { a.put(docTo(it)) }
        sp.edit().putString("docs", a.toString()).apply()
    }

    fun loadCurrentId(): Long = sp.getLong("current", -1L)

    fun saveCurrentId(id: Long) {
        sp.edit().putLong("current", id).apply()
    }

    private fun docTo(d: Doc): JSONObject {
        val es = JSONArray()
        for (e in d.entries) {
            val o = JSONObject()
                .put("id", e.id).put("key", e.key).put("name", e.name)
                .put("qty", e.qty).put("unit", e.unit)
                .put("rateUnit", e.rateUnit).put("checked", e.checked)
            if (e.rate != null) o.put("rate", e.rate)
            if (e.fixed != null) o.put("fixed", e.fixed)
            es.put(o)
        }
        return JSONObject()
            .put("id", d.id).put("title", d.title).put("createdAt", d.createdAt)
            .put("status", d.status).put("paid", d.paid).put("mode", d.mode)
            .put("entries", es)
    }

    private fun docFrom(o: JSONObject): Doc {
        val a = o.getJSONArray("entries")
        val es = (0 until a.length()).map {
            val e = a.getJSONObject(it)
            Entry(
                id = e.getLong("id"),
                key = e.getString("key"),
                name = e.optString("name"),
                qty = e.getDouble("qty"),
                unit = e.getString("unit"),
                rate = if (e.has("rate")) e.getDouble("rate") else null,
                rateUnit = e.optString("rateUnit", "kg"),
                fixed = if (e.has("fixed")) e.getDouble("fixed") else null,
                checked = e.optBoolean("checked", false)
            )
        }
        return Doc(
            id = o.getLong("id"),
            title = o.optString("title"),
            createdAt = o.getLong("createdAt"),
            entries = es,
            status = o.optString("status", "OPEN"),
            paid = o.optDouble("paid", 0.0),
            mode = o.optString("mode", "HOME")
        )
    }
}
