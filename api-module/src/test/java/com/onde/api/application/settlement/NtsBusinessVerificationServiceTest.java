package com.onde.api.application.settlement;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NtsBusinessVerificationServiceTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void verifyBusinessPostsValidatePayloadWithRepresentativeAndOpenDate() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>("");
        startServer(requestBody, """
                {"request_cnt":1,"status_code":"OK","data":[{"b_no":"1234567890","valid":"01","valid_msg":"확인되었습니다."}]}
                """);
        NtsBusinessVerificationService service = service();

        NtsBusinessVerificationService.BusinessVerificationResult result =
                service.verifyBusiness("123-45-67890", "홍길동", "20200101");

        assertTrue(result.verified());
        assertEquals("확인되었습니다.", result.message());
        assertTrue(requestBody.get().contains("\"businesses\""));
        assertTrue(requestBody.get().contains("\"b_no\":\"1234567890\""));
        assertTrue(requestBody.get().contains("\"start_dt\":\"20200101\""));
        assertTrue(requestBody.get().contains("\"p_nm\":\"홍길동\""));
    }

    @Test
    void verifyBusinessReturnsFalseWhenNtsSaysInvalid() throws Exception {
        startServer(new AtomicReference<>(), """
                {"request_cnt":1,"status_code":"OK","data":[{"b_no":"1234567890","valid":"02","valid_msg":"확인할 수 없습니다."}]}
                """);
        NtsBusinessVerificationService service = service();

        NtsBusinessVerificationService.BusinessVerificationResult result =
                service.verifyBusiness("1234567890", "가짜대표", "19990101");

        assertFalse(result.verified());
        assertEquals("확인할 수 없습니다.", result.message());
    }

    @Test
    void verifyBusinessReturnsFalseWithoutServiceKey() {
        NtsBusinessVerificationService service = new NtsBusinessVerificationService();
        ReflectionTestUtils.setField(service, "serviceKey", "");
        ReflectionTestUtils.setField(service, "apiUrl", "http://127.0.0.1/validate");

        NtsBusinessVerificationService.BusinessVerificationResult result =
                service.verifyBusiness("1234567890", "홍길동", "20200101");

        assertFalse(result.verified());
        assertEquals("사업자 진위확인 서비스 키가 설정되어 있지 않습니다.", result.message());
    }

    private NtsBusinessVerificationService service() {
        NtsBusinessVerificationService service = new NtsBusinessVerificationService();
        ReflectionTestUtils.setField(service, "serviceKey", "test-key");
        ReflectionTestUtils.setField(service, "apiUrl", "http://127.0.0.1:" + server.getAddress().getPort() + "/validate");
        return service;
    }

    private void startServer(AtomicReference<String> requestBody, String response) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/validate", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            writeJson(exchange, response);
        });
        server.start();
    }

    private void writeJson(HttpExchange exchange, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
