package com.user.permissions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;

import com.user.permissions.appuser.AppUser;
import com.user.permissions.appuser.repository.AppUserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public  class GenericTesting  {
	

	@LocalServerPort
    protected int port;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    

    @Autowired
    protected TestRestTemplate restTemplate;

    
    public AppUser testUser;
    public AppUser adminUser;
    public String baseUrl = "";
    public String authUrl = "";

    
	public void setUp () {
    	
		
		userRepository.deleteAll();
	    // Create test users
        testUser = new AppUser();
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setName("Test User");
        testUser.setRole("USER");
        testUser = userRepository.save(testUser);

        adminUser = new AppUser();
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(passwordEncoder.encode("admin123"));
        adminUser.setName("Admin User");
        adminUser.setRole("ADMIN");
        adminUser = userRepository.save(adminUser);
        
        System.out.println("Created test users:");
        System.out.println("- " + testUser.getEmail() + " (ID: " + testUser.getId() + ")");
        System.out.println("- " + adminUser.getEmail() + " (ID: " + adminUser.getId() + ")");
	}
	
	
}
