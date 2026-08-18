package com.example.worktimeoff.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BusinessDayCalculatorTest {

    // -----------------------------------------------------------------------
    // countBusinessDays
    // -----------------------------------------------------------------------

    @Test
    void decTwentyFourToTwentyEight_returnsTwo() {
        // 2020: Dec 24 (Thu) ✓, Dec 25 Christmas (Fri) ✗, Dec 26 (Sat) ✗, Dec 27 (Sun) ✗, Dec 28 (Mon) ✓
        // Same pattern in 2026: Dec 24 (Thu) ✓, Dec 25 Christmas (Fri) ✗, Dec 26 (Sat) ✗, Dec 27 (Sun) ✗, Dec 28 (Mon) ✓
        long days = BusinessDayCalculator.countBusinessDays(
            LocalDate.of(2020, 12, 24), LocalDate.of(2020, 12, 28));
        assertEquals(2, days, "Dec 24-28 2020 should be 2 business days (Christmas on Fri, Dec 26/27 weekend)");
    }

    @Test
    void singleWeekday_returnsOne() {
        long days = BusinessDayCalculator.countBusinessDays(
            LocalDate.of(2024, 1, 3), LocalDate.of(2024, 1, 3)); // Wednesday
        assertEquals(1, days);
    }

    @Test
    void saturday_returnsZero() {
        long days = BusinessDayCalculator.countBusinessDays(
            LocalDate.of(2024, 1, 6), LocalDate.of(2024, 1, 6)); // Saturday
        assertEquals(0, days);
    }

    @Test
    void sunday_returnsZero() {
        long days = BusinessDayCalculator.countBusinessDays(
            LocalDate.of(2024, 1, 7), LocalDate.of(2024, 1, 7)); // Sunday
        assertEquals(0, days);
    }

    @Test
    void startAfterEnd_returnsZero() {
        long days = BusinessDayCalculator.countBusinessDays(
            LocalDate.of(2024, 1, 10), LocalDate.of(2024, 1, 5));
        assertEquals(0, days);
    }

    @Test
    void fullWorkWeek_returnsFive() {
        // March 2-6 2026: Mon, Tue, Wed, Thu, Fri – no federal holidays
        long days = BusinessDayCalculator.countBusinessDays(
            LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 6));
        assertEquals(5, days);
    }

    @Test
    void weekWithThanksgiving_returnsFour() {
        // Thanksgiving 2024 = Nov 28 (Thursday)
        // Mon Nov 25 – Fri Nov 29: 5 days minus Thanksgiving = 4
        long days = BusinessDayCalculator.countBusinessDays(
            LocalDate.of(2024, 11, 25), LocalDate.of(2024, 11, 29));
        assertEquals(4, days);
    }

    @Test
    void julyFourthOnWeekday_isExcluded() {
        // July 4 2024 is Thursday
        long days = BusinessDayCalculator.countBusinessDays(
            LocalDate.of(2024, 7, 4), LocalDate.of(2024, 7, 4));
        assertEquals(0, days);
    }

    @Test
    void newYearsDayOnMonday_isExcluded() {
        // Jan 1 2024 is Monday
        long days = BusinessDayCalculator.countBusinessDays(
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1));
        assertEquals(0, days);
    }

    @Test
    void spansMultipleYears_countsCorrectly() {
        // Dec 30 2024 (Mon) and Jan 2 2025 (Thu) are business days
        // Dec 31 (Tue), Jan 1 (Wed, New Year's-excluded), Jan 2 (Thu) = 3 biz days
        long days = BusinessDayCalculator.countBusinessDays(
            LocalDate.of(2024, 12, 30), LocalDate.of(2025, 1, 2));
        // Dec30(Mon), Dec31(Tue) = 2; Jan1=holiday; Jan2(Thu)=1 => 3
        assertEquals(3, days);
    }

    // -----------------------------------------------------------------------
    // getFederalHolidays
    // -----------------------------------------------------------------------

    @Test
    void federalHolidays2024_containsExpectedDates() {
        Set<LocalDate> holidays = BusinessDayCalculator.getFederalHolidays(2024);
        // Christmas Dec 25 (Wed)
        assertTrue(holidays.contains(LocalDate.of(2024, 12, 25)));
        // New Year's Jan 1 (Mon)
        assertTrue(holidays.contains(LocalDate.of(2024, 1, 1)));
        // Independence Day Jul 4 (Thu)
        assertTrue(holidays.contains(LocalDate.of(2024, 7, 4)));
        // Thanksgiving 4th Thu of Nov = Nov 28
        assertTrue(holidays.contains(LocalDate.of(2024, 11, 28)));
        // Labor Day 1st Mon of Sep = Sep 2
        assertTrue(holidays.contains(LocalDate.of(2024, 9, 2)));
    }

    @Test
    void christmasOnSaturday_observedFriday() {
        // Christmas 2021: Dec 25 is Saturday -> observed Dec 24 (Friday)
        Set<LocalDate> holidays = BusinessDayCalculator.getFederalHolidays(2021);
        assertTrue(holidays.contains(LocalDate.of(2021, 12, 24)),
            "Christmas observed on Friday when Dec 25 falls on Saturday");
    }

    @Test
    void newYearsDayOnSunday_observedMonday() {
        // New Year's Day 2023: Jan 1 is Sunday -> observed Jan 2 (Monday)
        Set<LocalDate> holidays = BusinessDayCalculator.getFederalHolidays(2023);
        assertTrue(holidays.contains(LocalDate.of(2023, 1, 2)),
            "New Year's Day observed on Monday when Jan 1 falls on Sunday");
    }
}
