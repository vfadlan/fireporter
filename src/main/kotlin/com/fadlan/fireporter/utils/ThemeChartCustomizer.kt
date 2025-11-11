package com.fadlan.fireporter.utils

import net.sf.jasperreports.charts.JRChart
import net.sf.jasperreports.charts.JRChartCustomizer
import org.jfree.chart.JFreeChart
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer
import java.awt.BasicStroke
import java.awt.Color
import kotlin.random.Random


class ThemeChartCustomizer : JRChartCustomizer {
    override fun customize(chart: JFreeChart?, jasperChart: JRChart) {
        try {
            val plot = chart?.plot as? XYPlot ?: return
            val renderer = plot.renderer as? XYLineAndShapeRenderer ?: return

            val seriesCount = plot.dataset.seriesCount
            for (i in 0 until seriesCount) {
                val randomDarkColor = generateRandomDarkColor()
                renderer.setSeriesPaint(i, randomDarkColor)
                renderer.setSeriesStroke(i, BasicStroke(1.5f))
                renderer.setSeriesShapesVisible(i, false)
                renderer.setSeriesShapesFilled(i, true)
            }
        } catch (e: Exception) {
            println("Error customizing chart: ${e.message}")
        }
    }

    private fun generateRandomDarkColor(): Color {
        val hue = Random.nextFloat()
        val saturation = 0.4f + Random.nextFloat() * 0.5f
        val brightness = 0.2f + Random.nextFloat() * 0.5f
        return Color.getHSBColor(hue, saturation, brightness)
    }
}