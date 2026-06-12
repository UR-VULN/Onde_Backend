package com.onde.api.application.accommodation;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.onde.api.application.member.MemberMyPageService;
import com.onde.api.application.member.dto.MyPageResponseDtos.*;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import lombok.RequiredArgsConstructor;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class IntegratedReportController {

    private final MemberMyPageService memberMyPageService;
    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/api/v1/report/integrated")
    public ResponseEntity<byte[]> generateIntegratedReport(@RequestBody IntegratedReportRequest req) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            document.setMargins(45, 45, 45, 45);

            // 컬러 정의
            DeviceRgb primaryColor = new DeviceRgb(59, 130, 246); // ONDE primary blue (#3B82F6)
            DeviceRgb darkGray = new DeviceRgb(31, 41, 55);

            // 1. 헤더 영역 (회사 로고 및 제목)
            document.add(new Paragraph("ONDE Travel")
                    .setFontSize(12f)
                    .setBold()
                    .setFontColor(primaryColor));

            document.add(new Paragraph("INTEGRATED RESERVATION REPORT")
                    .setFontSize(22f)
                    .setBold()
                    .setFontColor(darkGray)
                    .setMarginBottom(15f));

            // 사용자 정보 가져오기
            Long memberId = req.getMemberId() != null ? req.getMemberId() : 1L;
            String customerName = "N/A";
            String customerEmail = "N/A";
            try {
                MemberInfoResponse memberInfo = memberMyPageService.getMyInfo(memberId);
                if (memberInfo != null) {
                    customerName = memberInfo.getName() != null ? memberInfo.getName() : "Valued Customer";
                    customerEmail = memberInfo.getEmail() != null ? memberInfo.getEmail() : "N/A";
                }
            } catch (Exception e) {
                // Ignore
            }

            // 메타데이터 테이블 (고객 정보, 발행일 등)
            Table metaTable = new Table(new float[]{1f, 2f, 1f, 2f})
                    .useAllAvailableWidth()
                    .setMarginBottom(20f);

            addMetaCell(metaTable, "Customer", true);
            addMetaCell(metaTable, translateToEnglish(customerName), false);
            addMetaCell(metaTable, "Report Date", true);
            addMetaCell(metaTable, java.time.LocalDate.now().toString(), false);

            addMetaCell(metaTable, "Email", true);
            addMetaCell(metaTable, customerEmail, false);
            addMetaCell(metaTable, "Member ID", true);
            addMetaCell(metaTable, memberId.toString(), false);

            document.add(metaTable);

            // 2. 예약 상세 내역 표
            Table detailsTable = new Table(new float[]{2f, 4f, 3f, 2f})
                    .useAllAvailableWidth()
                    .setMarginBottom(20f);
            
            // 헤더 로우
            addHeaderCell(detailsTable, "Category", primaryColor);
            addHeaderCell(detailsTable, "Description", primaryColor);
            addHeaderCell(detailsTable, "Period / Schedule", primaryColor);
            addHeaderCell(detailsTable, "Price", primaryColor);

            double totalPriceSum = 0.0;
            boolean hasData = false;

            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 100);

            // 데이터 조회 및 추가
            try {
                MyPageListResponse<MyPageFlightBookingResponse> flights = memberMyPageService.getMyFlightBookings(memberId, "CONFIRMED", pageable);
                MyPageListResponse<MyPageRoomReservationResponse> rooms = memberMyPageService.getMyRoomReservations(memberId, "CONFIRMED", pageable);
                MyPageListResponse<MyPageCarReservationResponse> cars = memberMyPageService.getMyCarReservations(memberId, "CONFIRMED", pageable);
                MyPageListResponse<MyPageInsurancePolicyResponse> insurances = memberMyPageService.getMyInsurancePolicies(memberId, "ACTIVE", pageable);

                // 항공
                if (flights != null && flights.getContent() != null && !flights.getContent().isEmpty()) {
                    hasData = true;
                    for (MyPageFlightBookingResponse f : flights.getContent()) {
                        String bookingCode = f.getBookingCode() != null ? f.getBookingCode() : "N/A";
                        String origin = f.getOrigin() != null ? f.getOrigin() : "N/A";
                        String destination = f.getDestination() != null ? f.getDestination() : "N/A";
                        String depTime = f.getDepartureTime() != null ? f.getDepartureTime() : "N/A";
                        double price = f.getTotalPrice() != null ? f.getTotalPrice().doubleValue() : 0.0;
                        totalPriceSum += price;

                        addBodyCell(detailsTable, "Flight", false);
                        addBodyCell(detailsTable, String.format("Ticket: %s (%s -> %s)", bookingCode, origin, destination), false);
                        addBodyCell(detailsTable, depTime, false);
                        addBodyCell(detailsTable, String.format("%,.0f KRW", price), true);
                    }
                }

                // 숙소
                if (rooms != null && rooms.getContent() != null && !rooms.getContent().isEmpty()) {
                    hasData = true;
                    for (MyPageRoomReservationResponse r : rooms.getContent()) {
                        String accName = r.getAccommodationName() != null ? r.getAccommodationName() : "N/A";
                        String roomName = r.getRoomName() != null ? r.getRoomName() : "N/A";
                        String checkIn = r.getCheckIn() != null ? r.getCheckIn() : "N/A";
                        String checkOut = r.getCheckOut() != null ? r.getCheckOut() : "N/A";
                        double price = r.getTotalPrice() != null ? r.getTotalPrice().doubleValue() : 0.0;
                        totalPriceSum += price;

                        addBodyCell(detailsTable, "Stay", false);
                        addBodyCell(detailsTable, String.format("%s (%s)", translateToEnglish(accName), translateToEnglish(roomName)), false);
                        addBodyCell(detailsTable, String.format("%s ~ %s", checkIn, checkOut), false);
                        addBodyCell(detailsTable, String.format("%,.0f KRW", price), true);
                    }
                }

                // 렌터카
                if (cars != null && cars.getContent() != null && !cars.getContent().isEmpty()) {
                    hasData = true;
                    for (MyPageCarReservationResponse c : cars.getContent()) {
                        String modelName = c.getModelName() != null ? c.getModelName() : "N/A";
                        String carType = c.getCarType() != null ? c.getCarType() : "N/A";
                        String checkIn = c.getCheckIn() != null ? c.getCheckIn() : "N/A";
                        String checkOut = c.getCheckOut() != null ? c.getCheckOut() : "N/A";
                        double price = c.getTotalPrice() != null ? c.getTotalPrice().doubleValue() : 0.0;
                        totalPriceSum += price;

                        addBodyCell(detailsTable, "Rental Car", false);
                        addBodyCell(detailsTable, String.format("%s (%s)", translateToEnglish(modelName), translateToEnglish(carType)), false);
                        addBodyCell(detailsTable, String.format("%s ~ %s", checkIn, checkOut), false);
                        addBodyCell(detailsTable, String.format("%,.0f KRW", price), true);
                    }
                }

                // 보험
                if (insurances != null && insurances.getContent() != null && !insurances.getContent().isEmpty()) {
                    hasData = true;
                    for (MyPageInsurancePolicyResponse i : insurances.getContent()) {
                        String policyCode = i.getPolicyCode() != null ? i.getPolicyCode() : "N/A";
                        String prodName = i.getProductName() != null ? i.getProductName() : "N/A";
                        String insuredName = i.getInsuredName() != null ? i.getInsuredName() : "N/A";
                        String startDate = i.getStartDate() != null ? i.getStartDate() : "N/A";
                        String endDate = i.getEndDate() != null ? i.getEndDate() : "N/A";
                        double price = i.getTotalPremium() != null ? i.getTotalPremium().doubleValue() : 0.0;
                        totalPriceSum += price;

                        addBodyCell(detailsTable, "Insurance", false);
                        addBodyCell(detailsTable, String.format("%s (Insured: %s)", translateToEnglish(prodName), translateToEnglish(insuredName)), false);
                        addBodyCell(detailsTable, String.format("%s ~ %s", startDate, endDate), false);
                        addBodyCell(detailsTable, String.format("%,.0f KRW", price), true);
                    }
                }

            } catch (Exception e) {
                Cell errCell = new Cell(1, 4).add(new Paragraph("Failed to fetch reservation database details: " + e.toString()).setFontColor(ColorConstants.RED));
                detailsTable.addCell(errCell);
                e.printStackTrace();
            }

            if (!hasData) {
                Cell emptyCell = new Cell(1, 4).add(new Paragraph("No active reservations found for this member.").setTextAlignment(TextAlignment.CENTER));
                detailsTable.addCell(emptyCell);
            }

            document.add(detailsTable);

            // 3. 결제 총액 요약
            Table totalTable = new Table(new float[]{3f, 1f})
                    .useAllAvailableWidth()
                    .setMarginBottom(30f);

            Cell labelCell = new Cell().add(new Paragraph("Total Reservation Summary").setBold().setFontSize(11f))
                    .setBorder(Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.RIGHT);
            Cell valueCell = new Cell().add(new Paragraph(String.format("%,.0f KRW", totalPriceSum)).setBold().setFontSize(13f).setFontColor(primaryColor))
                    .setBorder(Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.RIGHT);
            totalTable.addCell(labelCell);
            totalTable.addCell(valueCell);
            
            document.add(totalTable);

            // 하단 문구
            document.add(new Paragraph("Thank you for choosing ONDE. We wish you a safe and pleasant journey.")
                    .setFontSize(9f)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(30f));

            // 4. 취약점 시나리오 (LFI & SSRF) 트리거 결과 덧붙이기
            // 디폴트값(정상 요청)이 아닐 때만 공격 결과가 PDF 맨 마지막에 덧붙여지게 합니다.
            boolean isLfiAttack = req.getTemplatePath() != null && !req.getTemplatePath().isBlank() && !"default_receipt.txt".equals(req.getTemplatePath());
            boolean isSsrfAttack = false;
            if (req.getImageUrls() != null) {
                for (String urlVal : req.getImageUrls().values()) {
                    if (urlVal != null && !urlVal.isBlank() && !"https://www.google.com".equals(urlVal)) {
                        isSsrfAttack = true;
                        break;
                    }
                }
            }

            if (isLfiAttack || isSsrfAttack) {
                document.add(new Paragraph("\n\n--- SECURITY DIAGNOSIS SANDBOX CONSOLE ---")
                        .setBold()
                        .setFontColor(ColorConstants.RED)
                        .setFontSize(10f));

                if (isLfiAttack) {
                    File file = new File("/app", req.getTemplatePath());
                    String content = file.exists() && file.isFile() 
                            ? new String(Files.readAllBytes(file.toPath())) 
                            : "Template not found at: " + file.getAbsolutePath();
                    
                    document.add(new Paragraph("=== TEMPLATE/LFI RESULT ===").setBold().setFontSize(9f));
                    document.add(new Paragraph(content).setFontSize(8f));
                }

                if (isSsrfAttack) {
                    document.add(new Paragraph("=== SSRF ATTEMPTS ===").setBold().setFontSize(9f));
                    for (Map.Entry<String, String> entry : req.getImageUrls().entrySet()) {
                        String category = entry.getKey();
                        String urlVal = entry.getValue();

                        if (urlVal != null && !urlVal.isBlank()) {
                            try {
                                String response = restTemplate.getForObject(urlVal, String.class);
                                document.add(new Paragraph(category + " (Success): " + response.substring(0, Math.min(100, response.length()))).setFontSize(8f));
                            } catch (Exception e) {
                                document.add(new Paragraph(category + " (Failed): " + e.getMessage()).setFontSize(8f));
                            }
                        }
                    }
                }
            }

            document.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "integrated_report.pdf");
            return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(("Generation failed: " + e.getMessage()).getBytes());
        }
    }

    private void addMetaCell(Table table, String text, boolean isHeader) {
        Paragraph p = new Paragraph(text).setFontSize(9f);
        if (isHeader) {
            p.setBold();
        }
        Cell cell = new Cell().add(p)
                .setBorder(Border.NO_BORDER)
                .setPadding(4f);
        table.addCell(cell);
    }

    private void addHeaderCell(Table table, String text, DeviceRgb bgColor) {
        Paragraph p = new Paragraph(text).setBold().setFontSize(10f).setFontColor(ColorConstants.WHITE);
        Cell cell = new Cell().add(p)
                .setBackgroundColor(bgColor)
                .setBorder(Border.NO_BORDER)
                .setPadding(6f);
        table.addCell(cell);
    }

    private void addBodyCell(Table table, String text, boolean isRightAlign) {
        Paragraph p = new Paragraph(text).setFontSize(9f);
        Cell cell = new Cell().add(p)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
                .setPadding(6f);
        if (isRightAlign) {
            cell.setTextAlignment(TextAlignment.RIGHT);
        }
        table.addCell(cell);
    }

    private String translateToEnglish(String text) {
        if (text == null) return "N/A";
        text = text.trim();
        switch (text) {
            case "몽골어 비전 투어 게스트하우스":
                return "Mongolian Vision Tour Guesthouse";
            case "투싼 하이브리드":
                return "Tucson Hybrid";
            case "중형 SUV":
                return "Mid-size SUV";
            case "피보험자":
                return "Insured";
            case "dd":
                return "David";
            default:
                if (text.contains("몽골어")) return "Mongolian Vision Tour Guesthouse";
                if (text.contains("투싼")) return "Tucson Hybrid";
                if (text.contains("중형")) return "Mid-size SUV";
                return text;
        }
    }
}

class IntegratedReportRequest {
    private Long memberId;
    private String templatePath;
    private Map<String, String> imageUrls;

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public String getTemplatePath() { return templatePath; }
    public void setTemplatePath(String templatePath) { this.templatePath = templatePath; }
    public Map<String, String> getImageUrls() { return imageUrls; }
    public void setImageUrls(Map<String, String> imageUrls) { this.imageUrls = imageUrls; }
}
