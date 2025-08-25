package com.fadlan.fireporter.utils

import net.sf.jasperreports.engine.design.JasperDesign
import net.sf.jasperreports.engine.design.JRDesignGroup
import net.sf.jasperreports.engine.design.JRDesignBand
import net.sf.jasperreports.engine.design.JRDesignComponentElement
import net.sf.jasperreports.engine.design.JRDesignStaticText
import net.sf.jasperreports.engine.type.ModeEnum
import net.sf.jasperreports.components.table.StandardTable
import net.sf.jasperreports.components.table.StandardColumn
import net.sf.jasperreports.components.table.StandardColumnGroup
import net.sf.jasperreports.components.table.DesignCell
import java.awt.Color

class DynamicTableStyler {
    fun applyDynamicHeaderStyling(jasperDesign: JasperDesign, themeColorHex: String) {
        val backgroundColor = Color.decode(themeColorHex)

        // Find the single table in the report
        val group = jasperDesign.groupsList?.first() as? JRDesignGroup
        val band = group?.groupHeaderSection?.bands?.first() as? JRDesignBand
        val componentElement = band?.elements?.first() as? JRDesignComponentElement
        val table = componentElement?.component as? StandardTable

        table?.columns?.forEach { column ->
            when (column) {
                is StandardColumn -> {
                    column.columnHeader?.let { header ->
                        (header as? DesignCell)?.elements?.forEach { element ->
                            if (element is JRDesignStaticText) {
                                element.mode = ModeEnum.OPAQUE
                                element.backcolor = backgroundColor
                                element.forecolor = Color.WHITE
                            }
                        }
                    }
                }
                is StandardColumnGroup -> {
                    // Handle the "Balance Left" column group
                    column.columnHeader?.let { header ->
                        (header as? DesignCell)?.elements?.forEach { element ->
                            if (element is JRDesignStaticText) {
                                element.mode = ModeEnum.OPAQUE
                                element.backcolor = backgroundColor
                                element.forecolor = Color.WHITE
                            }
                        }
                    }
                    // Handle sub-columns (Source, Dest.)
                    column.columns?.forEach { subColumn ->
                        if (subColumn is StandardColumn) {
                            subColumn.columnHeader?.let { header ->
                                (header as? DesignCell)?.elements?.forEach { element ->
                                    if (element is JRDesignStaticText) {
                                        element.mode = ModeEnum.OPAQUE
                                        element.backcolor = backgroundColor
                                        element.forecolor = Color.WHITE
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}