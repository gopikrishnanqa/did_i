package com.druanlabs.didicheck.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.druanlabs.didicheck.ui.theme.BrandBlue
import com.druanlabs.didicheck.ui.theme.BrandCloudFill
import com.druanlabs.didicheck.ui.theme.BrandGreen
import com.druanlabs.didicheck.ui.theme.BrandSun

@Composable
fun SunCloudIllustration(modifier: Modifier = Modifier, size: Dp = 72.dp) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawCircle(
            color = BrandSun,
            radius = w * 0.28f,
            center = Offset(w * 0.62f, h * 0.38f),
        )
        drawCircle(color = BrandCloudFill.copy(alpha = 0.95f), radius = w * 0.22f, center = Offset(w * 0.38f, h * 0.58f))
        drawCircle(color = BrandCloudFill, radius = w * 0.26f, center = Offset(w * 0.58f, h * 0.55f))
        drawCircle(color = BrandCloudFill.copy(alpha = 0.9f), radius = w * 0.18f, center = Offset(w * 0.76f, h * 0.60f))
        drawRoundRect(
            color = BrandCloudFill,
            topLeft = Offset(w * 0.28f, h * 0.58f),
            size = Size(w * 0.52f, h * 0.22f),
            cornerRadius = CornerRadius(w * 0.1f, w * 0.1f),
        )
    }
}

@Composable
fun BackpackIllustration(modifier: Modifier = Modifier, size: Dp = 88.dp) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val bodyLeft = w * 0.22f
        val bodyTop = h * 0.28f
        val bodyW = w * 0.56f
        val bodyH = h * 0.58f
        // straps
        drawRoundRect(
            color = BrandBlue.copy(alpha = 0.55f),
            topLeft = Offset(w * 0.30f, h * 0.12f),
            size = Size(w * 0.12f, h * 0.22f),
            cornerRadius = CornerRadius(w * 0.06f),
        )
        drawRoundRect(
            color = BrandBlue.copy(alpha = 0.55f),
            topLeft = Offset(w * 0.58f, h * 0.12f),
            size = Size(w * 0.12f, h * 0.22f),
            cornerRadius = CornerRadius(w * 0.06f),
        )
        // body
        drawRoundRect(
            color = BrandBlue,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
        )
        // pocket
        drawRoundRect(
            color = BrandSun,
            topLeft = Offset(w * 0.34f, h * 0.48f),
            size = Size(w * 0.32f, h * 0.28f),
            cornerRadius = CornerRadius(w * 0.08f),
        )
        // zipper line
        drawLine(
            color = Color.White.copy(alpha = 0.55f),
            start = Offset(w * 0.50f, bodyTop + bodyH * 0.12f),
            end = Offset(w * 0.50f, bodyTop + bodyH * 0.32f),
            strokeWidth = 3f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun CheckBurstIllustration(modifier: Modifier = Modifier, size: Dp = 88.dp) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.52f
        val cy = h * 0.50f
        val rayColor = BrandGreen.copy(alpha = 0.45f)
        val rays = listOf(
            Offset(cx, cy - h * 0.42f) to Offset(cx, cy - h * 0.30f),
            Offset(cx + w * 0.30f, cy - h * 0.28f) to Offset(cx + w * 0.20f, cy - h * 0.18f),
            Offset(cx + w * 0.38f, cy) to Offset(cx + w * 0.26f, cy),
            Offset(cx + w * 0.28f, cy + h * 0.28f) to Offset(cx + w * 0.18f, cy + h * 0.18f),
            Offset(cx, cy + h * 0.42f) to Offset(cx, cy + h * 0.30f),
            Offset(cx - w * 0.28f, cy + h * 0.28f) to Offset(cx - w * 0.18f, cy + h * 0.18f),
            Offset(cx - w * 0.38f, cy) to Offset(cx - w * 0.26f, cy),
            Offset(cx - w * 0.30f, cy - h * 0.28f) to Offset(cx - w * 0.20f, cy - h * 0.18f),
        )
        rays.forEach { (start, end) ->
            drawLine(color = rayColor, start = start, end = end, strokeWidth = 5f, cap = StrokeCap.Round)
        }
        drawCircle(color = BrandGreen, radius = w * 0.26f, center = Offset(cx, cy))
        val check = Path().apply {
            moveTo(cx - w * 0.12f, cy + h * 0.01f)
            lineTo(cx - w * 0.02f, cy + h * 0.10f)
            lineTo(cx + w * 0.14f, cy - h * 0.10f)
        }
        drawPath(
            path = check,
            color = Color.White,
            style = Stroke(width = 6.5f, cap = StrokeCap.Round),
        )
    }
}
