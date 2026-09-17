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
@RequestMapping("/api/sessions")
@CrossOrigin(origins = "*")
public class SessionController {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    // POST create session (doctor — after appointment)
    @PostMapping
    public ResponseEntity<?> createSession(@RequestBody Map<String, Object> request) {
        try {
            Long bookingId = ((Number) request.get("bookingId")).longValue();
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            Session session = new Session();
            session.setBooking(booking);
            session.setDoctor(booking.getDoctor());
            session.setParentUser(booking.getParentUser());
            session.setService(booking.getService());
            session.setSessionDate(booking.getBookingDate());
            session.setSessionStartTime(booking.getSlotStartTime());
            session.setSessionEndTime(booking.getSlotEndTime());

            // Determine session number
            List<Session> existingSessions = sessionRepository
                    .findByBookingIdOrderBySessionNumberAsc(bookingId);
            session.setSessionNumber(existingSessions.size() + 1);

            if (request.get("sessionNotes") != null) {
                session.setSessionNotes((String) request.get("sessionNotes"));
            }
            if (request.get("medication") != null) {
                session.setMedication((String) request.get("medication"));
            }
            if (request.get("homework") != null) {
                session.setHomework((String) request.get("homework"));
            }
            if (request.get("outcome") != null) {
                session.setOutcome((String) request.get("outcome"));
            }
            if (request.get("status") != null) {
                session.setStatus((String) request.get("status"));
            }

            Session saved = sessionRepository.save(session);

            // Handle follow-up: create a SCHEDULED session under the SAME booking
            String outcome = (String) request.get("outcome");
            if ("NEEDS_FOLLOWUP".equals(outcome) &&
                    request.get("followupDate") != null && !((String) request.get("followupDate")).isEmpty() &&
                    request.get("followupSlot") != null && !((String) request.get("followupSlot")).isEmpty()) {

                String followupDateStr = (String) request.get("followupDate");
                String followupSlotStr = (String) request.get("followupSlot");
                String[] slotParts = followupSlotStr.split("-");

                // Save followup info on the completed session for display
                saved.setFollowupDate(LocalDate.parse(followupDateStr));
                saved.setFollowupTime(LocalTime.parse(slotParts[0]));
                saved = sessionRepository.save(saved);

                // Create the follow-up session under the SAME booking
                Session followUp = new Session();
                followUp.setBooking(booking);
                followUp.setDoctor(booking.getDoctor());
                followUp.setParentUser(booking.getParentUser());
                followUp.setService(booking.getService());
                followUp.setSessionDate(LocalDate.parse(followupDateStr));
                followUp.setSessionStartTime(LocalTime.parse(slotParts[0]));
                followUp.setSessionEndTime(LocalTime.parse(slotParts[1]));

                // Next session number
                List<Session> allSessions = sessionRepository.findByBookingIdOrderBySessionNumberAsc(booking.getId());
                followUp.setSessionNumber(allSessions.size() + 1);
                followUp.setStatus("SCHEDULED");
                Session savedFollowUp = sessionRepository.save(followUp);

                // Link completed session → follow-up session
                saved.setNextSession(savedFollowUp);
                sessionRepository.save(saved);

                // Keep booking active (CONFIRMED) since there's a scheduled follow-up
                booking.setStatus("CONFIRMED");
                bookingRepository.save(booking);

            } else if ("COMPLETED".equals(request.get("status"))) {
                // Only mark booking as COMPLETED if no follow-up needed
                if (!"NEEDS_FOLLOWUP".equals(outcome)) {
                    booking.setStatus("COMPLETED");
                    bookingRepository.save(booking);
                }
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET sessions for a booking (session history)
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<?> getSessionsByBooking(@PathVariable Long bookingId) {
        List<Session> sessions = sessionRepository.findByBookingIdOrderBySessionNumberAsc(bookingId);
        return ResponseEntity.ok(sessions.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    // GET doctor's sessions for a specific date
    @GetMapping("/doctor/{doctorId}/date/{date}")
    public ResponseEntity<?> getDoctorSessionsByDate(
            @PathVariable Long doctorId, @PathVariable String date) {
        LocalDate sessionDate = LocalDate.parse(date);
        List<Session> sessions = sessionRepository
                .findByDoctorIdAndSessionDateOrderBySessionStartTimeAsc(doctorId, sessionDate);
        return ResponseEntity.ok(sessions.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    // GET all sessions for a doctor
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<?> getDoctorSessions(@PathVariable Long doctorId) {
        List<Session> sessions = sessionRepository.findByDoctorIdOrderBySessionDateDescSessionStartTimeDesc(doctorId);
        return ResponseEntity.ok(sessions.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    // PUT update session (doctor updates notes, outcome, medication)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSession(
            @PathVariable Long id, @RequestBody Map<String, Object> request) {
        return sessionRepository.findById(id).map(session -> {
            if (request.containsKey("sessionNotes"))
                session.setSessionNotes((String) request.get("sessionNotes"));
            if (request.containsKey("medication"))
                session.setMedication((String) request.get("medication"));
            if (request.containsKey("homework"))
                session.setHomework((String) request.get("homework"));
            if (request.containsKey("outcome"))
                session.setOutcome((String) request.get("outcome"));
            if (request.containsKey("status"))
                session.setStatus((String) request.get("status"));

            // Handle referral to another service
            if (request.containsKey("referredToServiceId") && request.get("referredToServiceId") != null) {
                Long serviceId = ((Number) request.get("referredToServiceId")).longValue();
                serviceRepository.findById(serviceId).ifPresent(session::setReferredToService);
            }

            Session updated = sessionRepository.save(session);

            // If session completed, update booking
            if ("COMPLETED".equals(session.getStatus())) {
                Booking booking = session.getBooking();
                if ("RECOVERED".equals(session.getOutcome())) {
                    booking.setStatus("COMPLETED");
                }
                bookingRepository.save(booking);
            }

            return ResponseEntity.ok(toResponse(updated));
        }).orElse(ResponseEntity.notFound().build());
    }

    // POST create follow-up session from existing session
    @PostMapping("/{id}/follow-up")
    public ResponseEntity<?> createFollowUp(
            @PathVariable Long id, @RequestBody Map<String, Object> request) {
        return sessionRepository.findById(id).map(prevSession -> {
            try {
                Session followUp = new Session();
                followUp.setBooking(prevSession.getBooking());
                followUp.setDoctor(prevSession.getDoctor());
                followUp.setParentUser(prevSession.getParentUser());
                followUp.setService(prevSession.getService());

                // Parse follow-up date and time
                followUp.setSessionDate(LocalDate.parse((String) request.get("sessionDate")));
                followUp.setSessionStartTime(LocalTime.parse((String) request.get("sessionStartTime")));
                followUp.setSessionEndTime(LocalTime.parse((String) request.get("sessionEndTime")));

                // Increment session number
                List<Session> all = sessionRepository
                        .findByBookingIdOrderBySessionNumberAsc(prevSession.getBooking().getId());
                followUp.setSessionNumber(all.size() + 1);

                followUp.setStatus("SCHEDULED");

                if (request.get("sessionNotes") != null) {
                    followUp.setSessionNotes((String) request.get("sessionNotes"));
                }
                if (request.get("medication") != null) {
                    followUp.setMedication((String) request.get("medication"));
                }

                Session saved = sessionRepository.save(followUp);

                // Link previous session to this follow-up
                prevSession.setNextSession(saved);
                sessionRepository.save(prevSession);

                // Also create a booking entry for the follow-up slot
                Booking followUpBooking = new Booking();
                followUpBooking.setParentUser(prevSession.getParentUser());
                followUpBooking.setDoctor(prevSession.getDoctor());
                followUpBooking.setService(prevSession.getService());
                followUpBooking.setChildName(prevSession.getBooking().getChildName());
                followUpBooking.setChildAge(prevSession.getBooking().getChildAge());
                followUpBooking.setBookingDate(followUp.getSessionDate());
                followUpBooking.setSlotStartTime(followUp.getSessionStartTime());
                followUpBooking.setSlotEndTime(followUp.getSessionEndTime());
                followUpBooking.setBookingType("DIRECT_SERVICE");
                followUpBooking.setStatus("CONFIRMED");
                followUpBooking.setParentNotes("Follow-up session #" + followUp.getSessionNumber());
                bookingRepository.save(followUpBooking);

                return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    // GET doctor stats
    @GetMapping("/doctor/{doctorId}/stats")
    public ResponseEntity<?> getDoctorStats(@PathVariable Long doctorId) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("scheduledSessions", sessionRepository.countByDoctorIdAndStatus(doctorId, "SCHEDULED"));
        stats.put("completedSessions", sessionRepository.countByDoctorIdAndStatus(doctorId, "COMPLETED"));
        stats.put("inProgressSessions", sessionRepository.countByDoctorIdAndStatus(doctorId, "IN_PROGRESS"));
        return ResponseEntity.ok(stats);
    }

    // Helper: convert Session to response map
    private Map<String, Object> toResponse(Session s) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", s.getId());
        map.put("bookingId", s.getBooking().getId());
        map.put("doctorId", s.getDoctor().getId());
        map.put("doctorName", s.getDoctor().getFullName());
        map.put("parentUserId", s.getParentUser().getId());
        map.put("parentName", s.getParentUser().getFirstName() + " " +
                (s.getParentUser().getLastName() != null ? s.getParentUser().getLastName() : ""));
        map.put("serviceId", s.getService() != null ? s.getService().getId() : null);
        map.put("serviceName", s.getService() != null ? s.getService().getName() : "General Screening");
        map.put("sessionNumber", s.getSessionNumber());
        map.put("sessionDate", s.getSessionDate().toString());
        map.put("sessionStartTime", s.getSessionStartTime().toString());
        map.put("sessionEndTime", s.getSessionEndTime().toString());
        map.put("status", s.getStatus());
        map.put("sessionNotes", s.getSessionNotes());
        map.put("medication", s.getMedication());
        map.put("homework", s.getHomework());
        map.put("outcome", s.getOutcome());
        map.put("referredToServiceId", s.getReferredToService() != null ? s.getReferredToService().getId() : null);
        map.put("referredToServiceName", s.getReferredToService() != null ? s.getReferredToService().getName() : null);
        map.put("nextSessionId", s.getNextSession() != null ? s.getNextSession().getId() : null);
        map.put("childName", s.getBooking().getChildName());
        map.put("childAge", s.getBooking().getChildAge());
        map.put("followupDate", s.getFollowupDate() != null ? s.getFollowupDate().toString() : null);
        map.put("followupTime", s.getFollowupTime() != null ? s.getFollowupTime().toString() : null);
        map.put("createdAt", s.getCreatedAt());
        return map;
    }
}
