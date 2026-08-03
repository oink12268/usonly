package com.evho.usonly.global.filter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class FirebaseAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseAuthFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // CORS preflight 요청은 인증 없이 통과 (Spring MVC가 CORS 헤더 처리)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // /actuator/** 는 인증 제외
        if (request.getRequestURI().startsWith("/actuator/")) {
            return true;
        }
        // /api/** 이외의 경로(정적 리소스 등)는 필터 제외
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(response, "Authorization header missing");
            return;
        }

        String idToken = authHeader.substring(7);

        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            // 검증된 uid/email을 request 속성으로 저장 (컨트롤러에서 필요 시 사용 가능)
            request.setAttribute("firebaseUid", decodedToken.getUid());
            request.setAttribute("firebaseEmail", decodedToken.getEmail());
        } catch (Exception e) {
            logger.error("[FirebaseAuthFilter] 토큰 검증 실패: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            sendUnauthorized(response, "Invalid or expired Firebase token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}
