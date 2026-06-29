package com.onde.api.application.notification;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    /**
     * Thymeleaf HTML 템플릿을 비동기로 렌더링하여 이메일을 발송합니다.
     */
    @Async("imageUploadExecutor")
    public void sendHtmlEmailAsync(String to, String subject, String customerName, Long reservationId, String productName, int amount, int mileageUsed) {
        log.info("Starting async HTML email sending task in thread={}. Recipient: {}", Thread.currentThread().getName(), to);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom("no-reply@onde.com");
            helper.setTo(to);
            helper.setSubject(subject);
            
            Context context = new Context();
            context.setVariable("customerName", customerName);
            context.setVariable("reservationId", "RES-" + reservationId);
            context.setVariable("productName", productName);
            context.setVariable("customerEmail", to);
            context.setVariable("amount", amount);
            context.setVariable("mileageUsed", mileageUsed);
            context.setVariable("totalPaid", Math.max(0, amount - mileageUsed));
            
            String htmlContent = templateEngine.process("email_template", context);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Async HTML email sent successfully to={}", to);
        } catch (Exception e) {
            log.error("Failed to send async HTML email to={}. Error: {}", to, e.getMessage());
        }
    }
}
