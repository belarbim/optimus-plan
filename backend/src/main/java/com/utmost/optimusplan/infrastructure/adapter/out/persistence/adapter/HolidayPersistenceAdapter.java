package com.utmost.optimusplan.infrastructure.adapter.out.persistence.adapter;

import com.utmost.optimusplan.domain.model.PublicHoliday;
import com.utmost.optimusplan.domain.port.out.HolidayRepositoryPort;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.PublicHolidayJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.HolidayJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class HolidayPersistenceAdapter implements HolidayRepositoryPort {

    private final HolidayJpaRepository repo;

    @Override
    public PublicHoliday save(PublicHoliday holiday) {
        return toDomain(repo.save(toEntity(holiday)));
    }

    @Override
    public Optional<PublicHoliday> findById(UUID id) {
        return repo.findById(id).map(this::toDomain);
    }

    @Override
    public List<PublicHoliday> findAll() {
        return repo.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<PublicHoliday> findByMonth(String month) {
        YearMonth ym = YearMonth.parse(month);
        return repo.findByDateBetween(ym.atDay(1), ym.atEndOfMonth()).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<PublicHoliday> findByYear(int year) {
        return repo.findByDateBetween(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        repo.deleteById(id);
    }

    @Override
    public boolean existsByDate(LocalDate date) {
        return repo.existsByDate(date);
    }

    private PublicHoliday toDomain(PublicHolidayJpaEntity e) {
        return PublicHoliday.builder()
                .id(e.getId())
                .date(e.getDate())
                .name(e.getName())
                .recurring(e.isRecurring())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private PublicHolidayJpaEntity toEntity(PublicHoliday holiday) {
        PublicHolidayJpaEntity entity = holiday.getId() != null
                ? repo.findById(holiday.getId()).orElseGet(PublicHolidayJpaEntity::new)
                : new PublicHolidayJpaEntity();
        entity.setId(holiday.getId());
        entity.setDate(holiday.getDate());
        entity.setName(holiday.getName());
        entity.setRecurring(holiday.isRecurring());
        return entity;
    }
}
