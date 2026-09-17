package com.example.abhyasa_backend;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, Long> {

    List<Service> findByIsActiveTrueOrderByDisplayOrderAsc();

    List<Service> findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(String category);

    boolean existsByName(String name);
}
