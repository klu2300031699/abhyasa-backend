package com.example.abhyasa_backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Register user (hashes password with BCrypt)
    public User registerUser(User user) {

        // Check whether email already exists
        if (user.getEmail() != null && userRepository.existsByEmail(user.getEmail().trim().toLowerCase())) {
            throw new RuntimeException("Email is already registered");
        }

        // Check whether phone number already exists
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().trim().isEmpty()) {
            String phone = user.getPhoneNumber().trim();
            if (userRepository.existsByPhoneNumber(phone)) {
                throw new RuntimeException("Phone number is already registered");
            }
            // Also check with/without +91
            if (!phone.startsWith("+91") && userRepository.existsByPhoneNumber("+91" + phone)) {
                throw new RuntimeException("Phone number is already registered");
            }
        }

        // Hash the password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    // Verify raw password against hashed password
    public boolean checkPassword(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }

    // Get all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Get user by email
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() ->
                        new RuntimeException("User not found with email: " + email));
    }

    // Get user by email OR phone number
    public User getUserByIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new RuntimeException("Email or phone number is required");
        }

        String cleaned = identifier.trim();

        // 1. Try exact email match
        Optional<User> user = userRepository.findByEmail(cleaned.toLowerCase());
        if (user.isPresent()) {
            return user.get();
        }

        // 2. Try phone number exact match
        user = userRepository.findByPhoneNumber(cleaned);
        if (user.isPresent()) {
            return user.get();
        }

        // 3. If phone entered without +91, try with +91
        if (!cleaned.startsWith("+91")) {
            user = userRepository.findByPhoneNumber("+91" + cleaned);
            if (user.isPresent()) {
                return user.get();
            }
        }

        // 4. If phone entered with +91, try without +91
        if (cleaned.startsWith("+91")) {
            user = userRepository.findByPhoneNumber(cleaned.substring(3));
            if (user.isPresent()) {
                return user.get();
            }
        }

        throw new RuntimeException("User not found with details: " + identifier);
    }

    // Find or create Google user
    public User findOrCreateGoogleUser(String email, String firstName, String lastName) {

        Optional<User> existing = userRepository.findByEmail(email);

        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new user from Google data
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setFirstName(firstName != null ? firstName : "");
        newUser.setLastName(lastName != null ? lastName : "");
        // Google users don't have a password — store a hashed marker
        newUser.setPassword(passwordEncoder.encode("GOOGLE_OAUTH_" + System.currentTimeMillis()));

        return userRepository.save(newUser);
    }
}