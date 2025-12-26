package com.bball.security.auth;

import com.bball.security.config.JwtService;
import com.bball.security.user.Role;
import com.bball.security.user.User;
import com.bball.security.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private UserRepository repository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

    // We don't need to mock Redis for basic auth tests unless testing logout
    // @Mock private StringRedisTemplate redisTemplate;

    @InjectMocks
    private AuthenticationService authService;

    @Test
    @DisplayName("Should register user and return JWT")
    void shouldRegisterUser() {
        // 1. Setup Request
        RegisterRequest request = new RegisterRequest("John", "Doe", "john@test.com", "pass123");

        // 2. Mock Dependencies
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPass");
        when(jwtService.generateToken(any(User.class))).thenReturn("mock-jwt-token");

        // 3. Run Logic
        AuthenticationResponse response = authService.register(request);

        // 4. Verify
        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
        verify(repository).save(any(User.class)); // Ensure user was saved to DB
    }

    @Test
    @DisplayName("Should authenticate user and return JWT")
    void shouldAuthenticateUser() {
        // 1. Setup Request
        AuthenticationRequest request = new AuthenticationRequest("john@test.com", "pass123");
        User mockUser = User.builder()
                .email("john@test.com")
                .password("encodedPass")
                .role(Role.USER)
                .build();

        // 2. Mock DB finding the user
        when(repository.findByEmail(request.getEmail())).thenReturn(Optional.of(mockUser));
        when(jwtService.generateToken(mockUser)).thenReturn("mock-jwt-token");

        // 3. Run Logic
        AuthenticationResponse response = authService.authenticate(request);

        // 4. Verify
        assertEquals("mock-jwt-token", response.getToken());
        verify(authenticationManager).authenticate(any()); // Ensure auth manager was called
    }
}