package com.iot.widget.data.db

import androidx.room.TypeConverter
import java.time.LocalTime

/** Data layer · teaches Room to store types SQLite does not know: LocalTime as minutes of the day. */
class Converters {
    @TypeConverter
    fun localTimeToMinutes(time: LocalTime): Int = time.hour * 60 + time.minute

    @TypeConverter
    fun minutesToLocalTime(minutes: Int): LocalTime = LocalTime.of(minutes / 60, minutes % 60)
}
