package com.attentiq.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthRequest {

    @Data
    public static class Login {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
    }

    @Data
    public static class Register {
        @NotBlank
        private String name;

        @NotBlank @Email
        private String email;

        @NotBlank @Size(min = 6)
        private String password;

        @NotBlank
        private String role; // HOST or PARTICIPANT
    }
}
