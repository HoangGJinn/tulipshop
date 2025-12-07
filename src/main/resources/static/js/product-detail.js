let selectedVariantIndex = 0;
let selectedSize = null;

document.addEventListener('DOMContentLoaded', function() {
    // Khởi tạo: Chọn màu đầu tiên khi load trang
    const firstColorOption = document.querySelector('.color-swatch');
    if (firstColorOption) selectColor(firstColorOption);
    initImageZoom();
});

// Đổi ảnh chính khi click thumbnail
function changeMainImage(element) {
    document.getElementById('mainImage').src = element.src;
    document.querySelectorAll('.thumbnail-img').forEach(el => el.classList.remove('active-thumb'));
    element.classList.add('active-thumb');
}

// Xử lý chọn màu
function selectColor(element) {
    // 1. UI: Active ô màu
    document.querySelectorAll('.color-swatch').forEach(el => el.classList.remove('active'));
    element.classList.add('active');

    // 2. Data: Lấy variant tương ứng
    selectedVariantIndex = element.getAttribute('data-index');
    const variant = productData.variants[selectedVariantIndex];

    // 3. Images: Cập nhật ảnh chính và list thumbnails
    updateGallery(variant.images);

    // 4. Stock: Reset size và cập nhật trạng thái tồn kho các size
    selectedSize = null;
    updateSizeAvailability(variant.stockBySize);

    // Reset số lượng về 1 khi đổi màu
    document.getElementById('quantity').value = 1;
    updateStockDisplay("--");
}

function updateGallery(images) {
    const mainImg = document.getElementById('mainImage');
    const container = document.querySelector('.thumbnail-list');

    if(images.length > 0) mainImg.src = images[0];

    container.innerHTML = '';
    images.forEach((img, index) => {
        const thumb = document.createElement('img');
        thumb.src = img;
        thumb.className = `img-fluid thumbnail-img ${index === 0 ? 'active-thumb' : ''}`;
        thumb.onclick = () => changeMainImage(thumb);
        container.appendChild(thumb);
    });
}

function updateSizeAvailability(stockMap) {
    document.querySelectorAll('.btn-size').forEach(btn => {
        btn.classList.remove('active');
        const size = btn.getAttribute('data-size');
        const stock = stockMap[size] || 0;

        if (stock <= 0) {
            btn.disabled = true;
        } else {
            btn.disabled = false;
        }
    });
    document.getElementById('stockMessage').style.display = 'none';
}

// Xử lý chọn size
function selectSize(element) {
    document.querySelectorAll('.btn-size').forEach(btn => btn.classList.remove('active'));
    element.classList.add('active');

    selectedSize = element.getAttribute('data-size');

    // Lấy tồn kho cụ thể
    const variant = productData.variants[selectedVariantIndex];
    const stock = variant.stockBySize[selectedSize];

    updateStockDisplay(stock);
}

function updateStockDisplay(stock) {
    const stockCountEl = document.getElementById('stockCount');
    const msg = document.getElementById('stockMessage');
    const quantityInput = document.getElementById('quantity');

    stockCountEl.innerText = stock;

    if (stock !== "--" && stock < 5 && stock > 0) {
        msg.style.display = 'block';
        msg.innerText = `Chỉ còn ${stock} sản phẩm, nhanh tay kẻo hết!`;
    } else {
        msg.style.display = 'none';
    }

    // Reset số lượng nếu vượt quá tồn kho mới
    if (stock !== "--" && parseInt(quantityInput.value) > stock) {
        quantityInput.value = stock;
    }
}

// Tăng giảm số lượng
function updateQuantity(change) {
    const input = document.getElementById('quantity');
    let newValue = parseInt(input.value) + change;

    // Validate min
    if (newValue < 1) newValue = 1;

    // Validate max (dựa trên tồn kho)
    const stockText = document.getElementById('stockCount').innerText;
    if (stockText !== "--") {
        const maxStock = parseInt(stockText);
        if (newValue > maxStock) {
            alert(`Chỉ còn ${maxStock} sản phẩm trong kho!`);
            newValue = maxStock;
        }
    }

    input.value = newValue;
}

// Thêm vào yêu thích
function toggleWishlist(btn) {
    const icon = btn.querySelector('i');
    if (icon.classList.contains('far')) { // Đang rỗng
        icon.classList.remove('far');
        icon.classList.add('fas'); // Tim đặc (active)
        // TODO: Gọi API lưu wishlist
    } else {
        icon.classList.remove('fas');
        icon.classList.add('far');
        // TODO: Gọi API xóa wishlist
    }
}

// Thêm vào giỏ (sử dụng jQuery AJAX)
function addToCart() {
    if (!selectedSize) {
        alert("Vui lòng chọn kích thước!");
        return;
    }
    const variant = productData.variants[selectedVariantIndex];
    const quantity = parseInt(document.getElementById('quantity').value);
    
    // Lấy stockId từ map stockIdsBySize
    if (!variant.stockIdsBySize || !variant.stockIdsBySize[selectedSize]) {
        alert("Không tìm thấy thông tin sản phẩm!");
        return;
    }
    
    const stockId = variant.stockIdsBySize[selectedSize];
    
    // Kiểm tra tồn kho
    const stock = variant.stockBySize[selectedSize];
    if (stock < quantity) {
        alert(`Chỉ còn ${stock} sản phẩm trong kho!`);
        return;
    }

    // Gọi API thêm vào giỏ hàng
    $.ajax({
        url: '/v1/api/cart/add',
        type: 'POST',
        data: {
            stockId: stockId,
            quantity: quantity
        },
        success: function(data) {
        // Cập nhật số lượng trên icon giỏ hàng nếu có
            const cartBadge = $('.cart-badge, .cart-count');
            if (cartBadge.length && data.totalItems !== undefined) {
                cartBadge.text(data.totalItems);
        }
        
        alert(data.message || 'Đã thêm vào giỏ hàng!');
        },
        error: function(xhr, status, error) {
            alert('Lỗi: ' + (xhr.responseText || error));
        }
    });
}

function initImageZoom() {
    const container = document.querySelector('.main-image-container');
    const img = document.getElementById('mainImage');

    if (!container || !img) return;

    // Khi di chuyển chuột trong vùng ảnh
    container.addEventListener('mousemove', function(e) {
        // 1. Lấy kích thước và vị trí của container
        const { left, top, width, height } = container.getBoundingClientRect();

        // 2. Tính toán vị trí con trỏ chuột so với container
        const x = e.clientX - left;
        const y = e.clientY - top;

        // 3. Tính % vị trí để đặt làm tâm phóng to (transform-origin)
        // Ví dụ: Chuột ở giữa -> 50% 50%
        const xPercent = (x / width) * 100;
        const yPercent = (y / height) * 100;

        // 4. Áp dụng transform
        img.style.transformOrigin = `${xPercent}% ${yPercent}%`;
        img.style.transform = 'scale(2)'; // Phóng to 2 lần (có thể chỉnh số này)
    });

    // Khi chuột rời khỏi ảnh -> Reset về bình thường
    container.addEventListener('mouseleave', function() {
        img.style.transformOrigin = 'center center';
        img.style.transform = 'scale(1)';
    });
}

// Cập nhật lại hàm changeMainImage để reset zoom khi đổi ảnh
const originalChangeMainImage = changeMainImage; // Lưu hàm cũ nếu cần
changeMainImage = function(element) {
    const img = document.getElementById('mainImage');
    img.src = element.src;

    // Reset trạng thái zoom
    img.style.transform = 'scale(1)';

    // Highlight thumbnail (Logic cũ)
    document.querySelectorAll('.thumbnail-img').forEach(el => el.classList.remove('active-thumb'));
    element.classList.add('active-thumb');
}