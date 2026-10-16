package com.cleanvault.wallet.core.market

import com.cleanvault.wallet.data.model.AssetCategory
import com.cleanvault.wallet.data.model.MarketStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * Market Session Engine for US Equities & ETFs (NYSE/Nasdaq).
 * Handles:
 * - Timezone: America/New_York (Eastern Time - ET)
 * - Regular Trading Hours (RTH): Monday - Friday 09:30 AM to 04:00 PM ET
 * - Federal & NYSE official holiday closures
 * - Next market open calculation & formatting
 * - 24/7 bypass for pure crypto & cash stables
 */
class MarketHoursManager(
    private val zoneId: ZoneId = ZoneId.of("America/New_York")
) {

    companion object {
        val MARKET_OPEN_TIME: LocalTime = LocalTime.of(9, 30)
        val MARKET_CLOSE_TIME: LocalTime = LocalTime.of(16, 0)
    }

    /**
     * Determine real-time market status for an asset category.
     * Core Crypto (BTC, ETH, SOL) and Stables (USDC, USDT, USDY) trade 24/7 (always OPEN).
     * US Stocks and ETFs follow NYSE / Nasdaq trading calendars.
     */
    fun getCurrentMarketStatus(category: AssetCategory): MarketStatus {
        if (category == AssetCategory.CORE_CRYPTO ||
            category == AssetCategory.STABLECOIN ||
            category == AssetCategory.YIELD_STABLE
        ) {
            return MarketStatus.OPEN
        }

        val nowEt = ZonedDateTime.now(zoneId)
        return if (isMarketOpenNow(nowEt)) {
            MarketStatus.OPEN
        } else {
            MarketStatus.EXTENDED_OR_WEEKEND
        }
    }

    fun isMarketOpenNow(now: ZonedDateTime = ZonedDateTime.now(zoneId)): Boolean {
        val dayOfWeek = now.dayOfWeek
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false
        }

        val date = now.toLocalDate()
        if (isUsMarketHoliday(date)) {
            return false
        }

        val time = now.toLocalTime()
        return !time.isBefore(MARKET_OPEN_TIME) && time.isBefore(MARKET_CLOSE_TIME)
    }

    /**
     * Calculates the exact epoch milliseconds of the next NYSE/Nasdaq regular session open.
     */
    fun getNextMarketOpenEpochMillis(from: ZonedDateTime = ZonedDateTime.now(zoneId)): Long {
        return getNextMarketOpenDateTime(from).toInstant().toEpochMilli()
    }

    /**
     * Computes the ZonedDateTime for the next session open (09:30 AM ET).
     */
    fun getNextMarketOpenDateTime(from: ZonedDateTime = ZonedDateTime.now(zoneId)): ZonedDateTime {
        var candidateDate = from.toLocalDate()
        val candidateTime = from.toLocalTime()

        // If today is a trading day and it's before 9:30 AM ET, next open is today at 9:30 AM
        if (!isWeekend(candidateDate) && !isUsMarketHoliday(candidateDate) && candidateTime.isBefore(MARKET_OPEN_TIME)) {
            return candidateDate.atTime(MARKET_OPEN_TIME).atZone(zoneId)
        }

        // Otherwise advance day by day until we find a valid trading day
        candidateDate = candidateDate.plusDays(1)
        while (isWeekend(candidateDate) || isUsMarketHoliday(candidateDate)) {
            candidateDate = candidateDate.plusDays(1)
        }

        return candidateDate.atTime(MARKET_OPEN_TIME).atZone(zoneId)
    }

    /**
     * Formats next market open human-readably (e.g., "Monday at 9:30 AM ET", "Tomorrow at 9:30 AM ET").
     */
    fun getFormattedNextOpenTime(from: ZonedDateTime = ZonedDateTime.now(zoneId)): String {
        val nextOpen = getNextMarketOpenDateTime(from)
        val today = from.toLocalDate()
        val nextOpenDate = nextOpen.toLocalDate()

        val dayLabel = when {
            nextOpenDate == today -> "Today"
            nextOpenDate == today.plusDays(1) -> "Tomorrow"
            else -> nextOpenDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
        }

        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
        return "$dayLabel at ${nextOpen.format(timeFormatter)} ET"
    }

    private fun isWeekend(date: LocalDate): Boolean {
        return date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
    }

    /**
     * Checks if a given date is a NYSE/Nasdaq recognized holiday.
     * Evaluates standard observed rules:
     * - New Year's Day (Jan 1)
     * - Martin Luther King Jr. Day (Third Monday of January)
     * - Washington's Birthday / Presidents Day (Third Monday of February)
     * - Good Friday (Friday before Easter)
     * - Memorial Day (Last Monday of May)
     * - Juneteenth (June 19)
     * - Independence Day (July 4)
     * - Labor Day (First Monday of September)
     * - Thanksgiving Day (Fourth Thursday of November)
     * - Christmas Day (December 25)
     */
    fun isUsMarketHoliday(date: LocalDate): Boolean {
        val year = date.year

        // Fixed holidays with observation rules (if Sat -> Fri, if Sun -> Mon)
        val newYear = observeHoliday(LocalDate.of(year, Month.JANUARY, 1))
        val juneteenth = observeHoliday(LocalDate.of(year, Month.JUNE, 19))
        val independenceDay = observeHoliday(LocalDate.of(year, Month.JULY, 4))
        val christmas = observeHoliday(LocalDate.of(year, Month.DECEMBER, 25))

        if (date == newYear || date == juneteenth || date == independenceDay || date == christmas) {
            return true
        }

        // Floating holidays
        val mlkDay = LocalDate.of(year, Month.JANUARY, 1)
            .with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY))
            .plusWeeks(2)
        if (date == mlkDay) return true

        val presidentsDay = LocalDate.of(year, Month.FEBRUARY, 1)
            .with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY))
            .plusWeeks(2)
        if (date == presidentsDay) return true

        val memorialDay = LocalDate.of(year, Month.MAY, 1)
            .with(TemporalAdjusters.lastInMonth(DayOfWeek.MONDAY))
        if (date == memorialDay) return true

        val laborDay = LocalDate.of(year, Month.SEPTEMBER, 1)
            .with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY))
        if (date == laborDay) return true

        val thanksgiving = LocalDate.of(year, Month.NOVEMBER, 1)
            .with(TemporalAdjusters.firstInMonth(DayOfWeek.THURSDAY))
            .plusWeeks(3)
        if (date == thanksgiving) return true

        // Good Friday
        val goodFriday = calculateGoodFriday(year)
        if (date == goodFriday) return true

        return false
    }

    private fun observeHoliday(date: LocalDate): LocalDate {
        return when (date.dayOfWeek) {
            DayOfWeek.SATURDAY -> date.minusDays(1)
            DayOfWeek.SUNDAY -> date.plusDays(1)
            else -> date
        }
    }

    /**
     * Computes Good Friday using the Anonymous Gregorian algorithm for Easter Sunday.
     */
    private fun calculateGoodFriday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        val easterSunday = LocalDate.of(year, month, day)
        return easterSunday.minusDays(2)
    }
}
