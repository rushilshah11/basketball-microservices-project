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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class AuthGrpcService extends AuthServiceGrpc.AuthServiceImplBase {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @Override
    public void verifyToken(VerifyTokenRequest request, StreamObserver<VerifyTokenResponse> responseObserver) {
        String token = request.getToken();

        // 1. Clean the token (remove "Bearer " if sent)
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        boolean isValid = false;
        String userId = "";
        String email = "";

        try {
            // 2. Check if token is valid using your existing JwtService
            String username = jwtService.extractUsername(token);
            if (username != null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtService.isTokenValid(token, userDetails)) {
                    // 3. If valid, find the user to get their ID
                    User user = userRepository.findByEmail(username).orElse(null);
                    if (user != null) {
                        isValid = true;
                        userId = String.valueOf(user.getId()); // Convert Integer ID to String for gRPC
                        email = user.getEmail();
                        log.debug("gRPC Verification Success: User {}", email);
                    }
                }
            }
        } catch (Exception e) {
            log.error("gRPC Token Verification Failed: {}", e.getMessage());
        }

        // 4. Build the response object
        VerifyTokenResponse response = VerifyTokenResponse.newBuilder()
                .setValid(isValid)
                .setUserId(userId)
                .setEmail(email)
                .build();

        // 5. Send response back to Stats Service
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}