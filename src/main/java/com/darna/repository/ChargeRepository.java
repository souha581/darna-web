package com.darna.repository;

import com.darna.model.Charge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChargeRepository extends JpaRepository<Charge, Long> {

    List<Charge> findByApartmentIdOrderByDueDateDesc(Long apartmentId);

    List<Charge> findAllByOrderByDueDateDesc();
}
