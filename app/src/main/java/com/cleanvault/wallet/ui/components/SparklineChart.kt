package com.cleanvault.wallet.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cleanvault.wallet.ui.theme.VaultGreen
import com.cleanvault.wallet.ui.theme.VaultRed

@Composable
fun SparklineChart(
    points: List<Double>,
    modifier: Modifier = Modifier,
    isPositive: Boolean = true,
    height: Dp = 120.dp,
    showGradientFill: Boolean = true
) {
    if (points.size < 2) return

    val strokeColor = if (isPositive) VaultGreen else VaultRed
    val gradientBottom = strokeColor.copy(alpha = 0.0f)
    val gradientTop = strokeColor.copy(alpha = if (showGradientFill) 0.25f else 0.0f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val width = size.width
        val canvasHeight = size.height
        val min = points.minOrNull() ?: 0.0
        val max = points.maxOrNull() ?: 1.0
        val range = (max - min).coerceAtLeast(0.0001)

        val stepX = width / (points.size - 1)
        val strokePath = Path()
        val fillPath = Path()

        points.forEachIndexed { i, p ->
            val x = i * stepX
            val normalizedY = ((p - min) / range).toFloat()
            // Invert Y because canvas coordinates have (0,0) at top-left
            val y = canvasHeight - (normalizedY * (canvasHeight * 0.85f)) - (canvasHeight * 0.05f)

            if (i == 0) {
                strokePath.moveTo(x, y)
                fillPath.moveTo(x, canvasHeight)
                fillPath.lineTo(x, y)
            } else {
                val prevX = (i - 1) * stepX
                val prevNormY = ((points[i - 1] - min) / range).toFloat()
                val prevY = canvasHeight - (prevNormY * (canvasHeight * 0.85f)) - (canvasHeight * 0.05f)

                // Smooth cubic Bezier interpolation
                val cx1 = prevX + (stepX / 2f)
                val cy1 = prevY
                val cx2 = prevX + (stepX / 2f)
                val cy2 = y
                strokePath.cubicTo(cx1, cy1, cx2, cy2, x, y)
                fillPath.cubicTo(cx1, cy1, cx2, cy2, x, y)
            }
        }

        fillPath.lineTo(width, canvasHeight)
        fillPath.close()

        if (showGradientFill) {
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(gradientTop, gradientBottom),
                    startY = 0f,
                    endY = canvasHeight
                )
            )
        }

        drawPath(
            path = strokePath,
            color = strokeColor,
            style = Stroke(width = 2.5f.dp.toPx())
        )
    }
}
