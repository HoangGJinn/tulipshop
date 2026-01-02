// Rating Display JavaScript - Updated for new UI
document.addEventListener('DOMContentLoaded', function() {
    const ratingsSection = document.getElementById('ratingsSection');
    if (ratingsSection) {
        const productId = ratingsSection.getAttribute('data-product-id');
        if (productId) {
            loadRatings(productId);
        }
    }
});

async function loadRatings(productId) {
    try {
        // Load ratings and statistics
        const [ratingsResponse, statsResponse] = await Promise.all([
            fetch(`/api/ratings/product/${productId}`),
            fetch(`/api/ratings/product/${productId}/statistics`)
        ]);
        
        const ratingsData = await ratingsResponse.json();
        const statsData = await statsResponse.json();
        
        if (ratingsData.success && statsData.success) {
            renderRatings(ratingsData.data, statsData.data);
        } else {
            showEmptyState();
        }
    } catch (error) {
        console.error('Error loading ratings:', error);
        showEmptyState();
    }
}

function renderRatings(ratings, statistics) {
    // Hide loading, show content
    document.getElementById('ratingsLoading').style.display = 'none';
    document.getElementById('ratingsContent').style.display = 'block';
    
    // Update statistics
    document.getElementById('averageRating').textContent = statistics.averageStars.toFixed(1);
    document.getElementById('totalReviews').textContent = statistics.totalRatings;
    document.getElementById('reviewCount').textContent = ratings.length;
    
    // Update stars
    const starsHtml = renderStars(Math.round(statistics.averageStars));
    document.getElementById('averageStars').innerHTML = starsHtml;
    
    // Render review list
    const reviewList = document.getElementById('reviewList');
    if (ratings.length === 0) {
        reviewList.innerHTML = '<div class="text-center py-5 text-muted bg-light rounded"><p>Chưa có đánh giá nào.</p></div>';
    } else {
        reviewList.innerHTML = ratings.map(review => renderReviewItem(review)).join('');
    }
}

function renderStars(count) {
    let html = '';
    for (let i = 0; i < 5; i++) {
        if (i < count) {
            html += '<i class="fas fa-star"></i>';
        } else {
            html += '<i class="far fa-star"></i>';
        }
    }
    return html;
}

function renderReviewItem(review) {
    const stars = renderStars(review.stars);
    const timeAgo = formatTimeAgo(review.createdAt);
    const images = review.imageUrls && review.imageUrls.length > 0 
        ? `<div class="d-flex gap-2">${review.imageUrls.map(url => 
            `<img src="${url}" class="rounded border" style="width: 80px; height: 80px; object-fit: cover; cursor: pointer;" onclick="zoomReviewImage(this)">`
          ).join('')}</div>`
        : '';
    
    // Admin reply section
    const adminReply = review.adminReply ? `
        <div class="mt-3 p-3 bg-light rounded">
            <div class="d-flex align-items-center mb-2">
                <i class="fas fa-store text-primary me-2"></i>
                <strong class="text-primary">Phản hồi từ Tulip Shop</strong>
                <span class="text-muted small ms-2">• ${formatTimeAgo(review.replyTime)}</span>
            </div>
            <p class="mb-0 text-dark" style="font-size: 14px;">${escapeHtml(review.adminReply)}</p>
        </div>
    ` : '';
    
    return `
        <div class="card border-0 shadow-sm mb-3 review-card review-item" data-star="${review.stars}" data-has-media="${review.imageUrls && review.imageUrls.length > 0}">
            <div class="card-body p-4">
                <div class="d-flex justify-content-between align-items-start mb-2">
                    <div class="d-flex align-items-center gap-2">
                        <span class="fw-bold">${escapeHtml(review.userName)}</span>
                        <span class="text-muted small">• ${timeAgo}</span>
                    </div>
                    <div class="text-muted fst-italic small">
                        Đã xác thực từ <span class="fw-bold text-dark fst-normal"><i class="fas fa-shopping-bag me-1"></i> Tulip Shop</span>
                    </div>
                </div>

                <div class="mb-2 text-warning" style="font-size: 14px;">
                    ${stars}
                </div>

                ${review.variantInfo ? `<div class="mb-3 small text-muted">${escapeHtml(review.variantInfo)}</div>` : ''}

                <p class="text-dark mb-3" style="font-size: 14px;">${escapeHtml(review.content)}</p>

                ${images}
                
                ${adminReply}
            </div>
        </div>
    `;
}

function formatTimeAgo(dateString) {
    const date = new Date(dateString);
    const now = new Date();
    
    // Reset time to compare only dates
    const dateOnly = new Date(date.getFullYear(), date.getMonth(), date.getDate());
    const nowOnly = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    
    const diffTime = nowOnly - dateOnly;
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays === 0) return 'Hôm nay';
    if (diffDays === 1) return 'Hôm qua';
    if (diffDays < 7) return `${diffDays} ngày trước`;
    return date.toLocaleDateString('vi-VN');
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showEmptyState() {
    document.getElementById('ratingsLoading').style.display = 'none';
    document.getElementById('ratingsContent').style.display = 'block';
    document.getElementById('reviewList').innerHTML = '<div class="text-center py-5 text-muted bg-light rounded"><p>Chưa có đánh giá nào.</p></div>';
}

function zoomReviewImage(img) {
    // Simple lightbox - you can enhance this
    const modal = document.createElement('div');
    modal.style.cssText = 'position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.9);z-index:9999;display:flex;align-items:center;justify-content:center;cursor:pointer;';
    modal.innerHTML = `<img src="${img.src}" style="max-width:90%;max-height:90%;border-radius:8px;">`;
    modal.onclick = () => modal.remove();
    document.body.appendChild(modal);
}

// Filter functionality
let activeFilters = {
    stars: [],
    hasMedia: false
};

function toggleFilter(value, checkbox) {
    if (value === 'media') {
        activeFilters.hasMedia = checkbox.checked;
    } else {
        const starValue = parseInt(value);
        if (checkbox.checked) {
            activeFilters.stars.push(starValue);
        } else {
            activeFilters.stars = activeFilters.stars.filter(s => s !== starValue);
        }
    }
    
    applyFilters();
}

function applyFilters() {
    const reviewItems = document.querySelectorAll('.review-item');
    let visibleCount = 0;
    
    reviewItems.forEach(item => {
        const stars = parseInt(item.getAttribute('data-star'));
        const hasMedia = item.getAttribute('data-has-media') === 'true';
        
        let shouldShow = true;
        
        // Apply star filter
        if (activeFilters.stars.length > 0) {
            shouldShow = activeFilters.stars.includes(stars);
        }
        
        // Apply media filter
        if (shouldShow && activeFilters.hasMedia) {
            shouldShow = hasMedia;
        }
        
        if (shouldShow) {
            item.style.display = 'block';
            visibleCount++;
        } else {
            item.style.display = 'none';
        }
    });
    
    // Update count
    document.getElementById('reviewCount').textContent = visibleCount;
}
