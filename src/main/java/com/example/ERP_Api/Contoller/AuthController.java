package com.example.ERP_Api.Contoller;


import com.example.ERP_Api.Service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Body: {"name": "...", "email": "...", "password": "..."}
    // Matches create-account.html exactly.
    @PostMapping("/register")
    public ResponseEntity<AuthService.AuthResponse> register(@RequestBody AuthService.RegisterRequest req) {
        try {
            AuthService.AuthResponse response = authService.register(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            // Email already exists — create-account.html specifically
            // checks for 409 to show "account already exists".
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    // Body: {"email": "...", "password": "..."}
    // Matches login.html exactly — returns {"token": "..."} on success.
    @PostMapping("/login")
    public ResponseEntity<AuthService.AuthResponse> login(@RequestBody AuthService.LoginRequest req) {
        try {
            AuthService.AuthResponse response = authService.login(req);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // login.html specifically checks for 401 to show
            // "Incorrect email or password."
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}