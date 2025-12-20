package com.techforge.erp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Email Service - Handles sending emails via SMTP (Gmail).
 * Used for OTP verification during password reset.
 */
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Send OTP email for password reset.
     * @param to Recipient email address
     * @param otp The 6-digit OTP code
     */
    public void sendOtpEmail(String to, String otp) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("TechForge ERP - Password Reset OTP");

        String htmlContent = buildOtpEmailTemplate(otp);
        helper.setText(htmlContent, true);

        mailSender.send(message);
        System.out.println("[EmailService] OTP email sent successfully to: " + to);
    }

    /**
     * Build a professional HTML email template for OTP.
     */
    private String buildOtpEmailTemplate(String otp) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #F85B1A, #072083); padding: 30px; text-align: center; }
                    .header h1 { color: #ffffff; margin: 0; font-size: 28px; }
                    .content { padding: 40px 30px; text-align: center; }
                    .otp-box { background-color: #f8f9fa; border: 2px dashed #F85B1A; border-radius: 10px; padding: 20px; margin: 30px 0; }
                    .otp-code { font-size: 36px; font-weight: bold; color: #F85B1A; letter-spacing: 8px; }
                    .warning { color: #6b7280; font-size: 14px; margin-top: 20px; }
                    .footer { background-color: #f8f9fa; padding: 20px; text-align: center; color: #6b7280; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🐉 TechForge ERP</h1>
                    </div>
                    <div class="content">
                        <h2 style="color: #1a1a1a;">Password Reset Request</h2>
                        <p style="color: #6b7280;">We received a request to reset your password. Use the OTP code below to verify your identity:</p>
                        
                        <div class="otp-box">
                            <div class="otp-code">%s</div>
                        </div>
                        
                        <p class="warning">
                            ⚠️ This code will expire in <strong>10 minutes</strong>.<br>
                            If you didn't request this, please ignore this email.
                        </p>
                    </div>
                    <div class="footer">
                        <p>© 2025 TechForge ERP - Saiyan Edition</p>
                        <p>This is an automated email. Please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(otp);
    }

    /**
     * Send a simple text email (fallback).
     */
    public void sendSimpleEmail(String to, String subject, String body) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, false);

        mailSender.send(message);
    }
}

