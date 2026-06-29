package com.onde.api.application.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.onde.api.application.notification.dto.FcmTokenRequest;
import com.onde.api.application.notification.dto.FcmTokenResponse;
import com.onde.core.entity.member.Member;
import com.onde.core.entity.notification.FcmToken;
import com.onde.core.entity.reservation.Reservation;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.ForbiddenException;
import com.onde.core.exception.NotFoundException;
import com.onde.core.repository.FcmTokenRepository;
import com.onde.core.repository.MemberRepository;
import com.onde.core.repository.ReservationRepository;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final FirebaseMessaging firebaseMessaging;
    private final FcmTokenRepository fcmTokenRepository;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public FcmTokenResponse saveFcmToken(FcmTokenRequest req, Long memberId) {
        // 1. 회원 존재 여부 검증 (논리 FK)
        if (!memberRepository.existsById(memberId)) {
            throw new NotFoundException(ErrorCode.MEMBER_NOT_FOUND);
        }

        FcmToken token = fcmTokenRepository.findByMemberIdAndDeviceType(memberId, req.getDeviceType())
                .map(existingToken -> {
                    existingToken.updateToken(req.getFcmToken());
                    return existingToken;
                })
                .orElseGet(() -> FcmToken.builder()
                        .memberId(memberId)
                        .fcmToken(req.getFcmToken())
                        .deviceType(req.getDeviceType())
                        .build());

        fcmTokenRepository.save(token);
        return FcmTokenResponse.of(memberId, req.getDeviceType());
    }

    public void sendSinglePush(Long memberId, String title, String body) {
        List<FcmToken> tokens = fcmTokenRepository.findByMemberId(memberId);

        tokens.forEach(token -> {
            Message message = Message.builder()
                    .setToken(token.getFcmToken())
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            try {
                firebaseMessaging.send(message);
                log.info("FCM push sent successfully to memberId={}, device={}", memberId, token.getDeviceType());
            } catch (Exception e) {
                log.warn("FCM push failed for token={}: {}", token.getFcmToken(), e.getMessage());
            }
        });
    }

}
