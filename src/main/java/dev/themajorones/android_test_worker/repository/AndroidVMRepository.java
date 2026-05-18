package dev.themajorones.android_test_worker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.themajorones.models.entity.AndroidVMRecord;

@Repository
public interface AndroidVMRepository extends JpaRepository<AndroidVMRecord, Integer> {
}
