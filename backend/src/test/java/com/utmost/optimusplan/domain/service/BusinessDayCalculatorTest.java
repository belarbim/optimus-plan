package com.utmost.optimusplan.domain.service;

import com.utmost.optimusplan.domain.model.PublicHoliday;
import com.utmost.optimusplan.domain.model.WorkingDaysConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessDayCalculatorTest {

    // ── countBusinessDays ─────────────────────────────────────────────────────

    @Test
    void countBusinessDays_fullWeekNoHolidays_returns5() {
        // 2024-01-08 (Mon) to 2024-01-12 (Fri) = 5 days
        int days = BusinessDayCalculator.countBusinessDays(
                LocalDate.of(2024, 1, 8),
                LocalDate.of(2024, 1, 12),
                List.of());

        assertThat(days).isEqualTo(5);
    }

    @Test
    void countBusinessDays_weekendOnly_returns0() {
        // 2024-01-06 (Sat) to 2024-01-07 (Sun)
        int days = BusinessDayCalculator.countBusinessDays(
                LocalDate.of(2024, 1, 6),
                LocalDate.of(2024, 1, 7),
                List.of());

        assertThat(days).isZero();
    }

    @Test
    void countBusinessDays_singleBusinessDay_returns1() {
        int days = BusinessDayCalculator.countBusinessDays(
                LocalDate.of(2024, 1, 8),
                LocalDate.of(2024, 1, 8),
                List.of());

        assertThat(days).isEqualTo(1);
    }

    @Test
    void countBusinessDays_singleSaturday_returns0() {
        int days = BusinessDayCalculator.countBusinessDays(
                LocalDate.of(2024, 1, 6),
                LocalDate.of(2024, 1, 6),
                List.of());

        assertThat(days).isZero();
    }

    @Test
    void countBusinessDays_withHolidayOnBusinessDay_subtractsOne() {
        // Mon–Fri: 5 days; holiday on Wednesday
        PublicHoliday holiday = holiday(LocalDate.of(2024, 1, 10), false); // Wednesday

        int days = BusinessDayCalculator.countBusinessDays(
                LocalDate.of(2024, 1, 8),
                LocalDate.of(2024, 1, 12),
                List.of(holiday));

        assertThat(days).isEqualTo(4);
    }

    @Test
    void countBusinessDays_holidayOnWeekend_doesNotSubtract() {
        // Sat holiday should not affect business day count
        PublicHoliday holiday = holiday(LocalDate.of(2024, 1, 6), false); // Saturday

        int days = BusinessDayCalculator.countBusinessDays(
                LocalDate.of(2024, 1, 8),
                LocalDate.of(2024, 1, 12),
                List.of(holiday));

        assertThat(days).isEqualTo(5);
    }

    @Test
    void countBusinessDays_holidayOutsideRange_doesNotSubtract() {
        PublicHoliday holiday = holiday(LocalDate.of(2024, 1, 2), false); // outside Mon-Fri range

        int days = BusinessDayCalculator.countBusinessDays(
                LocalDate.of(2024, 1, 8),
                LocalDate.of(2024, 1, 12),
                List.of(holiday));

        assertThat(days).isEqualTo(5);
    }

    @Test
    void countBusinessDays_january2024_returns23() {
        // January 2024: 31 days, 1+6+7 = 9 weekend days, 0 holidays = 22 business days
        // Actually: Jan 1 Mon, Jan 6 Sat, Jan 7 Sun, Jan 13 Sat, Jan 14 Sun,
        // Jan 20 Sat, Jan 21 Sun, Jan 27 Sat, Jan 28 Sun → 9 weekend days → 31-9 = 22?
        // Let me just assert the count
        int days = BusinessDayCalculator.countBusinessDays(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                List.of());

        // Jan 2024: Mon Jan 1 is a weekday. 31 days - 9 weekends = 23 business days
        assertThat(days).isEqualTo(23);
    }

    @ParameterizedTest
    @CsvSource({
            "2024-01-01, 2024-01-31, 23", // January 2024
            "2024-02-01, 2024-02-29, 21", // February 2024 (leap year)
            "2024-03-01, 2024-03-31, 21", // March 2024
    })
    void countBusinessDays_knownMonths_matchExpected(String start, String end, int expected) {
        int days = BusinessDayCalculator.countBusinessDays(
                LocalDate.parse(start), LocalDate.parse(end), List.of());

        assertThat(days).isEqualTo(expected);
    }

    @Test
    void countBusinessDays_recurringHolidayMatchesYearIndependently() {
        // Recurring holiday: Jan 1 (any year)
        PublicHoliday newYear = holiday(LocalDate.of(2023, 1, 1), true);

        // In Jan 2024, Jan 1 is a Monday
        int days = BusinessDayCalculator.countBusinessDays(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 7),
                List.of(newYear));

        // Mon–Sun: 5 business days minus Jan 1 (recurring Mon) = 4
        assertThat(days).isEqualTo(4);
    }

    // ── getWorkingDays ────────────────────────────────────────────────────────

    @Test
    void getWorkingDays_withConfigOverride_returnsConfiguredValue() {
        WorkingDaysConfig config = WorkingDaysConfig.builder()
                .id(UUID.randomUUID())
                .month("2024-01")
                .avgDaysWorked(new BigDecimal("18.5"))
                .build();

        BigDecimal days = BusinessDayCalculator.getWorkingDays(
                YearMonth.of(2024, 1),
                List.of(),
                Optional.of(config));

        assertThat(days).isEqualByComparingTo(new BigDecimal("18.5"));
    }

    @Test
    void getWorkingDays_withoutConfig_returnsActualBusinessDayCount() {
        // Jan 2024: 23 business days
        BigDecimal days = BusinessDayCalculator.getWorkingDays(
                YearMonth.of(2024, 1),
                List.of(),
                Optional.empty());

        assertThat(days).isEqualByComparingTo(new BigDecimal("23"));
    }

    @Test
    void getWorkingDays_withoutConfig_andOneHoliday_returnsReducedCount() {
        PublicHoliday holiday = holiday(LocalDate.of(2024, 1, 15), false); // Monday

        BigDecimal days = BusinessDayCalculator.getWorkingDays(
                YearMonth.of(2024, 1),
                List.of(holiday),
                Optional.empty());

        assertThat(days).isEqualByComparingTo(new BigDecimal("22"));
    }

    @Test
    void getWorkingDays_configOverrideTakesPrecedenceOverHolidays() {
        WorkingDaysConfig config = WorkingDaysConfig.builder()
                .id(UUID.randomUUID())
                .month("2024-01")
                .avgDaysWorked(new BigDecimal("20"))
                .build();
        PublicHoliday holiday = holiday(LocalDate.of(2024, 1, 15), false);

        BigDecimal days = BusinessDayCalculator.getWorkingDays(
                YearMonth.of(2024, 1),
                List.of(holiday),
                Optional.of(config));

        // Override takes precedence, holiday is ignored for this calculation
        assertThat(days).isEqualByComparingTo(new BigDecimal("20"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static PublicHoliday holiday(LocalDate date, boolean recurring) {
        return PublicHoliday.builder()
                .id(UUID.randomUUID())
                .date(date)
                .name("Holiday")
                .locale("FR")
                .recurring(recurring)
                .build();
    }
}
