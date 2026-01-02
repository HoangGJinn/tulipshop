package com.tulip.service.impl;

import com.tulip.entity.Order;
import com.tulip.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    public EmailServiceImpl(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }
    // This annotation makes the method run in a separate thread (Multi-threading)
    // So the caller doesn't have to wait for it to finish
    @Async
    @Override
    public void sendOTPToEmail(String toEmail, String otp, String type) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject(type.equals("verify") ? "Xác nhận tài khoản của bạn" : "Đặt lại mật khẩu của bạn");

            String htmlContent = type.equals("verify") ? getHtmlContentForVerifyEmail(otp) : getHtmlContentForForgotPasswordEmail(otp);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("✅ OTP email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("❌ Failed to send OTP email to: {}. Error: {}", toEmail, e.getMessage(), e);
        }
    }

    @Async
    @Override
    public void sendOrderUpdateEmail(Order order) {
        try {
            if (order.getUser() == null) {
                log.error("❌ [EMAIL] Order #{} has no user!", order.getId());
                return;
            }
            
            String customerEmail = order.getUser().getEmail();
            if (customerEmail == null || customerEmail.trim().isEmpty()) {
                log.error("❌ [EMAIL] Order #{} user has no email address!", order.getId());
                return;
            }
            
            // Generate dynamic subject based on order status
            String subject = getEmailSubject(order);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(customerEmail);
            helper.setSubject(subject);

            // Create Thymeleaf context and add order data
            Context context = new Context();
            context.setVariable("order", order);

            // Process the template
            String htmlContent = templateEngine.process("mail/order-confirmation", context);
            helper.setText(htmlContent, true);

            log.info("📧 [EMAIL] Sending order {} email to: {} for order #{}", 
                    order.getStatus(), customerEmail, order.getId());
            mailSender.send(message);
            log.info("✅ [EMAIL] Order {} email sent successfully to: {} for order #{}", 
                    order.getStatus(), customerEmail, order.getId());
        } catch (MessagingException e) {
            log.error("❌ [EMAIL] MessagingException for order #{}. Error: {}", order.getId(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ [EMAIL] Unexpected error while sending order email for order #{}. Error: {}", 
                    order.getId(), e.getMessage(), e);
        }
    }

    @Async
    @Override
    @Deprecated
    public void sendOrderConfirmation(Order order) {
        // Delegate to the new method for backward compatibility
        sendOrderUpdateEmail(order);
    }

    /**
     * Generate email subject based on order status
     */
    private String getEmailSubject(Order order) {
        String orderId = order.getId() != null ? order.getId().toString() : "N/A";
        
        return switch (order.getStatus()) {
            case CONFIRMED -> "Tulipshop - Đơn hàng #" + orderId + " đã được xác nhận";
            case SHIPPING -> "Tulipshop - Đơn hàng #" + orderId + " đang trên đường giao đến bạn";
            case DELIVERED -> "Tulipshop - Đơn hàng #" + orderId + " đã giao thành công";
            default -> "Tulipshop - Cập nhật đơn hàng #" + orderId;
        };
    }

    private String getHtmlContentForVerifyEmail(String otp) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="UTF-8">
            <style>
                body {
                    background-color: #f4f4f4; /* Nền tổng xám rất nhạt để làm nổi bật khung mail */
                    font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
                    margin: 0;
                    padding: 0;
                }
                .email-wrapper {
                    max-width: 600px; /* Thu hẹp lại một chút cho gọn gàng */
                    margin: 40px auto;
                    padding: 20px;
                }
                .email-container {
                    background-color: #ffffff; /* Nền trắng tinh khôi */
                    border: 1px solid #e0e0e0; /* Viền mỏng nhẹ */
                    padding: 40px;
                    text-align: center; /* Căn giữa toàn bộ cho cân đối */
                }
                .brand-name {
                    font-size: 24px;
                    font-weight: bold;
                    letter-spacing: 3px;
                    color: #000000;
                    margin-bottom: 40px;
                    text-transform: uppercase;
                    border-bottom: 2px solid #000000;
                    display: inline-block;
                    padding-bottom: 10px;
                }
                .header h1 {
                    margin: 0 0 20px 0;
                    font-size: 20px;
                    font-weight: normal;
                    text-transform: uppercase;
                    letter-spacing: 1px;
                    color: #333333;
                }
                .intro {
                    font-size: 14px;
                    color: #555555;
                    line-height: 1.8;
                    margin-bottom: 30px;
                    padding: 0 20px;
                }
                .otp-container {
                    margin: 35px 0;
                }
                .otp-code {
                    display: inline-block;
                    font-size: 32px;
                    font-weight: 600;
                    color: #000000; /* Mã màu đen */
                    background: #ffffff;
                    padding: 15px 40px;
                    border: 1px solid #000000; /* Viền đen mảnh sang trọng */
                    letter-spacing: 8px; /* Tăng khoảng cách số cho thoáng */
                }
                .instructions {
                    font-size: 13px;
                    color: #777777;
                    line-height: 1.6;
                    margin-top: 30px;
                    font-style: italic;
                }
                .footer {
                    margin-top: 50px;
                    padding-top: 20px;
                    border-top: 1px solid #eeeeee;
                    font-size: 12px;
                    color: #999999;
                    text-transform: uppercase;
                    letter-spacing: 1px;
                }
                .btn-home {
                    text-decoration: none;
                    color: #000000;
                    font-weight: bold;
                    font-size: 12px;
                    margin-top: 10px;
                    display: inline-block;
                }
            </style>
            </head>
            <body>
            <div class="email-wrapper">
                <div class="email-container">
                    <div class="brand-name">TULIPSHOP</div>
                    
                    <div class="header">
                        <h1>Xác thực tài khoản</h1>
                    </div>
                    
                    <div class="intro">
                        Xin chào quý khách,<br/>
                        Để hoàn tất quá trình đăng nhập hoặc đăng ký tại Tulipshop, vui lòng sử dụng mã xác thực dưới đây.
                    </div>
                    
                    <div class="otp-container">
                        <div class="otp-code">%s</div>
                    </div>
                    
                    <div class="instructions">
                        Mã này có hiệu lực trong vòng <strong>5 phút</strong>.<br/>
                        Vì lý do bảo mật, tuyệt đối không chia sẻ mã này với bất kỳ ai.
                    </div>
                    
                    <div class="footer">
                        &copy; 2025 Tulipshop Fashion.<br/>
                        <a href="#" class="btn-home">Về trang chủ</a>
                    </div>
                </div>
            </div>
            </body>
            </html>
    """.formatted(otp);
    }

    private String getHtmlContentForForgotPasswordEmail(String otp) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="UTF-8">
            <style>
                body {
                    background-color: #f4f4f4; /* Nền tổng xám rất nhạt để làm nổi bật khung mail */
                    font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
                    margin: 0;
                    padding: 0;
                }
                .email-wrapper {
                    max-width: 600px; /* Thu hẹp lại một chút cho gọn gàng */
                    margin: 40px auto;
                    padding: 20px;
                }
                .email-container {
                    background-color: #ffffff; /* Nền trắng tinh khôi */
                    border: 1px solid #e0e0e0; /* Viền mỏng nhẹ */
                    padding: 40px;
                    text-align: center; /* Căn giữa toàn bộ cho cân đối */
                }
                .brand-name {
                    font-size: 24px;
                    font-weight: bold;
                    letter-spacing: 3px;
                    color: #000000;
                    margin-bottom: 40px;
                    text-transform: uppercase;
                    border-bottom: 2px solid #000000;
                    display: inline-block;
                    padding-bottom: 10px;
                }
                .header h1 {
                    margin: 0 0 20px 0;
                    font-size: 20px;
                    font-weight: normal;
                    text-transform: uppercase;
                    letter-spacing: 1px;
                    color: #333333;
                }
                .intro {
                    font-size: 14px;
                    color: #555555;
                    line-height: 1.8;
                    margin-bottom: 30px;
                    padding: 0 20px;
                }
                .otp-container {
                    margin: 35px 0;
                }
                .otp-code {
                    display: inline-block;
                    font-size: 32px;
                    font-weight: 600;
                    color: #000000; /* Mã màu đen */
                    background: #ffffff;
                    padding: 15px 40px;
                    border: 1px solid #000000; /* Viền đen mảnh sang trọng */
                    letter-spacing: 8px; /* Tăng khoảng cách số cho thoáng */
                }
                .instructions {
                    font-size: 13px;
                    color: #777777;
                    line-height: 1.6;
                    margin-top: 30px;
                    font-style: italic;
                }
                .footer {
                    margin-top: 50px;
                    padding-top: 20px;
                    border-top: 1px solid #eeeeee;
                    font-size: 12px;
                    color: #999999;
                    text-transform: uppercase;
                    letter-spacing: 1px;
                }
                .btn-home {
                    text-decoration: none;
                    color: #000000;
                    font-weight: bold;
                    font-size: 12px;
                    margin-top: 10px;
                    display: inline-block;
                }
            </style>
            </head>
            <body>
            <div class="email-wrapper">
                <div class="email-container">
                    <div class="brand-name">TULIPSHOP</div>
                    
                    <div class="header">
                        <h1>Đặt lại mật khẩu</h1>
                    </div>
                    
                    <div class="intro">
                        Xin chào quý khách,<br/>
                        Để hoàn tất quá trình đặt lại mật khẩu tại Tulipshop, vui lòng sử dụng mã xác thực dưới đây.
                    </div>
                    
                    <div class="otp-container">
                        <div class="otp-code">%s</div>
                    </div>
                    
                    <div class="instructions">
                        Mã này có hiệu lực trong vòng <strong>5 phút</strong>.<br/>
                        Vì lý do bảo mật, tuyệt đối không chia sẻ mã này với bất kỳ ai.
                    </div>
                    
                    <div class="footer">
                        &copy; 2025 Tulipshop Fashion.<br/>
                        <a href="#" class="btn-home">Về trang chủ</a>
                    </div>
                </div>
            </div>
            </body>
            </html>
    """.formatted(otp);
    }
}
