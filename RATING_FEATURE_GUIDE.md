# 🌟 Hướng Dẫn Chức Năng Đánh Giá Sản Phẩm Thông Minh - TulipShop

## 📋 Tổng Quan

Chức năng đánh giá sản phẩm thông minh cho phép khách hàng đánh giá sản phẩm sau khi hoàn tất đơn hàng, với thuật toán tính điểm hữu ích (Utility Score) để ưu tiên hiển thị các đánh giá chất lượng.

## ✨ Tính Năng Chính

### 1. Backend Features

#### 1.1. Entity & Database
- **Rating Entity**: Đã được mở rộng với các trường:
  - `utilityScore` (Double): Điểm hữu ích của đánh giá
  - `orderId` (Long): Liên kết với đơn hàng để kiểm tra quyền đánh giá
  
- **RatingImage Entity**: Lưu trữ hình ảnh đánh giá

#### 1.2. Thuật Toán Tính Utility Score
```
Điểm cơ bản: 0
+ Số lượng từ: +1 điểm cho mỗi 10 từ (tối đa 20 điểm)
+ Hình ảnh: 
  - Ảnh đầu tiên: +30 điểm
  - Mỗi ảnh thêm: +10 điểm (tối đa thêm 20 điểm)
+ Chất lượng: Loại bỏ spam/nội dung vô nghĩa
```

**Ví dụ:**
- Đánh giá 50 từ + 2 ảnh = 5 + 30 + 10 = 45 điểm (High Quality)
- Đánh giá 100 từ + 3 ảnh = 10 + 30 + 20 = 60 điểm (High Quality)
- Đánh giá 20 từ + 0 ảnh = 2 điểm (Low Quality)

#### 1.3. API Endpoints

**POST /api/ratings**
- Submit đánh giá sản phẩm
- Request: `multipart/form-data`
  - orderId: Long
  - productId: Long
  - stars: Integer (1-5)
  - content: String (max 2000 chars)
  - variantInfo: String (optional)
  - images: List<MultipartFile> (max 5 images)

**GET /api/ratings/product/{productId}**
- Lấy danh sách đánh giá của sản phẩm
- Sắp xếp: utilityScore DESC, createdAt DESC

**GET /api/ratings/product/{productId}/statistics**
- Lấy thống kê đánh giá (tổng số, trung bình, phân bố sao)

**GET /api/ratings/can-rate**
- Kiểm tra user có thể đánh giá sản phẩm không
- Params: productId, orderId

#### 1.4. Validation Rules
- User chỉ được đánh giá khi:
  - Đơn hàng có trạng thái COMPLETED/DELIVERED
  - Sản phẩm có trong đơn hàng
  - Chưa đánh giá sản phẩm này trong đơn hàng đó

### 2. Frontend Features

#### 2.1. Rating Modal (order-detail.html)
- **Vị trí**: Trang chi tiết đơn hàng
- **Trigger**: Nút "Đánh giá" bên cạnh mỗi sản phẩm (chỉ hiện với đơn COMPLETED/DELIVERED)
- **Tính năng**:
  - Interactive star rating (hover effect)
  - Textarea với character counter (max 2000)
  - Image upload với preview (max 5 ảnh)
  - Drag & drop support
  - Real-time validation
  - AJAX submission (không reload trang)

#### 2.2. Rating Display (product-detail.html)
- **Vị trí**: Cuối trang chi tiết sản phẩm
- **Layout**:
  - **Summary Card**: Điểm trung bình, tổng số đánh giá
  - **Breakdown**: Phân bố theo số sao (progress bar)
  - **Rating List**: Danh sách đánh giá
    - Sắp xếp thông minh (utilityScore DESC)
    - Badge "Đánh giá chất lượng" cho rating có utilityScore >= 40
    - Hiển thị avatar, tên, số sao, ngày
    - Grid hình ảnh với lightbox
    - Variant info

### 3. Notification System

#### 3.1. Real-time Notification (WebSocket)
- Gửi thông báo ngay khi đơn hàng DELIVERED
- Nội dung: "Bạn đã nhận được hàng? Hãy chia sẻ trải nghiệm..."
- Link: Đến trang chi tiết đơn hàng

#### 3.2. Email Reminder
- Template: `mail/rating-reminder.html`
- Gửi sau khi đơn hàng DELIVERED
- Nội dung:
  - Lời cảm ơn
  - Danh sách sản phẩm trong đơn
  - Nút "Đánh giá ngay"
  - Mẹo về đánh giá chất lượng

### 4. Image Upload với Cloudinary

#### 4.1. Tại sao dùng Cloudinary?
- ✅ **Tốc độ tải nhanh**: CDN toàn cầu
- ✅ **Tối ưu hình ảnh**: Tự động resize, compress
- ✅ **Không tốn dung lượng server**: Lưu trữ cloud
- ✅ **Reliable**: Uptime 99.9%

#### 4.2. Implementation
- Sử dụng `CloudinaryService` có sẵn trong project
- Upload vào folder `tulip-fashion` trên Cloudinary
- Trả về `secure_url` để lưu vào database

## 🚀 Cài Đặt & Sử Dụng

### 1. Database Migration

Chạy migration script để thêm bảng ratings:
```sql
-- File: src/main/resources/db/migration/V1__Add_Rating_Tables.sql
-- Sẽ tự động chạy khi khởi động ứng dụng
```

### 2. Configuration

Đã được cấu hình trong `application.properties`:
```properties
# Cloudinary Configuration (đã có sẵn)
cloudinary.cloud-name=diawi4gde
cloudinary.api-key=453996322555967
cloudinary.api-secret=jrqf4lny19CzLVKuv7tyL7M0tPk

# File Upload Limits
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=50MB
```

**Lưu ý**: Hình ảnh đánh giá sẽ được upload lên Cloudinary (folder: `tulip-fashion`), không lưu trên server local.

### 3. Khởi Động Ứng Dụng

```bash
mvn spring-boot:run
```

### 4. Test Chức Năng

#### Bước 1: Tạo đơn hàng test
1. Đăng nhập với tài khoản user
2. Thêm sản phẩm vào giỏ hàng
3. Đặt hàng

#### Bước 2: Cập nhật trạng thái đơn hàng (Admin)
1. Đăng nhập admin
2. Vào quản lý đơn hàng
3. Cập nhật trạng thái: PENDING → CONFIRMED → SHIPPING → DELIVERED

#### Bước 3: Kiểm tra notification
- User sẽ nhận được:
  - Thông báo real-time trên website
  - Email nhắc đánh giá

#### Bước 4: Đánh giá sản phẩm
1. Vào "Đơn hàng của tôi"
2. Click vào đơn hàng đã DELIVERED
3. Click nút "Đánh giá" bên cạnh sản phẩm
4. Điền thông tin:
   - Chọn số sao
   - Viết nội dung (càng dài càng tốt)
   - Upload hình ảnh (càng nhiều càng tốt)
5. Submit

#### Bước 5: Xem đánh giá
1. Vào trang chi tiết sản phẩm
2. Scroll xuống phần "Đánh giá sản phẩm"
3. Kiểm tra:
   - Thống kê tổng quan
   - Phân bố sao
   - Danh sách đánh giá (sắp xếp theo utilityScore)
   - Badge "Đánh giá chất lượng" cho rating có điểm cao

## 📁 Cấu Trúc File

### Backend
```
src/main/java/com/tulip/
├── entity/product/
│   ├── Rating.java (updated)
│   └── RatingImage.java
├── dto/
│   ├── RatingRequest.java (new)
│   ├── RatingDTO.java (new)
│   └── RatingStatistics.java (new)
├── repository/
│   └── RatingRepository.java (new)
├── service/
│   ├── RatingService.java (new)
│   ├── EmailService.java (updated)
│   └── impl/
│       ├── RatingServiceImpl.java (new)
│       ├── EmailServiceImpl.java (updated)
│       └── OrderServiceImpl.java (updated)
├── controller/api/
│   └── RatingApiController.java (new)
├── config/
│   └── CloudinaryConfig.java (existing - reused)
└── util/
    └── (FileUploadUtil.java removed - using CloudinaryService instead)
```

### Frontend
```
src/main/resources/
├── static/
│   ├── css/
│   │   ├── rating-modal.css (new)
│   │   └── rating-display.css (new)
│   ├── js/
│   │   ├── rating-modal.js (new)
│   │   └── rating-display.js (new)
│   └── uploads/
│       └── ratings/ (new)
└── templates/
    ├── order/
    │   └── order-detail.html (updated)
    ├── product/
    │   └── product-detail.html (updated)
    └── mail/
        └── rating-reminder.html (new)
```

## 🎨 UI/UX Highlights

### Rating Modal
- **Design**: Modern gradient header (purple theme)
- **Interactive**: Hover effect trên stars
- **User-friendly**: 
  - Character counter
  - Image preview với remove button
  - Drag & drop support
  - Loading state khi submit

### Rating Display
- **Smart Sort**: Đánh giá chất lượng lên đầu
- **Visual Hierarchy**: 
  - Summary card nổi bật với gradient
  - Progress bar cho phân bố sao
  - Badge "Đánh giá chất lượng" màu xanh
- **Image Gallery**: Grid layout với lightbox

## 🔒 Security & Validation

### Backend Validation
- ✅ Kiểm tra quyền đánh giá (order status, ownership)
- ✅ Validate input (stars 1-5, content max 2000 chars)
- ✅ Spam detection (loại bỏ nội dung spam)
- ✅ File upload validation (type, size)

### Frontend Validation
- ✅ Required fields check
- ✅ Character limit
- ✅ Image count limit (max 5)
- ✅ File type check (images only)

## 📊 Performance Optimization

- **Lazy Loading**: Ratings load sau khi page render
- **AJAX**: Submit không reload trang
- **Async Email**: Email gửi bất đồng bộ
- **Index**: Database index trên utilityScore, createdAt
- **Cloudinary CDN**: Hình ảnh load nhanh từ CDN toàn cầu
- **Image Optimization**: Cloudinary tự động tối ưu kích thước và format

## 🐛 Troubleshooting

### Lỗi upload ảnh lên Cloudinary
- Kiểm tra cấu hình Cloudinary trong `application.properties`
- Kiểm tra API key và secret còn hiệu lực
- Kiểm tra kết nối internet
- Kiểm tra log để xem lỗi chi tiết: `❌ Lỗi upload ảnh đánh giá lên Cloudinary`
- Cloudinary free tier có giới hạn: 25 credits/month, 25GB storage

### Không nhận được email
- Kiểm tra cấu hình SMTP trong application.properties
- Kiểm tra log để xem lỗi chi tiết
- Email được gửi bất đồng bộ, có thể delay vài giây

### Rating không hiển thị
- Mở Developer Console kiểm tra lỗi JavaScript
- Kiểm tra API `/api/ratings/product/{productId}` có trả về data không
- Kiểm tra `data-product-id` attribute trong HTML

## 🎯 Best Practices

### Cho User
1. **Viết đánh giá chi tiết**: Càng nhiều từ càng tốt (tối thiểu 50 từ)
2. **Thêm hình ảnh**: Ít nhất 2-3 ảnh thực tế
3. **Trung thực**: Đánh giá dựa trên trải nghiệm thực tế

### Cho Developer
1. **Monitor logs**: Theo dõi log để phát hiện lỗi sớm
2. **Cloudinary quota**: Theo dõi usage trên Cloudinary dashboard
3. **Regular cleanup**: Xóa ảnh trên Cloudinary của rating đã bị xóa (nếu cần)
4. **Backup**: Backup database thường xuyên
5. **Testing**: Test kỹ flow đánh giá trước khi deploy

## 📈 Future Enhancements

- [ ] Thêm reaction (helpful/not helpful) cho đánh giá
- [ ] Reply từ shop cho đánh giá
- [ ] Filter đánh giá theo số sao
- [ ] Sort đánh giá theo nhiều tiêu chí
- [ ] Report spam/inappropriate content
- [ ] AI-powered sentiment analysis
- [ ] Video review support

## 📞 Support

Nếu có vấn đề, vui lòng:
1. Kiểm tra log trong console
2. Kiểm tra database có dữ liệu không
3. Kiểm tra network tab trong DevTools
4. Liên hệ team để được hỗ trợ

---

**Developed with ❤️ by TulipShop Team**
