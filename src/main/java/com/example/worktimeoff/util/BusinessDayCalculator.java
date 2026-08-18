package com.example.worktimeoff.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class for computing business days, excluding weekends and US federal holidays.
 */
public final class BusinessDayCalculator {

    private BusinessDayCalculator() {}

    /**
     * Fixed-date US federal holidays (month/day, year-independent).
     * Observed dates (Monday shifts) are handled in {@link #getFederalHolidays(int)}.
     */
    private static final Set<MonthDay> FIXED_HOLIDAYS = Set.of(
        MonthDay.of(1, 1),   // New Year's Day
        MonthDay.of(6, 19),  // Juneteenth
        MonthDay.of(7, 4),   // Independence Day
        MonthDay.of(11, 11), // Veterans Day
        MonthDay.of(12, 25)  // Christmas Day
    );

    /**
     * Returns all federal holiday dates for the given year, including observed (Monday) substitutions.
     */
    public static Set<LocalDate> getFederalHolidays(int year) {
        // Fixed holidays with Saturday->Friday and Sunday->Monday substitution
        Set<LocalDate> holidays = FIXED_HOLIDAYS.stream()
            .flatMap(md -> {
                LocalDate actual = md.atYear(year);
                return Stream.of(actual, getObservedDate(actual));
            })
            .collect(Collectors.toSet());

        // MLK Jr. Day – 3rd Monday of January
        holidays.add(nthDayOfWeekInMonth(year, 1, DayOfWeek.MONDAY, 3));
        // Presidents' Day – 3rd Monday of February
        holidays.add(nthDayOfWeekInMonth(year, 2, DayOfWeek.MONDAY, 3));
        // Memorial Day – last Monday of May
        holidays.add(lastMondayOfMonth(year, 5));
        // Labor Day – 1st Monday of September
        holidays.add(nthDayOfWeekInMonth(year, 9, DayOfWeek.MONDAY, 1));
        // Columbus Day – 2nd Monday of October
        holidays.add(nthDayOfWeekInMonth(year, 10, DayOfWeek.MONDAY, 2));
        // Thanksgiving – 4th Thursday of November
        holidays.add(nthDayOfWeekInMonth(year, 11, DayOfWeek.THURSDAY, 4));

        return holidays;
    }

    /**
     * Counts business days (weekdays that are not federal holidays) between start and end dates, inclusive.
     */
    public static long countBusinessDays(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) return 0;

        // Pre-compute holidays for all years spanned by the range
        int startYear = start.getYear();
        int endYear = end.getYear();
        Set<LocalDate> holidays = Stream.iterate(startYear, y -> y <= endYear, y -> y + 1)
            .flatMap(y -> getFederalHolidays(y).stream())
            .collect(Collectors.toSet());

        long count = 0;
        LocalDate current = start;
        while (!current.isAfter(end)) {
            DayOfWeek dow = current.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY && !holidays.contains(current)) {
                count++;
            }
            current = current.plusDays(1);
        }
        return count;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private static LocalDate getObservedDate(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case SATURDAY -> date.minusDays(1); // observed Friday
            case SUNDAY   -> date.plusDays(1);  // observed Monday
            default       -> date;
        };
    }

    private static LocalDate nthDayOfWeekInMonth(int year, int month, DayOfWeek dow, int n) {
        LocalDate first = LocalDate.of(year, month, 1);
        int offset = (dow.getValue() - first.getDayOfWeek().getValue() + 7) % 7;
        return first.plusDays(offset + (long) (n - 1) * 7);
    }

    private static LocalDate lastMondayOfMonth(int year, int month) {
        LocalDate lastDay = LocalDate.of(year, month, 1).withDayOfMonth(
            LocalDate.of(year, month, 1).lengthOfMonth());
        int offset = (lastDay.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue() + 7) % 7;
        return lastDay.minusDays(offset);
    }
}
