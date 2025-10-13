package za.ac.cput.controller;

import za.ac.cput.domain.Contact;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class UserContactDTO {
    public Long userId;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be 2-50 characters")
    public String userFirstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be 2-50 characters")
    public String userLastName;

    public String role;

    @Valid
    @NotNull(message = "Contact information is required")
    public ContactDTO contact;

    public Contact toContact() {
        if (contact == null) return null;
        return new Contact(contact.email, contact.phoneNumber, contact.address, contact.password);
    }

    public static class ContactDTO {
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    public String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+27|0)[6-8][0-9]{8}$", message = "Phone number must be a valid South African number")
    public String phoneNumber;

    @NotBlank(message = "Address is required")
    @Size(min = 10, max = 200, message = "Address must be between 10 and 200 characters")
    public String address;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}", message = "Password must contain uppercase, lowercase and a digit")
    public String password;
    }
}
