package com.hisablist.app.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Share {

    private fun line(e: Entry, lang: String, shop: Boolean): String {
        val name = Disp.name(e, lang)
        if (!shop) return (if (e.checked) "✅ " else "⬜ ") + name + " – " + Disp.qty(e, lang)
        val t = e.total()
        return if (e.fixed != null) "• $name = ${Fmt.money(e.fixed)}"
        else if (e.rate != null && t != null) "• $name  ${Disp.qty(e, lang)} × ${Fmt.money(e.rate)}/${Disp.unit(e.rateUnit, lang)} = ${Fmt.money(t)}"
        else "• $name  ${Disp.qty(e, lang)}"
    }

    fun billText(doc: Doc, s: Settings, lang: String): String {
        val shop = s.mode == "SHOP"
        val date = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(doc.createdAt))
        val sb = StringBuilder()
        if (shop) {
            sb.append("🧾 ").append(s.shopName.ifBlank { S.get("app_title", lang) }).append('\n')
            if (s.phone.isNotBlank()) sb.append("📞 ").append(s.phone).append('\n')
            if (doc.title.isNotBlank()) sb.append(doc.title).append('\n')
        } else {
            sb.append("🛒 ").append(doc.title.ifBlank { S.get("list_default", lang) }).append('\n')
        }
        sb.append(date).append("\n────────────\n")
        doc.entries.forEach { sb.append(line(it, lang, shop)).append('\n') }
        if (shop) {
            sb.append("────────────\n")
            sb.append(S.get("bill_total", lang)).append(": ").append(Fmt.money(doc.total))
            if (doc.due() > 0) sb.append('\n').append(S.get("due", lang)).append(": ").append(Fmt.money(doc.due()))
            if (s.upi.isNotBlank()) sb.append('\n').append(S.get("pay_upi", lang)).append(": ").append(s.upi)
        }
        return sb.toString().trimEnd()
    }

    fun send(ctx: Context, text: String, bmp: Bitmap?) {
        val i = Intent(Intent.ACTION_SEND)
        if (bmp != null) {
            val dir = File(ctx.cacheDir, "qr").apply { mkdirs() }
            val f = File(dir, "bill_qr.png")
            FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)
            i.type = "image/png"
            i.putExtra(Intent.EXTRA_STREAM, uri)
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            i.type = "text/plain"
        }
        i.putExtra(Intent.EXTRA_TEXT, text)
        ctx.startActivity(Intent.createChooser(i, null))
    }
}
