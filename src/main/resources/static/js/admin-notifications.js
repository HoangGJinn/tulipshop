/**
 * Admin Notifications Management
 * Quản lý gửi thông báo từ Admin
 */

// State
let notifications = [];
let filteredNotifications = [];

// Initialize
document.addEventListener('DOMContentLoaded', function() {
    console.log('🔔 Admin Notifications initialized');
    loadNotifications();
    setupEventListeners();
    updatePreview();
});

/**
 * Load danh sách thông báo đã gửi
 */
async function loadNotifications() {
    try {
        const response = await fetch('/v1/api/admin/notifications?page=0&size=100');
        const result = await response.json();
        
        if (result.status === 'success') {
            notifications = result.data || [];
            filteredNotifications = notifications;
            
            renderNotificationsTable();
            updateStatistics();
        } else {
            console.error('Error loading notifications:', result.message);
        }
        
    } catch (error) {
        console.error('Error loading notifications:', error);
        showError('Không thể tải danh sách thông báo');
    }
}

/**
 * Render bảng thông báo
 */
function renderNotificationsTable() {
    const tbody = document.getElementById('notifications-table-body');
    
    if (filteredNotifications.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="p-20 text-center">
                    <div class="text-gray-400">
                        <i data-lucide="inbox" class="w-12 h-12 mx-auto mb-3 opacity-50"></i>
                        <p class="text-sm font-medium">Chưa có thông báo nào</p>
                        <p class="text-xs mt-1">Nhấn "Tạo thông báo mới" để bắt đầu</p>
                    </div>
                </td>
            </tr>
        `;
        lucide.createIcons();
        return;
    }
    
    tbody.innerHTML = filteredNotifications.map(notif => `
        <tr class="hover:bg-gray-50 transition-colors">
            <td class="px-6 py-4">
                ${getTypeBadge(notif.type)}
            </td>
            <td class="px-6 py-4">
                <div class="font-bold text-sm">${escapeHtml(notif.title)}</div>
            </td>
            <td class="px-6 py-4">
                <div class="text-sm text-gray-600 line-clamp-2">${escapeHtml(notif.content)}</div>
            </td>
            <td class="px-6 py-4">
                ${getTargetBadge(notif.targetType, notif.recipientEmail)}
            </td>
            <td class="px-6 py-4">
                <div class="text-xs text-gray-500">${formatDate(notif.createdAt)}</div>
            </td>
            <td class="px-6 py-4">
                <span class="badge badge-order">Đã gửi</span>
            </td>
            <td class="px-6 py-4">
                <button onclick="deleteNotification(${notif.id})" 
                        class="text-red-600 hover:text-red-800 transition-colors"
                        title="Xóa thông báo">
                    <i data-lucide="trash-2" class="w-4 h-4"></i>
                </button>
            </td>
        </tr>
    `).join('');
    
    lucide.createIcons();
}

/**
 * Update statistics
 */
function updateStatistics() {
    const total = notifications.length;
    // Chỉ đếm PROMOTION và SYSTEM (không đếm ORDER)
    const promotion = notifications.filter(n => n.type === 'PROMOTION').length;
    const system = notifications.filter(n => n.type === 'SYSTEM').length;
    
    document.getElementById('stat-total').textContent = total;
    document.getElementById('stat-promotion').textContent = promotion;
    document.getElementById('stat-system').textContent = system;
}

/**
 * Setup event listeners
 */
function setupEventListeners() {
    // Form inputs - update preview
    const titleInput = document.getElementById('notification-title');
    const contentInput = document.getElementById('notification-content');
    const typeSelect = document.getElementById('notification-type');
    
    if (titleInput) titleInput.addEventListener('input', updatePreview);
    if (contentInput) contentInput.addEventListener('input', updatePreview);
    if (typeSelect) typeSelect.addEventListener('change', updatePreview);
    
    // Search and filters
    const searchInput = document.getElementById('search-input');
    const typeFilter = document.getElementById('type-filter');
    const targetFilter = document.getElementById('target-filter');
    
    if (searchInput) searchInput.addEventListener('input', handleFilter);
    if (typeFilter) typeFilter.addEventListener('change', handleFilter);
    if (targetFilter) targetFilter.addEventListener('change', handleFilter);
}

/**
 * Open create modal
 */
function openCreateModal() {
    const modal = document.getElementById('create-modal');
    if (modal) {
        modal.classList.remove('hidden');
        resetForm();
        updatePreview();
        lucide.createIcons();
    }
}

/**
 * Close create modal
 */
function closeCreateModal() {
    const modal = document.getElementById('create-modal');
    if (modal) {
        modal.classList.add('hidden');
        resetForm();
    }
}

/**
 * Reset form
 */
function resetForm() {
    const form = document.getElementById('notification-form');
    if (form) {
        form.reset();
        toggleRecipientEmail();
    }
}

/**
 * Toggle recipient email field
 */
function toggleRecipientEmail() {
    const targetType = document.querySelector('input[name="targetType"]:checked')?.value;
    const emailGroup = document.getElementById('recipient-email-group');
    const emailInput = document.getElementById('recipient-email');
    
    if (targetType === 'SPECIFIC') {
        emailGroup?.classList.remove('hidden');
        if (emailInput) emailInput.required = true;
    } else {
        emailGroup?.classList.add('hidden');
        if (emailInput) {
            emailInput.required = false;
            emailInput.value = '';
        }
    }
}

/**
 * Update preview
 */
function updatePreview() {
    const title = document.getElementById('notification-title')?.value || 'Tiêu đề thông báo';
    const content = document.getElementById('notification-content')?.value || 'Nội dung thông báo sẽ hiển thị ở đây...';
    const type = document.getElementById('notification-type')?.value || 'ORDER';
    
    const previewTitle = document.querySelector('.preview-title');
    const previewText = document.querySelector('.preview-text');
    const previewIcon = document.querySelector('.preview-icon');
    
    if (previewTitle) previewTitle.textContent = title;
    if (previewText) previewText.textContent = content;
    
    // Update icon color based on type
    if (previewIcon) {
        previewIcon.className = 'preview-icon';
        if (type === 'ORDER') {
            previewIcon.style.backgroundColor = '#10b981';
        } else if (type === 'PROMOTION') {
            previewIcon.style.backgroundColor = '#eab308';
        } else if (type === 'SYSTEM') {
            previewIcon.style.backgroundColor = '#3b82f6';
        }
    }
}

/**
 * Submit notification
 */
async function submitNotification() {
    try {
        // Validate form
        const form = document.getElementById('notification-form');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }
        
        // Get form data
        const type = document.getElementById('notification-type').value;
        const targetType = document.querySelector('input[name="targetType"]:checked').value;
        const recipientEmail = document.getElementById('recipient-email').value;
        const title = document.getElementById('notification-title').value;
        const content = document.getElementById('notification-content').value;
        const targetUrl = document.getElementById('target-url').value;
        const imageFile = document.getElementById('image-file').files[0];
        
        // Validate
        if (!type) {
            showError('Vui lòng chọn loại thông báo');
            return;
        }
        
        if (targetType === 'SPECIFIC' && !recipientEmail) {
            showError('Vui lòng nhập email người nhận');
            return;
        }
        
        // Prepare FormData
        const formData = new FormData();
        formData.append('type', type);
        formData.append('targetType', targetType);
        if (recipientEmail) formData.append('recipientEmail', recipientEmail);
        formData.append('title', title);
        formData.append('content', content);
        if (targetUrl) formData.append('targetUrl', targetUrl);
        if (imageFile) formData.append('imageFile', imageFile);
        
        // Show loading
        const submitBtn = document.querySelector('button[onclick="submitNotification()"]');
        const originalText = submitBtn.innerHTML;
        submitBtn.innerHTML = '<i data-lucide="loader" class="w-4 h-4 animate-spin"></i> Đang gửi...';
        submitBtn.disabled = true;
        lucide.createIcons();
        
        // Send request
        const response = await fetch('/v1/api/admin/notifications/send', {
            method: 'POST',
            body: formData
        });
        
        const result = await response.json();
        
        // Hide loading
        submitBtn.innerHTML = originalText;
        submitBtn.disabled = false;
        lucide.createIcons();
        
        if (response.ok && result.status === 'success') {
            showSuccess(result.message || 'Đã gửi thông báo thành công!');
            closeCreateModal();
            loadNotifications();
        } else {
            showError(result.message || 'Có lỗi xảy ra khi gửi thông báo');
        }
        
    } catch (error) {
        console.error('Error submitting notification:', error);
        showError('Không thể gửi thông báo. Vui lòng thử lại.');
        
        // Hide loading
        const submitBtn = document.querySelector('button[onclick="submitNotification()"]');
        if (submitBtn) {
            submitBtn.innerHTML = '<i data-lucide="send" class="w-4 h-4"></i> Gửi thông báo';
            submitBtn.disabled = false;
            lucide.createIcons();
        }
    }
}

/**
 * Handle filter
 */
function handleFilter() {
    const searchTerm = document.getElementById('search-input')?.value.toLowerCase() || '';
    const typeFilter = document.getElementById('type-filter')?.value || '';
    const targetFilter = document.getElementById('target-filter')?.value || '';
    
    filteredNotifications = notifications.filter(notif => {
        const matchSearch = !searchTerm || 
            notif.title.toLowerCase().includes(searchTerm) ||
            notif.content.toLowerCase().includes(searchTerm);
        
        const matchType = !typeFilter || notif.type === typeFilter;
        const matchTarget = !targetFilter || notif.targetType === targetFilter;
        
        return matchSearch && matchType && matchTarget;
    });
    
    renderNotificationsTable();
}

/**
 * Get type badge HTML
 */
function getTypeBadge(type) {
    const badges = {
        'ORDER': '<span class="badge badge-order">📦 Đơn hàng</span>',
        'PROMOTION': '<span class="badge badge-promotion">🎁 Khuyến mãi</span>',
        'SYSTEM': '<span class="badge badge-system">⚙️ Hệ thống</span>'
    };
    return badges[type] || '<span class="badge">Unknown</span>';
}

/**
 * Get target badge HTML
 */
function getTargetBadge(targetType, email) {
    if (targetType === 'ALL') {
        return '<span class="badge badge-all">Tất cả</span>';
    } else {
        return `<span class="badge badge-specific">${escapeHtml(email)}</span>`;
    }
}

/**
 * Format date
 */
function formatDate(dateString) {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

/**
 * Escape HTML
 */
function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
}

/**
 * Show success message
 */
function showSuccess(message) {
    // Sử dụng toast notification system
    if (typeof window.showNotification === 'function') {
        window.showNotification('success', 'Thành công', message);
    } else {
        alert(message);
    }
}

/**
 * Show error message
 */
function showError(message) {
    // Sử dụng toast notification system
    if (typeof window.showNotification === 'function') {
        window.showNotification('error', 'Lỗi', message);
    } else {
        alert(message);
    }
}

// Close modal when clicking outside
document.addEventListener('click', function(e) {
    const modal = document.getElementById('create-modal');
    if (e.target === modal) {
        closeCreateModal();
    }
});

// Close modal with Escape key
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        closeCreateModal();
    }
});


/**
 * Preview image before upload
 */
function previewImage(input) {
    const preview = document.getElementById('image-preview');
    const previewImg = document.getElementById('preview-img');
    
    if (input.files && input.files[0]) {
        const reader = new FileReader();
        
        reader.onload = function(e) {
            previewImg.src = e.target.result;
            preview.classList.remove('hidden');
        };
        
        reader.readAsDataURL(input.files[0]);
    }
}

/**
 * Remove image
 */
function removeImage() {
    const input = document.getElementById('image-file');
    const preview = document.getElementById('image-preview');
    
    input.value = '';
    preview.classList.add('hidden');
}

/**
 * Delete notification
 */
async function deleteNotification(id) {
    // Confirm before delete
    if (!confirm('Bạn có chắc chắn muốn xóa thông báo này?')) {
        return;
    }
    
    try {
        const response = await fetch(`/v1/api/admin/notifications/${id}`, {
            method: 'DELETE'
        });
        
        const result = await response.json();
        
        if (response.ok && result.status === 'success') {
            showSuccess(result.message || 'Đã xóa thông báo thành công');
            loadNotifications(); // Reload list
        } else {
            showError(result.message || 'Không thể xóa thông báo');
        }
    } catch (error) {
        console.error('Error deleting notification:', error);
        showError('Có lỗi xảy ra khi xóa thông báo');
    }
}
