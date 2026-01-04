/**
 * Admin Live Chat Dashboard
 */
class AdminLiveChat {
    constructor() {
        this.stompClient = null;
        this.connected = false;
        this.sessions = [];
        this.currentSession = null;
        this.currentMessages = [];
        this.filterStatus = 'ALL'; // ALL, NEW, PROCESSING, CLOSED
        this.filterFromDate = '';
        this.filterToDate = '';
        
        this.initElements();
        this.initEventListeners();
        this.init();
    }
    
    initElements() {
        this.activeChatList = document.getElementById('activeChatList');
        this.closedChatList = document.getElementById('closedChatList');
        this.closedSectionToggle = document.getElementById('closedSectionToggle');
        this.closedChevron = document.getElementById('closedChevron');
        this.closedCount = document.getElementById('closedCount');
        this.chatMessages = document.getElementById('chatMessages');
        this.chatInput = document.getElementById('chatMessageInput');
        this.sendButton = document.getElementById('sendMessageBtn');
        this.refreshButton = document.getElementById('refreshChats');
        this.chatWindowHeader = document.getElementById('chatWindowHeader');
        this.chatInputArea = document.getElementById('chatInputArea');
        this.totalChats = document.getElementById('totalChats');
        this.newChats = document.getElementById('newChats');
        this.processingChats = document.getElementById('processingChats');
        this.statusFilter = document.getElementById('statusFilter');
        this.fromDateFilter = document.getElementById('fromDateFilter');
        this.toDateFilter = document.getElementById('toDateFilter');
        this.applyFilterBtn = document.getElementById('applyFilterBtn');
        this.clearFilterBtn = document.getElementById('clearFilterBtn');
    }
    
    initEventListeners() {
        if (this.sendButton) {
            this.sendButton.addEventListener('click', () => this.sendMessage());
        }
        
        if (this.chatInput) {
            this.chatInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    this.sendMessage();
                }
            });
        }
        
        if (this.refreshButton) {
            this.refreshButton.addEventListener('click', () => this.loadSessions());
        }
        
        if (this.applyFilterBtn) {
            this.applyFilterBtn.addEventListener('click', () => {
                this.filterStatus = this.statusFilter?.value || 'ALL';
                this.filterFromDate = this.fromDateFilter?.value || '';
                this.filterToDate = this.toDateFilter?.value || '';
                this.loadSessions();
            });
        }
        
        if (this.clearFilterBtn) {
            this.clearFilterBtn.addEventListener('click', () => {
                this.filterStatus = 'ALL';
                this.filterFromDate = '';
                this.filterToDate = '';
                if (this.statusFilter) this.statusFilter.value = 'ALL';
                if (this.fromDateFilter) this.fromDateFilter.value = '';
                if (this.toDateFilter) this.toDateFilter.value = '';
                this.loadSessions();
            });
        }
        
        if (this.closedSectionToggle) {
            this.closedSectionToggle.addEventListener('click', () => {
                const isVisible = this.closedChatList.style.display !== 'none';
                this.closedChatList.style.display = isVisible ? 'none' : 'block';
                if (this.closedChevron) {
                    this.closedChevron.style.transform = isVisible ? 'rotate(0deg)' : 'rotate(180deg)';
                }
            });
        }
    }
    
    async init() {
        await this.loadSessions();
        this.connect();
        
        // Auto refresh mỗi 5 giây
        setInterval(() => {
            if (this.connected) {
                this.loadSessions();
            }
        }, 5000);
    }
    
    connect() {
        const socket = new SockJS('/ws-chat');
        this.stompClient = Stomp.over(socket);
        this.stompClient.debug = null;
        
        const token = this.getAccessToken();
        const headers = {};
        if (token) {
            headers['Authorization'] = 'Bearer ' + token;
        }
        
        this.stompClient.connect(
            headers,
            () => this.onConnected(),
            (error) => this.onError(error)
        );
    }
    
    onConnected() {
        this.connected = true;
        
        // Subscribe vào topic thông báo
        this.stompClient.subscribe('/topic/admin/chat-notifications', (message) => {
            const data = JSON.parse(message.body);
            if (data.type === 'NEW_MESSAGE') {
                this.handleNewMessageNotification(data);
            }
        });
    }
    
    onError(error) {
        setTimeout(() => {
            if (!this.connected) {
                this.connect();
            }
        }, 3000);
    }
    
    handleNewMessageNotification(data) {
        // Reload sessions để cập nhật
        this.loadSessions();
        
        // Nếu đang xem session này, reload messages
        if (this.currentSession && this.currentSession.sessionToken === data.sessionToken) {
            this.loadMessages(this.currentSession.id);
        }
    }
    
    async loadSessions() {
        try {
            // Build URL with filter params
            let url = '/v1/api/live-chat/admin/sessions';
            const params = new URLSearchParams();
            
            if (this.filterStatus && this.filterStatus !== 'ALL') {
                params.append('status', this.filterStatus);
            }
            if (this.filterFromDate) {
                params.append('fromDate', this.filterFromDate);
            }
            if (this.filterToDate) {
                params.append('toDate', this.filterToDate);
            }
            
            if (params.toString()) {
                url += '?' + params.toString();
            }
            
            const response = await fetch(url, {
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include'
            });
            
            if (response.ok) {
                this.sessions = await response.json();
                this.renderSessions();
            }
        } catch (error) {
            // Silently fail - user can retry with refresh button
        }
    }
    
    renderSessions() {
        // Phân loại sessions
        const activeSessions = this.sessions.filter(s => s.status !== 'CLOSED');
        const closedSessions = this.sessions.filter(s => s.status === 'CLOSED');
        
        // Render active sessions
        if (!this.activeChatList) return;
        
        if (activeSessions.length === 0) {
            this.activeChatList.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-comments"></i>
                    <p>Chưa có chat đang hoạt động</p>
                </div>
            `;
        } else {
            this.activeChatList.innerHTML = activeSessions.map(session => this.renderSessionItem(session)).join('');
        }
        
        // Render closed sessions
        if (!this.closedChatList) return;
        
        if (this.closedCount) {
            this.closedCount.textContent = closedSessions.length;
        }
        
        // Cập nhật badges cho tabs
        const activeBadge = document.getElementById('activeBadge');
        const closedBadge = document.getElementById('closedBadge');
        if (activeBadge) {
            activeBadge.textContent = activeSessions.length;
        }
        if (closedBadge) {
            closedBadge.textContent = closedSessions.length;
        }
        
        if (closedSessions.length === 0) {
            this.closedChatList.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-archive"></i>
                    <p>Chưa có chat đã đóng</p>
                </div>
            `;
        } else {
            this.closedChatList.innerHTML = closedSessions.map(session => this.renderSessionItem(session)).join('');
        }
        
        // Cập nhật thống kê
        this.updateStats();
    }
    
    renderSessionItem(session) {
        const isActive = this.currentSession && this.currentSession.id === session.id;
        const statusClass = session.status === 'NEW' ? 'status-new' : 
                           session.status === 'PROCESSING' ? 'status-processing' : 
                           'status-closed';
        const unreadBadge = session.unreadCount > 0 ? 
            `<span class="unread-badge">${session.unreadCount}</span>` : '';
        
        return `
            <div class="chat-item ${isActive ? 'active' : ''}" onclick="adminLiveChat.selectSession(${session.id})">
                <div class="chat-item-header">
                    <strong>${this.escapeHtml(session.customerName)}</strong>
                    <span class="chat-item-time">${this.formatTime(session.updatedAt)}</span>
                </div>
                <div class="chat-item-email">${this.escapeHtml(session.customerEmail)}</div>
                <div class="chat-item-preview">${this.escapeHtml(session.lastMessagePreview || 'Chưa có tin nhắn')}</div>
                <div class="chat-item-footer">
                    <span class="chat-item-status ${statusClass}">${this.getStatusText(session.status)}</span>
                    ${unreadBadge}
                </div>
            </div>
        `;
    }
    
    async selectSession(sessionId) {
        const session = this.sessions.find(s => s.id === sessionId);
        if (!session) return;
        
        this.currentSession = session;
        this.renderSessions();
        
        // Load messages
        await this.loadMessages(sessionId);
        
        // Update header
        this.updateChatHeader(session);
        
        // Show/hide input area based on session status
        if (this.chatInputArea) {
            if (session.status === 'CLOSED') {
                this.chatInputArea.style.display = 'none';
                // Hiển thị thông báo session đã đóng
                this.chatMessages.insertAdjacentHTML('beforeend', `
                    <div class="alert alert-info text-center mt-3">
                        <i class="fas fa-info-circle"></i> Cuộc hội thoại này đã kết thúc
                    </div>
                `);
            } else if (session.status === 'NEW') {
                // Session mới - admin có thể trả lời ngay
                this.chatInputArea.style.display = 'block';
                if (this.chatInput) {
                    this.chatInput.disabled = false;
                    this.chatInput.placeholder = 'Nhập tin nhắn để trả lời...';
                }
                if (this.sendButton) {
                    this.sendButton.disabled = false;
                }
            } else {
                // Session đang PROCESSING - cho phép chat bình thường
                this.chatInputArea.style.display = 'block';
                if (this.chatInput) {
                    this.chatInput.disabled = false;
                    this.chatInput.placeholder = 'Nhập tin nhắn...';
                }
                if (this.sendButton) {
                    this.sendButton.disabled = false;
                }
            }
        }
        
        // Subscribe vào kênh của session này
        if (this.connected && this.stompClient) {
            const subscription = `/topic/chat/${session.sessionToken}`;
            // Unsubscribe cũ nếu có
            if (this.currentSubscription) {
                this.stompClient.unsubscribe(this.currentSubscription);
            }
            // Subscribe mới
            this.currentSubscription = this.stompClient.subscribe(subscription, (message) => {
                const data = JSON.parse(message.body);
                if (data.type !== 'MESSAGES_READ') {
                    this.handleNewMessage(data);
                }
            });
        }
        
        // Đánh dấu đã đọc
        await this.markAsRead(sessionId);
    }
    
    async loadMessages(sessionId) {
        try {
            const response = await fetch(`/v1/api/live-chat/session/${sessionId}/messages`, {
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            
            if (response.ok) {
                this.currentMessages = await response.json();
                this.renderMessages();
            }
        } catch (error) {
            // Silently fail - messages will be loaded on next update
        }
    }
    
    renderMessages() {
        if (!this.chatMessages) return;
        
        if (this.currentMessages.length === 0) {
            this.chatMessages.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-comment-dots"></i>
                    <p>Chưa có tin nhắn nào</p>
                </div>
            `;
            return;
        }
        
        this.chatMessages.innerHTML = this.currentMessages.map(msg => {
            const isStaff = msg.senderType === 'SUPPORT_AGENT';
            return `
                <div class="message ${isStaff ? 'staff' : 'customer'}">
                    <div class="message-bubble">
                        ${this.escapeHtml(msg.content)}
                        <div class="message-time">${this.formatTime(msg.timestamp)}</div>
                    </div>
                </div>
            `;
        }).join('');
        
        this.scrollToBottom();
    }
    
    handleNewMessage(message) {
        this.currentMessages.push(message);
        this.renderMessages();
        this.loadSessions(); // Refresh để cập nhật unread count
    }
    
    async sendMessage() {
        if (!this.currentSession || !this.chatInput) return;
        
        const content = this.chatInput.value.trim();
        if (!content || !this.connected) return;
        
        this.sendButton.disabled = true;
        
        // Nếu session đang NEW, tự động gọi assign trước khi gửi tin nhắn
        if (this.currentSession.status === 'NEW') {
            try {
                await this.assignSession(this.currentSession.id);
                // Đợi một chút để state được cập nhật
                await new Promise(resolve => setTimeout(resolve, 300));
            } catch (error) {
                this.sendButton.disabled = false;
                alert('Không thể nhận xử lý session. Vui lòng thử lại.');
                return;
            }
        }
        
        const message = {
            sessionToken: this.currentSession.sessionToken,
            content: content,
            senderType: 'SUPPORT_AGENT'
        };
        
        this.stompClient.send('/app/chat.sendMessage', {}, JSON.stringify(message));
        
        this.chatInput.value = '';
        this.sendButton.disabled = false;
        
        // Tin nhắn sẽ được thêm khi nhận từ server
    }
    
    async markAsRead(sessionId) {
        try {
            await fetch(`/v1/api/live-chat/session/${sessionId}/read`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
        } catch (error) {
            // Non-critical operation, ignore failures
        }
    }
    
    updateChatHeader(session) {
        if (!this.chatWindowHeader) return;
        
        const statusText = this.getStatusText(session.status);
        const statusClass = session.status.toLowerCase();
        
        // Format ngày giờ
        const createdDate = session.createdAt ? new Date(session.createdAt) : new Date();
        const formattedDateTime = createdDate.toLocaleString('vi-VN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
        
        // Lấy tên nhân viên (nếu có)
        const staffName = session.staffName || session.staffEmail || 'Chưa có';
        
        this.chatWindowHeader.innerHTML = `
            <div class="chat-info">
                <h3>${this.escapeHtml(session.customerName || 'Khách vãng lai')}</h3>
                <div class="session-metadata">
                    <div class="meta-item">
                        <i class="fas fa-envelope"></i>
                        <span>${this.escapeHtml(session.customerEmail || 'N/A')}</span>
                    </div>
                    <div class="meta-item">
                        <i class="fas fa-calendar-alt"></i>
                        <span>${formattedDateTime}</span>
                    </div>
                    <div class="meta-item">
                        <i class="fas fa-user-tie"></i>
                        <span>Nhân viên: ${this.escapeHtml(staffName)}</span>
                    </div>
                    <span class="status-badge ${statusClass}">${statusText}</span>
                </div>
            </div>
            <div class="chat-actions">
                ${session.status === 'NEW' ? `
                    <button class="action-btn primary" onclick="window.adminLiveChat.assignSession(${session.id})">
                        <i class="fas fa-hand-paper"></i> Nhận xử lý
                    </button>
                ` : ''}
                ${session.status !== 'CLOSED' ? `
                    <button class="action-btn" onclick="window.adminLiveChat.closeSession(${session.id})">
                        <i class="fas fa-times-circle"></i> Đóng chat
                    </button>
                ` : ''}
            </div>
        `;
    }
    
    async assignSession(sessionId) {
        try {
            const response = await fetch(`/v1/api/live-chat/admin/session/${sessionId}/assign`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            
            if (response.ok) {
                await this.loadSessions();
                if (this.currentSession && this.currentSession.id === sessionId) {
                    const session = this.sessions.find(s => s.id === sessionId);
                    if (session) {
                        this.currentSession = session;
                        this.updateChatHeader(session);
                    }
                }
            }
        } catch (error) {
            // Session assignment failed - user can retry manually
        }
    }
    
    async closeSession(sessionId) {
        if (!confirm('Bạn có chắc muốn đóng cuộc trò chuyện này?')) {
            return;
        }
        
        try {
            const response = await fetch(`/v1/api/live-chat/admin/session/${sessionId}/close`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            
            if (response.ok) {
                await this.loadSessions();
                if (this.currentSession && this.currentSession.id === sessionId) {
                    this.currentSession = null;
                    this.currentMessages = [];
                    this.renderMessages();
                    if (this.chatInputArea) {
                        this.chatInputArea.style.display = 'none';
                    }
                    this.chatWindowHeader.innerHTML = `
                        <div class="chat-info">
                            <h3>Chọn một cuộc trò chuyện</h3>
                        </div>
                    `;
                }
            }
        } catch (error) {
            // Session close failed - user can retry manually
        }
    }
    
    updateStats() {
        if (this.totalChats) {
            this.totalChats.textContent = this.sessions.length;
        }
        if (this.newChats) {
            this.newChats.textContent = this.sessions.filter(s => s.status === 'NEW').length;
        }
        if (this.processingChats) {
            this.processingChats.textContent = this.sessions.filter(s => s.status === 'PROCESSING').length;
        }
    }
    
    scrollToBottom() {
        if (this.chatMessages) {
            this.chatMessages.scrollTop = this.chatMessages.scrollHeight;
        }
    }
    
    getStatusText(status) {
        const statusMap = {
            'NEW': 'Mới',
            'PROCESSING': 'Đang xử lý',
            'CLOSED': 'Đã đóng'
        };
        return statusMap[status] || status;
    }
    
    formatTime(timestamp) {
        if (!timestamp) return 'Vừa xong';
        
        const date = new Date(timestamp);
        const now = new Date();
        const diff = now - date;
        
        if (diff < 60000) return 'Vừa xong';
        if (diff < 3600000) return Math.floor(diff / 60000) + ' phút trước';
        if (diff < 86400000) return Math.floor(diff / 3600000) + ' giờ trước';
        
        return date.toLocaleDateString('vi-VN', { 
            day: '2-digit', 
            month: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
    }
    
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
    
    getAccessToken() {
        const match = document.cookie.match(/(^| )accessToken=([^;]+)/);
        return match ? match[2] : null;
    }
}

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    if (document.getElementById('activeChatList')) {
        window.adminLiveChat = new AdminLiveChat();
    }
});

