package com.autodesk.backend.service;

import com.autodesk.backend.dto.request.LoginRequest;
import com.autodesk.backend.dto.request.RegisterRequest;
import com.autodesk.backend.dto.response.AuthResponse;
import com.autodesk.backend.dto.response.MessageResponse;
import com.autodesk.backend.entity.Role;
import com.autodesk.backend.entity.User;
import com.autodesk.backend.repository.UserRepository;
import com.autodesk.backend.security.JwtUtils;
import com.autodesk.backend.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @Transactional
    public MessageResponse registerUser(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Error: Email is already in use!");
        }

        Role userRole = Role.ROLE_EMPLOYEE;

        User user = User.builder()
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(userRole)
                .build();

        userRepository.save(user);

        return new MessageResponse("User registered successfully!");
    }

    public AuthResponse loginUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Role userRole = userDetails.getRole();

        return AuthResponse.builder()
                .token(jwt)
                .tokenType("Bearer")
                .id(userDetails.getId())
                .name(userDetails.getName())
                .email(userDetails.getEmail())
                .role(userRole)
                .build();
    }
}
