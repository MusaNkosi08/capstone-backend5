package za.ac.cput.controller;

public class LoginDTO {
    public String email;
    public String password;
    // Optional requested user type: 'admin' or 'customer' (student)
    public String userType;
}

