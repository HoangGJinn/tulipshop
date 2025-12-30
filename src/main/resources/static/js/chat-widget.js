/**
 * Chat Widget - Hiển thị ở góc màn hình
 * 
 * Sử dụng WebSocket với STOMP để real-time messaging
 * Lịch sử chat lấy bằng REST API
 */

class ChatWidget {
    constructor() {
        this.stompClient = null;
        this.chatRoomId = null;
        this.isConnected = false;
        this.isOpen = false;
        this.typingTimeout = null;

        this.init();
    }

    init() {
        this.createWidgetHTML();
        this.bindEvents();

        // Only load chat room if user is logged in
        if (this.isUserLoggedIn()) {
            this.loadChatRoom();
        } else {
            // Show widget but disable until login
            this.updateStatus('Đăng nhập để bắt đầu chat');
        }
    }

    isUserLoggedIn() {
        // Check if user is logged in by checking for token or user ID
        return this.getToken() !== null || window.currentUserId !== null;
    }

    createWidgetHTML() {
        const widgetHTML = `
            <div id="chat-widget" class="chat-widget">
                <!-- Chat Button (floating) -->
                <button id="chat-toggle-btn" class="chat-toggle-btn" title="Chat với chúng tôi">
                    <i class="fas fa-comments"></i>
                    <span class="chat-badge" id="chat-badge" style="display: none;">0</span>
                </button>

                <!-- Chat Window -->
                <div id="chat-window" class="chat-window" style="display: none;">
                    <div class="chat-header">
                        <div class="chat-header-info">
                            <h6 class="mb-0">Tư vấn trực tuyến</h6>
                            <small id="chat-status" class="text-muted">Đang kết nối...</small>
                        </div>
                        <button id="chat-close-btn" class="btn btn-sm btn-link text-white">
                            <i class="fas fa-times"></i>
                        </button>
                    </div>
                    
                    <div id="chat-messages" class="chat-messages">
                        <div class="chat-loading text-center py-3">
                            <div class="spinner-border spinner-border-sm text-primary" role="status">
                                <span class="visually-hidden">Đang tải...</span>
                            </div>
                        </div>
                    </div>
                    
                    <div id="chat-typing-indicator" class="chat-typing-indicator" style="display: none;">
                        <small class="text-muted"><i class="fas fa-ellipsis-h"></i> Đang gõ...</small>
                    </div>
                    
                    <div class="chat-input-container">
                        <input type="text" id="chat-input" class="chat-input" 
                               placeholder="Nhập tin nhắn..." disabled>
                        <button id="chat-send-btn" class="chat-send-btn" disabled>
                            <i class="fas fa-paper-plane"></i>
                        </button>
                    </div>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', widgetHTML);
    }

    bindEvents() {
        // Toggle chat window
        document.getElementById('chat-toggle-btn').addEventListener('click', () => {
            this.toggleChat();
        });

        // Close chat
        document.getElementById('chat-close-btn').addEventListener('click', () => {
            this.closeChat();
        });

        // Send message
        document.getElementById('chat-send-btn').addEventListener('click', () => {
            this.sendMessage();
        });

        // Enter key to send
        document.getElementById('chat-input').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.sendMessage();
            } else {
                this.handleTyping();
            }
        });
    }

    async loadChatRoom() {
        if (!this.isUserLoggedIn()) {
            this.updateStatus('Vui lòng đăng nhập');
            return;
        }

        try {
            const response = await fetch('/v1/api/chat/rooms', {
                method: 'POST',
                credentials: 'include'
            });

            if (response.status === 401) {
                // User not authenticated
                this.updateStatus('Vui lòng đăng nhập');
                this.disableInput();
                return;
            }

            if (!response.ok) {
                throw new Error('Failed to load chat room');
            }

            const room = await response.json();
            this.chatRoomId = room.id;

            // Load chat history
            await this.loadChatHistory();

            // Connect WebSocket
            this.connectWebSocket();

        } catch (error) {
            console.error('Error loading chat room:', error);
            this.updateStatus('Không thể kết nối. Vui lòng thử lại.');
            this.showError('Không thể kết nối. Vui lòng đăng nhập và thử lại.');
        }
    }

    async loadChatHistory() {
        if (!this.chatRoomId) return;

        try {
            const response = await fetch(
                `/v1/api/chat/rooms/${this.chatRoomId}/messages?page=0&size=50`,
                { credentials: 'include' }
            );

            if (!response.ok) throw new Error('Failed to load history');

            const data = await response.json();
            const messages = data.content || [];

            // Clear loading
            const loadingEl = document.getElementById('chat-loading');
            if (loadingEl) loadingEl.style.display = 'none';

            const messagesContainer = document.getElementById('chat-messages');
            if (messages.length === 0) {
                messagesContainer.innerHTML = `
                    <div class="text-center py-4 text-muted">
                        <i class="fas fa-comments mb-2" style="font-size: 2rem; opacity: 0.3;"></i>
                        <p>Chưa có tin nhắn nào. Hãy bắt đầu cuộc trò chuyện!</p>
                    </div>
                `;
            } else {
                // Clear and render messages (newest first, reverse for display)
                messagesContainer.innerHTML = '';
                messages.reverse().forEach(msg => this.renderMessage(msg));
            }

            // Scroll to bottom
            this.scrollToBottom();

        } catch (error) {
            console.error('Error loading chat history:', error);
        }
    }

    connectWebSocket() {
        // Get token from cookie
        const token = this.getToken();

        // Check verification: either token exists OR window.currentUserId exists (session auth)
        if (!token && !this.getCurrentUserId()) {
            console.warn('No authentication token found - user needs to login');
            this.updateStatus('Vui lòng đăng nhập để chat');
            this.disableInput();

            // Show login message in chat window if it's open
            if (this.isOpen) {
                const messagesContainer = document.getElementById('chat-messages');
                if (messagesContainer) {
                    const loadingEl = document.getElementById('chat-loading');
                    if (loadingEl) loadingEl.style.display = 'none';
                    messagesContainer.innerHTML = `
                        <div class="text-center py-4">
                            <i class="fas fa-user-lock mb-3 text-primary" style="font-size: 3rem;"></i>
                            <h6>Đăng nhập để bắt đầu chat</h6>
                            <p class="text-muted small">Vui lòng đăng nhập để được tư vấn trực tuyến</p>
                            <a href="/login" class="btn btn-primary btn-sm mt-2">Đăng nhập</a>
                        </div>
                    `;
                }
            }
            return;
        }

        // Connect to WebSocket
        // Only append token if it exists
        const url = token ? '/ws-chat?token=' + encodeURIComponent(token) : '/ws-chat';
        const socket = new SockJS(url);
        this.stompClient = Stomp.over(socket);

        // Disable debug logs
        this.stompClient.debug = () => { };

        this.stompClient.connect({},
            (frame) => {
                this.isConnected = true;
                this.updateStatus('Đã kết nối');
                this.enableInput();

                // Subscribe to chat room messages
                this.stompClient.subscribe(
                    `/topic/chat/rooms/${this.chatRoomId}/messages`,
                    (message) => {
                        const msg = JSON.parse(message.body);
                        this.renderMessage(msg);
                        this.scrollToBottom();
                        this.updateUnreadBadge();
                    }
                );

                // Subscribe to typing indicator
                this.stompClient.subscribe(
                    `/topic/chat/rooms/${this.chatRoomId}/typing`,
                    (message) => {
                        const typing = JSON.parse(message.body);
                        if (typing.userId !== this.getCurrentUserId()) {
                            this.showTypingIndicator(typing.isTyping);
                        }
                    }
                );

                // Subscribe to room status updates
                this.stompClient.subscribe(
                    `/topic/chat/rooms/${this.chatRoomId}/assigned`,
                    (message) => {
                        this.updateStatus('Nhân viên đã nhận cuộc trò chuyện');
                    }
                );

                // Join room
                this.joinRoom();
            },
            (error) => {
                console.error('WebSocket connection error:', error);
                this.updateStatus('Mất kết nối. Đang thử lại...');
                this.isConnected = false;
                this.disableInput();

                // Retry connection after 5 seconds
                setTimeout(() => this.connectWebSocket(), 5000);
            }
        );
    }

    joinRoom() {
        if (!this.stompClient || !this.isConnected || !this.chatRoomId) return;

        this.stompClient.send('/app/chat/join', {}, JSON.stringify({
            chatRoomId: this.chatRoomId
        }));
    }

    sendMessage() {
        if (!this.isUserLoggedIn()) {
            if (confirm('Bạn cần đăng nhập để gửi tin nhắn. Bạn có muốn đăng nhập không?')) {
                window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname);
            }
            return;
        }

        const input = document.getElementById('chat-input');
        const content = input.value.trim();

        if (!content || !this.isConnected || !this.chatRoomId) {
            if (!this.isConnected) {
                alert('Đang kết nối... Vui lòng đợi một chút.');
            }
            return;
        }

        // Send via WebSocket
        this.stompClient.send('/app/chat/send', {}, JSON.stringify({
            chatRoomId: this.chatRoomId,
            content: content,
            type: 'TEXT'
        }));

        // Clear input
        input.value = '';

        // Stop typing indicator
        this.sendTypingIndicator(false);
    }

    handleTyping() {
        // Debounce typing indicator
        clearTimeout(this.typingTimeout);

        this.sendTypingIndicator(true);

        this.typingTimeout = setTimeout(() => {
            this.sendTypingIndicator(false);
        }, 1000);
    }

    sendTypingIndicator(isTyping) {
        if (!this.stompClient || !this.isConnected || !this.chatRoomId) return;

        this.stompClient.send('/app/chat/typing', {}, JSON.stringify({
            chatRoomId: this.chatRoomId,
            isTyping: isTyping
        }));
    }

    renderMessage(message) {
        const messagesContainer = document.getElementById('chat-messages');
        const isOwnMessage = message.senderId === this.getCurrentUserId();

        const messageHTML = `
            <div class="chat-message ${isOwnMessage ? 'own' : ''}">
                ${!isOwnMessage ? `<div class="chat-message-avatar">
                    <img src="${message.senderAvatar || '/images/default-avatar.png'}" 
                         alt="${message.senderName}" onerror="this.src='/images/default-avatar.png'">
                </div>` : ''}
                <div class="chat-message-content">
                    ${!isOwnMessage ? `<div class="chat-message-name">${this.escapeHtml(message.senderName)}</div>` : ''}
                    <div class="chat-message-text">${this.escapeHtml(message.content)}</div>
                    <div class="chat-message-time">${this.formatTime(message.createdAt)}</div>
                </div>
            </div>
        `;

        messagesContainer.insertAdjacentHTML('beforeend', messageHTML);
    }

    showTypingIndicator(show) {
        document.getElementById('chat-typing-indicator').style.display =
            show ? 'block' : 'none';
        if (show) {
            this.scrollToBottom();
        }
    }

    toggleChat() {
        // Check if user is logged in
        if (!this.isUserLoggedIn()) {
            // Redirect to login or show message
            if (confirm('Bạn cần đăng nhập để sử dụng chat. Bạn có muốn đăng nhập không?')) {
                window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname);
            }
            return;
        }

        // If chat room not loaded yet, load it now
        if (!this.chatRoomId && this.isUserLoggedIn()) {
            this.loadChatRoom();
        }

        this.isOpen = !this.isOpen;
        const window = document.getElementById('chat-window');
        window.style.display = this.isOpen ? 'block' : 'none';

        if (this.isOpen) {
            this.scrollToBottom();
            // Mark as seen
            if (this.chatRoomId) {
                fetch(`/v1/api/chat/rooms/${this.chatRoomId}/seen`, {
                    method: 'POST',
                    credentials: 'include'
                }).catch(err => console.error('Error marking as seen:', err));
            }
        }
    }

    closeChat() {
        this.isOpen = false;
        document.getElementById('chat-window').style.display = 'none';
    }

    scrollToBottom() {
        const messagesContainer = document.getElementById('chat-messages');
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    updateStatus(text) {
        document.getElementById('chat-status').textContent = text;
    }

    enableInput() {
        document.getElementById('chat-input').disabled = false;
        document.getElementById('chat-send-btn').disabled = false;
    }

    disableInput() {
        document.getElementById('chat-input').disabled = true;
        document.getElementById('chat-send-btn').disabled = true;
    }

    showError(message) {
        const messagesContainer = document.getElementById('chat-messages');
        messagesContainer.innerHTML = `
            <div class="alert alert-danger m-2">
                <i class="fas fa-exclamation-circle"></i> ${this.escapeHtml(message)}
            </div>
        `;
    }

    updateUnreadBadge() {
        if (!this.chatRoomId) return;

        fetch(`/v1/api/chat/rooms/${this.chatRoomId}/unread`, {
            credentials: 'include'
        })
            .then(res => res.json())
            .then(count => {
                const badge = document.getElementById('chat-badge');
                if (count > 0 && !this.isOpen) {
                    badge.textContent = count > 99 ? '99+' : count;
                    badge.style.display = 'block';
                } else {
                    badge.style.display = 'none';
                }
            })
            .catch(err => console.error('Error updating badge:', err));
    }

    getToken() {
        // Try to get from cookie
        const cookies = document.cookie.split(';');
        for (let cookie of cookies) {
            const [name, value] = cookie.trim().split('=');
            if (name === 'accessToken' && value) {
                return decodeURIComponent(value);
            }
        }
        return null;
    }

    getCurrentUserId() {
        // This should be set from server-side
        return window.currentUserId || null;
    }

    formatTime(dateString) {
        const date = new Date(dateString);
        const now = new Date();
        const diff = now - date;

        if (diff < 60000) return 'Vừa xong';
        if (diff < 3600000) return `${Math.floor(diff / 60000)} phút trước`;
        if (diff < 86400000) return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
        return date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// Initialize chat widget when DOM is ready
// Always show the widget button, even for non-logged-in users
(function () {
    function initChatWidget() {
        // Only create one instance
        if (window.chatWidget) {
            return;
        }

        try {
            window.chatWidget = new ChatWidget();
            console.log('Chat widget initialized');
        } catch (error) {
            console.error('Error initializing chat widget:', error);
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initChatWidget);
    } else {
        // DOM already loaded
        initChatWidget();
    }
})();

