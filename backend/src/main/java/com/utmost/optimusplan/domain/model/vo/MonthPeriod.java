package com.utmost.optimusplan.domain.model.vo;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public record MonthPeriod(String value) {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    public MonthPeriod {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Month must not be blank");
        }
        try {
            YearMonth.parse(value, FMT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Month must be in yyyy-MM format");
        }
    }

    public static MonthPeriod of(String value) {
        return new MonthPeriod(value);
    }

    public YearMonth toYearMonth() {
        return YearMonth.parse(value, FMT);
    }

    public LocalDate firstDay() {
        return toYearMonth().atDay(1);
    }

    public LocalDate lastDay() {
        return toYearMonth().atEndOfMonth();
    }
}
