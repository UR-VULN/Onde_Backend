package com.onde.admin.application.notification;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.onde.admin.application.notification.dto.BroadcastRequest;
import com.onde.admin.application.notification.dto.BroadcastResponse;
import com.onde.core.entity.notification.FcmToken;
import com.onde.core.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminNotificationService {

    private final FcmTokenRepository fcmTokenRepository;
    private final FirebaseMessaging firebaseMessaging;

    public BroadcastResponse broadcastFcm(BroadcastRequest req) {
        List<String> tokenList;

        if (req.isTargetAll()) {
            tokenList = fcmTokenRepository.findAll().stream()
                    .map(FcmToken::getFcmToken)
                    .toList();
        } else {
            if (req.getTargetRoles() == null || req.getTargetRoles().isEmpty()) {
                return BroadcastResponse.builder()
                        .sentCount(0)
                        .sentAt(LocalDateTime.now())
                        .build();
            }
            tokenList = fcmTokenRepository.findByMemberRoleIn(req.getTargetRoles()).stream()
                    .map(FcmToken::getFcmToken)
                    .toList();
        }

        if (tokenList.isEmpty()) {
            return BroadcastResponse.builder()
                    .sentCount(0)
                    .sentAt(LocalDateTime.now())
                    .build();
        }

        int sentCount = 0;
        // FCM Multicast는 1회 발송 시 최대 500개 제한이 있으므로 청크 분할 발송 필수
        List<List<String>> chunks = partition(tokenList, 500);

        for (List<String> chunk : chunks) {
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(chunk)
                    .setNotification(Notification.builder()
                            .setTitle(req.getTitle())
                            .setBody(req.getBody())
                            .build())
                    .build();

            try {
                BatchResponse response = firebaseMessaging.sendMulticast(message);
                int successCount = response.getSuccessCount();
                sentCount += successCount;
                log.info("Multicast FCM sent: successCount={}, failureCount={}", successCount, response.getFailureCount());
            } catch (Exception e) {
                log.error("FCM Multicast delivery error in admin broadcast", e);
            }
        }

        return BroadcastResponse.builder()
                .sentCount(sentCount)
                .sentAt(LocalDateTime.now())
                .build();
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }
}
