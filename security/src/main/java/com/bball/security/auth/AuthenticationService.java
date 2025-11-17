package com.bball.security.auth;

import com.bball.security.config.JwtService;
import com.bball.security.user.Role;
import com.bball.security.user.User;
import com.bball.security.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest request) {
        var user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();
        repository.save(user);
        var jwtToken = jwtService.generateToken(user);

        return AuthenticationResponse.builder().token(jwtToken).build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = repository.findByEmail(request.getEmail()).orElseThrow();
        var jwtToken = jwtService.generateToken(user);

        return AuthenticationResponse.builder().token(jwtToken).build();
    }

    public LogoutResponse logout() {
        // Clear the security context
        SecurityContextHolder.clearContext();
        
        // TODO: For production, implement token blacklist:
        // 1. Store invalidated tokens in Redis with TTL matching token expiration
        // 2. Check blacklist in JwtAuthenticationFilter before validating token
        // 3. This prevents token reuse until natural expiration
        
        return LogoutResponse.builder()
                .message("Logged out successfully. Please delete the token on the client side.")
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
