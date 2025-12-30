/**
 * Notification WebSocket Client - Optimized for High-Traffic
 * Real-time, High-Performance, Smart "Read" Logic
 */

class NotificationManager {
    constructor() {
        this.stompClient = null;
        this.connected = false;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 3000;
        this.notificationsLoaded = false;
        this.currentPage = { all: 0, ORDER: 0, PROMOTION: 0, SYSTEM: 0 };
        this.hasMore = { all: true, ORDER: true, PROMOTION: true, SYSTEM: true };
        
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => this.init());
        } else {
            this.init();
        }
    }
    
    /**
     * Khởi tạo notification manager
     */
    init() {
        console.log('🔔 Initializing Notification Manager...');
        
        // Load số lượng thông báo chưa đọc ngay lập tức (async)
        this.loadUnreadCount();
        
        // Bind event handlers
        this.bindEvents();
        
        // Kết nối WebSocket NGAY LẬP TỨC để nhận real-time notifications
        this.connect();
        
        // Lazy loading: Chỉ load thông báo khi mở dropdown
        const dropdownBtn = document.getElementById('notificationDropdownBtn');
        if (dropdownBtn) {
            dropdownBtn.addEventListener('shown.bs.dropdown', () => {
                console.log('📂 Dropdown opened, loading notifications...');
                if (!this.notificationsLoaded) {
                    this.loadNotifications(null, 0, 5); // Load 5 tin đầu tiên
                    this.notificationsLoaded = true;
                }
            });
            
            // Debug: Log khi dropdown được click
            dropdownBtn.addEventListener('click', () => {
                console.log('🖱️ Notification bell clicked');
            });
        } else {
            console.warn('⚠️ Notification dropdown button not found');
        }
    }
    
    /**
     * Kết nối đến WebSocket server
     */
    connect() {
        const token = this.getAccessToken();
        
        if (!token) {
            console.warn('⚠️ No access token found, skipping WebSocket connection');
            return;
        }
        
        console.log('🔌 Connecting to WebSocket...');
        
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);
        this.stompClient.debug = null; // Tắt debug log
        
        const headers = { 'Authorization': 'Bearer ' + token };
        
        this.stompClient.connect(
            headers,
            (frame) => this.onConnected(frame),
            (error) => this.onError(error)
        );
    }
    
    /**
     * Callback khi kết nối thành công
     */
    onConnected(frame) {
        console.log('✅ WebSocket connected');
        this.connected = true;
        this.reconnectAttempts = 0;
        
        // Subscribe đến kênh cá nhân
        this.stompClient.subscribe('/user/queue/notifications', (message) => {
            this.onNotificationReceived(message);
        });
        
        // Subscribe đến kênh broadcast
        this.stompClient.subscribe('/topic/public-notifications', (message) => {
            this.onNotificationReceived(message);
        });
        
        console.log('📡 Subscribed to notification channels');
    }
    
    /**
     * Callback khi có lỗi kết nối
     */
    onError(error) {
        console.error('❌ WebSocket error:', error);
        this.connected = false;
        
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`🔄 Reconnecting... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
            setTimeout(() => this.connect(), this.reconnectDelay);
        }
    }
    
    /**
     * Callback khi nhận được thông báo mới
     */
    onNotificationReceived(message) {
        try {
            const notification = JSON.parse(message.body);
            console.log('🔔 New notification received:', notification);
            
            // Hiển thị toast với ảnh thumbnail
            this.showToast(notification);
            
            // Prepend vào danh sách (nếu dropdown đang mở)
            this.prependNotification(notification);
            
            // Cập nhật badge
            this.incrementUnreadCount(notification.type);
            
            // Hiệu ứng rung chuông
            this.animateBell();
            
        } catch (error) {
            console.error('Error processing notification:', error);
        }
    }
    
    /**
     * Hiệu ứng rung chuông
     */
    animateBell() {
        const bell = document.querySelector('.notification-bell i');
        if (bell) {
            bell.classList.add('animate-ring');
            setTimeout(() => bell.classList.remove('animate-ring'), 1000);
        }
    }
    
    /**
     * Hiển thị toast notification với ảnh thumbnail
     */
    showToast(notification) {
        // Tạo thumbnail HTML
        let thumbnailHtml = '';
        if (notification.imageUrl) {
            thumbnailHtml = `<img src="${notification.imageUrl}" alt="Notification" style="width: 50px; height: 50px; border-radius: 8px; object-fit: cover; margin-right: 12px;">`;
        } else {
            thumbnailHtml = `<i class="bi ${this.getIconByType(notification.type)}" style="font-size: 2rem; margin-right: 12px; color: #007bff;"></i>`;
        }
        
        const toastHtml = `
            <div class="toast notification-toast" role="alert" aria-live="assertive" aria-atomic="true" data-bs-delay="5000">
                <div class="toast-header">
                    <i class="bi bi-bell-fill text-primary me-2"></i>
                    <strong class="me-auto">${this.escapeHtml(notification.title)}</strong>
                    <small class="text-muted">Vừa xong</small>
                    <button type="button" class="btn-close" data-bs-dismiss="toast"></button>
                </div>
                <div class="toast-body d-flex align-items-center">
                    ${thumbnailHtml}
                    <div class="flex-grow-1">
                        ${this.escapeHtml(notification.content)}
                        ${notification.link ? `<a href="${notification.link}" class="btn btn-sm btn-primary mt-2">Xem chi tiết</a>` : ''}
                    </div>
                </div>
            </div>
        `;
        
        let toastContainer = document.getElementById('toastContainer');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'toastContainer';
            toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
            toastContainer.style.zIndex = '9999';
            document.body.appendChild(toastContainer);
        }
        
        toastContainer.insertAdjacentHTML('beforeend', toastHtml);
        
        const toastElement = toastContainer.lastElementChild;
        const toast = new bootstrap.Toast(toastElement);
        toast.show();
        
        toastElement.addEventListener('hidden.bs.toast', () => toastElement.remove());
    }
    
    /**
     * Prepend thông báo vào danh sách
     */
    prependNotification(notification) {
        const tabId = this.getTabIdByType(notification.type);
        const listElement = document.getElementById(tabId);
        
        if (listElement) {
            const loading = listElement.querySelector('.notification-loading');
            if (loading) loading.remove();
            
            const emptyMsg = listElement.querySelector('.text-center.text-muted');
            if (emptyMsg) emptyMsg.remove();
            
            const notificationHtml = this.createNotificationHtml(notification);
            listElement.insertAdjacentHTML('afterbegin', notificationHtml);
            
            // Giới hạn 20 thông báo
            const items = listElement.querySelectorAll('.notification-item');
            if (items.length > 20) {
                items[items.length - 1].remove();
            }
        }
        
        // Thêm vào tab "Tất cả"
        if (notification.type) {
            const allListElement = document.getElementById('allNotifications');
            if (allListElement) {
                const loading = allListElement.querySelector('.notification-loading');
                if (loading) loading.remove();
                
                const emptyMsg = allListElement.querySelector('.text-center.text-muted');
                if (emptyMsg) emptyMsg.remove();
                
                const notificationHtml = this.createNotificationHtml(notification);
                allListElement.insertAdjacentHTML('afterbegin', notificationHtml);
                
                const items = allListElement.querySelectorAll('.notification-item');
                if (items.length > 20) {
                    items[items.length - 1].remove();
                }
            }
        }
    }
    
    /**
     * Tạo HTML cho một thông báo với ảnh thumbnail
     */
    createNotificationHtml(notification) {
        const timeAgo = this.formatTimeAgo(notification.createdAt);
        const unreadClass = notification.isRead ? '' : 'unread';
        const link = notification.link || '#';
        
        // Icon hoặc ảnh với kích thước 50x50px, bo góc rounded
        let iconHtml = '';
        if (notification.imageUrl) {
            iconHtml = `<img src="${notification.imageUrl}" alt="Notification" loading="lazy" style="width: 50px; height: 50px; border-radius: 8px; object-fit: cover;">`;
        } else {
            iconHtml = `<i class="bi ${this.getIconByType(notification.type)}"></i>`;
        }
        
        return `
            <a href="${link}" class="notification-item ${unreadClass}" data-id="${notification.id}" data-type="${notification.type}">
                <div class="notification-icon">
                    ${iconHtml}
                </div>
                <div class="notification-content">
                    <div class="notification-title">${this.escapeHtml(notification.title)}</div>
                    <div class="notification-text">${this.escapeHtml(notification.content)}</div>
                    <div class="notification-time">${timeAgo}</div>
                </div>
                ${!notification.isRead ? '<span class="unread-dot"></span>' : ''}
            </a>
        `;
    }
    
    /**
     * Load số lượng thông báo chưa đọc (async)
     */
    async loadUnreadCount() {
        try {
            const response = await fetch('/v1/api/notifications/unread/count');
            if (response.ok) {
                const data = await response.json();
                this.updateUnreadBadges(data);
            }
        } catch (error) {
            console.error('Error loading unread count:', error);
        }
    }
    
    /**
     * Load danh sách thông báo với pagination
     */
    async loadNotifications(type = null, page = 0, size = 5) {
        try {
            const tabKey = type || 'all';
            
            if (!this.hasMore[tabKey] && page > 0) {
                return;
            }
            
            const url = type 
                ? `/v1/api/notifications/type/${type}?page=${page}&size=${size}`
                : `/v1/api/notifications?page=${page}&size=${size}`;
            
            const response = await fetch(url);
            if (response.ok) {
                const data = await response.json();
                
                this.hasMore[tabKey] = !data.last;
                this.currentPage[tabKey] = page;
                
                this.renderNotifications(data.content, type, page > 0);
                
                if (this.hasMore[tabKey]) {
                    this.addLoadMoreButton(type);
                }
            }
        } catch (error) {
            console.error('Error loading notifications:', error);
        }
    }
    
    /**
     * Render danh sách thông báo
     */
    renderNotifications(notifications, type = null, append = false) {
        const tabId = type ? this.getTabIdByType(type) : 'allNotifications';
        const listElement = document.getElementById(tabId);
        
        if (listElement) {
            if (notifications.length === 0 && !append) {
                listElement.innerHTML = '<div class="text-center text-muted py-4">Không có thông báo</div>';
            } else {
                const notificationsHtml = notifications.map(n => this.createNotificationHtml(n)).join('');
                
                if (append) {
                    const oldLoadMore = listElement.querySelector('.load-more-btn');
                    if (oldLoadMore) oldLoadMore.remove();
                    
                    listElement.insertAdjacentHTML('beforeend', notificationsHtml);
                } else {
                    listElement.innerHTML = notificationsHtml;
                }
            }
        }
    }
    
    /**
     * Thêm nút "Tải thêm"
     */
    addLoadMoreButton(type = null) {
        const tabId = type ? this.getTabIdByType(type) : 'allNotifications';
        const listElement = document.getElementById(tabId);
        
        if (listElement) {
            const oldBtn = listElement.querySelector('.load-more-btn');
            if (oldBtn) oldBtn.remove();
            
            const loadMoreHtml = `
                <div class="text-center py-3 load-more-btn">
                    <button class="btn btn-sm btn-outline-primary" onclick="window.notificationManager.loadMore('${type}')">
                        <i class="bi bi-arrow-down-circle"></i> Tải thêm
                    </button>
                </div>
            `;
            listElement.insertAdjacentHTML('beforeend', loadMoreHtml);
        }
    }
    
    /**
     * Load thêm thông báo
     */
    loadMore(type = null) {
        const tabKey = type || 'all';
        const nextPage = this.currentPage[tabKey] + 1;
        this.loadNotifications(type, nextPage, 5);
    }
    
    /**
     * Cập nhật badge số lượng chưa đọc
     */
    updateUnreadBadges(counts) {
        const totalBadge = document.getElementById('notificationBadge');
        if (totalBadge) {
            if (counts.total > 0) {
                totalBadge.textContent = counts.total > 99 ? '99+' : counts.total;
                totalBadge.style.display = 'inline-block';
            } else {
                totalBadge.style.display = 'none';
            }
        }
        
        this.updateTabBadge('orderBadge', counts.order);
        this.updateTabBadge('promotionBadge', counts.promotion);
        this.updateTabBadge('systemBadge', counts.system);
    }
    
    /**
     * Cập nhật badge của một tab
     */
    updateTabBadge(badgeId, count) {
        const badge = document.getElementById(badgeId);
        if (badge) {
            if (count > 0) {
                badge.textContent = count > 99 ? '99+' : count;
                badge.style.display = 'inline-block';
            } else {
                badge.style.display = 'none';
            }
        }
    }
    
    /**
     * Tăng số lượng chưa đọc
     */
    incrementUnreadCount(type) {
        const totalBadge = document.getElementById('notificationBadge');
        if (totalBadge) {
            const current = parseInt(totalBadge.textContent) || 0;
            totalBadge.textContent = current + 1;
            totalBadge.style.display = 'inline-block';
        }
        
        const badgeMap = {
            'ORDER': 'orderBadge',
            'PROMOTION': 'promotionBadge',
            'SYSTEM': 'systemBadge'
        };
        
        const badgeId = badgeMap[type];
        if (badgeId) {
            const badge = document.getElementById(badgeId);
            if (badge) {
                const current = parseInt(badge.textContent) || 0;
                badge.textContent = current + 1;
                badge.style.display = 'inline-block';
            }
        }
    }
    
    /**
     * Đánh dấu thông báo là đã đọc
     */
    async markAsRead(notificationId) {
        console.log('📝 Calling API to mark as read:', notificationId);
        try {
            const response = await fetch(`/v1/api/notifications/${notificationId}/read`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            
            console.log('📡 API response:', response.status, response.ok);
            
            if (response.ok) {
                console.log('✅ Successfully marked as read');
                // Cập nhật UI: Xóa class unread và dot cho TẤT CẢ các instance của thông báo này
                const items = document.querySelectorAll(`.notification-item[data-id="${notificationId}"]`);
                console.log('🔄 Updating UI for', items.length, 'items');
                items.forEach(item => {
                    item.classList.remove('unread');
                    const dot = item.querySelector('.unread-dot');
                    if (dot) dot.remove();
                });
                
                // Reload unread count
                await this.loadUnreadCount();
                
                return true;
            }
            console.warn('⚠️ API returned non-OK status');
            return false;
        } catch (error) {
            console.error('❌ Error marking notification as read:', error);
            return false;
        }
    }
    
    /**
     * Đánh dấu tất cả là đã đọc
     */
    async markAllAsRead() {
        try {
            const response = await fetch('/v1/api/notifications/read-all', {
                method: 'PUT'
            });
            
            if (response.ok) {
                document.querySelectorAll('.notification-item.unread').forEach(item => {
                    item.classList.remove('unread');
                    const dot = item.querySelector('.unread-dot');
                    if (dot) dot.remove();
                });
                
                this.updateUnreadBadges({ total: 0, order: 0, promotion: 0, system: 0 });
            }
        } catch (error) {
            console.error('Error marking all as read:', error);
        }
    }
    
    /**
     * Bind event handlers
     */
    bindEvents() {
        // Click vào thông báo -> đánh dấu đã đọc
        document.addEventListener('click', (e) => {
            const item = e.target.closest('.notification-item');
            if (!item) return;
            
            const notificationId = item.dataset.id;
            const link = item.getAttribute('href');
            const isUnread = item.classList.contains('unread');
            
            console.log('🖱️ Clicked notification:', {
                id: notificationId,
                link: link,
                isUnread: isUnread
            });
            
            // Nếu là thông báo chưa đọc
            if (isUnread) {
                // LUÔN ngăn navigation mặc định để đánh dấu đã đọc trước
                e.preventDefault();
                
                console.log('📖 Marking notification as read:', notificationId);
                
                // Đánh dấu đã đọc
                this.markAsRead(notificationId).then((success) => {
                    console.log('✅ Mark as read result:', success);
                    if (success) {
                        // Navigate sau khi đánh dấu thành công (nếu có link hợp lệ)
                        if (link && link !== '#' && link !== 'javascript:void(0)') {
                            console.log('🔗 Navigating to:', link);
                            window.location.href = link;
                        }
                    } else {
                        // Nếu API fail, vẫn cho phép navigate
                        if (link && link !== '#' && link !== 'javascript:void(0)') {
                            console.log('🔗 Navigating to (fallback):', link);
                            window.location.href = link;
                        }
                    }
                });
            } else {
                console.log('ℹ️ Notification already read');
            }
            // Nếu đã đọc rồi, cho phép navigate bình thường (không preventDefault)
        });
        
        // Click "Đánh dấu tất cả là đã đọc"
        const markAllReadBtn = document.getElementById('markAllRead');
        if (markAllReadBtn) {
            markAllReadBtn.addEventListener('click', (e) => {
                e.preventDefault();
                this.markAllAsRead();
            });
        }
        
        // Switch tabs - lazy load
        document.querySelectorAll('button[data-bs-toggle="tab"]').forEach(tab => {
            tab.addEventListener('shown.bs.tab', (e) => {
                const type = e.target.dataset.type;
                const tabKey = type || 'all';
                const tabId = type ? this.getTabIdByType(type) : 'allNotifications';
                const listElement = document.getElementById(tabId);
                const hasContent = listElement && listElement.querySelector('.notification-item');
                
                if (!hasContent) {
                    this.loadNotifications(type, 0, 5);
                }
            });
        });
        
        // Ngăn dropdown đóng khi click bên trong (stopPropagation)
        const dropdown = document.querySelector('.notification-dropdown');
        if (dropdown) {
            dropdown.addEventListener('click', (e) => {
                // Chỉ stopPropagation nếu KHÔNG phải click vào notification-item
                if (!e.target.closest('.notification-item')) {
                    e.stopPropagation();
                }
            });
        }
    }
    
    /**
     * Helper: Lấy access token từ cookie
     */
    getAccessToken() {
        const name = 'accessToken=';
        const decodedCookie = decodeURIComponent(document.cookie);
        const ca = decodedCookie.split(';');
        for (let i = 0; i < ca.length; i++) {
            let c = ca[i];
            while (c.charAt(0) === ' ') {
                c = c.substring(1);
            }
            if (c.indexOf(name) === 0) {
                return c.substring(name.length, c.length);
            }
        }
        return null;
    }
    
    /**
     * Helper: Escape HTML
     */
    escapeHtml(text) {
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
     * Helper: Format thời gian
     */
    formatTimeAgo(dateString) {
        const date = new Date(dateString);
        const now = new Date();
        const seconds = Math.floor((now - date) / 1000);
        
        if (seconds < 60) return 'Vừa xong';
        if (seconds < 3600) return Math.floor(seconds / 60) + ' phút trước';
        if (seconds < 86400) return Math.floor(seconds / 3600) + ' giờ trước';
        if (seconds < 604800) return Math.floor(seconds / 86400) + ' ngày trước';
        
        return date.toLocaleDateString('vi-VN');
    }
    
    /**
     * Helper: Lấy icon theo loại thông báo
     */
    getIconByType(type) {
        const icons = {
            'ORDER': 'bi-box-seam',
            'PROMOTION': 'bi-gift',
            'SYSTEM': 'bi-info-circle'
        };
        return icons[type] || 'bi-bell';
    }
    
    /**
     * Helper: Lấy tab ID theo loại
     */
    getTabIdByType(type) {
        const tabs = {
            'ORDER': 'orderNotifications',
            'PROMOTION': 'promotionNotifications',
            'SYSTEM': 'systemNotifications'
        };
        return tabs[type] || 'allNotifications';
    }
}

// Khởi tạo global instance
window.notificationManager = new NotificationManager();
