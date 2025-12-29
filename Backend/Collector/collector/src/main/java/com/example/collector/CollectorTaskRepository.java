package com.example.collector;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollectorTaskRepository extends JpaRepository<CollectorTask, Long> {
    List<CollectorTask> findByCollectorId(String collectorId);
    List<CollectorTask> findByCollectorIdAndStatus(String collectorId, String status);
    List<CollectorTask> findByStatus(String status);
    Optional<CollectorTask> findByTaskId(String taskId);
    List<CollectorTask> findByProblemTypeIsNotNull();
}