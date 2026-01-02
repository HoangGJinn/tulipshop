// Admin Ratings Management

let currentPage = 0;
let totalPages = 0;
let currentRatingId = null;

// Load ratings on page load
document.addEventListener('DOMContentLoaded', function() {
    loadRatings();
});

// Load ratings with filters
function loadRatings(page = 0) {
    currentPage = page;
    
    const stars = document.getElementById('filterStars').value;
    const hasReply = document.getElementById('filterReply').value;
    
    let url = `/api/admin/ratings?page=${page}&size=20`;
    if (stars) url += `&stars=${stars}`;
    if (hasReply) url += `&hasReply=${hasReply}`;
    
    fetch(url)
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                renderRatings(data.data);
                updatePagination(data.currentPage, data.totalPages, data.totalElements);
                document.getElementById('totalRatings').textContent = data.totalElements;
            } else {
                showError('Không thể tải dữ liệu đánh giá');
            }
        })
        .catch(error => {
            console.error('Error loading ratings:', error);
            showError('Có lỗi xảy ra khi tải dữ liệu');
        });
}

// Render ratings table
function renderRatings(ratings) {
    const tbody = document.getElementById('ratingsTableBody');
    
    if (ratings.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="p-20 text-center">
                    <div class="text-gray-400">
                        <i data-lucide="inbox" class="w-12 h-12 mx-auto mb-3 opacity-50"></i>
                        <p class="text-sm font-medium">Không có đánh giá nào</p>
                    </div>
                </td>
            </tr>
        `;
        if (typeof lucide !== 'undefined') lucide.createIcons();
        return;
    }
    
    tbody.innerHTML = ratings.map(rating => {
        const isLowRating = rating.stars <= 2;
        const hasReply = rating.adminReply !== null;
        const isVisible = rating.isVisible;
        
        // Chỉ highlight đỏ nếu là đánh giá thấp VÀ chưa phản hồi
        const shouldHighlight = isLowRating && !hasReply;
        
        return `
            <tr class="border-b border-gray-100 hover:bg-gray-50 transition-colors ${shouldHighlight ? 'bg-red-50 border-l-4 border-l-red-500' : ''}">
                <td class="px-6 py-4">
                    <div class="flex items-center gap-3">
                        <img src="${rating.userAvatar || '/images/default-avatar.png'}" 
                             alt="${rating.userName}" 
                             class="w-10 h-10 rounded-full object-cover">
                        <div>
                            <div class="font-medium text-sm">${rating.userName}</div>
                            <div class="text-xs text-gray-500">${formatDate(rating.createdAt)}</div>
                        </div>
                    </div>
                </td>
                <td class="px-6 py-4">
                    <a href="/product/${rating.productId}" target="_blank" 
                       class="text-blue-600 hover:underline font-medium text-sm">
                        ${rating.productName}
                    </a>
                    ${rating.variantInfo ? `<div class="text-xs text-gray-500 mt-1">${rating.variantInfo}</div>` : ''}
                </td>
                <td class="px-6 py-4 text-center">
                    <div class="text-yellow-400 text-lg">
                        ${renderStars(rating.stars)}
                    </div>
                </td>
                <td class="px-6 py-4">
                    <div class="text-sm text-gray-700 line-clamp-2 max-w-xs" title="${rating.content || 'Không có nội dung'}">
                        ${rating.content || '<em class="text-gray-400">Không có nội dung</em>'}
                    </div>
                    ${rating.imageUrls && rating.imageUrls.length > 0 ? `
                        <div class="flex gap-2 mt-2">
                            ${rating.imageUrls.slice(0, 3).map(url => `
                                <img src="${url}" class="w-12 h-12 rounded object-cover cursor-pointer hover:opacity-75" 
                                     onclick="window.open('${url}', '_blank')">
                            `).join('')}
                            ${rating.imageUrls.length > 3 ? `<div class="text-xs text-gray-500 self-center">+${rating.imageUrls.length - 3}</div>` : ''}
                        </div>
                    ` : ''}
                    ${hasReply ? `
                        <div class="mt-2 p-2 bg-green-50 rounded border border-green-200">
                            <div class="text-xs font-bold text-green-700 mb-1">
                                <i data-lucide="check-circle" class="w-3 h-3 inline-block"></i> Đã phản hồi:
                            </div>
                            <div class="text-xs text-gray-700">${rating.adminReply}</div>
                            <div class="text-xs text-gray-500 mt-1">${formatDate(rating.replyTime)}</div>
                        </div>
                    ` : ''}
                </td>
                <td class="px-6 py-4 text-center">
                    <div class="flex flex-col items-center gap-1">
                        <div class="text-xs text-gray-500">Utility Score</div>
                        <div class="text-lg font-bold ${rating.utilityScore >= 40 ? 'text-green-600' : 'text-gray-600'}">
                            ${rating.utilityScore.toFixed(1)}
                        </div>
                        ${rating.utilityScore >= 40 ? '<div class="text-xs text-green-600 font-medium">Chất lượng cao</div>' : ''}
                    </div>
                </td>
                <td class="px-6 py-4 text-center">
                    ${hasReply ? 
                        '<span class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800"><i data-lucide="check-circle" class="w-3 h-3 mr-1"></i> Đã phản hồi</span>' :
                        '<span class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800"><i data-lucide="clock" class="w-3 h-3 mr-1"></i> Chờ phản hồi</span>'
                    }
                    ${!isVisible ? '<div class="mt-1"><span class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-800"><i data-lucide="eye-off" class="w-3 h-3 mr-1"></i> Đã ẩn</span></div>' : ''}
                </td>
                <td class="px-6 py-4 text-center">
                    <div class="flex gap-2 justify-center items-center">
                        <button onclick="openReplyModal(${rating.id}, '${escapeHtml(rating.userName)}', '${escapeHtml(rating.content || '')}', ${rating.stars})" 
                                class="px-3 py-1.5 bg-blue-600 text-white rounded-lg text-xs font-medium hover:bg-blue-700 transition-colors"
                                title="Phản hồi">
                            <i data-lucide="message-square" class="w-4 h-4"></i>
                        </button>
                        <label class="relative inline-flex items-center cursor-pointer">
                            <input type="checkbox" ${isVisible ? 'checked' : ''} 
                                   onchange="toggleVisibility(${rating.id})"
                                   class="sr-only peer"
                                   title="${isVisible ? 'Ẩn đánh giá' : 'Hiện đánh giá'}">
                            <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                        </label>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
    
    // Reinitialize lucide icons
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }
}

// Render stars
function renderStars(count) {
    let stars = '';
    for (let i = 0; i < 5; i++) {
        if (i < count) {
            stars += '<span class="text-yellow-400">★</span>';
        } else {
            stars += '<span class="text-gray-300">★</span>';
        }
    }
    return stars;
}

// Format date
function formatDate(dateString) {
    if (!dateString) return '';
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

// Escape HTML
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Open reply modal
function openReplyModal(ratingId, userName, content, stars) {
    currentRatingId = ratingId;
    
    document.getElementById('originalReview').innerHTML = `
        <div class="mb-2">
            <strong class="text-sm">${userName}</strong> - ${renderStars(stars)}
        </div>
        <div class="text-sm text-gray-700">${content || '<em class="text-gray-400">Không có nội dung</em>'}</div>
    `;
    
    document.getElementById('replyContent').value = '';
    
    const modal = document.getElementById('replyModal');
    modal.classList.remove('hidden');
    modal.classList.add('flex');
    
    // Reinitialize lucide icons
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }
}

// Close reply modal
function closeReplyModal() {
    const modal = document.getElementById('replyModal');
    modal.classList.add('hidden');
    modal.classList.remove('flex');
    
    // Reset AI suggestions
    document.getElementById('aiSuggestions').classList.add('hidden');
    document.getElementById('aiSuggestionsContent').innerHTML = '';
    resetAISuggestButton();
}

// Generate AI suggestions
function generateAISuggestions() {
    if (!currentRatingId) {
        showError('Không tìm thấy đánh giá');
        return;
    }
    
    const btn = document.getElementById('aiSuggestBtn');
    const btnText = document.getElementById('aiSuggestBtnText');
    const suggestionsContainer = document.getElementById('aiSuggestions');
    const suggestionsContent = document.getElementById('aiSuggestionsContent');
    
    // Show loading state
    btn.disabled = true;
    btn.classList.add('opacity-75', 'cursor-not-allowed');
    btnText.innerHTML = '<i data-lucide="loader" class="w-4 h-4 animate-spin inline-block"></i> Đang tạo gợi ý...';
    if (typeof lucide !== 'undefined') lucide.createIcons();
    
    // Call API
    fetch(`/api/admin/ratings/${currentRatingId}/suggest-reply`, {
        method: 'POST'
    })
    .then(response => response.json())
    .then(data => {
        if (data.success && data.suggestions) {
            // Render suggestions
            renderAISuggestions(data.suggestions);
            suggestionsContainer.classList.remove('hidden');
            
            // Change button text
            btnText.textContent = 'Tạo gợi ý mới';
        } else {
            showError(data.message || 'Không thể tạo gợi ý');
        }
    })
    .catch(error => {
        console.error('Error generating AI suggestions:', error);
        showError('AI đang bận, vui lòng thử lại sau');
    })
    .finally(() => {
        resetAISuggestButton();
    });
}

// Render AI suggestions
function renderAISuggestions(suggestions) {
    const container = document.getElementById('aiSuggestionsContent');
    
    const colors = [
        { bg: 'bg-green-50', border: 'border-green-200', text: 'text-green-700', hover: 'hover:bg-green-100' },
        { bg: 'bg-blue-50', border: 'border-blue-200', text: 'text-blue-700', hover: 'hover:bg-blue-100' },
        { bg: 'bg-purple-50', border: 'border-purple-200', text: 'text-purple-700', hover: 'hover:bg-purple-100' }
    ];
    
    container.innerHTML = suggestions.map((suggestion, index) => {
        const color = colors[index % colors.length];
        // Store text in data attribute to avoid escaping issues
        return `
            <button type="button" 
                    data-suggestion-text="${escapeHtml(suggestion.text)}"
                    onclick="selectAISuggestion(this.getAttribute('data-suggestion-text'))"
                    class="w-full text-left p-3 ${color.bg} border ${color.border} rounded-lg ${color.hover} transition-all cursor-pointer group">
                <div class="flex items-start gap-2">
                    <div class="flex-shrink-0 mt-0.5">
                        <i data-lucide="message-circle" class="w-4 h-4 ${color.text}"></i>
                    </div>
                    <div class="flex-1">
                        <div class="text-xs font-bold ${color.text} mb-1">${suggestion.type}</div>
                        <div class="text-sm text-gray-700">${suggestion.text}</div>
                    </div>
                    <div class="flex-shrink-0 opacity-0 group-hover:opacity-100 transition-opacity">
                        <i data-lucide="arrow-right" class="w-4 h-4 ${color.text}"></i>
                    </div>
                </div>
            </button>
        `;
    }).join('');
    
    // Reinitialize lucide icons
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }
}

// Select AI suggestion
function selectAISuggestion(text) {
    const textarea = document.getElementById('replyContent');
    textarea.value = text;
    textarea.focus();
    
    // Visual feedback
    textarea.classList.add('ring-2', 'ring-green-500');
    setTimeout(() => {
        textarea.classList.remove('ring-2', 'ring-green-500');
    }, 1000);
}

// Reset AI suggest button
function resetAISuggestButton() {
    const btn = document.getElementById('aiSuggestBtn');
    const btnText = document.getElementById('aiSuggestBtnText');
    
    btn.disabled = false;
    btn.classList.remove('opacity-75', 'cursor-not-allowed');
    btnText.innerHTML = '<i data-lucide="sparkles" class="w-4 h-4 inline-block"></i> Dùng AI gợi ý';
    
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }
}

// Submit reply
function submitReply() {
    const replyContent = document.getElementById('replyContent').value.trim();
    
    if (!replyContent) {
        showError('Vui lòng nhập nội dung phản hồi');
        return;
    }
    
    fetch(`/api/admin/ratings/${currentRatingId}/reply`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ reply: replyContent })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showSuccess(data.message);
            closeReplyModal();
            loadRatings(currentPage);
        } else {
            showError(data.message);
        }
    })
    .catch(error => {
        console.error('Error submitting reply:', error);
        showError('Có lỗi xảy ra khi gửi phản hồi');
    });
}

// Toggle visibility
function toggleVisibility(ratingId) {
    fetch(`/api/admin/ratings/${ratingId}/toggle-visibility`, {
        method: 'POST'
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showSuccess(data.message);
            loadRatings(currentPage);
        } else {
            showError(data.message);
        }
    })
    .catch(error => {
        console.error('Error toggling visibility:', error);
        showError('Có lỗi xảy ra');
    });
}

// Update pagination
function updatePagination(current, total, totalElements) {
    totalPages = total;
    
    document.getElementById('showingFrom').textContent = current * 20 + 1;
    document.getElementById('showingTo').textContent = Math.min((current + 1) * 20, totalElements);
    document.getElementById('showingTotal').textContent = totalElements;
    
    const pagination = document.getElementById('pagination');
    let html = '';
    
    // Previous button
    html += `
        <button onclick="loadRatings(${current - 1}); return false;" 
                ${current === 0 ? 'disabled' : ''}
                class="px-3 py-1 border border-gray-300 rounded-lg text-sm font-medium hover:bg-gray-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
            <i data-lucide="chevron-left" class="w-4 h-4"></i>
        </button>
    `;
    
    // Page numbers
    for (let i = 0; i < total; i++) {
        if (i === 0 || i === total - 1 || (i >= current - 2 && i <= current + 2)) {
            html += `
                <button onclick="loadRatings(${i}); return false;"
                        class="px-3 py-1 border rounded-lg text-sm font-medium transition-colors ${i === current ? 'bg-black text-white border-black' : 'border-gray-300 hover:bg-gray-50'}">
                    ${i + 1}
                </button>
            `;
        } else if (i === current - 3 || i === current + 3) {
            html += '<span class="px-2 text-gray-400">...</span>';
        }
    }
    
    // Next button
    html += `
        <button onclick="loadRatings(${current + 1}); return false;"
                ${current === total - 1 ? 'disabled' : ''}
                class="px-3 py-1 border border-gray-300 rounded-lg text-sm font-medium hover:bg-gray-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
            <i data-lucide="chevron-right" class="w-4 h-4"></i>
        </button>
    `;
    
    pagination.innerHTML = html;
    
    // Reinitialize lucide icons
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }
}

// Reset filters
function resetFilters() {
    document.getElementById('filterStars').value = '';
    document.getElementById('filterReply').value = '';
    loadRatings(0);
}

// Show success message
function showSuccess(message) {
    // Use existing toast notification if available
    if (typeof showToast === 'function') {
        showToast(message, 'success');
    } else {
        alert(message);
    }
}

// Show error message
function showError(message) {
    // Use existing toast notification if available
    if (typeof showToast === 'function') {
        showToast(message, 'error');
    } else {
        alert(message);
    }
}
