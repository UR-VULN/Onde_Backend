package com.onde.api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    // 유효한 자격증명(토큰)을 제공하지 않고 접근하려 할 때 401 에러를 리턴
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        // 1. 응답 타입을 JSON과 UTF-8(한글 깨짐 방지)로 명시적 설정
        response.setContentType("application/json;charset=UTF-8");
        // 2. HTTP 상태 코드를 401(Unauthorized)로 설정
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        // 3. Postman Body에 보일 JSON 메시지 직접 작성해서 밀어넣기
        response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"인증에 실패하였습니다. (토큰이 없거나 만료되었거나 권한이 부족합니다.)\"}");
    }
}