package com.recurringflex.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// All fields optional: only non-null fields are updated. Changing role requires ADMIN.
public class UpdateUserRequest {
    @Email(message = "Email must be valid")
    private String email;

    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    private String password;

    @Pattern(regexp = "(?i)USER|ADMIN", message = "Role must be USER or ADMIN")
    private String role;

    public UpdateUserRequest() {}

    public UpdateUserRequest(String email, String password, String role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
