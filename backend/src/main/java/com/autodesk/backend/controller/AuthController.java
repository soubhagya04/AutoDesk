package com.autodesk.backend.controller;

import com.autodesk.backend.dto.request.LoginRequest;
import com.autodesk.backend.dto.request.RegisterRequest;
import com.autodesk.backend.dto.response.AuthResponse;
import com.autodesk.backend.dto.response.MessageResponse;
import com.autodesk.backend.entity.Role;
import com.autodesk.backend.security.UserDetailsImpl;
import com.autodesk.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        MessageResponse response = authService.registerUser(registerRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse authResponse = authService.loginUser(loginRequest);
        return ResponseEntity.ok(authResponse);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        Role userRole = Role.valueOf(userDetails.getAuthorities().iterator().next().getAuthority());

        AuthResponse authResponse = AuthResponse.builder()
                .id(userDetails.getId())
                .name(userDetails.getName())
                .email(userDetails.getEmail())
                .role(userRole)
                .build();

        return ResponseEntity.ok(authResponse);
    }
}
