package com.utmost.optimusplan.domain.port.in;
import com.utmost.optimusplan.domain.model.Grade;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
public interface GradeUseCase {
    record CreateGradeCommand(String name, BigDecimal dailyCost) {}
    record UpdateGradeCommand(UUID id, String name) {}
    Grade create(CreateGradeCommand cmd);
    Grade update(UpdateGradeCommand cmd);
    void delete(UUID id);
    Grade findById(UUID id);
    List<Grade> findAll();
}
