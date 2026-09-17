package com.example.abhyasa_backend;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, Long> {

    List<DoctorAvailability> findByDoctorIdAndDayOfWeekAndIsActiveTrue(Long doctorId, String dayOfWeek);

    List<DoctorAvailability> findByDoctorIdAndIsActiveTrue(Long doctorId);

    void deleteByDoctorId(Long doctorId);
}
