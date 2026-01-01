# 🔍 Debug Steps cho ERR_INCOMPLETE_CHUNKED_ENCODING

## Vấn Đề
- Lỗi: `ERR_INCOMPLETE_CHUNKED_ENCODING` ở dòng 13:1
- Trang product-detail không load được
- HTML bị ngắt giữa chừng

## Nguyên Nhân Có Thể

### 1. Template Rendering Error
- Thymeleaf gặp lỗi khi render
- Thiếu data hoặc null pointer
- Infinite loop trong template

### 2. Lazy Loading Issue (Hibernate)
- Entity có relationship chưa được fetch
- N+1 query problem
- LazyInitializationException

### 3. Memory/Timeout Issue
- Query quá lâu
- Data quá lớn
- Server timeout

## Các Bước Debug

### Bước 1: Kiểm Tra Server Log
```bash
# Xem log khi access trang product detail
# Tìm exception hoặc error
```

**Cần tìm:**
- `LazyInitializationException`
- `NullPointerException`
- `TemplateProcessingException`
- `OutOfMemoryError`

### Bước 2: Test Với Ratings Section Disabled
Tôi đã comment ratings section. Hãy:

1. **Restart server**
2. **Clear browser cache** (Ctrl + Shift + Delete)
3. **Hard reload** (Ctrl + F5)
4. **Thử access product detail**

**Nếu load được:**
- ✅ Vấn đề ở ratings section
- ➡️ Chuyển sang Bước 3

**Nếu vẫn lỗi:**
- ❌ Vấn đề ở chỗ khác
- ➡️ Chuyển sang Bước 4

### Bước 3: Fix Ratings Section
Nếu vấn đề ở ratings section:

```html
<!-- Thay vì load ngay, delay một chút -->
<div class="container product-ratings-section">
    <div id="ratingsSection" th:attr="data-product-id=${product.id}">
        <div class="text-center py-5">
            <p>Đang tải đánh giá...</p>
        </div>
    </div>
</div>

<script>
// Load ratings sau khi page đã render xong
setTimeout(() => {
    if (typeof ratingDisplay !== 'undefined') {
        ratingDisplay = new RatingDisplay(document.getElementById('ratingsSection').dataset.productId);
        ratingDisplay.load();
    }
}, 1000);
</script>
```

### Bước 4: Kiểm Tra ProductService
Nếu vấn đề không phải ratings:

```java
// Kiểm tra method getProductDetail
// Đảm bảo tất cả relationships được fetch đúng

@Transactional(readOnly = true)
public ProductDetailDTO getProductDetail(Long id) {
    Product product = productRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Product not found"));
    
    // Eager fetch tất cả relationships cần thiết
    Hibernate.initialize(product.getVariants());
    product.getVariants().forEach(variant -> {
        Hibernate.initialize(variant.getImages());
        Hibernate.initialize(variant.getStocks());
    });
    
    return convertToDTO(product);
}
```

### Bước 5: Simplify Template
Tạo template đơn giản để test:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Test Product Detail</title>
</head>
<body>
    <h1 th:text="${product.name}">Product Name</h1>
    <p th:text="${product.description}">Description</p>
    
    <!-- Test từng phần một -->
    <div th:if="${product.variants != null}">
        <p>Variants: <span th:text="${#lists.size(product.variants)}">0</span></p>
    </div>
</body>
</html>
```

### Bước 6: Check Browser Console
Mở DevTools (F12) và kiểm tra:

1. **Console Tab**: Xem JavaScript errors
2. **Network Tab**: 
   - Click vào request bị lỗi
   - Xem Response tab
   - Xem Headers tab (status code)
3. **Sources Tab**: Xem file nào bị incomplete

## Quick Fixes

### Fix 1: Tăng Timeout
```properties
# application.properties
server.connection-timeout=60000
spring.mvc.async.request-timeout=60000
```

### Fix 2: Disable Lazy Loading Tạm Thời
```java
// Entity
@OneToMany(fetch = FetchType.EAGER)
private List<Variant> variants;
```

### Fix 3: Add Error Handler
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model) {
        log.error("Error rendering page", e);
        model.addAttribute("error", e.getMessage());
        return "error/500";
    }
}
```

## Giải Pháp Tạm Thời

Nếu cần chạy ngay:

1. **Comment ratings section** (đã làm)
2. **Restart server**
3. **Test các trang khác** xem có bị không
4. **Kiểm tra log** để tìm root cause

## Next Steps

Sau khi tìm ra nguyên nhân:

1. **Fix root cause**
2. **Uncomment ratings section**
3. **Test kỹ lại**
4. **Optimize query** nếu cần

---

**Status**: Đã comment ratings section, đang chờ test lại.
