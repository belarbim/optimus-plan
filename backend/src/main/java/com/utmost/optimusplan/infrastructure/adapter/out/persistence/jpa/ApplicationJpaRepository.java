package com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa;

import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.ApplicationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ApplicationJpaRepository extends JpaRepository<ApplicationJpaEntity, UUID> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    @Query("SELECT a FROM ApplicationJpaEntity a LEFT JOIN FETCH a.team WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<ApplicationJpaEntity> searchByNameContaining(@Param("query") String query);

    @Query("SELECT a FROM ApplicationJpaEntity a LEFT JOIN FETCH a.team")
    List<ApplicationJpaEntity> findAllWithTeam();
}
