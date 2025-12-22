package com.bball.security.grpc;

import com.bball.common.grpc.AuthServiceGrpc;
import com.bball.common.grpc.VerifyTokenRequest;
import com.bball.common.grpc.VerifyTokenResponse;
import com.bball.security.config.JwtService;
import com.bball.security.user.User;
import com.bball.security.user.UserRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.redis.core.StringRedisTemplate; // <--- 1. IMPORT THIS
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class AuthGrpcService extends AuthServiceGrpc.AuthServiceImplBase {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate; // <--- 2. INJECT REDIS

    @Override
    public void verifyToken(VerifyTokenRequest request, StreamObserver<VerifyTokenResponse> responseObserver) {
        String token = request.getToken();

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        boolean isValid = false;
        String userId = "";
        String email = "";

        try {
            // --- 3. ADD BLACKLIST CHECK HERE ---
            if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token))) {
                log.warn("gRPC Verification Failed: Token is on blacklist");
                // We leave isValid = false and return immediately
            }
            // -----------------------------------
            else {
                // proceed with normal signature check
                String username = jwtService.extractUsername(token);
                if (username != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (jwtService.isTokenValid(token, userDetails)) {
                        User user = userRepository.findByEmail(username).orElse(null);
                        if (user != null) {
                            isValid = true;
                            userId = String.valueOf(user.getId());
                            email = user.getEmail();
                            log.debug("gRPC Verification Success: User {}", email);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("gRPC Token Verification Failed: {}", e.getMessage());
        }

        VerifyTokenResponse response = VerifyTokenResponse.newBuilder()
                .setValid(isValid)
                .setUserId(userId)
                .setEmail(email)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}