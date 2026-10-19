package com.mistareas.app

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE d MMM", Locale("es", "MX"))

/** Texto corto para mostrar una fecha: Hoy, Mañana, Atrasada, o el día. */
fun dateLabelFor(d: LocalDate?): String {
    if (d == null) return "Sin fecha"
    val today = LocalDate.now()
    return when {
        d == today -> "Hoy"
        d == today.plusDays(1) -> "Mañana"
        d.isBefore(today) -> "Atrasada · " + d.format(DATE_FORMAT)
        else -> d.format(DATE_FORMAT)
    }
}

data class Task(
    val id: Long,
    val title: String,
    val date: LocalDate?,
    val done: Boolean
) {
    fun isOverdue(): Boolean = !done && date != null && date.isBefore(LocalDate.now())

    fun dateLabel(): String = dateLabelFor(date)
}
