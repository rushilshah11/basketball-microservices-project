package com.bball.security.auth;

import com.bball.security.config.JwtService;
import com.bball.security.user.Role;
import com.bball.security.user.User;
import com.bball.security.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final StringRedisTemplate redisTemplate;


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
        var user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Email not recognized. Please create an account."));
        try{
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Incorrect password. Please try again.");
        }

        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder().token(jwtToken).build();
    }

    public LogoutResponse logout(HttpServletRequest request) {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return LogoutResponse.builder().message("No token found").build();
        }

        String jwt = authHeader.substring(7);

        // 1. Get expiration time from token
        java.util.Date expiration = jwtService.extractExpiration(jwt);
        long ttlInMillis = expiration.getTime() - System.currentTimeMillis();

        if (ttlInMillis > 0) {
            // 2. Add to Redis Blacklist with TTL matching the token's remaining life
            // Key: "blacklist:<token>", Value: "true"
            redisTemplate.opsForValue().set("blacklist:" + jwt, "true", ttlInMillis, TimeUnit.MILLISECONDS);
        }

        SecurityContextHolder.clearContext();

        return LogoutResponse.builder()
                .message("Logged out successfully")
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
