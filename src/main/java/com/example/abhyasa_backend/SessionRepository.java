package com.example.abhyasa_backend;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {

    List<Session> findByBookingIdOrderBySessionNumberAsc(Long bookingId);

    List<Session> findByDoctorIdAndSessionDateOrderBySessionStartTimeAsc(Long doctorId, LocalDate sessionDate);

    List<Session> findByDoctorIdOrderBySessionDateDescSessionStartTimeDesc(Long doctorId);

    List<Session> findByParentUserIdOrderBySessionDateDesc(Long parentUserId);

    long countByDoctorIdAndStatus(Long doctorId, String status);

    // Find SCHEDULED sessions for a doctor on a specific date (for slot blocking)
    List<Session> findByDoctorIdAndSessionDateAndStatus(Long doctorId, LocalDate sessionDate, String status);
}
