// Biến toàn cục
let selectedVariantIndex = 0;
let selectedSize = null;

document.addEventListener('DOMContentLoaded', function() {
    // 1. Khởi tạo: Chọn màu đầu tiên
    const firstColorOption = document.querySelector('.color-swatch');
    if (firstColorOption) {
        selectColor(firstColorOption);
    }

    // 2. Khởi tạo Zoom ảnh
    initImageZoom();
});

// --- LOGIC XỬ LÝ ẢNH ---
function changeMainImage(element) {
    const mainImg = document.getElementById('mainImage');
    mainImg.src = element.src;

    // Reset zoom
    mainImg.style.transform = 'scale(1)';

    // Highlight thumbnail
    document.querySelectorAll('.thumbnail-img').forEach(el => el.classList.remove('active-thumb'));
    element.classList.add('active-thumb');
}

function initImageZoom() {
    const container = document.querySelector('.main-image-container');
    const img = document.getElementById('mainImage');

    if (!container || !img) return;

    container.addEventListener('mousemove', function(e) {
        const { left, top, width, height } = container.getBoundingClientRect();
        const x = e.clientX - left;
        const y = e.clientY - top;

        // Tính % vị trí chuột
        const xPercent = (x / width) * 100;
        const yPercent = (y / height) * 100;

        img.style.transformOrigin = `${xPercent}% ${yPercent}%`;
        img.style.transform = 'scale(2)'; // Phóng to 2x
    });

    container.addEventListener('mouseleave', function() {
        img.style.transformOrigin = 'center center';
        img.style.transform = 'scale(1)';
    });
}

// --- LOGIC CHỌN MÀU & SIZE ---

function selectColor(element) {
    // 1. UI Active
    document.querySelectorAll('.color-swatch').forEach(el => el.classList.remove('active'));
    element.classList.add('active');

    // 2. Lấy dữ liệu Variant
    selectedVariantIndex = element.getAttribute('data-index');
    const variant = productData.variants[selectedVariantIndex];

    // 3. Cập nhật ảnh
    updateGallery(variant.images);

    // 4. Reset Size & Cập nhật nút Size (Disable nếu hết hàng)
    selectedSize = null;
    document.querySelectorAll('.btn-size').forEach(btn => btn.classList.remove('active'));
    updateSizeAvailability(variant.stockBySize);

    // 5. Reset hiển thị tồn kho
    document.getElementById('quantity').value = 1;
    updateStockDisplay("--");
}

function updateGallery(images) {
    const mainImg = document.getElementById('mainImage');
    const container = document.querySelector('.thumbnail-list');

    if(images && images.length > 0) {
        mainImg.src = images[0];
    }

    if(container) {
        container.innerHTML = '';
        images.forEach((img, index) => {
            const thumb = document.createElement('img');
            thumb.src = img;
            thumb.className = `img-fluid thumbnail-img ${index === 0 ? 'active-thumb' : ''}`;
            thumb.onclick = () => changeMainImage(thumb);
            container.appendChild(thumb);
        });
    }
}

// Hàm kiểm tra tồn kho để disable nút size
function updateSizeAvailability(stockMap) {
    document.querySelectorAll('.btn-size').forEach(btn => {
        const size = btn.getAttribute('data-size');
        const stockInfo = stockMap[size];

        // 👇 FIX LỖI OBJECT: Lấy số lượng an toàn
        let quantity = 0;
        if (typeof stockInfo === 'object' && stockInfo !== null) {
            quantity = stockInfo.quantity;
        } else if (typeof stockInfo === 'number') {
            quantity = stockInfo;
        }

        if (quantity <= 0) {
            btn.disabled = true;
            btn.classList.add('disabled');
            btn.style.opacity = '0.5';
            btn.style.cursor = 'not-allowed';
        } else {
            btn.disabled = false;
            btn.classList.remove('disabled');
            btn.style.opacity = '1';
            btn.style.cursor = 'pointer';
        }
    });
    document.getElementById('stockMessage').style.display = 'none';
}

function selectSize(element) {
    if (element.disabled) return;

    // UI Active
    document.querySelectorAll('.btn-size').forEach(btn => btn.classList.remove('active'));
    element.classList.add('active');

    selectedSize = element.getAttribute('data-size');

    // Lấy tồn kho
    const variant = productData.variants[selectedVariantIndex];
    const stockInfo = variant.stockBySize[selectedSize];

    // 👇 FIX LỖI [object Object]: Trích xuất số lượng
    let quantity = 0;
    if (typeof stockInfo === 'object' && stockInfo !== null) {
        quantity = stockInfo.quantity;
    } else if (typeof stockInfo === 'number') {
        quantity = stockInfo;
    }

    updateStockDisplay(quantity);
}

function updateStockDisplay(stock) {
    const stockCountEl = document.getElementById('stockCount');
    const msg = document.getElementById('stockMessage');
    const quantityInput = document.getElementById('quantity');

    // Hiển thị số lượng
    stockCountEl.innerText = stock;

    // Cảnh báo nếu sắp hết hàng
    if (stock !== "--" && stock < 10 && stock > 0) {
        msg.style.display = 'block';
        msg.innerText = `Chỉ còn ${stock} sản phẩm, nhanh tay kẻo hết!`;
    } else {
        msg.style.display = 'none';
    }

    // Reset input số lượng nếu đang nhập quá tồn kho
    if (stock !== "--" && parseInt(quantityInput.value) > stock) {
        quantityInput.value = stock;
    }
}

// --- LOGIC GIỎ HÀNG & SỐ LƯỢNG ---

function updateQuantity(change) {
    const input = document.getElementById('quantity');
    let newValue = parseInt(input.value) + change;

    // Min = 1
    if (newValue < 1) newValue = 1;

    // Max = Tồn kho hiện tại
    const stockText = document.getElementById('stockCount').innerText;
    if (stockText !== "--") {
        const maxStock = parseInt(stockText);
        if (newValue > maxStock) {
            // Hiệu ứng rung hoặc thông báo nhỏ
            input.classList.add('is-invalid');
            setTimeout(() => input.classList.remove('is-invalid'), 500);
            newValue = maxStock;
        }
    }
    input.value = newValue;
}

function addToCart() {
    if (!selectedSize) {
        showWarning('Chưa chọn kích thước', 'Vui lòng chọn size bạn muốn mua!');
        return;
    }

    const variant = productData.variants[selectedVariantIndex];
    const quantity = parseInt(document.getElementById('quantity').value);
    const stockInfo = variant.stockBySize[selectedSize];

    // Lấy Stock ID để gửi về server
    let stockId = null;
    if (typeof stockInfo === 'object' && stockInfo !== null) {
        stockId = stockInfo.id; // Nếu backend gửi object {id:..., quantity:...}
    } else {
        // Trường hợp backend chỉ gửi số, ta không lấy được ID ở đây.
        // Bạn cần đảm bảo backend gửi Object Stock hoặc có logic khác.
        // Tạm thời alert lỗi nếu không có ID
        console.error("Thiếu Stock ID", stockInfo);
        showError('Lỗi', 'Không tìm thấy thông tin sản phẩm trong kho');
        return;
    }

    // UI Loading
    const btn = document.getElementById('addToCartBtn');
    const originalText = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> ĐANG THÊM...';

    const formData = new FormData();
    formData.append('stockId', stockId);
    formData.append('quantity', quantity);

    fetch(window.API_BASE_URL + '/cart/add', {
        method: 'POST',
        body: formData,
        headers: {
            'Accept': 'application/json'
        },
        credentials: 'include' // Để gửi cookie
    })
        .then(async response => {
            // Kiểm tra content-type để đảm bảo là JSON
            const contentType = response.headers.get('content-type');
            const isJson = contentType && contentType.includes('application/json');
            
            // Xử lý response 401 (chưa đăng nhập)
            if (response.status === 401) {
                let errorMessage = 'Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng';
                if (isJson) {
                    try {
                        const data = await response.json();
                        if (data.message) {
                            errorMessage = data.message;
                        }
                    } catch (e) {
                        console.error('Error parsing JSON:', e);
                    }
                }
                throw new Error('LOGIN_REQUIRED:' + errorMessage);
            }
            
            if (!response.ok) {
                let errorMessage = 'Có lỗi xảy ra';
                if (isJson) {
                    try {
                        const data = await response.json();
                        if (data.message) {
                            errorMessage = data.message;
                        }
                    } catch (e) {
                        console.error('Error parsing JSON:', e);
                    }
                } else {
                    // Nếu không phải JSON, có thể là HTML redirect
                    errorMessage = 'Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng';
                    throw new Error('LOGIN_REQUIRED:' + errorMessage);
                }
                throw new Error(errorMessage);
            }
            
            if (!isJson) {
                throw new Error('LOGIN_REQUIRED:Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng');
            }
            
            return response.json();
        })
        .then(data => {
            if (data.status === 'success') {
                showSuccess('Đã thêm vào giỏ!', 'Sản phẩm đã được thêm vào giỏ hàng của bạn');
                // Update cart count badge (nếu có)
                updateCartBadge(data.totalItems);
            } else {
                throw new Error(data.message || 'Có lỗi xảy ra');
            }
        })
        .catch(error => {
            if (error.message && error.message.startsWith('LOGIN_REQUIRED:')) {
                const message = error.message.replace('LOGIN_REQUIRED:', '');
                Swal.fire({
                    icon: 'info',
                    title: 'Yêu cầu đăng nhập',
                    text: message || 'Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng',
                    confirmButtonText: 'Đăng nhập ngay'
                }).then((result) => {
                    if (result.isConfirmed) window.location.href = '/login';
                });
            } else {
                showError('Lỗi', error.message || 'Có lỗi xảy ra');
            }
        })
        .finally(() => {
            btn.disabled = false;
            btn.innerHTML = originalText;
        });
}

function updateCartBadge(count) {
    const badges = document.querySelectorAll('.cart-badge, .cart-count');
    badges.forEach(el => {
        el.innerText = count;
        el.style.display = count > 0 ? 'inline-block' : 'none';
    });
}

// --- CÁC HÀM PHỤ TRỢ (Review, Wishlist...) ---

function toggleWishlist(btn) {
    const icon = btn.querySelector('i');
    if (icon.classList.contains('far')) {
        icon.classList.remove('far');
        icon.classList.add('fas', 'text-danger');
        icon.classList.remove('text-dark');
        // Call API Add Wishlist here
    } else {
        icon.classList.remove('fas', 'text-danger');
        icon.classList.add('far', 'text-dark');
        // Call API Remove Wishlist here
    }
}

function previewColor(element) {
    const newSrc = element.getAttribute('data-img');
    const targetId = element.getAttribute('data-target');
    const targetImg = document.getElementById(targetId);
    if (targetImg && newSrc) targetImg.src = newSrc;
}

// Hàm zoom ảnh review
function zoomReviewImage(img) {
    const modalImg = document.getElementById('reviewImageZoom');
    const modalEl = document.getElementById('reviewImageModal');
    if (modalImg && modalEl) {
        modalImg.src = img.src;
        const myModal = new bootstrap.Modal(modalEl);
        myModal.show();
    }
}

// Hàm lọc review
function toggleFilter(filterType, checkboxInput) {
    if (checkboxInput.checked) {
        document.querySelectorAll('.filter-cb').forEach(cb => {
            if (cb !== checkboxInput) cb.checked = false;
        });
    }

    const finalType = checkboxInput.checked ? filterType : 'all';
    const reviews = document.querySelectorAll('.review-item');

    reviews.forEach(review => {
        const starAttr = review.getAttribute('data-star');
        const mediaAttr = review.getAttribute('data-has-media');
        const starRating = parseInt(starAttr);
        const hasMedia = (mediaAttr === 'true');

        let shouldShow = false;
        if (finalType === 'all') shouldShow = true;
        else if (finalType === 'media') shouldShow = hasMedia;
        else shouldShow = (starRating === parseInt(finalType));

        if (shouldShow) {
            review.style.display = 'block';
            review.classList.remove('animate__fadeIn');
            void review.offsetWidth;
            review.classList.add('animate__animated', 'animate__fadeIn');
        } else {
            review.style.display = 'none';
        }
    });
}