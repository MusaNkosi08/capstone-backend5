/* UserController.java
   Author: Aimee Paulus (222814969)
   Date: 25 May 2025
*/

package za.ac.cput.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.domain.User;
import za.ac.cput.domain.Contact;
import za.ac.cput.service.IUserService;
import za.ac.cput.security.JwtUtil;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final IUserService userService;
    private final JwtUtil jwtUtil;

    public UserController(IUserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/create")
    public ResponseEntity<User> createUser(@Valid @RequestBody UserContactDTO dto) {
        Contact contact = dto.toContact();
        // Enforce public-created users to have CUSTOMER role only
        String role = "CUSTOMER";
        User user = new User.UserBuilder()
            .setUserFirstName(dto.userFirstName)
            .setUserLastName(dto.userLastName)
            .setRole(role)
            .setContact(contact)
            .build();
        return ResponseEntity.ok(userService.create(user));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(@PathVariable Long userId) {
        User user = userService.read(userId);
        return (user != null) ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }

    @GetMapping("/all")
    public ResponseEntity<List<User>> getAllUsers() {
        // Return all users except administrative accounts
        List<User> all = userService.findAll();
        List<User> nonAdmin = all.stream()
                .filter(u -> u.getRole() == null || !u.getRole().equalsIgnoreCase("ADMIN"))
                .collect(Collectors.toList());
        return ResponseEntity.ok(nonAdmin);
    }

    @PutMapping("/update")
    public ResponseEntity<User> updateUser(@Valid @RequestBody UserContactDTO dto) {
        // update must include an existing user id
        if (dto.userId == null) {
            return ResponseEntity.badRequest().build();
        }
        Contact contact = dto.toContact();
        User user = new User.UserBuilder()
                .setUserId(dto.userId)
                .setUserFirstName(dto.userFirstName)
                .setUserLastName(dto.userLastName)
                // Ensure updates through this public API cannot set admin role
                .setRole("CUSTOMER")
                .setContact(contact)
                .build();
        return ResponseEntity.ok(userService.update(user));
    }

    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        boolean deleted = userService.delete(userId);
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginDTO loginDTO) {
        User user = userService.login(loginDTO.email, loginDTO.password);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        // If client requested a specific userType (e.g., 'admin' or 'customer'), enforce it
        if (loginDTO.userType != null && !loginDTO.userType.isBlank()) {
            String requested = loginDTO.userType.trim().toUpperCase();
            String actual = (user.getRole() == null) ? "CUSTOMER" : user.getRole().trim().toUpperCase();
            if (requested.equals("ADMIN") && !actual.equals("ADMIN")) {
                return ResponseEntity.status(403).build();
            }
            if (requested.equals("CUSTOMER") && actual.equals("ADMIN")) {
                // Admins cannot login as customers via the public student login path
                return ResponseEntity.status(403).build();
            }
        }

        String role = user.getRole();
        // Use overload that embeds role into token
        String token = jwtUtil.generateToken(user.getContact().getEmail(), role != null ? role : "CUSTOMER");
        return ResponseEntity.ok(new AuthResponse(user, token));
    }

    // Validation error handler to return readable messages
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }
}
