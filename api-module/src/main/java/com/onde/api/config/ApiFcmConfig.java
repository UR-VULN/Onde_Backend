package com.onde.api.config;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiFcmConfig {

    @Bean
    public FirebaseMessaging firebaseMessaging() throws FirebaseMessagingException {
        FirebaseMessaging mock = Mockito.mock(FirebaseMessaging.class);

        // 단건 메시지 발송 모킹
        Mockito.when(mock.send(Mockito.any())).thenReturn("mock-msg-12345");

        // 멀티캐스트 메시지 발송 모킹
        BatchResponse mockResponse = Mockito.mock(BatchResponse.class);
        Mockito.when(mockResponse.getSuccessCount()).thenReturn(500);
        Mockito.when(mockResponse.getFailureCount()).thenReturn(0);
        Mockito.when(mock.sendMulticast(Mockito.any())).thenReturn(mockResponse);

        return mock;
    }
}
