package com.example.data.plan

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

interface TimeProvider {
    fun currentTimeMillis(): Long
    fun todayDateString(): String // YYYY-MM-DD
    fun currentMonthString(): String // YYYY-MM
    fun dayOfWeek(): Int // Calendar.SUNDAY ... Calendar.SATURDAY
    fun startOfDayMillis(dateString: String = todayDateString()): Long
    fun endOfDayMillis(nowMillis: Long = currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
    fun dueNowCutoff(nowMillis: Long = currentTimeMillis()): Long = nowMillis
    fun dueTodayCutoff(nowMillis: Long = currentTimeMillis()): Long = endOfDayMillis(nowMillis)
}

class SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()

    override fun todayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(currentTimeMillis()))
    }

    override fun currentMonthString(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(Date(currentTimeMillis()))
    }

    override fun dayOfWeek(): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = currentTimeMillis()
        return cal.get(Calendar.DAY_OF_WEEK)
    }

    override fun startOfDayMillis(dateString: String): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.parse(dateString)?.time ?: currentTimeMillis()
    }
}

class TestTimeProvider(
    var fixedTimeMillis: Long = System.currentTimeMillis()
) : TimeProvider {
    override fun currentTimeMillis(): Long = fixedTimeMillis

    override fun todayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(fixedTimeMillis))
    }

    override fun currentMonthString(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(Date(fixedTimeMillis))
    }

    override fun dayOfWeek(): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = fixedTimeMillis
        return cal.get(Calendar.DAY_OF_WEEK)
    }

    override fun startOfDayMillis(dateString: String): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.parse(dateString)?.time ?: fixedTimeMillis
    }

    fun advanceDays(days: Int) {
        fixedTimeMillis += days * 86_400_000L
    }

    fun advanceMillis(deltaMillis: Long) {
        fixedTimeMillis += deltaMillis
    }
}
