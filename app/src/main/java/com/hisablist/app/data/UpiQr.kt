package com.hisablist.app.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.Locale

class QrGrid(val size: Int, private val dark: BooleanArray) {
    fun at(x: Int, y: Int): Boolean = dark[y * size + x]
}

/** Offline UPI QR: builds the standard upi://pay URI and encodes it on-device (ZXing core, no network). */
object UpiQr {

    fun uri(upiId: String, shopName: String, amount: Double, note: String = ""): String {
        val sb = StringBuilder("upi://pay?pa=").append(Uri.encode(upiId.trim(), "@.-_"))
        if (shopName.isNotBlank()) sb.append("&pn=").append(Uri.encode(shopName.trim()))
        if (amount > 0) sb.append("&am=").append(String.format(Locale.US, "%.2f", amount))
        sb.append("&cu=INR")
        if (note.isNotBlank()) sb.append("&tn=").append(Uri.encode(note.trim().take(40)))
        return sb.toString()
    }

    fun grid(content: String): QrGrid {
        val hints = mapOf<EncodeHintType, Any>(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val m = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 1, 1, hints)
        val n = m.width
        val data = BooleanArray(n * m.height)
        for (y in 0 until m.height) for (x in 0 until n) data[y * n + x] = m.get(x, y)
        return QrGrid(n, data)
    }

    /** Shareable PNG card: shop name, amount, QR, UPI id. */
    fun cardBitmap(grid: QrGrid, shop: String, amountText: String, upi: String): Bitmap {
        val module = 14
        val pad = 48
        val qrPx = grid.size * module
        val w = qrPx + pad * 2
        val h = w + 230
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)

        val ink = Color.parseColor("#FF18181B")
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink; style = Paint.Style.STROKE; strokeWidth = 8f }
        c.drawRect(4f, 4f, w - 4f, h - 4f, border)

        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink; textSize = 46f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
        }
        val amount = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink; textSize = 78f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
        }
        val small = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY; textSize = 34f; textAlign = Paint.Align.CENTER
        }
        c.drawText(shop.ifBlank { "Hisab & List" }, w / 2f, 86f, title)
        c.drawText(amountText, w / 2f, 176f, amount)

        val top = 210f
        val dot = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
        for (y in 0 until grid.size) for (x in 0 until grid.size) {
            if (grid.at(x, y)) {
                val l = pad + x * module.toFloat()
                val t = top + y * module
                c.drawRect(l, t, l + module, t + module, dot)
            }
        }
        c.drawText(upi, w / 2f, top + qrPx + 56f, small)
        return bmp
    }
}
