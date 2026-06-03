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
import com.onde.api.config.AwsS3Service;
import com.onde.api.application.notification.dto.PresignedUrlResponse;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.kernel.colors.ColorConstants;

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
    private final com.onde.core.repository.PaymentRepository paymentRepository;
    private final AwsS3Service awsS3Service;

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

    public byte[] generatePdfReceipt(Long reservationId, Long memberId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.RESERVATION_NOT_FOUND));

        if (!reservation.getUserId().equals(memberId)) {
            throw new ForbiddenException(ErrorCode.RESERVATION_NOT_OWNER);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(50, 50, 50, 50);

            // 1. Title Section
            Paragraph title = new Paragraph("RECEIPT")
                    .setBold()
                    .setFontSize(24)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(30f);
            document.add(title);

            // 2. Info Table
            Table infoTable = new Table(2)
                    .useAllAvailableWidth()
                    .setMarginBottom(20f);

            addTableCell(infoTable, "Receipt No:", true);
            addTableCell(infoTable, "REC-" + reservation.getId() + "-" + System.currentTimeMillis() % 100000, false);

            addTableCell(infoTable, "Date:", true);
            addTableCell(infoTable, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), false);

            addTableCell(infoTable, "Customer Email:", true);
            addTableCell(infoTable, member.getEmail(), false);

            document.add(infoTable);

            // Line Separator
            Paragraph line = new Paragraph("----------------------------------------------------------------------------------------------------------------")
                    .setMarginBottom(20f);
            document.add(line);

            // 3. Itemized Details Table
            Table detailsTable = new Table(new float[]{3f, 1f})
                    .useAllAvailableWidth()
                    .setMarginBottom(30f);

            // Table Headers
            addTableHeaderCell(detailsTable, "Product Description");
            addTableHeaderCell(detailsTable, "Amount");

            // 결제 및 요금 정보 안전 바인딩
            com.onde.core.entity.payment.Payment payment = paymentRepository.findFirstByReservationIdOrderByIdDesc(reservationId).orElse(null);
            BigDecimal totalAmount = payment != null ? payment.getTotalAmount() : (reservation.getTotalPrice() != null ? reservation.getTotalPrice() : BigDecimal.ZERO);
            Integer usedMileage = payment != null ? payment.getUsedMileage() : 0;
            BigDecimal finalPaid = totalAmount.subtract(BigDecimal.valueOf(usedMileage));

            // Item Row
            String productDesc = reservation.getTargetType() != null 
                    ? String.format("ONDE %s Reservation (ID: %d)", reservation.getTargetType().name(), reservation.getTargetId())
                    : String.format("ONDE Reservation (ID: %d)", reservation.getId());
            addTableCell(detailsTable, productDesc, false);
            addTableCell(detailsTable, String.format("%,.0f KRW", totalAmount), false);

            // Mileage Row
            addTableCell(detailsTable, "Used Mileage Discount", false);
            addTableCell(detailsTable, String.format("-%,d KRW", usedMileage), false);

            // Total Row
            addTableCell(detailsTable, "Total Paid Amount", true);
            addTableCell(detailsTable, String.format("%,.0f KRW", finalPaid.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : finalPaid), true);

            document.add(detailsTable);

            // Thank You Message
            Paragraph footer = new Paragraph("Thank you for traveling with ONDE! Wish you a pleasant journey.")
                    .setItalic()
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(footer);

            document.close();
        } catch (Exception e) {
            log.error("PDF generation failed for reservationId={}", reservationId, e);
            e.printStackTrace(); // 콘솔창에 디버그 스택 트레이스 강제 출력
            throw new RuntimeException("영수증 PDF 생성 실패: " + e.toString(), e);
        }

        return baos.toByteArray();
    }

    public byte[] generatePdfTicket(Long reservationId, Long memberId) {
        return generatePdfReceipt(reservationId, memberId);
    }

    private void addTableCell(Table table, String text, boolean isBold) {
        Paragraph p = new Paragraph(text).setFontSize(10);
        if (isBold) {
            p.setBold();
        }
        Cell cell = new Cell().add(p)
                .setBorder(Border.NO_BORDER)
                .setPadding(6);
        table.addCell(cell);
    }

    private void addTableHeaderCell(Table table, String text) {
        Paragraph p = new Paragraph(text).setBold().setFontSize(11);
        Cell cell = new Cell().add(p)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 1f))
                .setPadding(8);
        table.addCell(cell);
    }

    public PresignedUrlResponse getReceiptPresignedUrl(Long reservationId, Long memberId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.RESERVATION_NOT_FOUND));

        if (!reservation.getUserId().equals(memberId)) {
            throw new ForbiddenException(ErrorCode.RESERVATION_NOT_OWNER);
        }

        String filename = "receipt_reservation_" + reservationId + ".pdf";
        String downloadUrl = awsS3Service.generatePresignedUrl("receipts", filename);

        return PresignedUrlResponse.builder()
                .reservationId(reservationId)
                .presignedUrl(downloadUrl)
                .fileUrl(stripQueryString(downloadUrl))
                .build();
    }

    public PresignedUrlResponse getTicketPresignedUrl(Long reservationId, Long memberId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.RESERVATION_NOT_FOUND));

        if (!reservation.getUserId().equals(memberId)) {
            throw new ForbiddenException(ErrorCode.RESERVATION_NOT_OWNER);
        }

        String filename = "ticket_reservation_" + reservationId + ".pdf";
        String downloadUrl = awsS3Service.generatePresignedUrl("tickets", filename);

        return PresignedUrlResponse.builder()
                .reservationId(reservationId)
                .presignedUrl(downloadUrl)
                .fileUrl(stripQueryString(downloadUrl))
                .build();
    }

    private String stripQueryString(String url) {
        int queryIndex = url.indexOf('?');
        return queryIndex >= 0 ? url.substring(0, queryIndex) : url;
    }
}
