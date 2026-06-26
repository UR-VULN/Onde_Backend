package com.onde.api.application.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.onde.core.entity.notification.FcmToken;
import com.onde.core.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final FirebaseMessaging firebaseMessaging;
    private final FcmTokenRepository fcmTokenRepository;

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
