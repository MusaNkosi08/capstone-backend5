package za.ac.cput.controller;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.domain.Contact;
import za.ac.cput.domain.User;
import za.ac.cput.repository.IUserRepository;
import za.ac.cput.security.JwtUtil;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                          IUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public String register(@RequestBody Map<String, Object> body) {
        // Expecting body to provide contact and user details
    Map<String, Object> contactMap = (Map<String, Object>) body.get("contact");
    String email = (String) contactMap.getOrDefault("email", "");
    String phone = (String) contactMap.getOrDefault("phoneNumber", "");
    String address = (String) contactMap.getOrDefault("address", "");
    String rawPassword = (String) body.getOrDefault("password", "");
    Contact contact = new Contact(email, phone, address);

        User user = new User.UserBuilder()
                .setUserFirstName((String) body.getOrDefault("userFirstName", ""))
                .setUserLastName((String) body.getOrDefault("userLastName", ""))
                .setRole((String) body.getOrDefault("role", "USER"))
                .setContact(contact)
                .setPassword(passwordEncoder.encode(rawPassword))
                .build();

        userRepository.save(user);
        return "User registered successfully";
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) throws AuthenticationException {
        String email = body.get("email");
        String password = body.get("password");

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

    // Retrieve the user's role and include it in the token so the JwtAuthenticationFilter
    // can restore the correct authority from the token's claims.
    User user = userRepository.findByContactEmail(email).orElse(null);
    String role = user != null && user.getRole() != null ? user.getRole() : "USER";
    
    // Only allow admin users to log in through this endpoint
    if (user == null || !role.equalsIgnoreCase("ADMIN")) {
        throw new AuthenticationException("Access denied. This endpoint is for administrators only.") {};
    }

    String token = jwtUtil.generateToken(email, role);
    return Map.of("token", token, "user", user);
    }
}
