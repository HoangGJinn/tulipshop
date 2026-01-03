# Hướng dẫn khắc phục lỗi không tạo được Voucher

## Vấn đề
Không thể tạo voucher vì thiếu các trường `quantity` và `usedCount` trong entity Voucher.

## Các thay đổi đã thực hiện

### 1. Cập nhật Entity Voucher
- ✅ Đã thêm trường `quantity` (số lượng voucher phát hành)
- ✅ Đã thêm trường `usedCount` (số lượng đã sử dụng)
- ✅ Đã cập nhật phương thức `isValid()` để kiểm tra số lượng còn lại

### 2. Cập nhật VoucherServiceImpl
- ✅ Khởi tạo `usedCount = 0` khi tạo voucher mới

### 3. Cập nhật AdminVoucherController
- ✅ Thêm route `/admin/vouchers/form` để xử lý cả create và edit
- ✅ Thêm attribute `isEditMode` để phân biệt chế độ tạo mới và chỉnh sửa
- ✅ Khởi tạo giá trị mặc định cho `quantity = 100` và `usedCount = 0`

### 4. Tạo file migration SQL
- ✅ File `voucher_migration.sql` để thêm cột vào database

## Cách khắc phục

### Bước 1: Restart ứng dụng
Nếu ứng dụng đang chạy, hãy dừng lại và chạy lại:
```bash
# Dừng ứng dụng (Ctrl+C)
# Sau đó chạy lại
mvn spring-boot:run
```

### Bước 2: Kiểm tra database
Nếu JPA không tự động cập nhật schema, bạn cần chạy migration SQL thủ công:

**Cách 1: Sử dụng MySQL Workbench hoặc phpMyAdmin**
1. Mở MySQL Workbench/phpMyAdmin
2. Chọn database của bạn
3. Chạy script trong file `src/main/resources/voucher_migration.sql`

**Cách 2: Sử dụng command line**
```bash
mysql -u root -p tulipshop < src/main/resources/voucher_migration.sql
```
(Thay `root` và `tulipshop` bằng username và database name của bạn)

### Bước 3: Kiểm tra lại
1. Mở trình duyệt và truy cập: `http://localhost:8787/admin/vouchers`
2. Click nút "Thêm Voucher"
3. Điền thông tin voucher:
   - Mã voucher: VD: `SALE10`
   - Loại giảm giá: Chọn Phần trăm (%) hoặc Số tiền cố định (₫)
   - Giá trị giảm: VD: `10` (nếu chọn %)
   - Đơn hàng tối thiểu: VD: `100000` (hoặc để trống)
   - Số lượng phát hành: VD: `100`
   - Trạng thái: Bật "Đang hoạt động"
4. Click "Tạo Voucher"

## Lưu ý quan trọng

### Về trường quantity
- `quantity`: Tổng số voucher có thể sử dụng
- `usedCount`: Số lần voucher đã được sử dụng
- Voucher sẽ không hợp lệ khi `usedCount >= quantity`

### Về validation
- Mã voucher sẽ tự động chuyển thành chữ IN HOA
- Mã voucher phải là duy nhất (unique)
- Giá trị giảm phải > 0

## Troubleshooting

### Nếu vẫn không tạo được voucher:
1. Mở Console trong trình duyệt (F12)
2. Kiểm tra tab Network để xem response từ API
3. Kiểm tra tab Console để xem có lỗi JavaScript không
4. Kiểm tra log của Spring Boot để xem lỗi backend

### Nếu gặp lỗi "Column 'quantity' not found":
- Chạy lại migration SQL ở Bước 2
- Hoặc xóa bảng `vouchers` và để JPA tự tạo lại

### Nếu gặp lỗi "Duplicate entry for key 'code'":
- Mã voucher đã tồn tại, hãy dùng mã khác
