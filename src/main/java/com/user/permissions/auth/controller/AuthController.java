package com.user.permissions.auth.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.user.permissions.appuser.AppUser;
import com.user.permissions.appuser.repository.AppUserRepository;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AuthController {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials, HttpSession session) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPassword())) {
        	AppUser user = userOpt.get();
            
            // Store user in session
            session.setAttribute("user", user);
            session.setAttribute("authenticated", true);
            
            // Return user data (without password)
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "name", user.getName(),
                "role", user.getRole()
            ));
            
            return ResponseEntity.ok(response);
        }
        
        return ResponseEntity.badRequest().body(Map.of(
            "success", false,
            "error", "Invalid email or password"
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
    	AppUser user = (AppUser) session.getAttribute("user");
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        
        if (user != null && Boolean.TRUE.equals(authenticated)) {
            return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "user", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "name", user.getName(),
                    "role", user.getRole()
                )
            ));
        }
        
        return ResponseEntity.ok(Map.of("authenticated", false));
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateUser(@RequestBody Map<String, String> request, HttpSession session) {
        AppUser sessionUser = (AppUser) session.getAttribute("user");
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        
        // Check authentication
        if (sessionUser == null || !Boolean.TRUE.equals(authenticated)) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        String newName = request.get("name");
        String newEmail = request.get("email");
        String newPassword = request.get("password");
        
        // Validation
        Map<String, String> errors = new HashMap<>();
        
        // Validate name
        if (newName == null || newName.trim().isEmpty()) {
            errors.put("name", "Name is required and cannot be empty");
        }
        
        // Validate email
        if (newEmail == null || newEmail.trim().isEmpty()) {
            errors.put("email", "Email is required and cannot be empty");
        } else if (!isValidEmail(newEmail.trim())) {
            errors.put("email", "Please provide a valid email address");
        }
        
        // Validate password
        if (newPassword == null || newPassword.trim().isEmpty()) {
            errors.put("password", "Password is required and cannot be empty");
        } else if (newPassword.trim().length() < 6) {
            errors.put("password", "Password must be at least 6 characters long");
        }
        
        // Check if email already exists (exclude current user)
        if (newEmail != null && !newEmail.trim().isEmpty() && isValidEmail(newEmail.trim())) {
            Optional<AppUser> existingUser = userRepository.findByEmail(newEmail.trim().toLowerCase());
            if (existingUser.isPresent() && !existingUser.get().getId().equals(sessionUser.getId())) {
                errors.put("email", "Email already exists. Please choose a different email.");
            }
        }
        
        // Return validation errors if any
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "errors", errors
            ));
        }
        
        // Find and update user in database
        Optional<AppUser> userOpt = userRepository.findById(sessionUser.getId());
        if (userOpt.isPresent()) {
            AppUser user = userOpt.get();
            
            // Update fields
            user.setName(newName.trim());
            user.setEmail(newEmail.trim().toLowerCase());
            user.setPassword(passwordEncoder.encode(newPassword.trim()));
            
            // Save to database
            try {
                userRepository.save(user);
                
                // Update session with new user data
                session.setAttribute("user", user);
                
                // Return success response (without password)
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "User updated successfully",
                    "user", Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "name", user.getName(),
                        "role", user.getRole()
                    )
                ));
                
            } catch (Exception e) {
                // Handle database errors (like unique constraint violations)
                return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to update user. Please try again."
                ));
            }
        }
        
        return ResponseEntity.badRequest().body(Map.of(
            "success", false,
            "error", "User not found"
        ));
    }

    // Helper method for email validation
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        // Simple email validation regex
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }
}