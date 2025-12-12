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

// Thêm vào giỏ
function addToCart() {
    if (!selectedSize) {
        alert("Vui lòng chọn kích thước!");
        return;
    }
    const variant = productData.variants[selectedVariantIndex];
    const quantity = parseInt(document.getElementById('quantity').value);
    
    // Lấy stockId từ stockBySize map
    const stockInfo = variant.stockBySize[selectedSize];
    if (!stockInfo || !stockInfo.id) {
        alert("Lỗi: Không tìm thấy thông tin tồn kho!");
        return;
    }
    const stockId = stockInfo.id;

    // Disable button và hiển thị loading
    const btn = document.getElementById('addToCartBtn');
    const originalText = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang thêm...';

    // Gọi API thêm vào giỏ hàng
    const formData = new FormData();
    formData.append('stockId', stockId);
    formData.append('quantity', quantity);

    fetch('/v1/api/cart/add', {
        method: 'POST',
        body: formData
    })
        .then(response => {
            if (response.status === 401) {
                return response.text().then(text => {
                    throw new Error(text || 'Vui lòng đăng nhập để mua hàng');
                });
            }
            if (!response.ok) {
                return response.text().then(text => {
                    throw new Error(text || 'Có lỗi xảy ra');
                });
            }
            return response.json();
        })
        .then(data => {
            if (data.status === 'success') {
                // Hiển thị thông báo thành công
                alert(data.message || 'Đã thêm vào giỏ hàng thành công!');
                
                // Cập nhật số lượng trên icon giỏ hàng nếu có
                if (data.totalItems !== undefined) {
                    const cartBadge = document.querySelector('.cart-badge, .cart-count');
                    if (cartBadge) {
                        cartBadge.textContent = data.totalItems;
                        cartBadge.style.display = data.totalItems > 0 ? 'inline' : 'none';
                    }
                }
            } else {
                throw new Error(data.message || 'Có lỗi xảy ra');
            }
        })
        .catch(error => {
            alert('Lỗi: ' + error.message);
        })
        .finally(() => {
            btn.disabled = false;
            btn.innerHTML = originalText;
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

function processTryOn() {
    // 1. Chuẩn bị dữ liệu
    const currentMainImageSrc = document.getElementById('mainImage').src;
    document.getElementById('tryOnClothImg').src = currentMainImageSrc;

    const formData = new FormData();
    formData.append("clothUrl", currentMainImageSrc);

    // Kiểm tra xem user đang ở Tab nào (Upload hay Template)
    const isUploadTab = document.getElementById('upload-tab').classList.contains('active');

    if (isUploadTab) {
        const fileInput = document.getElementById('userImageFile');
        if (fileInput.files.length > 0) {
            formData.append("userImage", fileInput.files[0]);
        } else {
            alert("Vui lòng tải ảnh của bạn lên!");
            return;
        }
    } else {
        const selectedTemplate = document.querySelector('input[name="modelTemplate"]:checked');
        if (selectedTemplate) {
            formData.append("templateUrl", selectedTemplate.value);
        } else {
            alert("Vui lòng chọn một người mẫu!");
            return;
        }
    }

    // 2. UI Loading
    const btn = document.getElementById('btnTryOn');
    const placeholder = document.getElementById('placeholderResult');
    const loading = document.getElementById('loadingAi');
    const resultImg = document.getElementById('resultImage');

    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> ĐANG XỬ LÝ...';

    placeholder.style.display = 'none';
    resultImg.style.display = 'none';
    loading.style.display = 'block';

    // 3. Gửi FormData (Không cần set Content-Type, browser tự làm)
    fetch('/api/virtual-try-on', {
        method: 'POST',
        body: formData
    })
        .then(response => {
            if (!response.ok) throw new Error("Lỗi Server");
            return response.json();
        })
        .then(data => {
            if (data.resultUrl) {
                resultImg.src = data.resultUrl;
                loading.style.display = 'none';
                resultImg.style.display = 'block';
            } else {
                alert("Lỗi: " + (data.error || "Không có kết quả"));
                resetTryOnUI();
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert("Có lỗi xảy ra: " + error.message);
            resetTryOnUI();
        })
        .finally(() => {
            btn.disabled = false;
            btn.innerHTML = 'TIẾN HÀNH THỬ ĐỒ <i class="fas fa-arrow-right ms-2"></i>';
        });
}

function previewUpload(input) {
    if (input.files && input.files[0]) {
        const reader = new FileReader();
        reader.onload = function(e) {
            document.getElementById('uploadPreview').src = e.target.result;
            document.getElementById('uploadPreviewContainer').style.display = 'block';
            document.querySelector('.upload-box').style.display = 'none';
        }
        reader.readAsDataURL(input.files[0]);
    }
}

function clearUpload() {
    document.getElementById('userImageFile').value = "";
    document.getElementById('uploadPreviewContainer').style.display = 'none';
    document.querySelector('.upload-box').style.display = 'block';
}

function resetTryOnUI() {
    document.getElementById('loadingAi').style.display = 'none';
    document.getElementById('placeholderResult').style.display = 'block';
}