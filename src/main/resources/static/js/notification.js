/**
 * Notification Simple - Enhanced with Smooth Animations
 * @author Tulip Shop
 * @version 2.0
 */

// Guard to prevent redeclaration when script is loaded multiple times
if (typeof window.TulipNotification === 'undefined') {

    window.TulipNotification = class TulipNotification {
        constructor() {
            this.stompClient = null;
            this.connected = false;
            this.notificationsLoaded = false;
            this.currentPage = { all: 0, ORDER: 0, PROMOTION: 0, SYSTEM: 0 };
            this.hasMore = { all: true, ORDER: true, PROMOTION: true, SYSTEM: true };
            this.currentTab = 'all';

            this.initElements();
            this.initEventListeners();
            this.init();
        }

        /**
         * Initialize DOM elements
         */
        initElements() {
            this.bellBtn = document.getElementById('notificationBellBtn');
            this.panel = document.getElementById('notificationPanel');
            this.markAllReadBtn = document.getElementById('markAllReadBtn');
            this.countBadge = document.getElementById('notificationCountBadge');
            this.headerCount = document.getElementById('headerCount');
            this.toastContainer = document.getElementById('notificationToastContainer');
        }

        /**
         * Initialize event listeners
         */
        initEventListeners() {
            // Bell button toggle
            if (this.bellBtn) {
                this.bellBtn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    this.togglePanel();
                });
            }

            // Mark all as read
            if (this.markAllReadBtn) {
                this.markAllReadBtn.addEventListener('click', () => this.markAllAsRead());
            }

            // Close panel when clicking outside
            document.addEventListener('click', (e) => {
                if (this.panel && !this.panel.contains(e.target) && !this.bellBtn.contains(e.target)) {
                    this.closePanel();
                }
            });

            // Tab switching
            document.querySelectorAll('.tab-btn').forEach(btn => {
                btn.addEventListener('click', (e) => this.switchTab(e.currentTarget.dataset.tab));
            });

            // Notification item clicks
            document.addEventListener('click', (e) => {
                const item = e.target.closest('.notification-item');
                if (item) this.handleNotificationClick(item, e);
            });
        }

        /**
         * Initialize notification system
         */
        init() {
            this.loadUnreadCount();
            this.connect();
        }

        /**
         * Toggle notification panel with smooth animation
         */
        togglePanel() {
            const isOpen = this.panel.classList.contains('open');

            if (isOpen) {
                this.closePanel();
            } else {
                this.openPanel();
            }
        }

        /**
         * Open panel with animation
         */
        openPanel() {
            this.panel.classList.add('open');

            // Load notifications on first open
            if (!this.notificationsLoaded) {
                this.loadNotifications(null, 0, 10);
                this.notificationsLoaded = true;
            }

            // Add entrance animation
            requestAnimationFrame(() => {
                this.panel.style.opacity = '1';
                this.panel.style.transform = 'translateY(0) scale(1)';
            });
        }

        /**
         * Close panel with animation
         */
        closePanel() {
            this.panel.classList.remove('open');
        }

        /**
         * Switch between tabs
         */
        switchTab(tab) {
            this.currentTab = tab;

            // Update active tab button
            document.querySelectorAll('.tab-btn').forEach(btn => {
                btn.classList.toggle('active', btn.dataset.tab === tab);
            });

            // Update active content
            document.querySelectorAll('.notification-tab-content').forEach(content => {
                content.classList.toggle('active', content.dataset.tab === tab);
            });

            // Load notifications if not loaded yet
            const listId = this.getListId(tab);
            const list = document.getElementById(listId);
            const hasItems = list && list.querySelectorAll('.notification-item').length > 0;

            if (!hasItems) {
                const type = tab === 'all' ? null : document.querySelector(`.tab-btn[data-tab="${tab}"]`).dataset.type;
                this.loadNotifications(type, 0, 10);
            }
        }

        /**
         * Get styling configuration by notification type
         */
        getStyleByType(type) {
            const styles = {
                'ORDER': { bg: '#E8F5E9', color: '#2E7D32', icon: 'fas fa-box-open' },
                'PROMOTION': { bg: '#FFF8E1', color: '#F57F17', icon: 'fas fa-tag' },
                'SYSTEM': { bg: '#F5F5F5', color: '#424242', icon: 'fas fa-info-circle' }
            };
            return styles[type] || styles['SYSTEM'];
        }

        /**
         * Create notification HTML with enhanced styling
         */
        createNotificationHtml(notification) {
            const styles = this.getStyleByType(notification.type);
            const unreadClass = notification.isRead ? '' : 'unread';
            const timeAgo = this.formatTimeAgo(notification.createdAt);

            const iconHtml = notification.imageUrl
                ? `<img src="${notification.imageUrl}" style="width:100%;height:100%;object-fit:cover;border-radius:50%;" alt="icon">`
                : `<i class="${styles.icon}"></i>`;

            return `
            <a href="${notification.link || 'javascript:void(0)'}" 
               class="notification-item ${unreadClass}" 
               data-id="${notification.id}"
               style="animation: fadeIn 0.3s ease">
                <div class="notification-item-icon" style="background: ${styles.bg}; color: ${styles.color}">
                    ${iconHtml}
                </div>
                <div class="notification-item-content">
                    <div class="notification-item-title">${this.escapeHtml(notification.title)}</div>
                    <div class="notification-item-text">${this.escapeHtml(notification.content)}</div>
                    <div class="notification-item-time">${timeAgo}</div>
                </div>
            </a>
        `;
        }

        /**
         * Show toast notification with smooth animation
         */
        showToast(notification) {
            const styles = this.getStyleByType(notification.type);
            const toast = document.createElement('div');
            toast.className = 'notification-toast';

            const iconHtml = notification.imageUrl
                ? `<img src="${notification.imageUrl}" style="width:30px;height:30px;border-radius:50%;object-fit:cover;" alt="icon">`
                : `<i class="${styles.icon}" style="color:${styles.color};font-size:18px;"></i>`;

            toast.innerHTML = `
            <div style="flex-shrink:0;">${iconHtml}</div>
            <div style="flex:1;">
                <div style="font-weight:600;font-size:13px;margin-bottom:2px;">${this.escapeHtml(notification.title)}</div>
                <div style="font-size:12px;color:#666;">${this.escapeHtml(notification.content)}</div>
            </div>
        `;

            toast.onclick = () => {
                if (notification.link && notification.link !== 'javascript:void(0)') {
                    window.location.href = notification.link;
                }
                this.removeToast(toast);
            };

            this.toastContainer.appendChild(toast);

            // Auto remove after 5 seconds
            setTimeout(() => this.removeToast(toast), 5000);
        }

        /**
         * Remove toast with exit animation
         */
        removeToast(toast) {
            toast.classList.add('toast-exit');
            setTimeout(() => toast.remove(), 300);
        }

        /**
         * Connect to WebSocket
         */
        connect() {
            const token = this.getAccessToken();
            if (!token) return;

            const socket = new SockJS('/ws');
            this.stompClient = Stomp.over(socket);
            this.stompClient.debug = null;

            const headers = { 'Authorization': 'Bearer ' + token };
            this.stompClient.connect(
                headers,
                () => this.onConnected(),
                (error) => console.error('WebSocket connection error:', error)
            );
        }

        /**
         * Handle WebSocket connection success
         */
        onConnected() {
            this.connected = true;

            // Subscribe to personal notifications
            this.stompClient.subscribe('/user/queue/notifications', (message) => {
                this.onNotificationReceived(message);
            });

            // Subscribe to public notifications
            this.stompClient.subscribe('/topic/public-notifications', (message) => {
                this.onNotificationReceived(message);
            });
        }

        /**
         * Handle incoming notification
         */
        onNotificationReceived(message) {
            try {
                const notification = JSON.parse(message.body);
                this.showToast(notification);
                this.prependNotification(notification);
                this.incrementUnreadCount();
            } catch (error) {
                console.error('Error processing notification:', error);
            }
        }

        /**
         * Load unread notification count
         */
        async loadUnreadCount() {
            try {
                const response = await fetch('/v1/api/notifications/unread/count');
                if (response.ok) {
                    const counts = await response.json();
                    this.updateUnreadBadges(counts);
                }
            } catch (error) {
                console.error('Error loading unread count:', error);
            }
        }

        /**
         * Load notifications with pagination
         */
        async loadNotifications(type, page, size) {
            const tabKey = type || 'all';

            if (!this.hasMore[tabKey] && page > 0) return;

            const url = type
                ? `/v1/api/notifications/type/${type}?page=${page}&size=${size}`
                : `/v1/api/notifications?page=${page}&size=${size}`;

            try {
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
         * Render notifications to DOM
         */
        renderNotifications(notifications, type, append) {
            const tab = type ? type.toLowerCase() : 'all';
            const list = document.getElementById(this.getListId(tab));

            if (!list) return;

            // Clear spinner on first load
            if (!append) list.innerHTML = '';

            if (notifications.length === 0 && !append) {
                list.innerHTML = `
                <div class="notification-empty" style="animation: fadeIn 0.5s ease;">
                    <i class="far fa-bell"></i>
                    <p>Chưa có thông báo nào</p>
                </div>
            `;
                return;
            }

            const html = notifications.map(n => this.createNotificationHtml(n)).join('');

            if (append) {
                const loadMoreBtn = list.querySelector('.load-more-container');
                if (loadMoreBtn) loadMoreBtn.remove();
                list.insertAdjacentHTML('beforeend', html);
            } else {
                list.innerHTML = html;
            }
        }

        /**
         * Get list element ID by tab
         */
        getListId(tab) {
            const mapping = {
                'all': 'allNotificationList',
                'order': 'orderNotificationList',
                'promotion': 'promotionNotificationList',
                'system': 'systemNotificationList'
            };
            return mapping[tab] || 'allNotificationList';
        }

        /**
         * Add load more button
         */
        addLoadMoreButton(type) {
            const tab = type ? type.toLowerCase() : 'all';
            const list = document.getElementById(this.getListId(tab));

            if (list) {
                const html = `
                <div class="load-more-container">
                    <button class="load-more-btn" onclick="window.tulipNotification.loadMore('${type || ''}')">
                        <i class="fas fa-chevron-down"></i> Xem thêm
                    </button>
                </div>
            `;
                list.insertAdjacentHTML('beforeend', html);
            }
        }

        /**
         * Load more notifications
         */
        loadMore(type) {
            const t = (type === 'null' || type === '') ? null : type;
            const tabKey = t || 'all';
            this.loadNotifications(t, this.currentPage[tabKey] + 1, 10);
        }

        /**
         * Prepend new notification to list
         */
        prependNotification(notification) {
            const list = document.getElementById('allNotificationList');
            if (list) {
                const empty = list.querySelector('.notification-empty');
                if (empty) list.innerHTML = '';

                list.insertAdjacentHTML('afterbegin', this.createNotificationHtml(notification));
            }
        }

        /**
         * Update unread badges
         */
        updateUnreadBadges(counts) {
            if (this.headerCount) {
                this.headerCount.textContent = counts.total > 99 ? '99+' : counts.total;
                this.headerCount.style.display = counts.total > 0 ? 'inline-block' : 'none';
            }

            if (this.countBadge) {
                this.countBadge.style.display = counts.total > 0 ? 'block' : 'none';
            }
        }

        /**
         * Increment unread count
         */
        incrementUnreadCount() {
            const current = parseInt(this.headerCount?.textContent || '0');
            this.updateUnreadBadges({ total: current + 1 });
        }

        /**
         * Mark all notifications as read
         */
        async markAllAsRead() {
            try {
                const response = await fetch('/v1/api/notifications/read-all', { method: 'PUT' });

                if (response.ok) {
                    document.querySelectorAll('.notification-item.unread').forEach(item => {
                        item.classList.remove('unread');
                    });
                    this.updateUnreadBadges({ total: 0 });
                }
            } catch (error) {
                console.error('Error marking all as read:', error);
            }
        }

        /**
         * Mark single notification as read
         */
        async markAsRead(notificationId) {
            try {
                const response = await fetch(`/v1/api/notifications/${notificationId}/read`, {
                    method: 'PUT'
                });
                return response.ok;
            } catch (error) {
                console.error('Error marking as read:', error);
                return false;
            }
        }

        /**
         * Handle notification item click
         */
        handleNotificationClick(item, event) {
            const isUnread = item.classList.contains('unread');

            if (isUnread) {
                event.preventDefault();

                const notificationId = item.dataset.id;
                const link = item.getAttribute('href');

                this.markAsRead(notificationId).then(() => {
                    item.classList.remove('unread');

                    if (link && link !== 'javascript:void(0)') {
                        setTimeout(() => {
                            window.location.href = link;
                        }, 200); // Small delay for smooth transition
                    }
                });
            }
        }

        /**
         * Get access token from cookie
         */
        getAccessToken() {
            const match = document.cookie.match(new RegExp('(^| )accessToken=([^;]+)'));
            return match ? match[2] : null;
        }

        /**
         * Escape HTML to prevent XSS
         */
        escapeHtml(text) {
            if (!text) return '';
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
         * Format time ago
         */
        formatTimeAgo(dateString) {
            const seconds = Math.floor((new Date() - new Date(dateString)) / 1000);

            if (seconds < 60) return 'Vừa xong';
            if (seconds < 3600) return Math.floor(seconds / 60) + ' phút trước';
            if (seconds < 86400) return Math.floor(seconds / 3600) + ' giờ trước';
            if (seconds < 604800) return Math.floor(seconds / 86400) + ' ngày trước';

            return new Date(dateString).toLocaleDateString('vi-VN');
        }
    }

    // Initialize on DOM ready
    document.addEventListener('DOMContentLoaded', () => {
        if (document.querySelector('.notification-widget-container') && !window.tulipNotification) {
            window.tulipNotification = new window.TulipNotification();
        }
    });

} // End guard

