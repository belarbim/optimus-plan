package com.utmost.optimusplan.domain.port.out;
import com.utmost.optimusplan.domain.model.Grade;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface GradeRepositoryPort {
    Grade save(Grade grade);
    Optional<Grade> findById(UUID id);
    List<Grade> findAll();
    void deleteById(UUID id);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, UUID id);
}
