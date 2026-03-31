package com.utmost.optimusplan.infrastructure.adapter.in.web;

import com.utmost.optimusplan.domain.model.PublicHoliday;
import com.utmost.optimusplan.domain.port.in.HolidayUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayUseCase holidayUseCase;

    record HolidayRequest(
            @NotNull LocalDate date,
            @NotBlank String name,
            boolean recurring) {}

    record HolidayResponse(
            UUID id,
            String date,
            String name,
            boolean recurring,
            String createdAt) {

        static HolidayResponse from(PublicHoliday h) {
            return new HolidayResponse(
                    h.getId(),
                    h.getDate() != null ? h.getDate().toString() : null,
                    h.getName(),
                    h.isRecurring(),
                    h.getCreatedAt() != null ? h.getCreatedAt().toString() : null);
        }
    }

    @GetMapping
    public List<HolidayResponse> getAll(@RequestParam(required = false) Integer year) {
        return holidayUseCase.findAll(year).stream()
                .map(HolidayResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public HolidayResponse getById(@PathVariable UUID id) {
        return HolidayResponse.from(holidayUseCase.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HolidayResponse create(@Valid @RequestBody HolidayRequest req) {
        return HolidayResponse.from(
                holidayUseCase.create(new HolidayUseCase.CreateHolidayCommand(
                        req.date(), req.name(), req.recurring())));
    }

    @PutMapping("/{id}")
    public HolidayResponse update(@PathVariable UUID id, @Valid @RequestBody HolidayRequest req) {
        return HolidayResponse.from(
                holidayUseCase.update(new HolidayUseCase.UpdateHolidayCommand(
                        id, req.date(), req.name(), req.recurring())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        holidayUseCase.delete(id);
    }
}
