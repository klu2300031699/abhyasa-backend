package com.example.abhyasa_backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/doctors")
@CrossOrigin(origins = "*")
public class DoctorController {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private DoctorAvailabilityRepository availabilityRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // GET all active doctors (public)
    @GetMapping
    public List<Map<String, Object>> getActiveDoctors() {
        return doctorRepository.findByIsActiveTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // GET all doctors including inactive (admin)
    @GetMapping("/all")
    public List<Map<String, Object>> getAllDoctors() {
        return doctorRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // GET doctor by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getDoctorById(@PathVariable Long id) {
        return doctorRepository.findById(id)
                .map(d -> ResponseEntity.ok(toResponse(d)))
                .orElse(ResponseEntity.notFound().build());
    }

    // POST create doctor (admin) — auto-creates user account
    @PostMapping
    public ResponseEntity<?> createDoctor(@RequestBody Map<String, Object> request) {
        try {
            String email = ((String) request.get("email")).trim().toLowerCase();
            String password = request.get("password") != null
                    ? (String) request.get("password") : "Doctor@123";

            // Create user account with DOCTOR role
            User user;
            Optional<User> existingUser = userRepository.findByEmail(email);
            if (existingUser.isPresent()) {
                user = existingUser.get();
                user.setRole("DOCTOR");
                if (request.get("password") != null) {
                    user.setPassword(passwordEncoder.encode(password));
                }
                userRepository.save(user);
            } else {
                user = new User();
                user.setEmail(email);
                String fullName = (String) request.get("fullName");
                String[] parts = fullName != null ? fullName.trim().split(" ", 2) : new String[]{"Doctor", ""};
                user.setFirstName(parts[0]);
                user.setLastName(parts.length > 1 ? parts[1] : "");
                user.setPhoneNumber((String) request.get("phoneNumber"));
                user.setPassword(passwordEncoder.encode(password));
                user.setRole("DOCTOR");
                user = userRepository.save(user);
            }

            // Create doctor profile
            Doctor doctor = new Doctor();
            doctor.setUser(user);
            doctor.setFullName((String) request.get("fullName"));
            doctor.setTitle((String) request.get("title"));
            doctor.setBadge((String) request.get("badge"));
            doctor.setDepartment((String) request.get("department"));
            doctor.setExperience((String) request.get("experience"));
            doctor.setBio((String) request.get("bio"));
            doctor.setApproach((String) request.get("approach"));
            doctor.setImageUrl((String) request.get("imageUrl"));
            doctor.setPhoneNumber((String) request.get("phoneNumber"));
            doctor.setEmail(email);
            doctor.setEducation(request.get("education") != null ? request.get("education").toString() : null);
            doctor.setSpecialties(request.get("specialties") != null ? request.get("specialties").toString() : null);
            doctor.setLanguages(request.get("languages") != null ? request.get("languages").toString() : null);

            // Assign services
            if (request.get("serviceIds") != null) {
                @SuppressWarnings("unchecked")
                List<Number> serviceIds = (List<Number>) request.get("serviceIds");
                List<Service> services = serviceIds.stream()
                        .map(id -> serviceRepository.findById(id.longValue()).orElse(null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                doctor.setServices(services);
            }

            Doctor saved = doctorRepository.save(doctor);

            Map<String, Object> response = toResponse(saved);
            response.put("generatedPassword", password);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT update doctor (admin)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDoctor(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return doctorRepository.findById(id).map(doctor -> {
            if (request.containsKey("fullName")) doctor.setFullName((String) request.get("fullName"));
            if (request.containsKey("title")) doctor.setTitle((String) request.get("title"));
            if (request.containsKey("badge")) doctor.setBadge((String) request.get("badge"));
            if (request.containsKey("department")) doctor.setDepartment((String) request.get("department"));
            if (request.containsKey("experience")) doctor.setExperience((String) request.get("experience"));
            if (request.containsKey("bio")) doctor.setBio((String) request.get("bio"));
            if (request.containsKey("approach")) doctor.setApproach((String) request.get("approach"));
            if (request.containsKey("imageUrl")) doctor.setImageUrl((String) request.get("imageUrl"));
            if (request.containsKey("phoneNumber")) doctor.setPhoneNumber((String) request.get("phoneNumber"));
            if (request.containsKey("education")) doctor.setEducation(request.get("education").toString());
            if (request.containsKey("specialties")) doctor.setSpecialties(request.get("specialties").toString());
            if (request.containsKey("languages")) doctor.setLanguages(request.get("languages").toString());
            if (request.containsKey("isActive")) doctor.setIsActive((Boolean) request.get("isActive"));

            if (request.containsKey("serviceIds")) {
                @SuppressWarnings("unchecked")
                List<Number> serviceIds = (List<Number>) request.get("serviceIds");
                List<Service> services = serviceIds.stream()
                        .map(sid -> serviceRepository.findById(sid.longValue()).orElse(null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                doctor.setServices(services);
            }

            return ResponseEntity.ok(toResponse(doctorRepository.save(doctor)));
        }).orElse(ResponseEntity.notFound().build());
    }

    // DELETE soft-delete doctor (admin)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDoctor(@PathVariable Long id) {
        return doctorRepository.findById(id).map(doctor -> {
            doctor.setIsActive(false);
            doctorRepository.save(doctor);
            return ResponseEntity.ok(Map.of("message", "Doctor deactivated successfully"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ===== AVAILABILITY ENDPOINTS =====

    // GET doctor availability
    @GetMapping("/{id}/availability")
    public ResponseEntity<?> getDoctorAvailability(@PathVariable Long id) {
        if (!doctorRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        List<DoctorAvailability> slots = availabilityRepository.findByDoctorIdAndIsActiveTrue(id);
        return ResponseEntity.ok(slots.stream().map(s -> Map.of(
                "id", s.getId(),
                "dayOfWeek", s.getDayOfWeek(),
                "slotStartTime", s.getSlotStartTime().toString(),
                "slotEndTime", s.getSlotEndTime().toString(),
                "isActive", s.getIsActive()
        )).collect(Collectors.toList()));
    }

    // POST set doctor availability (admin) — replaces all slots for a day
    @PostMapping("/{id}/availability")
    public ResponseEntity<?> setDoctorAvailability(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        return doctorRepository.findById(id).map(doctor -> {
            String dayOfWeek = (String) request.get("dayOfWeek");

            @SuppressWarnings("unchecked")
            List<Map<String, String>> slots = (List<Map<String, String>>) request.get("slots");

            // Remove existing slots for this day
            List<DoctorAvailability> existing = availabilityRepository
                    .findByDoctorIdAndDayOfWeekAndIsActiveTrue(id, dayOfWeek);
            existing.forEach(s -> {
                s.setIsActive(false);
                availabilityRepository.save(s);
            });

            // Create new slots
            List<Map<String, Object>> created = new ArrayList<>();
            for (Map<String, String> slot : slots) {
                DoctorAvailability da = new DoctorAvailability();
                da.setDoctor(doctor);
                da.setDayOfWeek(dayOfWeek);
                da.setSlotStartTime(LocalTime.parse(slot.get("startTime")));
                da.setSlotEndTime(LocalTime.parse(slot.get("endTime")));
                da.setIsActive(true);
                DoctorAvailability saved = availabilityRepository.save(da);
                created.add(Map.of(
                        "id", saved.getId(),
                        "dayOfWeek", saved.getDayOfWeek(),
                        "slotStartTime", saved.getSlotStartTime().toString(),
                        "slotEndTime", saved.getSlotEndTime().toString()
                ));
            }

            return ResponseEntity.ok(Map.of("message", "Availability set successfully", "slots", created));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Helper: convert Doctor entity to response map (avoids circular refs)
    private Map<String, Object> toResponse(Doctor d) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", d.getId());
        map.put("userId", d.getUser() != null ? d.getUser().getId() : null);
        map.put("fullName", d.getFullName());
        map.put("title", d.getTitle());
        map.put("badge", d.getBadge());
        map.put("department", d.getDepartment());
        map.put("experience", d.getExperience());
        map.put("bio", d.getBio());
        map.put("approach", d.getApproach());
        map.put("imageUrl", d.getImageUrl());
        map.put("phoneNumber", d.getPhoneNumber());
        map.put("email", d.getEmail());
        map.put("isActive", d.getIsActive());
        map.put("education", d.getEducation());
        map.put("specialties", d.getSpecialties());
        map.put("languages", d.getLanguages());
        map.put("services", d.getServices().stream().map(s -> Map.of(
                "id", s.getId(),
                "name", s.getName(),
                "category", s.getCategory()
        )).collect(Collectors.toList()));
        map.put("createdAt", d.getCreatedAt());
        return map;
    }
}
