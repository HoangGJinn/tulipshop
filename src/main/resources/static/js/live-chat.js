/**
 * Live Chat Widget - Chat với nhân viên CSKH
 */
class LiveChat {
    constructor() {
        this.stompClient = null;
        this.connected = false;
        this.sessionToken = null;
        this.sessionId = null;
        this.messages = [];
        this.staffName = null;
        this.retryCount = 0;
        this.maxRetries = 3;

        this.initElements();
        this.initEventListeners();
        this.init();
    }

    initElements() {
        this.bubble = document.getElementById('liveChatBubble');
        this.window = document.getElementById('liveChatWindow');
        this.messagesContainer = document.getElementById('liveChatMessages');
        this.input = document.getElementById('liveChatInput');
        this.sendButton = document.getElementById('liveChatSendButton');
        this.closeButton = document.getElementById('closeLiveChat');
        this.statusText = document.getElementById('statusText');
        this.badge = document.getElementById('liveChatBadge');
        this.typingIndicator = document.getElementById('liveChatTypingIndicator');
        this.welcomeMessage = document.getElementById('welcomeMessage');
    }

    initEventListeners() {
        // Toggle chat window
        if (this.bubble) {
            this.bubble.addEventListener('click', () => this.toggleWindow());
        }

        // Close chat
        if (this.closeButton) {
            this.closeButton.addEventListener('click', () => this.closeWindow());
        }

        // Send message
        if (this.sendButton) {
            this.sendButton.addEventListener('click', () => this.sendMessage());
        }

        // Enter key to send
        if (this.input) {
            this.input.addEventListener('keypress', (e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    this.sendMessage();
                }
            });
        }
    }

    async init() {
        // Kiểm tra xem user đã đăng nhập chưa bằng cách gọi API
        const isLoggedIn = await this.checkLoginStatus();

        if (!isLoggedIn) {
            // Chưa đăng nhập - không cho phép sử dụng live chat
            return;
        }

        // Restore session token từ localStorage nếu có (để tái sử dụng session cũ)
        const savedToken = localStorage.getItem('liveChatToken');
        if (savedToken) {
            this.sessionToken = savedToken;
        }
    }

    async checkLoginStatus() {
        try {
            const response = await fetch('/v1/api/live-chat/check-auth', {
                method: 'GET',
                credentials: 'include' // Quan trọng: gửi cookies
            });
            return response.ok;
        } catch (error) {
            return false;
        }
    }

    async getOrCreateSession() {
        try {
            let url = '/v1/api/live-chat/session';
            const params = new URLSearchParams();

            if (this.sessionToken) {
                params.append('sessionToken', this.sessionToken);
            }

            if (params.toString()) {
                url += '?' + params.toString();
            }

            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include' // Gửi JWT cookies
            });

            // Kiểm tra Content-Type để đảm bảo là JSON
            const contentType = response.headers.get('content-type');
            if (!contentType || !contentType.includes('application/json')) {
                const text = await response.text();
                throw new Error('Server returned HTML instead of JSON. Status: ' + response.status);
            }

            if (response.ok) {
                const data = await response.json();

                this.sessionToken = data.sessionToken;
                this.sessionId = data.sessionId;
                localStorage.setItem('liveChatToken', this.sessionToken);
                this.retryCount = 0; // Reset retry count on success

                // Load tin nhắn cũ
                if (data.messages && data.messages.length > 0) {
                    this.messages = data.messages;
                    this.renderMessages();
                    
                    // Kiểm tra xem đã có tin nhắn từ staff chưa
                    const staffMessage = this.messages.find(msg => msg.senderType === 'SUPPORT_AGENT');
                    if (staffMessage) {
                        this.staffName = staffMessage.senderName;
                        if (this.connected) {
                            const statusText = this.staffName ? `Nhân viên ${this.staffName} đang hỗ trợ` : 'Nhân viên đang hỗ trợ';
                            this.updateStatus(statusText, true);
                        }
                    }
                } else {
                    this.messages = []; // Clear old messages
                }
            } else {
                // Xử lý lỗi từ server
                let errorData;
                try {
                    errorData = await response.json();
                } catch (e) {
                    errorData = {
                        error: 'Unknown error',
                        message: `HTTP ${response.status}: ${response.statusText}`
                    };
                }

                if (this.retryCount < this.maxRetries) {
                    this.retryCount++;
                    this.updateStatus(`Đang thử lại... (${this.retryCount}/${this.maxRetries})`, false);

                    // Thử lại sau 3 giây
                    setTimeout(() => {
                        if (!this.sessionToken) {
                            this.getOrCreateSession();
                        }
                    }, 3000);
                } else {
                    this.updateStatus('Không thể kết nối. Vui lòng refresh trang.', false);
                }
            }
        } catch (error) {

            if (this.retryCount < this.maxRetries) {
                this.retryCount++;
                this.updateStatus(`Đang thử lại... (${this.retryCount}/${this.maxRetries})`, false);

                // Thử lại sau 3 giây
                setTimeout(() => {
                    if (!this.sessionToken) {
                        this.getOrCreateSession();
                    }
                }, 3000);
            } else {
                this.updateStatus('Không thể kết nối. Vui lòng refresh trang.', false);
            }
        }
    }

    connect() {
        if (!this.sessionToken) {
            this.updateStatus('Đang tạo session...', false);
            // Thử tạo session lại
            this.getOrCreateSession().then(() => {
                if (this.sessionToken) {
                    this.connect();
                }
            });
            return;
        }

        try {
            const socket = new SockJS('/ws-chat');
            this.stompClient = Stomp.over(socket);
            this.stompClient.debug = null; // Tắt debug logs

            // Lấy token nếu có (cho user đã đăng nhập)
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
        } catch (error) {
            this.onError(error);
        }
    }

    onConnected() {
        this.connected = true;
        
        // Kiểm tra xem đã có tin nhắn từ staff chưa để update status cho đúng
        const staffMessage = this.messages.find(msg => msg.senderType === 'SUPPORT_AGENT');
        if (staffMessage) {
            this.staffName = staffMessage.senderName;
            const statusText = this.staffName ? `Nhân viên ${this.staffName} đang hỗ trợ` : 'Nhân viên đang hỗ trợ';
            this.updateStatus(statusText, true);
        } else {
            this.updateStatus('Đang chờ nhân viên...', false);
        }

        // Subscribe vào kênh riêng của session này
        this.stompClient.subscribe(`/topic/chat/${this.sessionToken}`, (message) => {
            this.onMessageReceived(JSON.parse(message.body));
        });
    }

    onError(error) {
        this.connected = false;
        this.updateStatus('Mất kết nối. Đang thử lại...', false);

        // Thử kết nối lại sau 3 giây
        setTimeout(() => {
            if (!this.connected && this.sessionToken) {
                this.connect();
            }
        }, 3000);
    }

    onMessageReceived(data) {
        if (data.type === 'MESSAGES_READ') {
            // Staff đã đọc tin nhắn
            return;
        }

        if (data.type === 'SESSION_CLOSED') {
            // Session đã bị đóng bởi staff
            this.handleSessionClosed(data.message || 'Cuộc trò chuyện đã kết thúc');
            return;
        }

        // Tin nhắn mới
        const message = data;

        // Kiểm tra xem có phải tin nhắn của mình vừa gửi không (để thay thế optimistic message)
        const isMyMessage = message.senderType === 'CUSTOMER' &&
            this.messages.length > 0 &&
            this.messages[this.messages.length - 1].senderType === 'CUSTOMER' &&
            typeof this.messages[this.messages.length - 1].id === 'number'; // Temporary ID

        if (isMyMessage) {
            // Thay thế optimistic message bằng message thật từ server
            this.messages[this.messages.length - 1] = message;
        } else {
            // Thêm tin nhắn mới
            this.messages.push(message);
        }

        // Cập nhật status khi nhận tin nhắn từ staff
        if (message.senderType === 'SUPPORT_AGENT') {
            // Lưu tên nhân viên nếu chưa có
            if (!this.staffName && message.senderName) {
                this.staffName = message.senderName;
            }
            const statusText = this.staffName ? `Nhân viên ${this.staffName} đang hỗ trợ` : 'Nhân viên đang hỗ trợ';
            this.updateStatus(statusText, true);
        }

        this.renderMessages();

        // Hiển thị badge nếu window đang đóng
        if (!this.window.classList.contains('open')) {
            this.showBadge();
        }

        // Scroll xuống cuối
        this.scrollToBottom();
    }

    sendMessage() {
        const content = this.input.value.trim();

        if (!content) {
            return;
        }

        if (!this.connected) {
            this.updateStatus('Đang kết nối...', false);
            // Thử kết nối lại
            this.connect();
            // Thử gửi lại sau 2 giây
            setTimeout(() => {
                if (this.connected) {
                    this.sendMessage();
                } else {
                    if (typeof window.showNotification === 'function') {
                        window.showNotification('error', 'Lỗi kết nối', 'Không thể kết nối đến server. Vui lòng thử lại!');
                    }
                }
            }, 2000);
            return;
        }

        if (!this.sessionToken) {
            if (typeof window.showNotification === 'function') {
                window.showNotification('error', 'Lỗi', 'Phiên chat chưa được khởi tạo. Vui lòng thử lại!');
            }
            return;
        }

        if (!this.stompClient) {
            return;
        }

        try {
            // Disable input trong lúc gửi
            this.sendButton.disabled = true;

            // Gửi qua WebSocket
            const message = {
                sessionToken: this.sessionToken,
                content: content,
                senderType: 'CUSTOMER'
            };

            this.stompClient.send('/app/chat.sendMessage', {}, JSON.stringify(message));

            // Optimistic update: Hiển thị tin nhắn ngay lập tức
            const optimisticMessage = {
                id: Date.now(), // Temporary ID
                sessionId: this.sessionId,
                sessionToken: this.sessionToken,
                content: content,
                senderType: 'CUSTOMER',
                senderId: null,
                senderName: 'Bạn',
                isRead: false,
                timestamp: new Date().toISOString()
            };
            this.messages.push(optimisticMessage);
            this.renderMessages();

            // Clear input ngay lập tức để UX tốt hơn
            this.input.value = '';
            this.sendButton.disabled = false;

            // Tin nhắn thật sẽ được thêm vào khi nhận từ server qua onMessageReceived
            // và sẽ thay thế tin nhắn optimistic này
        } catch (error) {
            this.sendButton.disabled = false;
            alert('Không thể gửi tin nhắn. Vui lòng thử lại.');
        }
    }

    renderMessages() {
        if (!this.messagesContainer) return;

        this.messagesContainer.innerHTML = '';

        this.messages.forEach(msg => {
            const messageDiv = document.createElement('div');
            messageDiv.className = 'message';

            if (msg.senderType === 'CUSTOMER') {
                messageDiv.classList.add('customer');
            } else if (msg.senderType === 'SUPPORT_AGENT') {
                messageDiv.classList.add('staff');
            } else {
                messageDiv.classList.add('system');
            }

            // Thêm tên nhân viên nếu là tin nhắn từ staff
            if (msg.senderType === 'SUPPORT_AGENT') {
                const senderName = document.createElement('div');
                senderName.className = 'message-sender-name';
                senderName.textContent = msg.senderName || 'Nhân viên CSKH';
                messageDiv.appendChild(senderName);
            }

            const bubble = document.createElement('div');
            bubble.className = 'message-bubble';
            bubble.textContent = msg.content;

            const time = document.createElement('div');
            time.className = 'message-time';
            time.textContent = this.formatTime(msg.timestamp);

            bubble.appendChild(time);
            messageDiv.appendChild(bubble);
            this.messagesContainer.appendChild(messageDiv);
        });

        this.scrollToBottom();
    }

    handleSessionClosed(message) {
        // Cập nhật status
        this.updateStatus('Cuộc trò chuyện đã kết thúc', false);
        
        // Ẩn input area và hiển thị nút bắt đầu mới
        const inputArea = document.querySelector('.live-chat-input');
        if (inputArea) {
            inputArea.innerHTML = `
                <div style="padding: 16px; text-align: center; background: white; border-top: 1px solid #e0e0e0;">
                    <button onclick="window.liveChat.startNewConversation()" 
                            style="width: 100%; padding: 12px 20px; background: linear-gradient(135deg, #667eea, #764ba2); 
                                   color: white; border: none; border-radius: 8px; font-size: 14px; font-weight: 600; 
                                   cursor: pointer; transition: all 0.3s ease; box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);">
                        <i class="fas fa-plus-circle" style="margin-right: 8px;"></i>
                        Bắt đầu cuộc trò chuyện mới
                    </button>
                </div>
            `;
        }
        
        // Hiển thị thông báo trong chat
        const systemMessage = {
            id: Date.now(),
            content: message,
            senderType: 'SYSTEM',
            timestamp: new Date().toISOString()
        };
        this.messages.push(systemMessage);
        this.renderMessages();
        
        // Xóa sessionToken để tạo phiên mới lần sau
        localStorage.removeItem('liveChatToken');
        this.sessionToken = null;
        this.sessionId = null;
        this.staffName = null;
        
        // Hiển thị notification nếu có
        if (typeof window.showNotification === 'function') {
            window.showNotification('info', 'Cuộc trò chuyện đã kết thúc', message, 5000);
        }
    }
    
    async startNewConversation() {
        // Disconnect WebSocket cũ trước
        if (this.stompClient && this.connected) {
            try {
                this.stompClient.disconnect(() => {});
            } catch (e) {
                // Ignore disconnect errors during cleanup
            }
            this.connected = false;
            this.stompClient = null;
        }
        
        // Reset trạng thái
        this.messages = [];
        this.sessionToken = null;
        this.sessionId = null;
        this.staffName = null;
        localStorage.removeItem('liveChatToken');
        
        // Hiển thị loading
        this.updateStatus('Đang tạo cuộc trò chuyện mới...', false);
        
        // Restore input area nhưng disable tạm thời
        const inputArea = document.querySelector('.live-chat-input');
        if (inputArea) {
            inputArea.innerHTML = `
                <div class="input-group">
                    <input type="text" id="liveChatInput" placeholder="Đang kết nối..." maxlength="1000" disabled>
                    <button id="liveChatSendButton" disabled>
                        <i class="fas fa-paper-plane"></i>
                    </button>
                </div>
            `;
            
            // Re-init elements
            this.input = document.getElementById('liveChatInput');
            this.sendButton = document.getElementById('liveChatSendButton');
        }
        
        // Clear messages UI
        if (this.messagesContainer) {
            this.messagesContainer.innerHTML = '';
        }
        
        // Tạo session mới
        await this.getOrCreateSession();
        
        // Kết nối WebSocket mới với session token mới và đợi kết nối thành công
        if (this.sessionToken) {
            // Kết nối và đợi
            await new Promise((resolve) => {
                this.connect();
                
                // Đợi kết nối thành công (tối đa 3 giây)
                const checkConnection = setInterval(() => {
                    if (this.connected) {
                        clearInterval(checkConnection);
                        resolve();
                    }
                }, 100);
                
                // Timeout sau 3 giây
                setTimeout(() => {
                    clearInterval(checkConnection);
                    resolve();
                }, 3000);
            });
        }
        
        // Enable input sau khi WebSocket đã kết nối
        if (this.input && this.sendButton) {
            this.input.disabled = false;
            this.input.placeholder = 'Nhập tin nhắn...';
            this.sendButton.disabled = false;
            
            // Add event listeners bây giờ
            this.sendButton.addEventListener('click', () => this.sendMessage());
            this.input.addEventListener('keypress', (e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    this.sendMessage();
                }
            });
        }
        
        this.updateStatus('Đang chờ nhân viên...', false);
        
        if (typeof window.showNotification === 'function') {
            window.showNotification('success', 'Thành công', 'Đã tạo cuộc trò chuyện mới. Bạn có thể gửi tin nhắn ngay!', 3000);
        }
    }

    toggleWindow() {
        if (this.window.classList.contains('open')) {
            this.closeWindow();
        } else {
            this.openWindow();
        }
    }

    async openWindow() {
        // Kiểm tra xem user đã đăng nhập chưa
        const isLoggedIn = await this.checkLoginStatus();

        if (!isLoggedIn) {
            // Chưa đăng nhập - hiển thị thông báo
            if (typeof window.showNotification === 'function') {
                window.showNotification('attention', 'Yêu cầu đăng nhập', 'Vui lòng đăng nhập để sử dụng tính năng chat với nhân viên!', 3000);
            }
            // Redirect đến trang login sau 1 giây
            setTimeout(() => {
                window.location.href = '/login';
            }, 1000);
            return;
        }

        this.window.classList.add('open');
        this.hideBadge();

        // Luôn gọi getOrCreateSession() để validate và load session
        // - Nếu sessionToken hợp lệ: backend sẽ trả về session cũ + messages
        // - Nếu sessionToken null hoặc session đã CLOSED: backend sẽ tạo mới
        await this.getOrCreateSession();

        // Kết nối WebSocket nếu chưa kết nối
        if (!this.connected) {
            this.connect();
        }

        this.scrollToBottom();

        // Đánh dấu đã đọc
        if (this.sessionId) {
            fetch(`/v1/api/live-chat/session/${this.sessionId}/read`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                }
            }).catch(err => {});
        }
    }

    closeWindow() {
        this.window.classList.remove('open');
    }

    scrollToBottom() {
        if (this.messagesContainer) {
            this.messagesContainer.scrollTop = this.messagesContainer.scrollHeight;
        }
    }

    updateStatus(text, isOnline) {
        if (this.statusText) {
            this.statusText.textContent = text;
            const statusIcon = this.statusText.previousElementSibling;
            if (statusIcon) {
                statusIcon.style.color = isOnline ? '#2ecc71' : '#95a5a6';
            }
        }
    }

    showBadge() {
        if (this.badge) {
            this.badge.style.display = 'flex';
        }
    }

    hideBadge() {
        if (this.badge) {
            this.badge.style.display = 'none';
        }
    }

    formatTime(timestamp) {
        if (!timestamp) return 'Vừa xong';

        const date = new Date(timestamp);
        const now = new Date();
        const diff = now - date;

        if (diff < 60000) return 'Vừa xong';
        if (diff < 3600000) return Math.floor(diff / 60000) + ' phút trước';
        if (diff < 86400000) return Math.floor(diff / 3600000) + ' giờ trước';

        return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    }

    getAccessToken() {
        const match = document.cookie.match(/(^| )accessToken=([^;]+)/);
        return match ? match[2] : null;
    }
}

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    if (document.getElementById('liveChatBubble')) {
        window.liveChat = new LiveChat();
    }
});

