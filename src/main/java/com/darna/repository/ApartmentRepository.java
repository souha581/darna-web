package com.darna.repository;

import com.darna.model.Apartment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApartmentRepository extends JpaRepository<Apartment, Long> {

    List<Apartment> findByAvailableTrue();

    List<Apartment> findByAvailableFalse();
}
