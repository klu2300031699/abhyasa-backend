package com.example.abhyasa_backend;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByParentUserIdOrderByBookingDateDesc(Long parentUserId);

    List<Booking> findByDoctorIdOrderByBookingDateDesc(Long doctorId);

    List<Booking> findByDoctorIdAndBookingDate(Long doctorId, LocalDate bookingDate);

    List<Booking> findByBookingDateOrderBySlotStartTimeAsc(LocalDate bookingDate);

    // Check if a slot is already booked for a specific doctor on a specific date
    boolean existsByDoctorIdAndBookingDateAndSlotStartTimeAndStatusNot(
            Long doctorId, LocalDate bookingDate, LocalTime slotStartTime, String excludeStatus);

    List<Booking> findByStatusOrderByBookingDateDesc(String status);

    long countByBookingDate(LocalDate date);

    long countByStatus(String status);
}
