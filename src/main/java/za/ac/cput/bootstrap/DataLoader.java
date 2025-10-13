package za.ac.cput.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.ac.cput.domain.Contact;
import za.ac.cput.domain.User;
import za.ac.cput.repository.IUserRepository;
import za.ac.cput.service.IContactService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds a default administrative account on first startup.
 * The admin email and password are read from properties:
 *  - app.admin.email (default: admin@adp.ac.za)
 *  - app.admin.password (default: admin123)
 * These can be provided as environment variables (APP_ADMIN_EMAIL, APP_ADMIN_PASSWORD)
 */
@Component
public class DataLoader implements ApplicationRunner {

    private final IUserRepository userRepository;
    private final IContactService contactService;
    private final PasswordEncoder passwordEncoder;

    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);

    @Value("${app.admin.email:admin@adp.ac.za}")
    private String adminEmail;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    public DataLoader(IUserRepository userRepository, IContactService contactService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.contactService = contactService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (userRepository.findByContactEmail(adminEmail).isEmpty()) {
            // Encode the password and persist user with cascade so contact is persisted in same context
            String encoded = passwordEncoder.encode(adminPassword);
            Contact adminContact = new Contact.Builder()
                    .setEmail(adminEmail)
                    .setPhoneNumber("+27123456789")
                    .setAddress("Campus Admin Office")
                    .setPassword(encoded)
                    .build();

            User admin = new User.UserBuilder()
                    .setUserFirstName("System")
                    .setUserLastName("Administrator")
                    .setRole("ADMIN")
                    .setContact(adminContact)
                    .build();
            userRepository.save(admin);
            logger.info("Seeded admin user with email={}", adminEmail);
        } else {
            logger.info("Admin user with email={} already exists, skipping seeding", adminEmail);
        }
    }
}
