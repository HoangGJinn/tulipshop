# Hướng dẫn Test Chức năng Gửi Email Xác nhận Đặt hàng

## Tổng quan
Chức năng này tự động gửi email xác nhận đơn hàng cho khách hàng sau khi:
1. Đặt hàng thành công (COD hoặc thanh toán online)
2. Thanh toán online thành công

## Các file đã tạo/cập nhật

### 1. Template Email
- **File**: `src/main/resources/templates/mail/order-confirmation.html`
- **Mô tả**: Template Thymeleaf với inline CSS, clone design từ hình ảnh mẫu
- **Tính năng**:
  - Hiển thị thông tin đơn hàng đầy đủ
  - Danh sách sản phẩm với ảnh, tên, màu, size, số lượng, giá
  - Địa chỉ giao hàng
  - Phương thức thanh toán
  - Tổng tiền, phí ship, giảm giá (nếu có)
  - Thông tin liên hệ
  - Responsive design cho email client

### 2. EmailService
- **File**: `src/main/java/com/tulip/service/EmailService.java`
- **Thêm method**: `void sendOrderConfirmation(Order order)`

### 3. EmailServiceImpl
- **File**: `src/main/java/com/tulip/service/impl/EmailServiceImpl.java`
- **Cập nhật**:
  - Inject `TemplateEngine` để xử lý Thymeleaf template
  - Implement method `sendOrderConfirmation()` với `@Async`
  - Sử dụng `Context` để truyền dữ liệu vào template
  - Xử lý exception và logging chi tiết

### 4. OrderServiceImpl
- **File**: `src/main/java/com/tulip/service/impl/OrderServiceImpl.java`
- **Cập nhật**:
  - Inject `EmailService`
  - Trong `placeOrder()`: Gửi email sau khi lưu đơn hàng
  - Trong `confirmOrderPayment()`: Gửi email sau khi thanh toán online thành công
  - Eager load các quan hệ (User, Profile, OrderItems, Product, Variant, Size) trước khi gửi email để tránh `LazyInitializationException`

## Cấu hình Email (application.properties)

Đã có sẵn:
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

**Lưu ý**: 
- Cần điền `spring.mail.username` và `spring.mail.password`
- Với Gmail, cần tạo App Password (không dùng mật khẩu thường)
- Hướng dẫn tạo App Password: https://support.google.com/accounts/answer/185833

## Cách Test

### Test 1: Đặt hàng COD
1. Đăng nhập vào hệ thống
2. Thêm sản phẩm vào giỏ hàng
3. Tiến hành checkout, chọn phương thức thanh toán COD
4. Hoàn tất đặt hàng
5. **Kết quả mong đợi**: Email xác nhận được gửi ngay lập tức đến email của khách hàng

### Test 2: Đặt hàng với thanh toán online (VNPay/Momo)
1. Đăng nhập vào hệ thống
2. Thêm sản phẩm vào giỏ hàng
3. Tiến hành checkout, chọn phương thức thanh toán online
4. Hoàn tất thanh toán
5. **Kết quả mong đợi**: Email xác nhận được gửi sau khi thanh toán thành công

### Kiểm tra Log
Xem console log để theo dõi quá trình gửi email:
```
🔄 Preparing to send order confirmation email to: customer@example.com
📧 Sending order confirmation email to: customer@example.com
✅ Order confirmation email sent successfully to: customer@example.com for order #123
```

Nếu có lỗi:
```
❌ Failed to send order confirmation email for order #123. Error: ...
```

## Xử lý Lỗi Thường gặp

### 1. LazyInitializationException
**Nguyên nhân**: Các quan hệ lazy không được load trước khi async method chạy
**Giải pháp**: Đã xử lý bằng `Hibernate.initialize()` trong OrderServiceImpl

### 2. Authentication Failed (Gmail)
**Nguyên nhân**: Sử dụng mật khẩu thường thay vì App Password
**Giải pháp**: Tạo App Password từ Google Account Settings

### 3. Template Not Found
**Nguyên nhân**: Đường dẫn template không đúng
**Giải pháp**: Đảm bảo file `order-confirmation.html` nằm trong `src/main/resources/templates/mail/`

### 4. Email không hiển thị đúng
**Nguyên nhân**: Email client không hỗ trợ CSS
**Giải pháp**: Template đã sử dụng inline CSS và table layout để tương thích tối đa

## Tính năng Nâng cao (Tùy chọn)

### 1. Thêm ảnh logo vào email
Cập nhật template, thay thế text logo bằng:
```html
<img src="https://your-domain.com/logo.png" alt="Tulipshop" style="height: 40px;">
```

### 2. Thêm tracking link
Thêm link theo dõi đơn hàng:
```html
<a th:href="@{https://your-domain.com/orders/{id}(id=${order.id})}" 
   style="...">Theo dõi đơn hàng</a>
```

### 3. Gửi email cho admin
Thêm method trong EmailService để gửi thông báo đơn hàng mới cho admin

### 4. Queue email với RabbitMQ/Kafka
Để xử lý volume lớn, có thể tích hợp message queue thay vì @Async đơn giản

## Lưu ý Bảo mật

1. **Không commit** `application.properties` có chứa thông tin email thật
2. Sử dụng **environment variables** cho production:
   ```properties
   spring.mail.username=${EMAIL_USERNAME}
   spring.mail.password=${EMAIL_PASSWORD}
   ```
3. Giới hạn rate limit gửi email để tránh bị spam filter

## Checklist Hoàn thành

- [x] Tạo template email với inline CSS
- [x] Cập nhật EmailService interface
- [x] Implement sendOrderConfirmation trong EmailServiceImpl
- [x] Tích hợp vào OrderServiceImpl (placeOrder)
- [x] Tích hợp vào OrderServiceImpl (confirmOrderPayment)
- [x] Xử lý LazyInitializationException
- [x] Thêm logging chi tiết
- [x] Sử dụng @Async để không block user experience
- [ ] Test với email thật
- [ ] Kiểm tra hiển thị trên Gmail, Outlook, Yahoo Mail
- [ ] Deploy lên production

## Kết luận

Chức năng gửi email xác nhận đơn hàng đã được tích hợp hoàn chỉnh với:
- Design đẹp mắt, professional
- Xử lý bất đồng bộ không ảnh hưởng performance
- Error handling và logging đầy đủ
- Tương thích với các email client phổ biến

Hãy test kỹ trước khi deploy production!
