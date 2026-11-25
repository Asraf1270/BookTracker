package com.openshelf.booktracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.openshelf.booktracker.data.local.dao.SessionDao
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

@Composable
fun StreakHeatmap(
    days: List<SessionDao.DayAggregate>,
    modifier: Modifier = Modifier
) {
    val byDay = days.associateBy { it.day }

    val today = LocalDate.now()
    val startOfWindow = today.minusWeeks(52)
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    val totalDays = ChronoUnit.DAYS.between(startOfWindow, today).toInt() + 1
    val totalWeeks = (totalDays + 6) / 7

    val maxPages = days.maxOfOrNull { it.pages }?.coerceAtLeast(1) ?: 1

    // Read theme colors here — Canvas draw block is not @Composable
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val baseColor = MaterialTheme.colorScheme.primary
    val todayBorder = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier) {
        // Month labels row
        Row(modifier = Modifier.padding(start = 24.dp, bottom = 4.dp)) {
            var lastMonth = -1
            for (w in 0 until totalWeeks) {
                val weekStart = startOfWindow.plusDays((w * 7).toLong())
                val month = weekStart.monthValue
                if (month != lastMonth) {
                    Text(
                        text = weekStart.month.name.take(3),
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                        modifier = Modifier.width(14.dp)
                    )
                    lastMonth = month
                } else {
                    Spacer(Modifier.width(14.dp))
                }
            }
        }

        Row {
            // Day-of-week labels
            Column(
                modifier = Modifier.width(24.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val labels = listOf("Sun", "", "Tue", "", "Thu", "", "Sat")
                labels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                        modifier = Modifier.height(10.dp)
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)   // 7 rows × 12dp — fixed literal
            ) {
                val cell = 10.dp.toPx()
                val gap = 2.dp.toPx()

                for (w in 0 until totalWeeks) {
                    for (d in 0 until 7) {
                        val index = w * 7 + d
                        if (index >= totalDays) break
                        val date = startOfWindow.plusDays(index.toLong())
                        if (date.isAfter(today)) continue

                        val key = date.toString()
                        val pages = byDay[key]?.pages ?: 0
                        val color = if (pages == 0) emptyColor
                                    else lerp(baseColor, emptyColor, 1f - (pages.toFloat() / maxPages))

                        val topLeft = Offset(w * (cell + gap), d * (cell + gap))
                        drawRoundRect(
                            color = color,
                            topLeft = topLeft,
                            size = Size(cell, cell),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )

                        if (date == today) {
                            drawRoundRect(
                                color = todayBorder,
                                topLeft = topLeft,
                                size = Size(cell, cell),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Legend
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Less", style = MaterialTheme.typography.labelSmall, color = labelColor)
            Spacer(Modifier.width(4.dp))
            listOf(
                emptyColor,
                lerp(baseColor, emptyColor, 0.66f),
                lerp(baseColor, emptyColor, 0.33f),
                baseColor
            ).forEach { c ->
                Canvas(Modifier.size(10.dp)) {
                    drawRoundRect(
                        color = c,
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
                Spacer(Modifier.width(2.dp))
            }
            Spacer(Modifier.width(4.dp))
            Text("More", style = MaterialTheme.typography.labelSmall, color = labelColor)
        }
    }
}

private fun lerp(from: Color, to: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * f,
        green = from.green + (to.green - from.green) * f,
        blue = from.blue + (to.blue - from.blue) * f,
        alpha = from.alpha + (to.alpha - from.alpha) * f
    )
}