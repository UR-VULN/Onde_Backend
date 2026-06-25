package com.onde.api.application.accommodation;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.io.image.ImageDataFactory;
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
        try {
            if (req.getTemplate() != null && !req.getTemplate().isBlank()) {
                String tpl = req.getTemplate();
                
                // 1. 경로 탈출 문자 필터링
                if (tpl.contains("..") || tpl.contains("./") || tpl.startsWith("/")) {
                    throw new IllegalArgumentException("템플릿 경로에 허용되지 않는 문자가 포함되어 있습니다.");
                }
                
                // 2. 허용된 템플릿 화이트리스트 검증
                if (!tpl.equals("verification") && !tpl.equals("business")) {
                    throw new IllegalArgumentException("허용되지 않은 템플릿입니다.");
                }
                
                // 3. 정규화된 경로가 안전한 baseDir 내에 있는지 2중 검증 (예방적 차원)
                java.nio.file.Path baseDir = java.nio.file.Paths.get("/app/templates").toAbsolutePath().normalize();
                java.nio.file.Path targetPath = baseDir.resolve(tpl).normalize();
                if (!targetPath.startsWith(baseDir)) {
                    throw new IllegalArgumentException("허용되지 않은 템플릿 경로입니다.");
                }
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage().getBytes());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage().getBytes());
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            document.setMargins(45, 45, 45, 45);

            // GmarketSansBold.otf 폰트 로드하여 CJK(한국어) 지원 확보
            try (java.io.InputStream is = getClass().getResourceAsStream("/GmarketSansBold.otf")) {
                if (is != null) {
                    byte[] fontBytes = is.readAllBytes();
                    com.itextpdf.kernel.font.PdfFont font = com.itextpdf.kernel.font.PdfFontFactory.createFont(
                            fontBytes,
                            com.itextpdf.io.font.PdfEncodings.IDENTITY_H,
                            com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                    document.setFont(font);
                }
            } catch (Exception e) {
                // Fallback to default Helvetica if font fails to load
            }

            // 컬러 및 타이틀 정의
            DeviceRgb primaryColor;
            String reportTitle;
            String templateType = req.getTemplate() != null ? req.getTemplate().trim() : "verification";
            boolean isBusiness = "business".equalsIgnoreCase(templateType);

            if (isBusiness) {
                primaryColor = new DeviceRgb(31, 41, 55); // 비즈니스용 다크 그레이/블랙
                reportTitle = "ONDE CORPORATE RESERVATION REPORT";
            } else {
                primaryColor = new DeviceRgb(59, 130, 246); // 기본 확인서용 온데 블루
                reportTitle = "INTEGRATED RESERVATION REPORT";
            }

            // 1. 헤더 영역 (회사 로고 및 제목)
            try {
                byte[] logoBytes = null;
                try (java.io.InputStream is = getClass().getResourceAsStream("/logo.png")) {
                    if (is != null) {
                        logoBytes = is.readAllBytes();
                    }
                }

                Image logoImg;
                if (logoBytes != null) {
                    logoImg = new Image(com.itextpdf.io.image.ImageDataFactory.create(logoBytes));
                } else {
                    logoImg = new Image(com.itextpdf.io.image.ImageDataFactory.create("https://onde.click/assets/logo.png"));
                }
                logoImg.setWidth(75f);
                logoImg.setMarginBottom(8f);
                document.add(logoImg);
            } catch (Exception e) {
                document.add(new Paragraph("ONDE Travel")
                        .setFontSize(12f)
                        .setBold()
                        .setFontColor(primaryColor));
            }

            document.add(new Paragraph(reportTitle)
                    .setFontSize(22f)
                    .setBold()
                    .setFontColor(new DeviceRgb(31, 41, 55))
                    .setMarginBottom(15f));

            // 사용자 정보 가져오기
            Long memberId = req.getMemberId() != null ? req.getMemberId() : 1L;
            String customerEmail = "N/A";
            try {
                MemberInfoResponse memberInfo = memberMyPageService.getMyInfo(memberId);
                if (memberInfo != null) {
                    customerEmail = memberInfo.getEmail() != null ? memberInfo.getEmail() : "N/A";
                }
            } catch (Exception e) {
                // Ignore
            }

            // 메타데이터 테이블 (발행일, 이메일, 그리고 비즈니스용인 경우 발급 번호 출력)
            Table metaTable;
            if (isBusiness) {
                metaTable = new Table(new float[] { 1.2f, 2f, 1.2f, 2f })
                        .useAllAvailableWidth()
                        .setMarginBottom(20f);

                // 발급 번호 자동 생성 (예: ONDE-CORP-20260615-XXXX)
                String formattedDate = java.time.LocalDate.now().toString().replace("-", "");
                int randomNum = (int) (Math.random() * 9000) + 1000;
                String issueNo = "ONDE-CORP-" + formattedDate + "-" + randomNum;

                addMetaCell(metaTable, "Report Date", true);
                addMetaCell(metaTable, java.time.LocalDate.now().toString(), false);
                addMetaCell(metaTable, "Issue No", true);
                addMetaCell(metaTable, issueNo, false);

                addMetaCell(metaTable, "Email", true);
                addMetaCell(metaTable, customerEmail, false);
                addMetaCell(metaTable, "Issued By", true);
                addMetaCell(metaTable, "ONDE Billing System", false);
            } else {
                metaTable = new Table(new float[] { 1f, 2f, 1f, 2f })
                        .useAllAvailableWidth()
                        .setMarginBottom(20f);

                addMetaCell(metaTable, "Report Date", true);
                addMetaCell(metaTable, java.time.LocalDate.now().toString(), false);
                addMetaCell(metaTable, "Email", true);
                addMetaCell(metaTable, customerEmail, false);
            }

            document.add(metaTable);

            // 2. 예약 상세 내역 표
            Table detailsTable;
            if (isBusiness) {
                detailsTable = new Table(new float[] { 2f, 4f, 3f, 2f })
                        .useAllAvailableWidth()
                        .setMarginBottom(20f);
                addHeaderCell(detailsTable, "Category", primaryColor);
                addHeaderCell(detailsTable, "Name", primaryColor);
                addHeaderCell(detailsTable, "Period / Schedule", primaryColor);
                addHeaderCell(detailsTable, "Price", primaryColor);
            } else {
                detailsTable = new Table(new float[] { 2f, 5f, 4f })
                        .useAllAvailableWidth()
                        .setMarginBottom(20f);
                addHeaderCell(detailsTable, "Category", primaryColor);
                addHeaderCell(detailsTable, "Name", primaryColor);
                addHeaderCell(detailsTable, "Period / Schedule", primaryColor);
            }

            double totalPriceSum = 0.0;
            boolean hasData = false;

            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 100);

            // 데이터 조회 및 추가
            try {
                MyPageListResponse<MyPageFlightBookingResponse> flights = memberMyPageService
                        .getMyFlightBookings(memberId, null, pageable);
                MyPageListResponse<MyPageRoomReservationResponse> rooms = memberMyPageService
                        .getMyRoomReservations(memberId, null, pageable);
                MyPageListResponse<MyPageCarReservationResponse> cars = memberMyPageService
                        .getMyCarReservations(memberId, null, pageable);
                MyPageListResponse<MyPageInsurancePolicyResponse> insurances = memberMyPageService
                        .getMyInsurancePolicies(memberId, null, pageable);

                // 항공
                if (flights != null && flights.getContent() != null && !flights.getContent().isEmpty()) {
                    hasData = true;
                    java.util.List<MyPageFlightBookingResponse> flightList = flights.getContent();
                    for (MyPageFlightBookingResponse f : flightList) {
                        String bookingCode = f.getBookingCode() != null ? f.getBookingCode() : "N/A";
                        String origin = f.getOrigin() != null ? f.getOrigin() : "N/A";
                        String destination = f.getDestination() != null ? f.getDestination() : "N/A";
                        String depTime = f.getDepartureTime() != null ? f.getDepartureTime() : "N/A";
                        double price = f.getTotalPrice() != null ? f.getTotalPrice().doubleValue() : 0.0;
                        totalPriceSum += price;

                        // 왕복 매칭 체크
                        String flightPeriod = depTime;
                        MyPageFlightBookingResponse returnFlight = null;
                        for (MyPageFlightBookingResponse other : flightList) {
                            if (!f.getBookingId().equals(other.getBookingId()) &&
                                    ((origin.equalsIgnoreCase(other.getDestination())
                                            && destination.equalsIgnoreCase(other.getOrigin())) ||
                                            (origin.equalsIgnoreCase(other.getOrigin())
                                                    && destination.equalsIgnoreCase(other.getDestination())))) {
                                returnFlight = other;
                                break;
                            }
                        }
                        if (returnFlight != null) {
                            String otherDep = returnFlight.getDepartureTime() != null ? returnFlight.getDepartureTime()
                                    : "N/A";
                            if (!"N/A".equals(otherDep)) {
                                if (depTime.compareTo(otherDep) <= 0) {
                                    flightPeriod = depTime + " ~ " + otherDep;
                                } else {
                                    flightPeriod = otherDep + " ~ " + depTime;
                                }
                            }
                        }

                        addBodyCell(detailsTable, "Flight", false);
                        addBodyCell(detailsTable,
                                String.format("Ticket: %s (%s -> %s)", bookingCode, origin, destination), false);
                        addBodyCell(detailsTable, flightPeriod, false);
                        if (isBusiness) {
                            addBodyCell(detailsTable, String.format("%,.0f KRW", price), true);
                        }
                    }
                }

                // 숙소
                if (rooms != null && rooms.getContent() != null && !rooms.getContent().isEmpty()) {
                    hasData = true;
                    for (MyPageRoomReservationResponse r : rooms.getContent()) {
                        String accName = r.getAccommodationName() != null ? r.getAccommodationName() : "N/A";
                        String checkIn = r.getCheckIn() != null ? r.getCheckIn() : "N/A";
                        String checkOut = r.getCheckOut() != null ? r.getCheckOut() : "N/A";
                        double price = r.getTotalPrice() != null ? r.getTotalPrice().doubleValue() : 0.0;
                        totalPriceSum += price;

                        addBodyCell(detailsTable, "Stay", false);
                        addBodyCell(detailsTable, accName, false);
                        addBodyCell(detailsTable, String.format("%s ~ %s", checkIn, checkOut), false);
                        if (isBusiness) {
                            addBodyCell(detailsTable, String.format("%,.0f KRW", price), true);
                        }
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

                        String carDetails;
                        if (carType == null || carType.isBlank() || "N/A".equalsIgnoreCase(carType)) {
                            carDetails = modelName;
                        } else {
                            carDetails = String.format("%s (%s)", modelName, carType);
                        }

                        addBodyCell(detailsTable, "Rental Car", false);
                        addBodyCell(detailsTable, carDetails, false);
                        addBodyCell(detailsTable, String.format("%s ~ %s", checkIn, checkOut), false);
                        if (isBusiness) {
                            addBodyCell(detailsTable, String.format("%,.0f KRW", price), true);
                        }
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
                        addBodyCell(detailsTable, prodName, false);
                        addBodyCell(detailsTable, String.format("%s ~ %s", startDate, endDate), false);
                        if (isBusiness) {
                            addBodyCell(detailsTable, String.format("%,.0f KRW", price), true);
                        }
                    }
                }

            } catch (Exception e) {
                int colSpan = isBusiness ? 4 : 3;
                Cell errCell = new Cell(1, colSpan)
                        .add(new Paragraph("Failed to fetch reservation database details: " + e.toString())
                                .setFontColor(ColorConstants.RED));
                detailsTable.addCell(errCell);
                e.printStackTrace();
            }

            if (!hasData) {
                int colSpan = isBusiness ? 4 : 3;
                Cell emptyCell = new Cell(1, colSpan).add(new Paragraph("No active reservations found for this member.")
                        .setTextAlignment(TextAlignment.CENTER));
                detailsTable.addCell(emptyCell);
            }

            document.add(detailsTable);

            // 3. 결제 총액 요약 (비즈니스용 양식에만 노출)
            if (isBusiness) {
                Table totalTable = new Table(new float[] { 3f, 1f })
                        .useAllAvailableWidth()
                        .setMarginBottom(30f);

                Cell labelCell = new Cell().add(new Paragraph("Total Reservation Summary").setBold().setFontSize(11f))
                        .setBorder(Border.NO_BORDER)
                        .setTextAlignment(TextAlignment.RIGHT);
                Cell valueCell = new Cell()
                        .add(new Paragraph(String.format("%,.0f KRW", totalPriceSum)).setBold().setFontSize(13f)
                                .setFontColor(primaryColor))
                        .setBorder(Border.NO_BORDER)
                        .setTextAlignment(TextAlignment.RIGHT);
                totalTable.addCell(labelCell);
                totalTable.addCell(valueCell);

                document.add(totalTable);
            }

            // 하단 문구
            document.add(new Paragraph("Thank you for choosing ONDE. We wish you a safe and pleasant journey.")
                    .setFontSize(9f)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(30f));

            // 4. 취약점 시나리오 (LFI) 트리거 결과 제거 (보안 패치 완료)
            // 기존의 LFI 취약점을 통한 샌드박스 파일 접근 로직을 제거했습니다.

            document.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "onde_settlement_report.pdf");
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


}

class IntegratedReportRequest {
    private Long memberId;
    private String template;

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }


}
