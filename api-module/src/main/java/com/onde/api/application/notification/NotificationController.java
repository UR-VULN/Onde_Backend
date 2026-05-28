package com.onde.api.application.notification;

import com.onde.api.application.notification.dto.FcmTokenRequest;
import com.onde.api.application.notification.dto.FcmTokenResponse;
import com.onde.api.security.LoginMember;
import com.onde.core.support.ApiResponse;
import com.onde.api.application.notification.dto.PresignedUrlResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/notifications/fcm_token")
    public ResponseEntity<ApiResponse<FcmTokenResponse>> saveFcmToken(
            @Valid @RequestBody FcmTokenRequest req,
            @LoginMember Long memberId) {

        FcmTokenResponse response = notificationService.saveFcmToken(req, memberId);
        return ResponseEntity.ok(ApiResponse.success(response, "FCM 토큰이 저장되었습니다."));
    }

    @GetMapping("/reservations/{reservationId}/receipt")
    public ResponseEntity<byte[]> downloadReceipt(
            @PathVariable("reservationId") Long reservationId,
            @LoginMember Long memberId) {

        byte[] pdfBytes = notificationService.generatePdfReceipt(reservationId, memberId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "receipt_reservation_" + reservationId + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/reservations/{reservationId}/receipt/presigned")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getReceiptPresignedUrl(
            @PathVariable("reservationId") Long reservationId,
            @LoginMember Long memberId) {

        PresignedUrlResponse response = notificationService.getReceiptPresignedUrl(reservationId, memberId);
        return ResponseEntity.ok(ApiResponse.success(response, "영수증 PDF 다운로드 Presigned URL 발급이 완료되었습니다."));
    }

    @GetMapping("/tickets/{reservationId}/download")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getTicketPresignedUrlFromDownload(
            @PathVariable("reservationId") Long reservationId,
            @LoginMember Long memberId) {

        PresignedUrlResponse response = notificationService.getTicketPresignedUrl(reservationId, memberId);
        return ResponseEntity.ok(ApiResponse.success(response, "E-ticket PDF 다운로드 Presigned URL 발급이 완료되었습니다."));
    }

    @GetMapping("/reservations/{reservationId}/ticket/presigned")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getTicketPresignedUrl(
            @PathVariable("reservationId") Long reservationId,
            @LoginMember Long memberId) {

        PresignedUrlResponse response = notificationService.getTicketPresignedUrl(reservationId, memberId);
        return ResponseEntity.ok(ApiResponse.success(response, "E-ticket PDF 다운로드 Presigned URL 발급이 완료되었습니다."));
    }
}
