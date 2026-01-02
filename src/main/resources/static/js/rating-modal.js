// Rating Modal JavaScript
class RatingModal {
    constructor() {
        this.modal = null;
        this.currentRating = 0;
        this.selectedImages = [];
        this.maxImages = 5;
        this.productData = null;
        this.orderId = null;
    }

    /**
     * Mở modal đánh giá
     */
    open(productData, orderId) {
        this.productData = productData;
        this.orderId = orderId;
        this.currentRating = 0;
        this.selectedImages = [];
        
        this.createModal();
        this.modal.show();
    }

    /**
     * Tạo HTML cho modal
     */
    createModal() {
        // Xóa modal cũ nếu có
        const existingModal = document.getElementById('ratingModal');
        if (existingModal) {
            existingModal.remove();
        }

        const modalHTML = `
            <div class="modal fade rating-modal" id="ratingModal" tabindex="-1">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">
                                <i class="fas fa-star me-2"></i>Đánh giá sản phẩm
                            </h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">
                            <!-- Product Info -->
                            <div class="rating-product-info">
                                <img src="${this.productData.image}" alt="${this.productData.name}">
                                <div class="rating-product-details">
                                    <h6>${this.productData.name}</h6>
                                    ${this.productData.variant ? `<p class="text-muted mb-0">${this.productData.variant}</p>` : ''}
                                </div>
                            </div>

                            <!-- Star Rating -->
                            <div class="star-rating-section">
                                <label>Đánh giá của bạn</label>
                                <div class="star-rating-container" id="starRating">
                                    ${[1, 2, 3, 4, 5].map(i => `
                                        <span class="star" data-rating="${i}">
                                            <i class="fas fa-star"></i>
                                        </span>
                                    `).join('')}
                                </div>
                                <div class="mt-2 text-muted" id="ratingText"></div>
                            </div>

                            <!-- Content -->
                            <div class="rating-content-section">
                                <label for="ratingContent">Chia sẻ trải nghiệm của bạn</label>
                                <textarea 
                                    class="form-control" 
                                    id="ratingContent" 
                                    placeholder="Hãy chia sẻ cảm nhận của bạn về sản phẩm này..."
                                    maxlength="2000"
                                ></textarea>
                                <div class="char-counter">
                                    <span id="charCount">0</span>/2000 ký tự
                                </div>
                            </div>

                            <!-- Image Upload -->
                            <div class="rating-images-section">
                                <label>Thêm hình ảnh (Tùy chọn)</label>
                                <div class="image-upload-area" id="imageUploadArea">
                                    <i class="fas fa-cloud-upload-alt"></i>
                                    <p>Nhấn để chọn ảnh hoặc kéo thả vào đây</p>
                                    <p class="text-muted small mb-0">Tối đa ${this.maxImages} ảnh</p>
                                    <input 
                                        type="file" 
                                        id="imageInput" 
                                        accept="image/*" 
                                        multiple
                                    >
                                </div>
                                <div class="image-preview-grid" id="imagePreviewGrid"></div>
                            </div>

                            <!-- Utility Score Info -->
                            <div class="utility-score-info">
                                <i class="fas fa-lightbulb"></i>
                                <strong>Mẹo:</strong> Đánh giá chi tiết với hình ảnh sẽ được ưu tiên hiển thị và giúp người mua khác nhiều hơn!
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                            <button type="button" class="btn btn-submit-rating" id="submitRatingBtn">
                                <i class="fas fa-paper-plane me-2"></i>Gửi đánh giá
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', modalHTML);
        this.modal = new bootstrap.Modal(document.getElementById('ratingModal'));
        
        this.attachEventListeners();
    }

    /**
     * Gắn event listeners
     */
    attachEventListeners() {
        // Star rating
        const stars = document.querySelectorAll('.star');
        stars.forEach(star => {
            star.addEventListener('click', () => {
                this.setRating(parseInt(star.dataset.rating));
            });
            
            star.addEventListener('mouseenter', () => {
                this.highlightStars(parseInt(star.dataset.rating));
            });
        });

        document.getElementById('starRating').addEventListener('mouseleave', () => {
            this.highlightStars(this.currentRating);
        });

        // Character counter
        const textarea = document.getElementById('ratingContent');
        textarea.addEventListener('input', () => {
            document.getElementById('charCount').textContent = textarea.value.length;
        });

        // Image upload
        const uploadArea = document.getElementById('imageUploadArea');
        const imageInput = document.getElementById('imageInput');

        uploadArea.addEventListener('click', () => imageInput.click());
        imageInput.addEventListener('change', (e) => this.handleImageSelect(e));

        // Drag & drop
        uploadArea.addEventListener('dragover', (e) => {
            e.preventDefault();
            uploadArea.style.borderColor = '#667eea';
        });

        uploadArea.addEventListener('dragleave', () => {
            uploadArea.style.borderColor = '#dee2e6';
        });

        uploadArea.addEventListener('drop', (e) => {
            e.preventDefault();
            uploadArea.style.borderColor = '#dee2e6';
            this.handleImageSelect({ target: { files: e.dataTransfer.files } });
        });

        // Submit button
        document.getElementById('submitRatingBtn').addEventListener('click', () => {
            this.submitRating();
        });
    }

    /**
     * Set rating value
     */
    setRating(rating) {
        this.currentRating = rating;
        this.highlightStars(rating);
        this.updateRatingText(rating);
    }

    /**
     * Highlight stars
     */
    highlightStars(rating) {
        const stars = document.querySelectorAll('.star');
        stars.forEach((star, index) => {
            if (index < rating) {
                star.classList.add('active');
            } else {
                star.classList.remove('active');
            }
        });
    }

    /**
     * Update rating text
     */
    updateRatingText(rating) {
        const texts = {
            1: 'Rất không hài lòng',
            2: 'Không hài lòng',
            3: 'Bình thường',
            4: 'Hài lòng',
            5: 'Rất hài lòng'
        };
        document.getElementById('ratingText').textContent = texts[rating] || '';
    }

    /**
     * Handle image selection
     */
    handleImageSelect(event) {
        const files = Array.from(event.target.files);
        
        if (this.selectedImages.length + files.length > this.maxImages) {
            alert(`Bạn chỉ có thể tải lên tối đa ${this.maxImages} ảnh`);
            return;
        }

        files.forEach(file => {
            if (file.type.startsWith('image/')) {
                this.selectedImages.push(file);
                this.addImagePreview(file);
            }
        });
    }

    /**
     * Add image preview
     */
    addImagePreview(file) {
        const reader = new FileReader();
        reader.onload = (e) => {
            const previewGrid = document.getElementById('imagePreviewGrid');
            const index = this.selectedImages.indexOf(file);
            
            const previewHTML = `
                <div class="image-preview-item" data-index="${index}">
                    <img src="${e.target.result}" alt="Preview">
                    <button class="remove-image" onclick="ratingModal.removeImage(${index})">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
            `;
            
            previewGrid.insertAdjacentHTML('beforeend', previewHTML);
        };
        reader.readAsDataURL(file);
    }

    /**
     * Remove image
     */
    removeImage(index) {
        this.selectedImages.splice(index, 1);
        this.refreshImagePreviews();
    }

    /**
     * Refresh image previews
     */
    refreshImagePreviews() {
        const previewGrid = document.getElementById('imagePreviewGrid');
        previewGrid.innerHTML = '';
        this.selectedImages.forEach(file => this.addImagePreview(file));
    }

    /**
     * Submit rating
     */
    async submitRating() {
        // Validate
        if (this.currentRating === 0) {
            alert('Vui lòng chọn số sao đánh giá');
            return;
        }

        const content = document.getElementById('ratingContent').value.trim();
        if (!content) {
            alert('Vui lòng nhập nội dung đánh giá');
            return;
        }

        // Disable submit button
        const submitBtn = document.getElementById('submitRatingBtn');
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Đang gửi...';

        try {
            // Prepare form data
            const formData = new FormData();
            formData.append('orderId', this.orderId);
            formData.append('productId', this.productData.id);
            formData.append('stars', this.currentRating);
            formData.append('content', content);
            
            if (this.productData.variant) {
                formData.append('variantInfo', this.productData.variant);
            }

            // Append images
            this.selectedImages.forEach((file, index) => {
                formData.append('images', file);
            });

            // Send request
            const response = await fetch('/api/ratings', {
                method: 'POST',
                body: formData
            });

            const result = await response.json();

            if (result.success) {
                // Show success message
                this.showSuccessMessage();
                
                // Close modal
                setTimeout(() => {
                    this.modal.hide();
                    // Reload page to show new rating
                    window.location.reload();
                }, 1500);
            } else {
                alert(result.message || 'Có lỗi xảy ra, vui lòng thử lại');
                submitBtn.disabled = false;
                submitBtn.innerHTML = '<i class="fas fa-paper-plane me-2"></i>Gửi đánh giá';
            }
        } catch (error) {
            console.error('Error submitting rating:', error);
            alert('Có lỗi xảy ra, vui lòng thử lại');
            submitBtn.disabled = false;
            submitBtn.innerHTML = '<i class="fas fa-paper-plane me-2"></i>Gửi đánh giá';
        }
    }

    /**
     * Show success message
     */
    showSuccessMessage() {
        const modalBody = document.querySelector('.rating-modal .modal-body');
        modalBody.innerHTML = `
            <div class="text-center py-5">
                <i class="fas fa-check-circle text-success" style="font-size: 4rem;"></i>
                <h4 class="mt-3">Cảm ơn bạn đã đánh giá!</h4>
                <p class="text-muted">Đánh giá của bạn sẽ giúp ích cho những người mua khác</p>
            </div>
        `;
    }
}

// Initialize global instance
const ratingModal = new RatingModal();
