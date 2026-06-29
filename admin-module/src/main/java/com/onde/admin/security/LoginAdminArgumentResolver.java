package com.onde.admin.security;

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

public class LoginAdminArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginAdmin.class) 
            && (parameter.getParameterType().equals(String.class) || parameter.getParameterType().equals(Long.class));
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        
        String adminId = request.getHeader("X-Admin-Id");
        
        if (adminId == null || adminId.isEmpty()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null
                    && authentication.isAuthenticated()
                    && authentication.getName() != null
                    && !"anonymousUser".equals(authentication.getName())) {
                adminId = authentication.getName();
            }
        }

        LoginAdmin annotation = parameter.getParameterAnnotation(LoginAdmin.class);
        if (annotation != null && annotation.required() && (adminId == null || adminId.isEmpty())) {
            throw new UnauthorizedException(ErrorCode.MEMBER_NOT_FOUND);
        }

        if (adminId == null || adminId.isEmpty()) {
            return null;
        }

        if (parameter.getParameterType().equals(Long.class)) {
            try {
                return Long.parseLong(adminId);
            } catch (NumberFormatException e) {
                throw new UnauthorizedException(ErrorCode.MEMBER_NOT_FOUND);
            }
        }
        return adminId;
    }
}
