package com.cyclediary.app.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthDay = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
private val monthDayWeek = DateTimeFormatter.ofPattern("M月d日 EEE", Locale.CHINA)
private val fullDate = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)

fun LocalDate.formatMonthDay(): String = format(monthDay)

fun LocalDate.formatMonthDayWeek(): String = format(monthDayWeek)

fun LocalDate.formatFull(): String = format(fullDate)

/** Material DatePicker 用的是 UTC 毫秒，这里做无时区的换算，避免差一天。 */
fun LocalDate.toUtcMillis(): Long = toEpochDay() * 86_400_000L

fun Long.toLocalDateUtc(): LocalDate = LocalDate.ofEpochDay(this / 86_400_000L)
