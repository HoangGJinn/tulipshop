/**
 * Refresh dashboard stats (chỉ ngày hôm nay)
 */
async function refreshDashboardStats() {
    const btn = document.getElementById('refreshStatsBtn');
    if (!btn) {
        console.error('Button #refreshStatsBtn not found!');
        return;
    }
    
    const icon = btn.querySelector('[data-lucide="refresh-cw"]');
    
    try {
        // Disable button và add spinning animation
        btn.disabled = true;
        if (icon) {
            icon.classList.add('animate-spin');
        }
        
        // Call API - trả về DashboardStatsDTO
        const response = await fetch('/v1/api/admin/revenue-stats/refresh-today', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include'
        });
        
        if (response.ok) {
            const stats = await response.json();
            
            // Update UI với dữ liệu mới
            updateDashboardUI(stats);
            
            // Show success message
            showNotification('Đã cập nhật dữ liệu thành công!', 'success');
        } else {
            const errorText = await response.text();
            console.error('Error refreshing stats:', response.status, errorText);
            showNotification('Không thể làm mới dữ liệu: ' + response.status, 'error');
        }
    } catch (error) {
        console.error('Error:', error);
        showNotification('Lỗi khi làm mới dữ liệu: ' + error.message, 'error');
    } finally {
        // Re-enable button
        btn.disabled = false;
        if (icon) {
            icon.classList.remove('animate-spin');
        }
    }
}

/**
 * Update dashboard UI with new stats
 */
function updateDashboardUI(stats) {
    // Update revenue
    const revenueElement = document.querySelector('[data-stat="revenue"]');
    if (revenueElement && stats.todayRevenue !== null) {
        revenueElement.textContent = formatCurrency(stats.todayRevenue);
    }
    
    const revenueChangeElement = document.querySelector('[data-stat="revenue-change"]');
    if (revenueChangeElement && stats.todayGrowthPercent !== null) {
        const changeText = `${stats.todayGrowthPercent >= 0 ? '+' : ''}${stats.todayGrowthPercent.toFixed(1)}%`;
        revenueChangeElement.querySelector('span').textContent = changeText;
        revenueChangeElement.className = stats.todayGrowthPercent >= 0 
            ? 'flex items-center font-medium px-1.5 py-0.5 rounded text-green-600 bg-green-50' 
            : 'flex items-center font-medium px-1.5 py-0.5 rounded text-red-600 bg-red-50';
    }
    
    // Update orders
    const ordersElement = document.querySelector('[data-stat="orders"]');
    if (ordersElement && stats.newOrders !== null) {
        ordersElement.textContent = stats.newOrders;
    }
    
    const ordersChangeElement = document.querySelector('[data-stat="orders-change"]');
    if (ordersChangeElement && stats.ordersGrowthPercent !== null) {
        ordersChangeElement.textContent = `${stats.ordersGrowthPercent >= 0 ? '+' : ''}${stats.ordersGrowthPercent.toFixed(1)}%`;
        ordersChangeElement.className = stats.ordersGrowthPercent >= 0 ? 'text-xs font-medium text-blue-600' : 'text-xs font-medium text-red-600';
    }
    
    // Update customers
    const customersElement = document.querySelector('[data-stat="customers"]');
    if (customersElement && stats.newCustomers !== null) {
        customersElement.textContent = stats.newCustomers;
    }
    
    const customersChangeElement = document.querySelector('[data-stat="customers-change"]');
    if (customersChangeElement && stats.customersGrowthPercent !== null) {
        customersChangeElement.textContent = `${stats.customersGrowthPercent >= 0 ? '+' : ''}${stats.customersGrowthPercent.toFixed(1)}%`;
        customersChangeElement.className = stats.customersGrowthPercent >= 0 ? 'text-xs font-medium text-green-600' : 'text-xs font-medium text-red-600';
    }
    
    // Update low stock
    const lowStockElement = document.querySelector('[data-stat="low-stock"]');
    if (lowStockElement && stats.lowStockCount !== null) {
        lowStockElement.textContent = stats.lowStockCount;
    }
    
    // Update confirmed orders
    const confirmedOrdersElement = document.querySelector('[data-stat="confirmed-orders"]');
    if (confirmedOrdersElement && stats.confirmedOrders !== null) {
        confirmedOrdersElement.textContent = stats.confirmedOrders;
    }
}

/**
 * Format currency (VND)
 */
function formatCurrency(amount) {
    if (amount >= 1000000000) {
        return (amount / 1000000000).toFixed(1) + ' tỷ';
    } else if (amount >= 1000000) {
        return (amount / 1000000).toFixed(1) + ' triệu';
    } else if (amount >= 1000) {
        return (amount / 1000).toFixed(0) + 'k';
    }
    return amount + 'đ';
}

/**
 * Show notification
 */
function showNotification(message, type = 'success') {
    // Tạo notification element
    const notification = document.createElement('div');
    notification.className = `fixed top-4 right-4 px-6 py-3 rounded-lg shadow-lg z-50 transition-all ${
        type === 'success' ? 'bg-green-500 text-white' : 'bg-red-500 text-white'
    }`;
    notification.textContent = message;
    
    document.body.appendChild(notification);
    
    // Auto remove sau 3 giây
    setTimeout(() => {
        notification.style.opacity = '0';
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

/**
 * Dashboard Real-time Clock and Date
 */

function updateClock() {
    const now = new Date();
    
    // Format time HH:MM:SS
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const seconds = String(now.getSeconds()).padStart(2, '0');
    const timeString = `${hours}:${minutes}:${seconds}`;
    
    // Update clock display
    const clockElement = document.getElementById('digital-clock');
    if (clockElement) {
        clockElement.textContent = timeString;
    }
    
    // Format date - Vietnamese style
    const days = ['Chủ nhật', 'Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy'];
    const dayName = days[now.getDay()];
    const day = String(now.getDate()).padStart(2, '0');
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const year = now.getFullYear();
    const dateString = `${dayName}, ${day}/${month}/${year}`;
    
    // Update date display
    const dateElement = document.getElementById('current-date');
    if (dateElement) {
        dateElement.textContent = dateString;
    }
}

/**
 * Animate numbers when they change
 */
function animateNumber(element, start, end, duration = 1000) {
    const startTime = performance.now();
    const isDecimal = end % 1 !== 0;
    
    function update(currentTime) {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1);
        
        // Easing function
        const easeOutQuart = 1 - Math.pow(1 - progress, 4);
        const current = start + (end - start) * easeOutQuart;
        
        if (isDecimal) {
            element.textContent = current.toFixed(1);
        } else {
            element.textContent = Math.floor(current).toLocaleString('vi-VN');
        }
        
        if (progress < 1) {
            requestAnimationFrame(update);
        } else {
            element.textContent = isDecimal ? end.toFixed(1) : end.toLocaleString('vi-VN');
        }
    }
    
    requestAnimationFrame(update);
}

/**
 * Add hover effects to stat cards
 */
function initializeStatCards() {
    const statCards = document.querySelectorAll('.stat-card');
    
    statCards.forEach(card => {
        // Add ripple effect on click
        card.addEventListener('click', function(e) {
            const ripple = document.createElement('div');
            ripple.className = 'ripple';
            ripple.style.left = e.clientX - card.offsetLeft + 'px';
            ripple.style.top = e.clientY - card.offsetTop + 'px';
            card.appendChild(ripple);
            
            setTimeout(() => ripple.remove(), 600);
        });
    });
}

/**
 * Initialize tooltips for stat cards
 */
function initializeTooltips() {
    const cards = document.querySelectorAll('[data-tooltip]');
    
    cards.forEach(card => {
        card.addEventListener('mouseenter', function() {
            const tooltip = this.querySelector('.stat-tooltip');
            if (tooltip) {
                tooltip.style.opacity = '1';
            }
        });
        
        card.addEventListener('mouseleave', function() {
            const tooltip = this.querySelector('.stat-tooltip');
            if (tooltip) {
                tooltip.style.opacity = '0';
            }
        });
    });
}

// Initialize dashboard
document.addEventListener('DOMContentLoaded', function() {
    // Update clock immediately
    updateClock();
    
    // Update every second
    setInterval(updateClock, 1000);
    
    // Initialize interactive features
    initializeStatCards();
    initializeTooltips();
    
    // Optional: Refresh stats periodically (uncomment if needed)
    // setTimeout(refreshDashboardStats, 5 * 60 * 1000); // Every 5 minutes
});
