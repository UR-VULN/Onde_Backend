package com.onde.api.application.payment;

import com.onde.api.application.payment.dto.request.*;
import com.onde.api.application.payment.dto.response.*;
import com.onde.api.security.LoginMember;
import com.onde.core.support.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 결제 관련 API 요청을 처리하는 컨트롤러 클래스입니다.
 * 결제 사전 등록/검증, 사후 검증 및 완료, 결제 취소(환불) 기능을 제공합니다.
 */
@Validated
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 결제 진행 전, 백엔드에 결제 요청 정보를 사전 등록하고 금액 정합성을 검증합니다.
     * 프론트엔드는 이 API의 응답으로 받은 merchantUid를 사용하여 결제창을 호출해야 합니다.
     *
     * @param userId 로그인한 회원의 식별자
     * @param body   결제 대상 예약 정보 및 마일리지 사용액 등이 포함된 요청 객체
     * @return 사전 검증 결과 및 merchantUid가 포함된 응답 객체
     */
    @PostMapping("/prepare")
    public ResponseEntity<ApiResponse<PaymentPrepareResponse>> preparePayment(
            @LoginMember Long userId,
            @Valid @RequestBody PaymentPrepareRequest body) {

        PaymentPrepareResponse result = paymentService.preparePayment(userId, body);
        return ResponseEntity.ok(ApiResponse.success(result, "결제 사전 검증 완료. merchantUid로 결제를 진행하세요."));
    }

    /**
     * 결제 완료 후, 실제 결제 금액이 사전에 약속된 금액과 일치하는지 최종 검증(사후 검증)을 수행하고
     * 결제 내역을 저장 및 유저 마일리지를 차감합니다.
     *
     * @param userId 로그인한 회원의 식별자
     * @param body   결제 식별값(impUid) 및 merchantUid가 포함된 요청 객체
     * @return 사후 검증 결과 및 결제 상태가 포함된 응답 객체
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<PaymentValidateResponse>> validatePayment(
            @LoginMember Long userId,
            @Valid @RequestBody PaymentValidateRequest body) {

        PaymentValidateResponse result = paymentService.validatePayment(userId, body);
        return ResponseEntity.ok(ApiResponse.success(result, "결제가 최종 승인되었습니다."));
    }

    /**
     * 이미 완료된 결제에 대하여 예약을 취소하고 환불(결제 취소 및 마일리지 롤백)을 수행합니다.
     *
     * @param userId    로그인한 회원의 식별자
     * @param paymentId 취소할 결제 내역의 식별자
     * @param body      취소 사유 등이 포함된 요청 객체
     * @return 환불(취소) 결과가 포함된 응답 객체
     */
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<ApiResponse<PaymentCancelResponse>> cancelPayment(
            @LoginMember Long userId,
            @PathVariable("paymentId") @Min(1) Long paymentId,
            @Valid @RequestBody PaymentCancelRequest body) {

        PaymentCancelResponse result = paymentService.cancelPayment(userId, paymentId, body);
        return ResponseEntity.ok(ApiResponse.success(result, "결제가 취소되었습니다."));
    }
}

