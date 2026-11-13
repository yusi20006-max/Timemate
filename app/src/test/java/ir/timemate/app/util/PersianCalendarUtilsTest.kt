package ir.timemate.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PersianCalendarUtilsTest {
    @Test
    fun nowruz1403ConvertsToGregorianMarch20_2024() {
        val gregorian = PersianCalendarUtils.jalaliToGregorian(1403, 1, 1)
        assertEquals(2024, gregorian.year)
        assertEquals(3, gregorian.month)
        assertEquals(20, gregorian.day)
    }

    @Test
    fun gregorianMarch20_2024ConvertsToNowruz1403() {
        val jalali = PersianCalendarUtils.gregorianToJalali(2024, 3, 20)
        assertEquals(1403, jalali.year)
        assertEquals(1, jalali.month)
        assertEquals(1, jalali.day)
    }

    @Test
    fun dateFormattingUsesStableNumericContract() {
        assertEquals("1403/01/01", PersianCalendarUtils.gregorianToJalali(2024, 3, 20).formatNumeric())
        assertEquals("۱۴۰۳/۰۱/۰۱", toPersianDigits("1403/01/01"))
    }
}
