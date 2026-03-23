package com.utmost.optimusplan.domain.service;

import com.utmost.optimusplan.domain.model.PublicHoliday;
import com.utmost.optimusplan.domain.model.WorkingDaysConfig;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BusinessDayCalculator {

    private BusinessDayCalculator() {}

    /**
     * Counts the number of business days (Mon-Fri, excluding public holidays)
     * between {@code from} and {@code to}, both inclusive.
     */
    public static int countBusinessDays(LocalDate from, LocalDate to,
                                        List<PublicHoliday> holidays) {
        Set<LocalDate> holidayDates = resolvedHolidayDates(holidays, from, to);
        int count = 0;
        LocalDate d = from;
        while (!d.isAfter(to)) {
            if (isWeekday(d) && !holidayDates.contains(d)) {
                count++;
            }
            d = d.plusDays(1);
        }
        return count;
    }

    /**
     * Returns the configured average working days for the given month, falling back to
     * the actual count of business days when no configuration is present.
     */
    public static BigDecimal getWorkingDays(YearMonth month,
                                            List<PublicHoliday> holidays,
                                            Optional<WorkingDaysConfig> config) {
        return config
                .map(WorkingDaysConfig::getAvgDaysWorked)
                .orElse(BigDecimal.valueOf(
                        countBusinessDays(month.atDay(1), month.atEndOfMonth(), holidays)));
    }

    /**
     * Returns a two-element array: {@code [remaining, total]} business days.
     * <ul>
     *   <li>{@code remaining} – business days from {@code from} (adjusted to the next
     *       weekday when {@code from} is a weekend) to {@code monthEnd}, inclusive.</li>
     *   <li>{@code total} – business days for the full month starting on the 1st.</li>
     * </ul>
     */
    public static int[] remainingAndTotal(LocalDate from, LocalDate monthEnd,
                                          List<PublicHoliday> holidays) {
        // Advance to the next weekday if the reference date falls on a weekend
        LocalDate adjusted = from;
        while (!adjusted.isAfter(monthEnd) && !isWeekday(adjusted)) {
            adjusted = adjusted.plusDays(1);
        }
        int total     = countBusinessDays(from.withDayOfMonth(1), monthEnd, holidays);
        int remaining = countBusinessDays(adjusted, monthEnd, holidays);
        return new int[]{remaining, total};
    }

    public static boolean isWeekday(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static Set<LocalDate> resolvedHolidayDates(List<PublicHoliday> holidays,
                                                        LocalDate from, LocalDate to) {
        return holidays.stream()
                .flatMap(h -> {
                    if (!h.isRecurring()) {
                        return Stream.of(h.getDate());
                    }
                    // For recurring holidays, project the holiday onto every year in the range
                    return IntStream.rangeClosed(from.getYear(), to.getYear())
                            .mapToObj(y -> {
                                try {
                                    return h.getDate().withYear(y);
                                } catch (Exception e) {
                                    // e.g. Feb 29 in a non-leap year
                                    return null;
                                }
                            })
                            .filter(d -> d != null && !d.isBefore(from) && !d.isAfter(to));
                })
                .collect(Collectors.toSet());
    }
}
