package com.onde.api.security;

import com.onde.core.entity.member.MemberRole;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginMember.class) 
            && (parameter.getParameterType().equals(Long.class) || parameter.getParameterType().equals(String.class));
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        
        // 1. [우선순위 1] 헤더에서 X-Member-Id 조회 (기존 테스트 및 단독 테스트용)
        String memberIdStr = request.getHeader("X-Member-Id");
        if (memberIdStr != null && !memberIdStr.isEmpty()) {
            try {
                if (parameter.getParameterType().equals(Long.class)) {
                    return Long.parseLong(memberIdStr);
                }
                return memberIdStr;
            } catch (NumberFormatException e) {
                // 파싱 실패 시 스프링 시큐리티 컨텍스트로 넘어감
            }
        }

        // 2. [우선순위 2] Spring Security Context의 인증 객체에서 Member ID 추출
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long memberId = userDetails.getMember().getId();
            
            if (parameter.getParameterType().equals(Long.class)) {
                return memberId;
            }
            return String.valueOf(memberId);
        }

        // 3. 필수 여부 검증 및 미인증 처리
        LoginMember annotation = parameter.getParameterAnnotation(LoginMember.class);
        if (annotation != null && annotation.required()) {
            throw new UnauthorizedException(ErrorCode.MEMBER_NOT_FOUND);
        }

        return null;
    }
}
