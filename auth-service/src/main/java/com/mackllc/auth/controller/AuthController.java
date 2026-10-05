package com.mackllc.auth.controller;

import com.mackllc.auth.dto.LoginRequest;
import com.mackllc.auth.dto.LoginResponse;
import com.mackllc.auth.dto.RegisterRequest;
import com.mackllc.auth.model.User;
import com.mackllc.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Boolean>> validate(@RequestParam String token) {
        return ResponseEntity.ok(Map.of("valid", authService.validateToken(token)));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "auth-service"));
    }
}
