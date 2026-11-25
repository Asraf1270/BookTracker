package com.openshelf.booktracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders 5 stars with proper full / half / empty states.
 *  - rating 4.5 → 4 full + 1 half + 0 empty
 *  - rating 3.7 → 3 full + 1 half + 1 empty   (rounded to nearest half)
 *  - rating 0.0 → 5 empty
 */
@Composable
fun RatingStars(
    rating: Float,
    modifier: Modifier = Modifier,
    starSize: Dp = 16.dp,
    filledColor: Color = MaterialTheme.colorScheme.tertiary,
    emptyColor: Color = MaterialTheme.colorScheme.outlineVariant
) {
    // Snap to nearest half-star
    val snapped = (rating * 2f).toInt().coerceIn(0, 10) / 2f

    Row(modifier = modifier) {
        for (i in 1..5) {
            val diff = snapped - (i - 1)
            val state = when {
                diff >= 1f -> StarState.FULL
                diff >= 0.5f -> StarState.HALF
                else -> StarState.EMPTY
            }
            Canvas(modifier = Modifier.size(starSize)) {
                drawStar(state, filledColor, emptyColor)
            }
        }
    }
}

private enum class StarState { FULL, HALF, EMPTY }

private fun DrawScope.drawStar(
    state: StarState,
    filledColor: Color,
    emptyColor: Color
) {
    val w = size.width
    val h = size.height
    val points = starPoints(w, h)

    fun pathFrom(indices: List<Int>): Path = Path().apply {
        moveTo(points[indices[0]].x, points[indices[0]].y)
        for (idx in indices.drop(1)) lineTo(points[idx].x, points[idx].y)
        close()
    }

    // Always draw the empty star as background
    drawPath(pathFrom((0 until 10).toList()), emptyColor)

    when (state) {
        StarState.FULL -> {
            drawPath(pathFrom((0 until 10).toList()), filledColor)
        }
        StarState.HALF -> {
            // Clip to left half
            val half = Path().apply {
                moveTo(0f, 0f)
                lineTo(w / 2f, 0f)
                lineTo(w / 2f, h)
                lineTo(0f, h)
                close()
            }
            clipPath(half) {
                drawPath(pathFrom((0 until 10).toList()), filledColor)
            }
        }
        StarState.EMPTY -> Unit
    }
}

/** Returns 10 points alternating outer/inner radius around the center. */
private fun starPoints(w: Float, h: Float): List<Offset> {
    val cx = w / 2f
    val cy = h / 2f
    val outerR = w / 2f * 0.95f
    val innerR = outerR * 0.4f
    val points = ArrayList<Offset>(10)
    for (i in 0 until 10) {
        val angle = -PI / 2 + i * PI / 5
        val r = if (i % 2 == 0) outerR else innerR
        points.add(
            Offset(
                x = cx + (r * cos(angle)).toFloat(),
                y = cy + (r * sin(angle)).toFloat()
            )
        )
    }
    return points
}