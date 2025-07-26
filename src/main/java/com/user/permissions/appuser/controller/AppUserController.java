package com.user.permissions.appuser.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.user.permissions.appuser.AppUser;
import com.user.permissions.appuser.repository.AppUserRepository;

import jakarta.servlet.http.HttpSession;

import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3001", allowCredentials = "true")
public class AppUserController {
	
	@Autowired
	private AppUserRepository appUserRepository; 
	
    @Autowired
    private PasswordEncoder passwordEncoder;
	
	@GetMapping("/list")
	public ResponseEntity<?> listUsers(HttpSession session) {
	    AppUser sessionUser = (AppUser) session.getAttribute("user");
	    Boolean authenticated = (Boolean) session.getAttribute("authenticated");
	    
	    // Check authentication
	    if (sessionUser == null || !Boolean.TRUE.equals(authenticated)) {
	        return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
	    }
	    
	    // Check if user has permission to list users (only ADMIN role)
	    //if (!"ADMIN".equals(sessionUser.getRole())) {
	    //    return ResponseEntity.status(403).body(Map.of("error", "Access denied. Admin privileges required."));
	    //}
	    
	    try {
	        // Get all users from database
	        List<AppUser> users = appUserRepository.findAll();
	        
	        // Convert to response format (exclude passwords) - using HashMap to allow null values
	        List<Map<String, Object>> userList = users.stream()
	            .map(user -> {
	                Map<String, Object> userMap = new HashMap<>();
	                userMap.put("id", user.getId());
	                userMap.put("email", user.getEmail());
	                userMap.put("name", user.getName());
	                userMap.put("role", user.getRole());
	                userMap.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
	                userMap.put("updatedAt", user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null);
	                return userMap;
	            })
	            .collect(Collectors.toList());
	        
	        return ResponseEntity.ok(Map.of(
	            "success", true,
	            "users", userList,
	            "totalCount", userList.size()
	        ));
	        
	    } catch (Exception e) {
	        return ResponseEntity.status(500).body(Map.of(
	            "success", false,
	            "error", "Failed to retrieve users. Please try again."
	        ));
	    }
	}

	@GetMapping("/search")
	public ResponseEntity<?> searchUsers(
	        @RequestParam(value = "email" , required = false) String email,
	        @RequestParam(value = "name"  , required = false) String name,
	        @RequestParam(value = "role"  , required = false) String role,
	        HttpSession session) {
	    
	    AppUser sessionUser = (AppUser) session.getAttribute("user");
	    Boolean authenticated = (Boolean) session.getAttribute("authenticated");
	    
	    // Check authentication
	    if (sessionUser == null || !Boolean.TRUE.equals(authenticated)) {
	        return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
	    }
	    
	    // Check if user has permission (only ADMIN role)
	    if (!"ADMIN".equals(sessionUser.getRole())) {
	        return ResponseEntity.status(403).body(Map.of("error", "Access denied. Admin privileges required."));
	    }
	    
	    try {
	        List<AppUser> users;
	        
	        // If no search parameters provided, return all users
	        if ((email == null || email.trim().isEmpty()) && 
	            (name == null || name.trim().isEmpty()) && 
	            (role == null || role.trim().isEmpty())) {
	            users = appUserRepository.findAll();
	        } else {
	        
	        	// Search with filters
	            users = appUserRepository.findAll().stream()
	                .filter(user -> {
	                    boolean matches = true;
	                    
	                    if (email != null && !email.trim().isEmpty()) {
	                        matches = matches && user.getEmail().toLowerCase()
	                            .contains(email.trim().toLowerCase());
	                    }
	                    
	                    if (name != null && !name.trim().isEmpty()) {
	                        matches = matches && user.getName().toLowerCase()
	                            .contains(name.trim().toLowerCase());
	                    }
	                    
	                    if (role != null && !role.trim().isEmpty()) {
	                        matches = matches && user.getRole().toLowerCase()
	                            .contains(role.trim().toLowerCase());
	                    }
	                    
	                    return matches;
	                })
	                .collect(Collectors.toList());
	        }
	        
	        // Convert to response format
	        // Convert to response format (exclude passwords) - using HashMap to allow null values
	        List<Map<String, Object>> userList = users.stream()
	            .map(user -> {
	                Map<String, Object> userMap = new HashMap<>();
	                userMap.put("id", user.getId());
	                userMap.put("email", user.getEmail());
	                userMap.put("name", user.getName());
	                userMap.put("role", user.getRole());
	                userMap.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
	                userMap.put("updatedAt", user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null);
	                return userMap;
	            })
	            .collect(Collectors.toList());

	        
	        return ResponseEntity.ok(Map.of(
	            "success", true,
	            "users", userList,
	            "totalCount", userList.size(),
	            "searchCriteria", Map.of(
	                "email", email != null ? email : "",
	                "name", name != null ? name : "",
	                "role", role != null ? role : ""
	            )
	        ));
	        
	    } catch (Exception e) {
	        return ResponseEntity.status(500).body(Map.of(
	            "success", false,
	            "error", "Failed to search users. Please try again."
	        ));
	    }
	}

	@GetMapping("/get/{id}")
	public ResponseEntity<?> getUserById(@PathVariable("id") Long id, HttpSession session) {
	    AppUser sessionUser = (AppUser) session.getAttribute("user");
	    Boolean authenticated = (Boolean) session.getAttribute("authenticated");
	    
	    // Check authentication
	    if (sessionUser == null || !Boolean.TRUE.equals(authenticated)) {
	        return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
	    }
	    
	    // Users can view their own profile, admins can view any profile
	    if (!"ADMIN".equals(sessionUser.getRole()) && !sessionUser.getId().equals(id)) {
	        return ResponseEntity.status(403).body(Map.of("error", "Access denied. You can only view your own profile."));
	    }
	    
	    try {
	        Optional<AppUser> userOpt = appUserRepository.findById(id);
	        
	        if (userOpt.isPresent()) {
	            AppUser user = userOpt.get();
	            
	            Map<String, Object> userMap = Map.of(
	                "id", user.getId(),
	                "email", user.getEmail(),
	                "name", user.getName(),
	                "role", user.getRole(),
	                "createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null,
	                "updatedAt", user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null
	            );
	            
	            return ResponseEntity.ok(Map.of(
	                "success", true,
	                "user", userMap
	            ));
	        } else {
	            return ResponseEntity.status(404).body(Map.of(
	                "success", false,
	                "error", "User not found"
	            ));
	        }
	        
	    } catch (Exception e) {
	        return ResponseEntity.status(500).body(Map.of(
	            "success", false,
	            "error", "Failed to retrieve user. Please try again."
	        ));
	    }
	}
	
	
	@PostMapping("/create")
	public ResponseEntity<?> createUser(@RequestBody Map<String, String> userRequest, HttpSession session) {
	    AppUser sessionUser = (AppUser) session.getAttribute("user");
	    Boolean authenticated = (Boolean) session.getAttribute("authenticated");
	    
	    // Check authentication
	    if (sessionUser == null || !Boolean.TRUE.equals(authenticated)) {
	        return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
	    }
	    
	    // Check if user has permission to create users (only ADMIN role)
	    if (!"ADMIN".equals(sessionUser.getRole())) {
	        return ResponseEntity.status(403).body(Map.of("error", "Access denied. Admin privileges required."));
	    }
	    
	    try {
	        // Validate required fields
	        String email = userRequest.get("email");
	        String password = userRequest.get("password");
	        String name = userRequest.get("name");
	        String role = userRequest.get("role");
	        
	        if (email == null || email.trim().isEmpty()) {
	            return ResponseEntity.status(400).body(Map.of("error", "Email is required"));
	        }
	        
	        if (password == null || password.trim().isEmpty()) {
	            return ResponseEntity.status(400).body(Map.of("error", "Password is required"));
	        }
	        
	        if (name == null || name.trim().isEmpty()) {
	            return ResponseEntity.status(400).body(Map.of("error", "Name is required"));
	        }
	        
	        if (role == null || role.trim().isEmpty()) {
	            return ResponseEntity.status(400).body(Map.of("error", "Role is required"));
	        }
	        
	        // Validate role values
	        if (!role.equals("USER") && !role.equals("ADMIN")) {
	            return ResponseEntity.status(400).body(Map.of("error", "Role must be either USER or ADMIN"));
	        }
	        
	        // Check if email already exists
	        if (appUserRepository.findByEmail(email.trim()).isPresent()) {
	            return ResponseEntity.status(400).body(Map.of("error", "Email already exists"));
	        }
	        
	        // Create new user
	        AppUser newUser = new AppUser();
	        newUser.setEmail(email.trim().toLowerCase());
	        newUser.setPassword(passwordEncoder.encode(password));
	        newUser.setName(name.trim());
	        newUser.setRole(role.trim().toUpperCase());
	        
	        AppUser savedUser = appUserRepository.save(newUser);
	        
	        // Return created user (without password)
	        Map<String, Object> userMap = Map.of(
	            "id", savedUser.getId(),
	            "email", savedUser.getEmail(),
	            "name", savedUser.getName(),
	            "role", savedUser.getRole(),
	            "createdAt", savedUser.getCreatedAt().toString(),
	            "updatedAt", savedUser.getUpdatedAt().toString()
	        );
	        
	        return ResponseEntity.status(201).body(Map.of(
	            "success", true,
	            "message", "User created successfully",
	            "user", userMap
	        ));
	        
	    } catch (Exception e) {
	        return ResponseEntity.status(500).body(Map.of(
	            "success", false,
	            "error", "Failed to create user. Please try again."
	        ));
	    }
	}

	@PutMapping("/update/{id}")
	public ResponseEntity<?> updateUser(@PathVariable("id") Long id, @RequestBody Map<String, String> userRequest, HttpSession session) {
	    AppUser sessionUser = (AppUser) session.getAttribute("user");
	    Boolean authenticated = (Boolean) session.getAttribute("authenticated");
	    
	    // Check authentication
	    if (sessionUser == null || !Boolean.TRUE.equals(authenticated)) {
	        return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
	    }
	    
	    // Users can update their own profile, admins can update any profile
	    if (!"ADMIN".equals(sessionUser.getRole()) && !sessionUser.getId().equals(id)) {
	        return ResponseEntity.status(403).body(Map.of("error", "Access denied. You can only update your own profile."));
	    }
	    
	    try {
	        Optional<AppUser> userOpt = appUserRepository.findById(id);
	        
	        if (!userOpt.isPresent()) {
	            return ResponseEntity.status(404).body(Map.of(
	                "success", false,
	                "error", "User not found"
	            ));
	        }
	        
	        AppUser userToUpdate = userOpt.get();
	        boolean isUpdated = false;
	        
	        // Update email if provided
	        String email = userRequest.get("email");
	        if (email != null && !email.trim().isEmpty()) {
	            email = email.trim().toLowerCase();
	            // Check if new email already exists (excluding current user)
	            Optional<AppUser> existingEmailUser = appUserRepository.findByEmail(email);
	            if (existingEmailUser.isPresent() && !existingEmailUser.get().getId().equals(id)) {
	                return ResponseEntity.status(400).body(Map.of("error", "Email already exists"));
	            }
	            userToUpdate.setEmail(email);
	            isUpdated = true;
	        }
	        
	        // Update name if provided
	        String name = userRequest.get("name");
	        if (name != null && !name.trim().isEmpty()) {
	            userToUpdate.setName(name.trim());
	            isUpdated = true;
	        }
	        
	        // Update role if provided (only admins can change roles)
	        String role = userRequest.get("role");
	        if (role != null && !role.trim().isEmpty()) {
	            if (!"ADMIN".equals(sessionUser.getRole())) {
	                return ResponseEntity.status(403).body(Map.of("error", "Only admins can update user roles"));
	            }
	            if (!role.equals("USER") && !role.equals("ADMIN")) {
	                return ResponseEntity.status(400).body(Map.of("error", "Role must be either USER or ADMIN"));
	            }
	            userToUpdate.setRole(role.trim().toUpperCase());
	            isUpdated = true;
	        }
	        
	        // Update password if provided
	        String password = userRequest.get("password");
	        if (password != null && !password.trim().isEmpty()) {
	            userToUpdate.setPassword(passwordEncoder.encode(password));
	            isUpdated = true;
	        }
	        
	        if (!isUpdated) {
	            return ResponseEntity.status(400).body(Map.of("error", "No valid fields provided for update"));
	        }
	        
	        AppUser updatedUser = appUserRepository.save(userToUpdate);
	        
	        // Return updated user (without password)
	        Map<String, Object> userMap = Map.of(
	            "id", updatedUser.getId(),
	            "email", updatedUser.getEmail(),
	            "name", updatedUser.getName(),
	            "role", updatedUser.getRole(),
	            "createdAt", updatedUser.getCreatedAt() != null ? updatedUser.getCreatedAt().toString() : null,
	            "updatedAt", updatedUser.getUpdatedAt().toString()
	        );
	        
	        return ResponseEntity.ok(Map.of(
	            "success", true,
	            "message", "User updated successfully",
	            "user", userMap
	        ));
	        
	    } catch (Exception e) {
	        return ResponseEntity.status(500).body(Map.of(
	            "success", false,
	            "error", "Failed to update user. Please try again."
	        ));
	    }
	}
	
	
	@DeleteMapping("/delete/{id}")
	public ResponseEntity<?> deleteUser(@PathVariable("id") Long id, HttpSession session) {
	    AppUser sessionUser = (AppUser) session.getAttribute("user");
	    Boolean authenticated = (Boolean) session.getAttribute("authenticated");
	    
	    // Check authentication
	    if (sessionUser == null || !Boolean.TRUE.equals(authenticated)) {
	        return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
	    }
	    
	    // Only admins can delete users, and they cannot delete themselves
	    if (!"ADMIN".equals(sessionUser.getRole())) {
	        return ResponseEntity.status(403).body(Map.of("error", "Access denied. Admin privileges required."));
	    }
	    
	    if (sessionUser.getId().equals(id)) {
	        return ResponseEntity.status(400).body(Map.of("error", "You cannot delete your own account"));
	    }
	    
	    try {
	        Optional<AppUser> userOpt = appUserRepository.findById(id);
	        
	        if (!userOpt.isPresent()) {
	            return ResponseEntity.status(404).body(Map.of(
	                "success", false,
	                "error", "User not found"
	            ));
	        }
	        
	        AppUser userToDelete = userOpt.get();
	        
	        // Store user info for response before deletion
	        Map<String, Object> deletedUserInfo = Map.of(
	            "id", userToDelete.getId(),
	            "email", userToDelete.getEmail(),
	            "name", userToDelete.getName(),
	            "role", userToDelete.getRole()
	        );
	        
	        // Delete the user
	        appUserRepository.delete(userToDelete);
	        
	        return ResponseEntity.ok(Map.of(
	            "success", true,
	            "message", "User deleted successfully",
	            "deletedUser", deletedUserInfo
	        ));
	        
	    } catch (Exception e) {
	        return ResponseEntity.status(500).body(Map.of(
	            "success", false,
	            "error", "Failed to delete user. Please try again."
	        ));
	    }
	}

}
