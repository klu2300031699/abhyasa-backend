package com.example.abhyasa_backend;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    // CREATE ACCOUNT
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> request) {

        try {

            String password = request.get("password");
            String confirmPassword = request.get("confirmPassword");

            // Check password
            if (password == null || confirmPassword == null) {
                return ResponseEntity
                        .badRequest()
                        .body("Password and Confirm Password are required");
            }

            // Check password match
            if (!password.equals(confirmPassword)) {
                return ResponseEntity
                        .badRequest()
                        .body("Passwords do not match");
            }

            // Create User object
            User user = new User();

            user.setFirstName(request.get("firstName"));
            user.setMiddleName(request.get("middleName"));
            user.setLastName(request.get("lastName"));
            user.setPhoneNumber(request.get("phoneNumber"));
            user.setDateOfBirth(request.get("dateOfBirth"));
            user.setEmail(request.get("email"));
            user.setPassword(password);

            // Save user
            User savedUser = userService.registerUser(user);

            // Response
            Map<String, Object> response = new HashMap<>();

            response.put("message", "Account created successfully");
            response.put("id", savedUser.getId());
            response.put("firstName", savedUser.getFirstName());
            response.put("lastName", savedUser.getLastName());
            response.put("email", savedUser.getEmail());

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // GET ALL USERS
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // GET USER BY EMAIL
    @GetMapping("/email/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {

        try {
            return ResponseEntity.ok(userService.getUserByEmail(email));

        } catch (RuntimeException e) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    // LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> request) {

        try {
            String email = request.get("email");
            String password = request.get("password");

            if (email == null || password == null) {
                return ResponseEntity
                        .badRequest()
                        .body(Map.of("error", "Email and password are required"));
            }

            User user = userService.getUserByEmail(email);

            // Check password using BCrypt
            if (!userService.checkPassword(password, user.getPassword())) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid email or password"));
            }

            // Success — return user info (no password)
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("id", user.getId());
            response.put("firstName", user.getFirstName());
            response.put("middleName", user.getMiddleName());
            response.put("lastName", user.getLastName());
            response.put("email", user.getEmail());
            response.put("phoneNumber", user.getPhoneNumber());
            response.put("dateOfBirth", user.getDateOfBirth());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        }
    }

    // GET PROFILE BY ID
    @GetMapping("/profile/{id}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long id) {

        try {
            User user = userService.getAllUsers().stream()
                    .filter(u -> u.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> profile = new HashMap<>();
            profile.put("id", user.getId());
            profile.put("firstName", user.getFirstName());
            profile.put("middleName", user.getMiddleName());
            profile.put("lastName", user.getLastName());
            profile.put("email", user.getEmail());
            profile.put("phoneNumber", user.getPhoneNumber());
            profile.put("dateOfBirth", user.getDateOfBirth());

            return ResponseEntity.ok(profile);

        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // GOOGLE LOGIN
    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> request) {

        try {
            String credential = request.get("credential");

            if (credential == null || credential.isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body(Map.of("error", "Google credential is required"));
            }

            // Verify the Google ID token
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(
                            "960010180575-mus1ie7mfq9hrqvb7cadrvvnqqsm0iso.apps.googleusercontent.com"))
                    .build();

            GoogleIdToken idToken = verifier.verify(credential);

            if (idToken == null) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid Google token"));
            }

            // Extract user info from Google token
            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String firstName = (String) payload.get("given_name");
            String lastName = (String) payload.get("family_name");

            // Find or create user
            User user = userService.findOrCreateGoogleUser(email, firstName, lastName);

            // Return user data
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Google login successful");
            response.put("id", user.getId());
            response.put("firstName", user.getFirstName());
            response.put("middleName", user.getMiddleName());
            response.put("lastName", user.getLastName());
            response.put("email", user.getEmail());
            response.put("phoneNumber", user.getPhoneNumber());
            response.put("dateOfBirth", user.getDateOfBirth());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Google authentication failed: " + e.getMessage()));
        }
    }
}
