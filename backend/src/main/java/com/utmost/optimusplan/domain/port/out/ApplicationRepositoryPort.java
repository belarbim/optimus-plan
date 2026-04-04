package com.utmost.optimusplan.domain.port.out;

import com.utmost.optimusplan.domain.model.Application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepositoryPort {

    Application save(Application application);

    Optional<Application> findById(UUID id);

    List<Application> findAll();

    List<Application> searchByName(String query);

    void deleteById(UUID id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);
}
