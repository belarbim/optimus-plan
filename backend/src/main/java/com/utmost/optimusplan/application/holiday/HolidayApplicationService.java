package com.utmost.optimusplan.application.holiday;

import com.utmost.optimusplan.domain.exception.DomainError;
import com.utmost.optimusplan.domain.exception.DomainException;
import com.utmost.optimusplan.domain.model.AuditLog;
import com.utmost.optimusplan.domain.model.PublicHoliday;
import com.utmost.optimusplan.domain.port.in.HolidayUseCase;
import com.utmost.optimusplan.domain.port.out.AuditRepositoryPort;
import com.utmost.optimusplan.domain.port.out.HolidayRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class HolidayApplicationService implements HolidayUseCase {

    private final HolidayRepositoryPort holidayRepo;
    private final AuditRepositoryPort   auditRepo;

    public HolidayApplicationService(HolidayRepositoryPort holidayRepo,
                                      AuditRepositoryPort auditRepo) {
        this.holidayRepo = holidayRepo;
        this.auditRepo   = auditRepo;
    }

    // -------------------------------------------------------------------------
    // HolidayUseCase
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<PublicHoliday> findAll(Integer year) {
        if (year == null) {
            return holidayRepo.findAll();
        }
        return holidayRepo.findByYear(year);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicHoliday findById(UUID id) {
        return holidayRepo.findById(id)
                .orElseThrow(() -> new DomainException(
                        new DomainError.NotFound("PublicHoliday", id)));
    }

    @Override
    public PublicHoliday create(CreateHolidayCommand cmd) {
        if (holidayRepo.existsByDate(cmd.date())) {
            throw new DomainException(new DomainError.Conflict(
                    "A holiday on " + cmd.date() + " already exists"));
        }

        PublicHoliday holiday = PublicHoliday.builder()
                .id(UUID.randomUUID())
                .date(cmd.date())
                .name(cmd.name())
                .recurring(cmd.recurring())
                .createdAt(LocalDateTime.now())
                .build();

        PublicHoliday saved = holidayRepo.save(holiday);
        audit("PublicHoliday", saved.getId(), "CREATE",
                Map.of("date", saved.getDate().toString(), "name", saved.getName()));
        return saved;
    }

    @Override
    public PublicHoliday update(UpdateHolidayCommand cmd) {
        PublicHoliday holiday = findById(cmd.id());

        holiday.setDate(cmd.date());
        holiday.setName(cmd.name());
        holiday.setRecurring(cmd.recurring());

        PublicHoliday saved = holidayRepo.save(holiday);
        audit("PublicHoliday", saved.getId(), "UPDATE",
                Map.of("date", saved.getDate().toString(), "name", saved.getName()));
        return saved;
    }

    @Override
    public void delete(UUID id) {
        PublicHoliday holiday = findById(id);
        holidayRepo.deleteById(id);
        audit("PublicHoliday", id, "DELETE",
                Map.of("date", holiday.getDate().toString(), "name", holiday.getName()));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void audit(String entityType, UUID id, String action, Map<String, Object> changes) {
        auditRepo.save(AuditLog.builder()
                .id(UUID.randomUUID())
                .entityType(entityType)
                .entityId(id)
                .action(action)
                .changes(changes)
                .actor("manager")
                .timestamp(LocalDateTime.now())
                .build());
    }
}
