# Email Debug Checklist

## Bước 1: Kiểm tra Console Log

Sau khi đặt hàng, hãy tìm các log sau trong console:

### Log từ OrderServiceImpl:
```
📦 Order #123 saved successfully. Preparing to send confirmation email...
📧 Calling emailService.sendOrderConfirmation for order #123
✅ Email service called successfully for order #123
```

### Log từ EmailServiceImpl:
```
🔄 [EMAIL] Starting sendOrderConfirmation for order #123
🔄 [EMAIL] Preparing to send order confirmation email to: customer@example.com for order #123
🔄 [EMAIL] Creating Thymeleaf context for order #123
🔄 [EMAIL] Processing template for order #123
📧 [EMAIL] Sending order confirmation email to: customer@example.com for order #123
✅ [EMAIL] Order confirmation email sent successfully to: customer@example.com for order #123
```

## Bước 2: Nếu KHÔNG thấy log nào

### Kiểm tra 1: @EnableAsync có được bật không?
```bash
# Tìm trong TulipshopApplication.java
grep -r "@EnableAsync" src/
```

### Kiểm tra 2: EmailService có được inject không?
- Xem trong OrderServiceImpl constructor có `private final EmailService emailService;`
- Kiểm tra Spring có khởi tạo bean không

### Kiểm tra 3: Application có restart sau khi thay đổi code không?
- Restart lại Spring Boot application
- Clear cache nếu cần

## Bước 3: Nếu thấy log nhưng không nhận được email

### Kiểm tra 1: Email credentials
```properties
spring.mail.username=hoanggiap1803@gmail.com
spring.mail.password=toivjdwxzqyhiynq
```
- Username có đúng không?
- Password có phải App Password không? (không phải mật khẩu thường)

### Kiểm tra 2: Test email credentials
Chạy lệnh sau để test kết nối SMTP:
```bash
telnet smtp.gmail.com 587
```

### Kiểm tra 3: Kiểm tra spam folder
- Email có thể bị đưa vào spam
- Kiểm tra cả Promotions tab trong Gmail

### Kiểm tra 4: Bật debug logging cho Spring Mail
Uncomment dòng này trong application.properties:
```properties
logging.level.org.springframework.mail=DEBUG
```

## Bước 4: Nếu có lỗi trong log

### Lỗi: "Authentication failed"
**Nguyên nhân**: Sai username/password hoặc chưa bật App Password
**Giải pháp**: 
1. Vào https://myaccount.google.com/apppasswords
2. Tạo App Password mới
3. Cập nhật vào application.properties

### Lỗi: "LazyInitializationException"
**Nguyên nhân**: Quan hệ lazy không được load
**Giải pháp**: Đã xử lý bằng Hibernate.initialize() - kiểm tra lại code

### Lỗi: "Template not found"
**Nguyên nhân**: File template không đúng vị trí
**Giải pháp**: Đảm bảo file ở `src/main/resources/templates/mail/order-confirmation.html`

### Lỗi: "Connection timeout"
**Nguyên nhân**: Firewall hoặc network issue
**Giải pháp**: 
- Kiểm tra firewall
- Thử port 465 thay vì 587
- Kiểm tra proxy settings

## Bước 5: Test thủ công

Tạo một test endpoint để gửi email thử:

```java
@RestController
@RequestMapping("/test")
public class EmailTestController {
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @GetMapping("/send-email/{orderId}")
    public String testEmail(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
        
        emailService.sendOrderConfirmation(order);
        return "Email sent! Check logs and inbox.";
    }
}
```

Sau đó gọi: `http://localhost:8787/test/send-email/123`

## Bước 6: Kiểm tra email trong database

```sql
SELECT id, email FROM users WHERE id = (SELECT user_id FROM orders WHERE id = 123);
```

Đảm bảo user có email hợp lệ.

## Các lỗi thường gặp

1. **Email không được gửi vì @Async không hoạt động**
   - Kiểm tra @EnableAsync trong main class
   - Kiểm tra thread pool configuration

2. **Email bị delay**
   - @Async chạy trong thread riêng nên có thể delay
   - Kiểm tra log sau 5-10 giây

3. **Gmail block email**
   - Gmail có thể block nếu gửi quá nhiều email
   - Sử dụng App Password thay vì mật khẩu thường
   - Bật "Less secure app access" (không khuyến khích)

4. **Template render lỗi**
   - Kiểm tra Thymeleaf syntax
   - Kiểm tra các biến có null không
   - Xem log chi tiết

## Quick Fix: Tắt @Async để test

Nếu muốn test nhanh, tạm thời comment @Async:

```java
// @Async
@Override
public void sendOrderConfirmation(Order order) {
    // ...
}
```

Restart app và test lại. Nếu email được gửi thì vấn đề là ở @Async configuration.

## Liên hệ

Nếu vẫn không được, hãy gửi cho tôi:
1. Full console log khi đặt hàng
2. Screenshot application.properties (che password)
3. Kết quả query: `SELECT * FROM orders ORDER BY id DESC LIMIT 1;`
