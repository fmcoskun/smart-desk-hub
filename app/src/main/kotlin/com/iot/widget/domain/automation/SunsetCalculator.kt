package com.iot.widget.domain.automation

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Domain layer · a place on Earth, in degrees; the sunset of the fixed demo location is calculated for it.
 */
data class GeoPoint(val latitude: Double, val longitude: Double)

/**
 * Domain layer · sunset time for "Auto-dim at sunset".
 *
 * Uses the classic sunrise/sunset algorithm from the US Naval Observatory's "Almanac for
 * Computers" (accurate to a minute or two, plenty for dimming a lamp). The location is fixed
 * to Istanbul so the app needs no location permission (spec §9b).
 */
object SunsetCalculator {
    val ISTANBUL = GeoPoint(41.0082, 28.9784)

    /** Official zenith: the sun's centre 50 arc-minutes below the horizon (refraction + radius). */
    private const val ZENITH = 90.833

    fun sunset(date: LocalDate, zone: ZoneId, at: GeoPoint = ISTANBUL): ZonedDateTime {
        val n = date.dayOfYear
        val lngHour = at.longitude / 15.0
        val t = n + (18.0 - lngHour) / 24.0                       // approximate time of setting

        val meanAnomaly = 0.9856 * t - 3.289
        val trueLongitude = norm360(
            meanAnomaly + 1.916 * sinD(meanAnomaly) + 0.020 * sinD(2 * meanAnomaly) + 282.634,
        )

        var rightAscension = norm360(degrees(atan(0.91764 * tanD(trueLongitude))))
        // put right ascension in the same quadrant as the true longitude, then convert to hours
        rightAscension += floor(trueLongitude / 90.0) * 90.0 - floor(rightAscension / 90.0) * 90.0
        rightAscension /= 15.0

        val sinDec = 0.39782 * sinD(trueLongitude)
        val cosDec = cos(asin(sinDec))
        val cosH = (cosD(ZENITH) - sinDec * sinD(at.latitude)) / (cosDec * cosD(at.latitude))
        val hourAngle = degrees(acos(cosH.coerceIn(-1.0, 1.0))) / 15.0

        val localMeanTime = hourAngle + rightAscension - 0.06571 * t - 6.622
        val utcHours = ((localMeanTime - lngHour) % 24.0 + 24.0) % 24.0
        val seconds = (utcHours * 3600).toLong()

        return date.atStartOfDay(ZoneOffset.UTC).plusSeconds(seconds).withZoneSameInstant(zone)
    }

    private fun norm360(deg: Double) = ((deg % 360.0) + 360.0) % 360.0
    private fun degrees(rad: Double) = Math.toDegrees(rad)
    private fun sinD(deg: Double) = sin(Math.toRadians(deg))
    private fun cosD(deg: Double) = cos(Math.toRadians(deg))
    private fun tanD(deg: Double) = tan(Math.toRadians(deg))
}
