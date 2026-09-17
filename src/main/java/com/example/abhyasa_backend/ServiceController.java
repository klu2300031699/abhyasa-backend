package com.example.abhyasa_backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = "*")
public class ServiceController {

    @Autowired
    private ServiceRepository serviceRepository;

    // GET all active services (public)
    @GetMapping
    public List<Service> getActiveServices() {
        return serviceRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    // GET all services including inactive (admin)
    @GetMapping("/all")
    public List<Service> getAllServices() {
        return serviceRepository.findAll();
    }

    // GET service by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getServiceById(@PathVariable Long id) {
        return serviceRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST create service (admin)
    @PostMapping
    public ResponseEntity<?> createService(@RequestBody Map<String, Object> request) {
        try {
            Service service = new Service();
            service.setName((String) request.get("name"));
            service.setDescription((String) request.get("description"));
            service.setCategory((String) request.get("category"));
            service.setColor((String) request.get("color"));
            service.setIconName((String) request.get("iconName"));

            if (request.get("durationMinutes") != null) {
                service.setDurationMinutes(((Number) request.get("durationMinutes")).intValue());
            }
            if (request.get("displayOrder") != null) {
                service.setDisplayOrder(((Number) request.get("displayOrder")).intValue());
            }

            Service saved = serviceRepository.save(service);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT update service (admin)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateService(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return serviceRepository.findById(id).map(service -> {
            if (request.containsKey("name")) service.setName((String) request.get("name"));
            if (request.containsKey("description")) service.setDescription((String) request.get("description"));
            if (request.containsKey("category")) service.setCategory((String) request.get("category"));
            if (request.containsKey("color")) service.setColor((String) request.get("color"));
            if (request.containsKey("iconName")) service.setIconName((String) request.get("iconName"));
            if (request.containsKey("durationMinutes"))
                service.setDurationMinutes(((Number) request.get("durationMinutes")).intValue());
            if (request.containsKey("displayOrder"))
                service.setDisplayOrder(((Number) request.get("displayOrder")).intValue());
            if (request.containsKey("isActive"))
                service.setIsActive((Boolean) request.get("isActive"));

            return ResponseEntity.ok(serviceRepository.save(service));
        }).orElse(ResponseEntity.notFound().build());
    }

    // DELETE soft-delete service (admin)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteService(@PathVariable Long id) {
        return serviceRepository.findById(id).map(service -> {
            service.setIsActive(false);
            serviceRepository.save(service);
            return ResponseEntity.ok(Map.of("message", "Service deactivated successfully"));
        }).orElse(ResponseEntity.notFound().build());
    }
}
