package com.bball.stats.auth;

import com.bball.common.grpc.AuthServiceGrpc;
import com.bball.common.grpc.VerifyTokenRequest;
import com.bball.common.grpc.VerifyTokenResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

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
        VerifyTokenRequest request = VerifyTokenRequest.newBuilder()
                .setToken(token)
                .build();

        VerifyTokenResponse response = authStub.verifyToken(request);

        if (!response.getValid()) {
            throw new RuntimeException("Invalid or Expired Token");
        }

        return Integer.parseInt(response.getUserId());
    }
}