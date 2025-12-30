// Biến toàn cục
let selectedVariantIndex = 0;
let selectedSize = null;

document.addEventListener('DOMContentLoaded', function () {
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

    container.addEventListener('mousemove', function (e) {
        const { left, top, width, height } = container.getBoundingClientRect();
        const x = e.clientX - left;
        const y = e.clientY - top;

        // Tính % vị trí chuột
        const xPercent = (x / width) * 100;
        const yPercent = (y / height) * 100;

        img.style.transformOrigin = `${xPercent}% ${yPercent}%`;
        img.style.transform = 'scale(2)'; // Phóng to 2x
    });

    container.addEventListener('mouseleave', function () {
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

    if (images && images.length > 0) {
        mainImg.src = images[0];
    }

    if (container) {
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

function buyNow() {
    if (!selectedSize) {
        showWarning('Chưa chọn kích thước', 'Vui lòng chọn size bạn muốn mua!');
        return;
    }

    const variant = productData.variants[selectedVariantIndex];
    const quantity = parseInt(document.getElementById('quantity').value);
    const stockInfo = variant.stockBySize[selectedSize];

    // Lấy Stock ID
    let stockId = null;
    if (typeof stockInfo === 'object' && stockInfo !== null) {
        stockId = stockInfo.id;
    } else {
        console.error("Thiếu Stock ID", stockInfo);
        showError('Lỗi', 'Không tìm thấy thông tin sản phẩm trong kho');
        return;
    }

    // UI Loading
    // const btn = event.target; // Cần pass event hoặc query selector
    // Tạm thời query button Mua Ngay (chúng ta sẽ add ID cho nó sau)
    const btn = document.getElementById('buyNowBtn');
    const originalText = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> ĐANG XỬ LÝ...';

    const formData = new FormData();
    formData.append('stockId', stockId);
    formData.append('quantity', quantity);

    fetch(window.API_BASE_URL + '/buy-now', {
        method: 'POST',
        body: formData,
        credentials: 'include'
    })
        .then(async response => {
            // Kiểm tra content-type để đảm bảo là JSON
            const contentType = response.headers.get('content-type');
            const isJson = contentType && contentType.includes('application/json');

            // Xử lý response 401 (chưa đăng nhập)
            if (response.status === 401) {
                let errorMessage = 'Vui lòng đăng nhập để mua hàng';
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
                }
                throw new Error(errorMessage);
            }
            return response.json();
        })
        .then(data => {
            if (data.redirectUrl) {
                window.location.href = data.redirectUrl;
            } else {
                showError('Lỗi', 'Không nhận được địa chỉ chuyển hướng');
            }
        })
        .catch(error => {
            if (error.message && error.message.startsWith('LOGIN_REQUIRED:')) {
                const message = error.message.replace('LOGIN_REQUIRED:', '');
                Swal.fire({
                    icon: 'info',
                    title: 'Yêu cầu đăng nhập',
                    text: message || 'Vui lòng đăng nhập để mua hàng',
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

// === EXPANDABLE CONTENT FUNCTIONALITY ===

class ExpandableContentManager {
    constructor() {
        this.sections = new Map();
        this.init();
    }

    init() {
        // Wait for DOM to be fully loaded
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => this.setupSections());
        } else {
            this.setupSections();
        }
    }

    setupSections() {
        document.querySelectorAll('.expandable-section').forEach(section => {
            this.setupSection(section);
        });
    }

    setupSection(section) {
        const content = section.querySelector('.expandable-content');
        const button = section.querySelector('.toggle-btn');
        const maxHeight = parseInt(content.dataset.maxHeight) || 400;

        if (!content || !button) {
            console.warn('Expandable section missing required elements:', section.id);
            return;
        }

        // Check if content needs truncation
        const actualHeight = content.scrollHeight;
        const needsTruncation = actualHeight > maxHeight;

        if (needsTruncation) {
            content.classList.add('collapsed');
            content.style.maxHeight = maxHeight + 'px';
            button.style.display = 'flex';

            // Add click event listener
            button.addEventListener('click', (e) => {
                e.preventDefault();
                this.toggleSection(section);
            });

            // Add keyboard support
            button.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    this.toggleSection(section);
                }
            });
        } else {
            // Content fits within limit, hide button and overlay
            button.style.display = 'none';
            const overlay = section.querySelector('.gradient-overlay');
            if (overlay) {
                overlay.style.display = 'none';
            }
        }

        // Store section data
        this.sections.set(section.id, {
            content,
            button,
            maxHeight,
            isExpanded: false,
            needsTruncation
        });
    }

    toggleSection(section) {
        const sectionData = this.sections.get(section.id);
        if (!sectionData || !sectionData.needsTruncation) return;

        const { content, button, maxHeight } = sectionData;
        const isExpanded = sectionData.isExpanded;

        // Add expanding animation class
        content.classList.add('expanding');

        if (isExpanded) {
            // Collapse
            this.collapseSection(section, sectionData);
        } else {
            // Expand
            this.expandSection(section, sectionData);
        }

        // Remove animation class after transition
        setTimeout(() => {
            content.classList.remove('expanding');
        }, 400);
    }

    expandSection(section, sectionData) {
        const { content, button } = sectionData;

        // Set max-height to actual content height for smooth animation
        content.style.maxHeight = content.scrollHeight + 'px';
        content.classList.remove('collapsed');
        content.classList.add('expanded');

        // Update button
        button.classList.add('expanded');
        button.querySelector('.btn-text').textContent = 'Thu gọn';

        // Update state
        sectionData.isExpanded = true;

        // Smooth scroll to ensure button visibility after expansion
        setTimeout(() => {
            const buttonRect = button.getBoundingClientRect();
            const windowHeight = window.innerHeight;

            // If button is not visible, scroll to it
            if (buttonRect.bottom > windowHeight || buttonRect.top < 0) {
                button.scrollIntoView({
                    behavior: 'smooth',
                    block: 'nearest',
                    inline: 'nearest'
                });
            }
        }, 450); // Wait for expansion animation to complete
    }

    collapseSection(section, sectionData) {
        const { content, button, maxHeight } = sectionData;

        // First set to actual height, then to collapsed height for smooth animation
        content.style.maxHeight = content.scrollHeight + 'px';

        // Force reflow
        content.offsetHeight;

        // Then collapse
        setTimeout(() => {
            content.style.maxHeight = maxHeight + 'px';
            content.classList.remove('expanded');
            content.classList.add('collapsed');
        }, 10);

        // Update button
        button.classList.remove('expanded');
        button.querySelector('.btn-text').textContent = 'Xem thêm';

        // Update state
        sectionData.isExpanded = false;

        // Scroll to section title if it's out of view
        setTimeout(() => {
            const sectionRect = section.getBoundingClientRect();
            if (sectionRect.top < 0) {
                section.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start',
                    inline: 'nearest'
                });
            }
        }, 450);
    }

    // Public method to expand a specific section
    expandSectionById(sectionId) {
        const section = document.getElementById(sectionId);
        if (section && this.sections.has(sectionId)) {
            const sectionData = this.sections.get(sectionId);
            if (!sectionData.isExpanded) {
                this.toggleSection(section);
            }
        }
    }

    // Public method to collapse a specific section
    collapseSectionById(sectionId) {
        const section = document.getElementById(sectionId);
        if (section && this.sections.has(sectionId)) {
            const sectionData = this.sections.get(sectionId);
            if (sectionData.isExpanded) {
                this.toggleSection(section);
            }
        }
    }

    // Public method to check if section is expanded
    isSectionExpanded(sectionId) {
        const sectionData = this.sections.get(sectionId);
        return sectionData ? sectionData.isExpanded : false;
    }
}

// Initialize expandable content manager
let expandableManager;

// Update the existing DOMContentLoaded event listener
document.addEventListener('DOMContentLoaded', function () {
    // 1. Khởi tạo: Chọn màu đầu tiên
    const firstColorOption = document.querySelector('.color-swatch');
    if (firstColorOption) {
        selectColor(firstColorOption);
    }

    // 2. Khởi tạo Zoom ảnh
    initImageZoom();

    // 3. Khởi tạo Expandable Content Manager
    expandableManager = new ExpandableContentManager();
});

// Utility function to handle window resize
let resizeTimeout;
window.addEventListener('resize', function () {
    clearTimeout(resizeTimeout);
    resizeTimeout = setTimeout(() => {
        if (expandableManager) {
            // Re-check content heights on resize
            expandableManager.sections.forEach((sectionData, sectionId) => {
                const section = document.getElementById(sectionId);
                if (section && sectionData.needsTruncation) {
                    const content = sectionData.content;
                    const actualHeight = content.scrollHeight;
                    const maxHeight = sectionData.maxHeight;

                    // Update truncation status
                    const needsTruncation = actualHeight > maxHeight;
                    sectionData.needsTruncation = needsTruncation;

                    const button = sectionData.button;
                    const overlay = section.querySelector('.gradient-overlay');

                    if (needsTruncation) {
                        button.style.display = 'flex';
                        if (overlay) overlay.style.display = 'block';
                    } else {
                        button.style.display = 'none';
                        if (overlay) overlay.style.display = 'none';
                    }
                }
            });
        }
    }, 250);
});

// Export for potential external use
window.ExpandableContentManager = ExpandableContentManager;

// === TABBED PRODUCT INFO FUNCTIONALITY ===

function switchTab(tabName) {
    // Remove active class from all tab headers
    document.querySelectorAll('.tab-header').forEach(header => {
        header.classList.remove('active');
    });

    // Hide all tab panels
    document.querySelectorAll('.tab-panel').forEach(panel => {
        panel.classList.remove('active');
    });

    // Add active class to clicked tab header
    const activeHeader = document.querySelector(`[data-tab="${tabName}"]`);
    if (activeHeader) {
        activeHeader.classList.add('active');
    }

    // Show corresponding tab panel
    const activePanel = document.getElementById(`tab-${tabName}`);
    if (activePanel) {
        activePanel.classList.add('active');
    }
}

// Initialize tabs on page load
document.addEventListener('DOMContentLoaded', function () {
    // Ensure the first tab is active by default
    const firstTab = document.querySelector('.tab-header[data-tab="description"]');
    const firstPanel = document.getElementById('tab-description');

    if (firstTab && firstPanel) {
        firstTab.classList.add('active');
        firstPanel.classList.add('active');
    }

    // Add keyboard support for tabs
    document.querySelectorAll('.tab-header').forEach(header => {
        header.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                const tabName = this.getAttribute('data-tab');
                switchTab(tabName);
            }
        });
    });
});

// Update the existing DOMContentLoaded event listener
document.addEventListener('DOMContentLoaded', function () {
    // 1. Khởi tạo: Chọn màu đầu tiên
    const firstColorOption = document.querySelector('.color-swatch');
    if (firstColorOption) {
        selectColor(firstColorOption);
    }

    // 2. Khởi tạo Zoom ảnh
    initImageZoom();

    // 3. Khởi tạo Expandable Content Manager (legacy)
    expandableManager = new ExpandableContentManager();

    // 4. Initialize tabs (handled in tab section above)
    // Tab initialization is handled in the tab-specific DOMContentLoaded listener
});

// Handle window resize for legacy expandable content
window.addEventListener('resize', function () {
    clearTimeout(resizeTimeout);
    resizeTimeout = setTimeout(() => {
        // Legacy expandable manager resize handling
        if (expandableManager) {
            expandableManager.sections.forEach((sectionData, sectionId) => {
                const section = document.getElementById(sectionId);
                if (section && sectionData.needsTruncation) {
                    const content = sectionData.content;
                    const actualHeight = content.scrollHeight;
                    const maxHeight = sectionData.maxHeight;

                    const needsTruncation = actualHeight > maxHeight;
                    sectionData.needsTruncation = needsTruncation;

                    const button = sectionData.button;
                    const overlay = section.querySelector('.gradient-overlay');

                    if (needsTruncation) {
                        button.style.display = 'flex';
                        if (overlay) overlay.style.display = 'block';
                    } else {
                        button.style.display = 'none';
                        if (overlay) overlay.style.display = 'none';
                    }
                }
            });
        }
    }, 250);
});

// Export for potential external use
window.ExpandableContentManager = ExpandableContentManager;