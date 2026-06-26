package com.onde.api.application.auth;

import com.onde.api.application.auth.dto.*;
import com.onde.api.application.auth.support.AuthClientContext;
import com.onde.api.application.auth.support.AuthSessionIssuer;
import com.onde.api.application.auth.support.SignupRolePolicy;
import com.onde.core.entity.auth.RefreshToken;
import com.onde.core.entity.member.Member;
import com.onde.core.entity.member.MemberRole;
import com.onde.core.entity.member.MemberStatus;
import com.onde.core.exception.BusinessException;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.ForbiddenException;
import com.onde.core.exception.UnauthorizedException;
import com.onde.core.repository.MemberRepository;
import com.onde.core.repository.RefreshTokenRepository;
import com.onde.core.security.AuthTokenBlacklistService;
import com.onde.core.security.JwtTokenProvider;
import com.onde.core.security.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginAttemptService loginAttemptService;
    private final AuthSessionIssuer authSessionIssuer;
    private final AuthTokenBlacklistService authTokenBlacklistService;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (request.getPasswordConfirm() != null && !request.getPassword().equals(request.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (request.getNickname() != null && memberRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        MemberRole role = SignupRolePolicy.resolve(request.getRole());
        MemberStatus initialStatus = role == MemberRole.SELLER ? MemberStatus.PENDING : MemberStatus.ACTIVE;
        Member member = Member.builder()
                .authSubjectId(UUID.randomUUID().toString())
                .email(request.getEmail())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .nickname(request.getNickname())
                .age(request.getAge())
                .role(role)
                .status(initialStatus)
                .build();
        member.markPasswordUpdatedNow();

        Member savedMember = memberRepository.save(member);

        return SignupResponse.builder()
                .memberId(savedMember.getId())
                .email(savedMember.getEmail())
                .name(savedMember.getName())
                .role(savedMember.getRole())
                .status(savedMember.getStatus())
                .nickname(savedMember.getNickname())
                .age(savedMember.getAge())
                .createdAt(savedMember.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public boolean checkNicknameDuplicate(String nickname) {
        return memberRepository.existsByNickname(nickname);
    }

    @Transactional(readOnly = true)
    public boolean checkEmailDuplicate(String email) {
        return memberRepository.existsByEmail(email);
    }

    @Transactional
    public AuthSessionResult login(LoginRequest request, HttpServletRequest servletRequest) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.LOGIN_CREDENTIALS_INVALID));

        loginAttemptService.assertNotLocked(member);

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            loginAttemptService.recordFailure(member.getId());
            throw new UnauthorizedException(ErrorCode.LOGIN_CREDENTIALS_INVALID);
        }

        if (member.getRole() == MemberRole.USER_ADMIN
                || member.getRole() == MemberRole.SELLER_ADMIN
                || member.getRole() == MemberRole.SUPER_ADMIN) {
            loginAttemptService.recordFailure(member.getId());
            throw new UnauthorizedException(ErrorCode.LOGIN_CREDENTIALS_INVALID);
        }

        if (member.getRole() == MemberRole.BLACKLIST || member.getStatus() == MemberStatus.BANNED) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN);
        }

        if (member.getRole() == MemberRole.SELLER && member.getStatus() == MemberStatus.PENDING) {
            throw new BusinessException(ErrorCode.SELLER_PENDING_APPROVAL);
        }

        loginAttemptService.recordSuccess(member.getId());
        return authSessionIssuer.issue(member, servletRequest);
    }

    @Transactional
    public AuthSessionResult adminLogin(LoginRequest request, HttpServletRequest servletRequest) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.LOGIN_CREDENTIALS_INVALID));

        loginAttemptService.assertNotLocked(member);

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            loginAttemptService.recordFailure(member.getId());
            throw new UnauthorizedException(ErrorCode.LOGIN_CREDENTIALS_INVALID);
        }

        if (member.getRole() != MemberRole.USER_ADMIN
                && member.getRole() != MemberRole.SELLER_ADMIN
                && member.getRole() != MemberRole.SUPER_ADMIN) {
            loginAttemptService.recordFailure(member.getId());
            throw new UnauthorizedException(ErrorCode.LOGIN_CREDENTIALS_INVALID);
        }

        if (member.getStatus() == MemberStatus.BANNED) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN);
        }

        loginAttemptService.recordSuccess(member.getId());
        return authSessionIssuer.issue(member, servletRequest);
    }

    @Transactional(readOnly = true)
    public RefreshSessionResult refresh(String refreshToken, HttpServletRequest servletRequest) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 Refresh Token입니다.");
        }

        String subjectId = jwtTokenProvider.getSubject(refreshToken);
        RefreshToken savedToken = refreshTokenRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("로그인 정보가 없거나 만료되었습니다."));

        if (!savedToken.getRefreshToken().equals(refreshToken)) {
            throw new IllegalArgumentException("Refresh Token이 일치하지 않습니다.");
        }

        AuthClientContext clientContext = AuthClientContext.from(servletRequest);
        if (!clientContext.matches(savedToken)) {
            throw new IllegalArgumentException("세션 환경이 일치하지 않습니다. 다시 로그인해 주세요.");
        }

        Member member = resolveMemberBySubject(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        String newAccessToken = jwtTokenProvider.createAccessToken(subjectId, member.getRole().getSecurityRole());
        String newJti = jwtTokenProvider.getJti(newAccessToken);
        savedToken.updateActiveAccess(newJti, clientContext.getClientIp(), clientContext.getUserAgentHash());
        refreshTokenRepository.save(savedToken);

        TokenRefreshResponse profile = TokenRefreshResponse.builder()
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenValidTimeInSeconds())
                .build();

        return RefreshSessionResult.builder()
                .accessToken(newAccessToken)
                .profile(profile)
                .build();
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        if (StringUtils.hasText(accessToken)) {
            authTokenBlacklistService.blacklistAccessToken(accessToken);
        }

        String refreshSubject = null;
        if (StringUtils.hasText(refreshToken) && jwtTokenProvider.validateToken(refreshToken)) {
            refreshSubject = jwtTokenProvider.getSubject(refreshToken);
        }

        if (refreshSubject != null) {
            refreshTokenRepository.deleteById(refreshSubject);
            return;
        }

        if (StringUtils.hasText(accessToken) && jwtTokenProvider.validateToken(accessToken)) {
            refreshTokenRepository.deleteById(jwtTokenProvider.getSubject(accessToken));
        }
    }

    private java.util.Optional<Member> resolveMemberBySubject(String subjectId) {
        return memberRepository.findByAuthSubjectId(subjectId)
                .or(() -> memberRepository.findByEmail(subjectId))
                .or(() -> memberRepository.findByProviderId(subjectId));
    }
}
