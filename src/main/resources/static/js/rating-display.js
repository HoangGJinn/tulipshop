// Rating Display JavaScript
class RatingDisplay {
    constructor(productId) {
        this.productId = productId;
        this.ratings = [];
        this.statistics = null;
    }

    /**
     * Load và hiển thị ratings
     */
    async load() {
        try {
            await Promise.all([
                this.loadRatings(),
                this.loadStatistics()
            ]);
            
            this.render();
        } catch (error) {
            console.error('Error loading ratings:', error);
        }
    }

    /**
     * Load danh sách ratings
     */
    async loadRatings() {
        const response = await fetch(`/api/ratings/product/${this.productId}`);
        const result = await response.json();
        
        if (result.success) {
            this.ratings = result.data;
        }
    }

    /**
     * Load thống kê ratings
     */
    async loadStatistics() {
        const response = await fetch(`/api/ratings/product/${this.productId}/statistics`);
        const result = await response.json();
        
        if (result.success) {
            this.statistics = result.data;
        }
    }

    /**
     * Render ratings section
     */
    render() {
        const container = document.getElementById('ratingsSection');
        if (!container) return;

        if (!this.ratings || this.ratings.length === 0) {
            container.innerHTML = this.renderEmptyState();
            return;
        }

        container.innerHTML = `
            <div class="ratings-header">
                <h3><i class="fas fa-star text-warning me-2"></i>Đánh giá sản phẩm</h3>
            </div>
            
            <div class="row mb-4">
                <div class="col-md-4">
                    ${this.renderSummary()}
                </div>
                <div class="col-md-8">
                    ${this.renderBreakdown()}
                </div>
            </div>
            
            <div class="ratings-list">
                ${this.ratings.map(rating => this.renderRatingItem(rating)).join('')}
            </div>
            
            ${this.renderLightbox()}
        `;

        this.attachEventListeners();
    }

    /**
     * Render rating summary
     */
    renderSummary() {
        if (!this.statistics) return '';

        const stars = this.renderStars(Math.round(this.statistics.averageStars));

        return `
            <div class="rating-summary">
                <div class="average-score">${this.statistics.averageStars.toFixed(1)}</div>
                <div class="stars">${stars}</div>
                <div class="total-reviews">${this.statistics.totalRatings} đánh giá</div>
            </div>
        `;
    }

    /**
     * Render rating breakdown
     */
    renderBreakdown() {
        if (!this.statistics) return '';

        const total = this.statistics.totalRatings;
        const breakdown = [
            { stars: 5, count: this.statistics.fiveStars },
            { stars: 4, count: this.statistics.fourStars },
            { stars: 3, count: this.statistics.threeStars },
            { stars: 2, count: this.statistics.twoStars },
            { stars: 1, count: this.statistics.oneStar }
        ];

        return `
            <div class="rating-breakdown">
                ${breakdown.map(item => {
                    const percentage = total > 0 ? (item.count / total * 100) : 0;
                    return `
                        <div class="rating-bar-item">
                            <div class="star-label">
                                <span>${item.stars}</span>
                                <i class="fas fa-star"></i>
                            </div>
                            <div class="progress">
                                <div class="progress-bar" style="width: ${percentage}%"></div>
                            </div>
                            <div class="count">${item.count}</div>
                        </div>
                    `;
                }).join('')}
            </div>
        `;
    }

    /**
     * Render single rating item
     */
    renderRatingItem(rating) {
        const stars = this.renderStars(rating.stars);
        const date = this.formatDate(rating.createdAt);
        const avatar = rating.userAvatar || '/images/default-avatar.png';
        const highQualityClass = rating.isHighQuality ? 'high-quality' : '';
        const highQualityBadge = rating.isHighQuality ? `
            <span class="rating-badge">
                <i class="fas fa-check-circle"></i>
                Đánh giá chất lượng
            </span>
        ` : '';

        return `
            <div class="rating-item ${highQualityClass}">
                <div class="rating-item-header">
                    <img src="${avatar}" alt="${rating.userName}" class="rating-user-avatar" onerror="this.src='/images/default-avatar.png'">
                    <div class="rating-user-info">
                        <div class="rating-user-name">${rating.userName}</div>
                        <div class="rating-stars">${stars}</div>
                    </div>
                    <div class="text-end">
                        ${highQualityBadge}
                        <div class="rating-date">${date}</div>
                    </div>
                </div>
                
                ${rating.variantInfo ? `
                    <div class="rating-variant-info">
                        <i class="fas fa-tag me-1"></i>${rating.variantInfo}
                    </div>
                ` : ''}
                
                <div class="rating-content">${this.escapeHtml(rating.content)}</div>
                
                ${rating.imageUrls && rating.imageUrls.length > 0 ? `
                    <div class="rating-images">
                        ${rating.imageUrls.map(url => `
                            <div class="rating-image-item" onclick="ratingDisplay.openLightbox('${url}')">
                                <img src="${url}" alt="Rating image">
                            </div>
                        `).join('')}
                    </div>
                ` : ''}
            </div>
        `;
    }

    /**
     * Render empty state
     */
    renderEmptyState() {
        return `
            <div class="ratings-empty">
                <i class="fas fa-star-half-alt"></i>
                <h5>Chưa có đánh giá nào</h5>
                <p>Hãy là người đầu tiên đánh giá sản phẩm này!</p>
            </div>
        `;
    }

    /**
     * Render lightbox
     */
    renderLightbox() {
        return `
            <div class="rating-lightbox" id="ratingLightbox" onclick="ratingDisplay.closeLightbox()">
                <button class="close-lightbox" onclick="ratingDisplay.closeLightbox()">
                    <i class="fas fa-times"></i>
                </button>
                <img src="" alt="Rating image" id="lightboxImage">
            </div>
        `;
    }

    /**
     * Render stars
     */
    renderStars(count) {
        let stars = '';
        for (let i = 0; i < 5; i++) {
            if (i < count) {
                stars += '<i class="fas fa-star"></i>';
            } else {
                stars += '<i class="far fa-star"></i>';
            }
        }
        return stars;
    }

    /**
     * Format date
     */
    formatDate(dateString) {
        const date = new Date(dateString);
        const now = new Date();
        const diffTime = Math.abs(now - date);
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

        if (diffDays === 0) {
            return 'Hôm nay';
        } else if (diffDays === 1) {
            return 'Hôm qua';
        } else if (diffDays < 7) {
            return `${diffDays} ngày trước`;
        } else {
            return date.toLocaleDateString('vi-VN');
        }
    }

    /**
     * Escape HTML
     */
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    /**
     * Open lightbox
     */
    openLightbox(imageUrl) {
        const lightbox = document.getElementById('ratingLightbox');
        const image = document.getElementById('lightboxImage');
        
        image.src = imageUrl;
        lightbox.classList.add('active');
    }

    /**
     * Close lightbox
     */
    closeLightbox() {
        const lightbox = document.getElementById('ratingLightbox');
        lightbox.classList.remove('active');
    }

    /**
     * Attach event listeners
     */
    attachEventListeners() {
        // Prevent lightbox close when clicking on image
        const lightboxImage = document.getElementById('lightboxImage');
        if (lightboxImage) {
            lightboxImage.addEventListener('click', (e) => {
                e.stopPropagation();
            });
        }
    }
}

// Global instance
let ratingDisplay = null;

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    const ratingsSection = document.getElementById('ratingsSection');
    if (ratingsSection) {
        const productId = ratingsSection.getAttribute('data-product-id');
        if (productId) {
            ratingDisplay = new RatingDisplay(productId);
            ratingDisplay.load();
        }
    }
});
