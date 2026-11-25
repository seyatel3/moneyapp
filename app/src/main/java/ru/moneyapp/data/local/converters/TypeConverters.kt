package ru.moneyapp.data.local.converters

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TypeConverters {

    @TypeConverter
    fun fromLocalDate(value: Long?): LocalDate? =
        value?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun toLocalDate(date: LocalDate?): Long? =
        date?.toEpochDay()

    @TypeConverter
    fun fromInstant(value: Long?): Instant? =
        value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun toInstant(instant: Instant?): Long? =
        instant?.toEpochMilli()
}
