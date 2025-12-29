package com.example.collector;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollectorRepository extends JpaRepository<Collector, Long> {
    Optional<Collector> findByCollectorId(String collectorId);
    List<Collector> findByStatus(String status);
    boolean existsByCollectorId(String collectorId);
}