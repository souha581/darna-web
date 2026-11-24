package com.darna.repository;

import com.darna.model.RepairRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepairRequestRepository extends JpaRepository<RepairRequest, Long> {

    List<RepairRequest> findByResidentUsernameOrderByCreatedAtDesc(String username);

    List<RepairRequest> findByAssignedWorkerUsernameOrderByCreatedAtDesc(String username);

    List<RepairRequest> findAllByOrderByCreatedAtDesc();
}
