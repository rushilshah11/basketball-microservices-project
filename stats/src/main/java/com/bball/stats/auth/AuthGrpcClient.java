package com.bball.stats.auth;

import com.bball.common.grpc.AuthServiceGrpc;
import com.bball.common.grpc.VerifyTokenRequest;
import com.bball.common.grpc.VerifyTokenResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class AuthGrpcClient {

    // "security-service" matches the name in application.yml
    @GrpcClient("security-service")
    private AuthServiceGrpc.AuthServiceBlockingStub authStub;

    /**
     * Verifies a token and returns the User ID if valid.
     * Throws exception if invalid.
     */
    public Integer verifyUser(String token) {
        // Remove "Bearer " prefix if present
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || token.trim().isEmpty()) {
            log.warn("Empty token provided for verification");
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Authorization token is required"
            );
        }

        try {
            VerifyTokenRequest request = VerifyTokenRequest.newBuilder()
                    .setToken(token)
                    .build();

            VerifyTokenResponse response = authStub.verifyToken(request);

            if (!response.getValid()) {
                log.warn("Token verification failed: invalid or expired token");
                throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid or Expired Token"
                );
            }

            return Integer.parseInt(response.getUserId());
        } catch (StatusRuntimeException e) {
            log.error("gRPC error verifying token: {} - {}", e.getStatus().getCode(), e.getMessage());
            if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Authentication service is unavailable. Please try again later."
                );
            } else if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                throw new ResponseStatusException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "Authentication service timeout. Please try again."
                );
            }
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Token verification failed: " + e.getMessage()
            );
        } catch (NumberFormatException e) {
            log.error("Invalid user ID format from auth service: {}", e.getMessage());
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Invalid response from authentication service"
            );
        } catch (Exception e) {
            log.error("Unexpected error during token verification: {}", e.getMessage(), e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An error occurred during authentication"
            );
        }
    }
}