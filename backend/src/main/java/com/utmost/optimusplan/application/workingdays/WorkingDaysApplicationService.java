package com.utmost.optimusplan.application.workingdays;

import com.utmost.optimusplan.domain.exception.DomainError;
import com.utmost.optimusplan.domain.exception.DomainException;
import com.utmost.optimusplan.domain.model.WorkingDaysConfig;
import com.utmost.optimusplan.domain.port.in.WorkingDaysUseCase;
import com.utmost.optimusplan.domain.port.out.WorkingDaysRepositoryPort;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class WorkingDaysApplicationService implements WorkingDaysUseCase {

    private static final String COL_MONTH    = "month";
    private static final String COL_AVG_DAYS = "avgDaysWorked";

    private final WorkingDaysRepositoryPort workingDaysRepo;

    public WorkingDaysApplicationService(WorkingDaysRepositoryPort workingDaysRepo) {
        this.workingDaysRepo = workingDaysRepo;
    }

    // -------------------------------------------------------------------------
    // WorkingDaysUseCase
    // -------------------------------------------------------------------------

    @Override
    public List<WorkingDaysConfig> importCsv(MultipartFile file) {
        List<String[]> rows = parseCsv(file);

        if (rows.isEmpty()) {
            throw new DomainException(new DomainError.Validation(
                    "CSV file is empty or contains only a header row"));
        }

        // Expect header row: month, avgDaysWorked
        String[] header = rows.get(0);
        int monthIdx    = findColumnIndex(header, COL_MONTH);
        int avgDaysIdx  = findColumnIndex(header, COL_AVG_DAYS);

        List<WorkingDaysConfig> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length <= Math.max(monthIdx, avgDaysIdx)) {
                continue; // skip malformed rows
            }

            String month = row[monthIdx].trim();
            String avgStr = row[avgDaysIdx].trim();

            if (month.isBlank() || avgStr.isBlank()) {
                continue;
            }

            // Validate month format
            if (!month.matches("\\d{4}-\\d{2}")) {
                throw new DomainException(new DomainError.Validation(
                        "Invalid month format at row " + (i + 1) + ": '" + month
                                + "'. Expected yyyy-MM"));
            }

            BigDecimal avgDays;
            try {
                avgDays = new BigDecimal(avgStr);
            } catch (NumberFormatException ex) {
                throw new DomainException(new DomainError.Validation(
                        "Invalid avgDaysWorked at row " + (i + 1) + ": '" + avgStr + "'"));
            }

            // Upsert: update if month already exists, otherwise insert
            WorkingDaysConfig config = workingDaysRepo.findByMonth(month)
                    .map(existing -> {
                        existing.setAvgDaysWorked(avgDays);
                        existing.setImportedAt(now);
                        return existing;
                    })
                    .orElse(WorkingDaysConfig.builder()
                            .id(UUID.randomUUID())
                            .month(month)
                            .avgDaysWorked(avgDays)
                            .importedAt(now)
                            .build());

            result.add(workingDaysRepo.save(config));
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkingDaysConfig> findAll() {
        return workingDaysRepo.findAll();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private List<String[]> parseCsv(MultipartFile file) {
        try (CSVReader reader = new CSVReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.readAll();
        } catch (IOException | CsvException ex) {
            throw new DomainException(new DomainError.Validation(
                    "Failed to parse CSV file: " + ex.getMessage()));
        }
    }

    private int findColumnIndex(String[] header, String columnName) {
        for (int i = 0; i < header.length; i++) {
            if (columnName.equalsIgnoreCase(header[i].trim())) {
                return i;
            }
        }
        throw new DomainException(new DomainError.Validation(
                "Required CSV column '" + columnName + "' not found in header"));
    }
}
