package com.onde.admin.application.notification;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.onde.admin.application.notification.dto.BroadcastRequest;
import com.onde.admin.application.notification.dto.BroadcastResponse;
import com.onde.core.entity.member.Member;
import com.onde.core.entity.notification.FcmToken;
import com.onde.core.repository.FcmTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AdminNotificationServiceTest {

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @InjectMocks
    private AdminNotificationService adminNotificationService;

    @Test
    @DisplayName("FCM 단체 공지 발송 시 토큰이 500개 이상(예: 1200개)이면 500개씩 3번 청크로 분할하여 전송한다.")
    void broadcastFcm_partitionSuccess() throws Exception {
        // given
        BroadcastRequest req = BroadcastRequest.builder()
                .title("점검 공지")
                .body("2시 점검입니다.")
                .targetAll(true)
                .build();

        // 1200개의 FcmToken 리스트 준비
        List<FcmToken> fcmTokens = new ArrayList<>();
        for (int i = 0; i < 1200; i++) {
            fcmTokens.add(FcmToken.builder()
                    .id((long) i)
                    .memberId(1L)
                    .fcmToken("token_" + i)
                    .build());
        }

        BatchResponse mockBatchResponse = Mockito.mock(BatchResponse.class);
        Mockito.when(mockBatchResponse.getSuccessCount()).thenReturn(500).thenReturn(500).thenReturn(200);

        Mockito.when(fcmTokenRepository.findAll()).thenReturn(fcmTokens);
        Mockito.when(firebaseMessaging.sendMulticast(Mockito.any())).thenReturn(mockBatchResponse);

        // when
        BroadcastResponse response = adminNotificationService.broadcastFcm(req);

        // then
        assertThat(response.getSentCount()).isEqualTo(1200); // 500 + 500 + 200 = 1200개 정상 전송 검증
        Mockito.verify(firebaseMessaging, Mockito.times(3)).sendMulticast(Mockito.any()); // 3번 분할 전송 검증
    }
}
