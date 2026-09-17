package com.example.abhyasa_backend;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    List<Doctor> findByIsActiveTrue();

    Optional<Doctor> findByEmail(String email);

    Optional<Doctor> findByUserId(Long userId);

    boolean existsByEmail(String email);
}
