package com.example.abhyasa_backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private DoctorAvailabilityRepository availabilityRepository;

    @Autowired
    private SessionRepository sessionRepository;

    // POST create booking (parent)
    @PostMapping
    public ResponseEntity<?> createBooking(@RequestBody Map<String, Object> request) {
        try {
            Long parentUserId = ((Number) request.get("parentUserId")).longValue();
            Long doctorId = ((Number) request.get("doctorId")).longValue();
            LocalDate bookingDate = LocalDate.parse((String) request.get("bookingDate"));
            LocalTime slotStartTime = LocalTime.parse((String) request.get("slotStartTime"));
            LocalTime slotEndTime = LocalTime.parse((String) request.get("slotEndTime"));

            // Verify parent user exists
            User parent = userRepository.findById(parentUserId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Verify doctor exists
            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));

            // Check if slot is already booked (excluding cancelled bookings)
            boolean slotTaken = bookingRepository.existsByDoctorIdAndBookingDateAndSlotStartTimeAndStatusNot(
                    doctorId, bookingDate, slotStartTime, "CANCELLED");

            if (slotTaken) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "This time slot is already booked. Please choose another slot."));
            }

            // Check doctor availability for this day
            String dayOfWeek = bookingDate.getDayOfWeek().name();
            List<DoctorAvailability> availableSlots = availabilityRepository
                    .findByDoctorIdAndDayOfWeekAndIsActiveTrue(doctorId, dayOfWeek);

            boolean isAvailable = availableSlots.stream()
                    .anyMatch(s -> s.getSlotStartTime().equals(slotStartTime));

            if (!isAvailable) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Doctor is not available at this time slot."));
            }

            Booking booking = new Booking();
            booking.setParentUser(parent);
            booking.setDoctor(doctor);
            booking.setBookingDate(bookingDate);
            booking.setSlotStartTime(slotStartTime);
            booking.setSlotEndTime(slotEndTime);
            booking.setChildName((String) request.get("childName"));
            booking.setParentNotes((String) request.get("parentNotes"));
            booking.setBookingType(request.get("bookingType") != null
                    ? (String) request.get("bookingType") : "DIRECT_SERVICE");

            if (request.get("childAge") != null) {
                booking.setChildAge(((Number) request.get("childAge")).intValue());
            }

            if (request.get("serviceId") != null) {
                Long serviceId = ((Number) request.get("serviceId")).longValue();
                Service service = serviceRepository.findById(serviceId).orElse(null);
                booking.setService(service);
            }

            Booking saved = bookingRepository.save(booking);
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET my bookings (parent)
    @GetMapping("/my/{userId}")
    public ResponseEntity<?> getMyBookings(@PathVariable Long userId) {
        List<Booking> bookings = bookingRepository.findByParentUserIdOrderByBookingDateDesc(userId);
        return ResponseEntity.ok(bookings.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    // GET doctor's assigned bookings
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<?> getDoctorBookings(@PathVariable Long doctorId) {
        List<Booking> bookings = bookingRepository.findByDoctorIdOrderByBookingDateDesc(doctorId);
        return ResponseEntity.ok(bookings.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    // GET doctor's bookings for a specific date
    @GetMapping("/doctor/{doctorId}/date/{date}")
    public ResponseEntity<?> getDoctorBookingsByDate(
            @PathVariable Long doctorId, @PathVariable String date) {
        LocalDate bookingDate = LocalDate.parse(date);
        List<Booking> bookings = bookingRepository.findByDoctorIdAndBookingDate(doctorId, bookingDate);
        return ResponseEntity.ok(bookings.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    // GET all bookings (admin)
    @GetMapping
    public ResponseEntity<?> getAllBookings() {
        List<Booking> bookings = bookingRepository.findAll();
        // Sort by date descending
        bookings.sort((a, b) -> b.getBookingDate().compareTo(a.getBookingDate()));
        return ResponseEntity.ok(bookings.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    // PUT update booking status
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateBookingStatus(
            @PathVariable Long id, @RequestBody Map<String, String> request) {
        return bookingRepository.findById(id).map(booking -> {
            String newStatus = request.get("status");
            booking.setStatus(newStatus);
            bookingRepository.save(booking);
            return ResponseEntity.ok(toResponse(booking));
        }).orElse(ResponseEntity.notFound().build());
    }

    // DELETE cancel booking
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelBooking(@PathVariable Long id) {
        return bookingRepository.findById(id).map(booking -> {
            booking.setStatus("CANCELLED");
            bookingRepository.save(booking);
            return ResponseEntity.ok(Map.of("message", "Booking cancelled successfully"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // GET available slots for a doctor on a specific date
    @GetMapping("/available-slots")
    public ResponseEntity<?> getAvailableSlots(
            @RequestParam Long doctorId, @RequestParam String date) {
        LocalDate bookingDate = LocalDate.parse(date);
        String dayOfWeek = bookingDate.getDayOfWeek().name();

        // Get all available slots for this doctor on this day
        List<DoctorAvailability> allSlots = availabilityRepository
                .findByDoctorIdAndDayOfWeekAndIsActiveTrue(doctorId, dayOfWeek);

        // Get booked slots (non-cancelled) for this doctor on this date
        List<Booking> booked = bookingRepository.findByDoctorIdAndBookingDate(doctorId, bookingDate);
        Set<LocalTime> bookedTimes = booked.stream()
                .filter(b -> !"CANCELLED".equals(b.getStatus()))
                .map(Booking::getSlotStartTime)
                .collect(Collectors.toSet());

        // Also check SCHEDULED follow-up sessions (they block slots too)
        List<Session> scheduledSessions = sessionRepository
                .findByDoctorIdAndSessionDateAndStatus(doctorId, bookingDate, "SCHEDULED");
        scheduledSessions.forEach(s -> bookedTimes.add(s.getSessionStartTime()));

        List<Map<String, Object>> availableSlots = allSlots.stream()
                .map(slot -> {
                    Map<String, Object> s = new LinkedHashMap<>();
                    s.put("startTime", slot.getSlotStartTime().toString());
                    s.put("endTime", slot.getSlotEndTime().toString());
                    s.put("isBooked", bookedTimes.contains(slot.getSlotStartTime()));
                    return s;
                })
                .sorted(Comparator.comparing(s -> (String) s.get("startTime")))
                .collect(Collectors.toList());

        return ResponseEntity.ok(availableSlots);
    }

    // GET booking stats (admin)
    @GetMapping("/stats")
    public ResponseEntity<?> getBookingStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalBookings", bookingRepository.count());
        stats.put("todayBookings", bookingRepository.countByBookingDate(LocalDate.now()));
        stats.put("pendingBookings", bookingRepository.countByStatus("PENDING"));
        stats.put("confirmedBookings", bookingRepository.countByStatus("CONFIRMED"));
        stats.put("completedBookings", bookingRepository.countByStatus("COMPLETED"));
        stats.put("cancelledBookings", bookingRepository.countByStatus("CANCELLED"));
        return ResponseEntity.ok(stats);
    }

    // Helper: convert Booking to response map
    private Map<String, Object> toResponse(Booking b) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", b.getId());
        map.put("parentUserId", b.getParentUser().getId());
        map.put("parentName", b.getParentUser().getFirstName() + " " +
                (b.getParentUser().getLastName() != null ? b.getParentUser().getLastName() : ""));
        map.put("parentEmail", b.getParentUser().getEmail());
        map.put("parentPhone", b.getParentUser().getPhoneNumber());
        map.put("doctorId", b.getDoctor().getId());
        map.put("doctorName", b.getDoctor().getFullName());
        map.put("doctorDepartment", b.getDoctor().getDepartment());
        map.put("serviceId", b.getService() != null ? b.getService().getId() : null);
        map.put("serviceName", b.getService() != null ? b.getService().getName() : "General Screening");
        map.put("childName", b.getChildName());
        map.put("childAge", b.getChildAge());
        map.put("bookingDate", b.getBookingDate().toString());
        map.put("slotStartTime", b.getSlotStartTime().toString());
        map.put("slotEndTime", b.getSlotEndTime().toString());
        map.put("status", b.getStatus());
        map.put("bookingType", b.getBookingType());
        map.put("parentNotes", b.getParentNotes());
        map.put("createdAt", b.getCreatedAt());
        return map;
    }
}
