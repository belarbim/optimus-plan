package com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.GradeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface GradeJpaRepository extends JpaRepository<GradeJpaEntity, UUID> {
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, UUID id);
}
