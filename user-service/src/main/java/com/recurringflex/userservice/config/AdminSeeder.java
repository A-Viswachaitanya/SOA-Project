package com.recurringflex.userservice.config;

import com.recurringflex.userservice.entity.User;
import com.recurringflex.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the initial ADMIN account on startup, since self-registration only creates USER accounts.
 * Credentials come from ADMIN_USERNAME / ADMIN_EMAIL / ADMIN_PASSWORD.
 */
@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    @Value("${admin.username}")
    private String username;

    @Value("${admin.email}")
    private String email;

    @Value("${admin.password}")
    private String password;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        User admin = new User();
        admin.setUsername(username);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole("ADMIN");
        try {
            userRepository.save(admin);
            log.info("Seeded admin account '{}'", username);
        } catch (DataIntegrityViolationException e) {
            // Another instance created it at the same time
            log.info("Admin account '{}' already exists", username);
        }
    }
}
