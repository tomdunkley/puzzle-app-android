package com.tomdunkley.dailypuzzles.data.network

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Mirrors the server's today_iso() logic: the "day" starts at 8am UK time (Europe/London).
 * Before 8am London time, yesterday's date is returned — matching the puzzle_id the
 * server would serve for an offline cache lookup without a network round trip.
 */
fun todayUtcIso(): String {
    val londonTz = TimeZone.getTimeZone("Europe/London")
    val cal = Calendar.getInstance(londonTz)
    if (cal.get(Calendar.HOUR_OF_DAY) < 8) {
        cal.add(Calendar.DAY_OF_YEAR, -1)
    }
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    formatter.timeZone = londonTz
    return formatter.format(cal.time)
}
