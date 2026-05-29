# 🌷 Tulip Shop - E-commerce Web Application - Group 3

**Trạng thái dự án:** Đã hoàn thành 🎉

Chào mừng đến với **Tulip Shop**! Đây là dự án website thương mại điện tử bán **quần áo thời trang** được xây dựng dựa trên nền tảng Java Spring Boot kết hợp với Thymeleaf, tập trung vào hiệu năng, bảo mật và trải nghiệm người dùng.

## 👥 Authors

* **Nguyễn Hoàng Giáp** (Project Lead)
* **Nguyễn Thành Vinh** (Contributor)
* **Dương Minh Duy**    (Contributor)

## 🛠 Công nghệ sử dụng (Tech Stack)

* **Backend:** Java 21, Spring Boot 3.5.8
* **Database:** MySQL 8.0+ (H2 cho development/testing)
* **ORM:** Spring Data JPA / Hibernate
* **Security:** Spring Security 6 (BCrypt Password Hashing)
* **Frontend:** Thymeleaf, HTML5, CSS3
* **Validation:** Jakarta Bean Validation
* **Email:** Spring Mail (Gmail SMTP)
* **Tools:** Maven, Lombok, Git
* **API shipping delivery integration:** [tulip-shipping](https://github.com/HoangGJinn/tulipshop-shipping)
* **Realtime:** WebSocket (STOMP, SockJS)
* **Cloud Storage:** Cloudinary
* **Payment Integration:** VNPAY, Momo
* **Scheduling:** Spring Scheduling
* **AI Integration:** Google AI API

## 🎬 Application Preview

### 🏠 Homepage

<p align="center">
  <img src="https://raw.githubusercontent.com/HoangGJinn/Project_Image/main/Tulip_Shop/customer_homepage.gif" alt="Tulip Shop Homepage" width="900"/>
</p>

<p align="center">
  <i>Main customer homepage experience</i>
</p>

---

## 📸 Screenshots

<p align="center">
  <a href="#screenshots">
    <img src="https://img.shields.io/badge/View%20All%20Screenshots-10%20Screens-success?style=for-the-badge" />
  </a>
</p>

<details>
<summary><b>📷 Click to explore the full application gallery</b></summary>

<br>

### 🛍️ Product Listing

<p align="center">
  <img src="https://raw.githubusercontent.com/HoangGJinn/Project_Image/main/Tulip_Shop/customer_list_product.png" width="900"/>
</p>

### 👕 Product Details

<p align="center">
  <img src="https://raw.githubusercontent.com/HoangGJinn/Project_Image/main/Tulip_Shop/customer_detail_product.png" width="900"/>
</p>

### 🛒 Shopping Cart

<p align="center">
  <img src="https://raw.githubusercontent.com/HoangGJinn/Project_Image/main/Tulip_Shop/customer_cart.png" width="900"/>
</p>

### 💳 Checkout & Payment

<p align="center">
  <img src="https://raw.githubusercontent.com/HoangGJinn/Project_Image/main/Tulip_Shop/customer_payment.png" width="900"/>
</p>

### 👤 Customer Profile

<p align="center">
  <img src="https://raw.githubusercontent.com/HoangGJinn/Project_Image/main/Tulip_Shop/customer_profile.png" width="900"/>
</p>

### 🤖 AI Shopping Assistant

<p align="center">
  <img src="https://raw.githubusercontent.com/HoangGJinn/Project_Image/main/Tulip_Shop/customer_chat_ai.png" width="900"/>
</p>

### 📊 Admin Dashboard

<p align="center">
  <img src="https://raw.githubusercontent.com/HoangGJinn/Project_Image/main/Tulip_Shop/admin_dashboard.png" width="900"/>
</p>

### 🏪 Store Management

<p align="center">
  <img src="https://raw.githubusercontent.com/HoangGJinn/Project_Image/main/Tulip_Shop/admin_store_management.png" width="900"/>
</p>

### 👥 User Management

<p align="center">
  <img src="https://raw.githubusercontent.com/HoangGJinn/Project_Image/main/Tulip_Shop/admin_user_management.png" width="900"/>
</p>

### 🎟️ Voucher Management

<p align="center">
  <img src="https://raw.githubusercontent.com/HoangGJinn/Project_Image/main/Tulip_Shop/admin_voucher.png" width="900"/>
</p>

</details>


## ⚙️ Cài đặt & Chạy dự án (Installation)

### Yêu cầu hệ thống
* Java 21+
* Maven 3.6+
* MySQL 8.0+ (hoặc H2 cho development)

### Các bước cài đặt

1. **Clone dự án:**
   ```bash
   git clone <repository-url>
   cd tulipshop
   ```

2. **Cấu hình Database & Biến môi trường:**
   * Tạo database rỗng trong MySQL:
     ```sql
     CREATE DATABASE tulipshop;
     ```
   * Copy file cấu hình mẫu:
     ```bash
     cp src/main/resources/application.properties.example src/main/resources/application.properties
     ```
   * Mở file `src/main/resources/application.properties` và cập nhật các thông tin cần thiết:
     ```properties
     spring.datasource.username=YOUR_USERNAME
     spring.datasource.password=YOUR_PASSWORD
     spring.mail.username=YOUR_EMAIL@gmail.com
     spring.mail.password=YOUR_EMAIL_APP_PASSWORD
     jwt.secret=YOUR_JWT_SECRET_KEY_HERE
     cloudinary.cloud-name=YOUR_CLOUDINARY_CLOUD_NAME
     cloudinary.api-key=YOUR_CLOUDINARY_API_KEY
     cloudinary.api-secret=YOUR_CLOUDINARY_API_SECRET
     vnp.tmn.code=YOUR_VNP_TMNCODE
     vnp.secret.key=YOUR_VNP_SECRET_KEY
     momo.partner.code=YOUR_MOMO_PARTNER_CODE
     momo.access.key=YOUR_MOMO_ACCESS_KEY
     momo.secret.key=YOUR_MOMO_SECRET_KEY
     google.ai.api.key=YOUR_GOOGLE_AI_API_KEY
     # ... và các thông tin cấu hình khác nếu sử dụng các dịch vụ tương ứng
     ```

3. **Chạy ứng dụng:**
   ```bash
   mvn spring-boot:run
   ```
   * Truy cập trang chủ: `http://localhost:8787/`

## 📁 Cấu trúc dự án (Project Structure)

```
src/main/java/com/tulip/
├── config/              # Cấu hình (Security, Data Initialization, WebSocket, Payment)
├── controller/          # Controllers (Auth, Home, API, Admin, Payment, Chat, Email, ...)
├── dto/                 # Data Transfer Objects (request/response)
├── entity/              # JPA Entities (User, Product, Order, Chat, ...)
│   ├── chat/            # Chat entities
│   ├── enums/           # Enum types (OrderStatus, PaymentStatus, ...)
│   └── product/         # Product-related entities
├── exception/           # Custom exceptions
├── mapper/              # DTO <-> Entity mappers
├── repository/          # JPA Repositories
├── scheduler/           # Scheduled tasks (thống kê, tự động hóa)
├── security/            # JWT, OAuth2, Interceptors
├── service/             # Business Logic
│   ├── impl/            # Service Implementations
│   └── integration/     # Tích hợp bên ngoài (shipping, ...)
└── util/                # Utilities (VnpayUtil, ...)

src/main/resources/
├── templates/           # Thymeleaf templates
│   ├── about.html
│   ├── cart.html
│   ├── contact.html
│   ├── index.html
│   ├── admin/           # Trang quản trị (dashboard, products, orders, ...)
│   ├── auth/            # Đăng nhập, đăng ký, xác thực
│   ├── error/           # Trang lỗi
│   ├── fragments/       # Các đoạn giao diện dùng chung (header, footer, ...)
│   ├── mail/            # Email templates
│   ├── order/           # Trang đặt hàng, chi tiết đơn
│   ├── payment/         # Kết quả thanh toán
│   ├── policies/        # Chính sách
│   ├── product/         # Trang sản phẩm
│   └── user/            # Trang người dùng
├── static/              # Static resources
│   ├── assets/          # 3D models, media
│   ├── css/             # Stylesheets
│   ├── images/          # Hình ảnh
│   └── js/              # JavaScript files
├── application.properties          # Cấu hình (không commit)
└── application.properties.example  # Cấu hình mẫu
```

## 📋 Quy tắc làm việc (Workflow & Contribution)

Để đảm bảo code luôn sạch sẽ và hạn chế xung đột (conflict), team tuân thủ các quy tắc sau:

### 1. Chiến lược phân nhánh (Branching Strategy)

Chúng ta sử dụng mô hình Git Flow đơn giản hóa:

* **`main`**: Nhánh Production. Chứa code ổn định nhất để chạy thật. **Tuyệt đối KHÔNG commit trực tiếp vào nhánh này.**
* **`dev`**: Nhánh Development. Đây là nhánh chính để phát triển. Mọi tính năng mới sẽ được merge vào đây trước khi lên `main`.
* **Feature Branches (`feature/...`)**: Khi làm tính năng mới, hãy tách nhánh từ `dev`.

**Quy trình chuẩn:**
1. Checkout sang nhánh `dev` và pull code mới nhất: `git checkout dev && git pull origin dev`.
2. Tạo nhánh mới từ `dev`: `git checkout -b feature/ten-tinh-nang`.
3. Code và commit trên nhánh feature đó.
4. Khi hoàn thành, tạo **Pull Request (PR)** để merge vào `dev`.

### 2. Quy chuẩn đặt tên (Naming Convention)

* **Tên Branch:** Viết thường, dùng dấu gạch nối `-`.
  * Tính năng mới: `feature/authentication`, `feature/cart`, `feature/product-list`.
  * Sửa lỗi: `fix/login-error`, `fix/css-header`.
* **Tên Biến/Class:** Tuân thủ chuẩn Java (CamelCase cho biến/hàm, PascalCase cho Class).

### 3. Quy chuẩn Commit (Commit Message)

Sử dụng chuẩn **Conventional Commits** để dễ dàng theo dõi lịch sử thay đổi.
Cấu trúc: `[Loại]: [Mô tả ngắn gọn]`

**Các loại (Type) phổ biến:**
* `feat`: Tính năng mới (VD: `feat: add login functionality`)
* `fix`: Sửa lỗi (VD: `fix: resolve null pointer in user service`)
* `docs`: Chỉ sửa tài liệu (VD: `docs: update readme`)
* `style`: Sửa format, css, không ảnh hưởng logic code (VD: `style: format code`, `style: update button color`)
* `refactor`: Sửa code để tối ưu hơn, không đổi tính năng (VD: `refactor: simplify auth controller`)
* `chore`: Các việc vặt khác (VD: `chore: update dependencies`)

**Ví dụ một commit tốt:**
> `feat: implement user registration with email validation`


## 📝 Lưu ý quan trọng

* **KHÔNG commit file `application.properties`** vào Git (đã có trong `.gitignore`)
* Sử dụng `application.properties.example` làm template
* Database sẽ tự động tạo tables khi chạy lần đầu (Hibernate `ddl-auto=update`)


