package com.example.patheaseapp.Hazard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

// Draws a simple round pin with a red cross inside, entirely in code —
// no drawable resource file needed.
fun createHazardMarkerIcon(context: Context, sizeDp: Int = 40): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val sizePx = (sizeDp * density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.RED
        style = Paint.Style.STROKE
        strokeWidth = sizePx * 0.08f
    }
    val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.RED
        style = Paint.Style.STROKE
        strokeWidth = sizePx * 0.12f
        strokeCap = Paint.Cap.ROUND
    }

    val radius = sizePx / 2f * 0.9f
    val cx = sizePx / 2f
    val cy = sizePx / 2f
    canvas.drawCircle(cx, cy, radius, circlePaint)
    canvas.drawCircle(cx, cy, radius, borderPaint)

    val crossSize = radius * 0.5f
    canvas.drawLine(cx - crossSize, cy, cx + crossSize, cy, crossPaint) // horizontal
    canvas.drawLine(cx, cy - crossSize, cx, cy + crossSize, crossPaint) // vertical

    return try {
        BitmapDescriptorFactory.fromBitmap(bitmap)
    } catch (e: Exception) {
        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
    }
}