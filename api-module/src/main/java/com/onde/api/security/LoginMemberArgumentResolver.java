package com.onde.api.security;

import com.onde.core.entity.member.MemberRole;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
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
        
        // Authorization 헤더나 X-Member-Id 헤더를 통해 간단히 memberId 획득
        String memberIdStr = request.getHeader("X-Member-Id");
        
        if (memberIdStr == null || memberIdStr.isEmpty()) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                memberIdStr = authHeader.substring(7).trim();
            }
        }

        LoginMember annotation = parameter.getParameterAnnotation(LoginMember.class);
        if (annotation != null && annotation.required() && (memberIdStr == null || memberIdStr.isEmpty())) {
            throw new UnauthorizedException(ErrorCode.MEMBER_NOT_FOUND);
        }

        if (memberIdStr == null || memberIdStr.isEmpty()) {
            return null;
        }

        try {
            if (parameter.getParameterType().equals(Long.class)) {
                return Long.parseLong(memberIdStr);
            }
            return memberIdStr;
        } catch (NumberFormatException e) {
            throw new UnauthorizedException(ErrorCode.MEMBER_NOT_FOUND);
        }
    }
}
