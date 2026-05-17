package dev.themajorones.android_test_worker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.themajorones.models.entity.TaskLog;

@Repository
public interface TaskLogRepository extends JpaRepository<TaskLog, Integer> {
}
