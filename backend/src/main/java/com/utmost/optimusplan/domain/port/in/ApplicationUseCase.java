package com.utmost.optimusplan.domain.port.in;

import com.utmost.optimusplan.domain.model.Application;

import java.util.List;
import java.util.UUID;

public interface ApplicationUseCase {

    record CreateApplicationCommand(String name, String description, UUID teamId) {}

    record UpdateApplicationCommand(UUID id, String name, String description, UUID teamId) {}

    record ImportApplicationRow(int rowNumber, String name, String description, String teamName) {}

    record ImportRowError(int row, String name, String reason) {}

    record ImportResult(int successCount, int errorCount, List<ImportRowError> errors) {}

    Application create(CreateApplicationCommand cmd);

    Application update(UpdateApplicationCommand cmd);

    void delete(UUID id);

    Application findById(UUID id);

    List<Application> findAll();

    List<Application> search(String query);

    ImportResult importBatch(List<ImportApplicationRow> rows);
}
