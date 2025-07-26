package com.user.permissions.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.user.permissions.appuser.AppUser;
import com.user.permissions.appuser.repository.AppUserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private AppUserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== DataInitializer: Starting data initialization ===");
        
        // Check if users already exist
        long userCount = userRepository.count();
        System.out.println("Current user count: " + userCount);
        
        if (userCount == 0) {
            System.out.println("No users found. Creating initial users...");
            
            // Create admin user
            AppUser admin = new AppUser();
            admin.setEmail("admin@example.com");
            admin.setName("Admin User");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");
            userRepository.save(admin);
            System.out.println("✓ Admin user created: admin@example.com / admin123");

            // Create regular user
            AppUser user = new AppUser();
            user.setEmail("user@example.com");
            user.setName("Regular User");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setRole("USER");
            userRepository.save(user);
            System.out.println("✓ Regular user created: user@example.com / user123");

            // Create additional test user
            AppUser testUser = new AppUser();
            testUser.setEmail("test@test.com");
            testUser.setName("Test User");
            testUser.setPassword(passwordEncoder.encode("test123"));
            testUser.setRole("USER");
            userRepository.save(testUser);
            System.out.println("✓ Test user created: test@test.com / test123");
            
            System.out.println("=== Initial users created successfully! ===");
            
        } else {
            System.out.println("Users already exist. Listing current users:");
            userRepository.findAll().forEach(existingUser -> 
                System.out.println("- " + existingUser.getEmail() + " (" + existingUser.getRole() + ")")
            );
        }
        
        System.out.println("=== DataInitializer: Completed ===");
    }
}