package com.bball.security.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import com.bball.security.util.AuditLogger;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final StringRedisTemplate redisTemplate;
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        return request.getServletPath().contains("/api/v1/auth");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try{
            final String authHeader = request.getHeader("Authorization");
            final String jwt;
            String userEmail = null;

            if (authHeader == null  || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }
            jwt  = authHeader.substring(7);
            if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + jwt))) {
                AuditLogger.info("auth_failure|reason=blacklisted_token|user=" + userEmail);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
                response.getWriter().write("Token is logged out (blacklisted)");
                return;
            }

            userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
            filterChain.doFilter(request, response);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // CATCH THE ERROR HERE
            AuditLogger.info("auth_failure|reason=expired_token|message=" + e.getMessage());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
            response.getWriter().write("Token Expired");
            response.getWriter().flush();
        } catch (Exception e) {
            // Catch other JWT errors (malformed, etc)
            AuditLogger.info("auth_failure|reason=invalid_token|message=" + e.getMessage());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Invalid Token");
        }

    }

}
