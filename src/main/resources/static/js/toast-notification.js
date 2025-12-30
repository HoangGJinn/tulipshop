(function() {
    'use strict';

    let timer1, timer2;

    function createNotificationHTML() {
        if (document.querySelector('.toast')) return;

        const container = document.getElementById('notification-container') || document.body;
        const toastHTML = `
            <div class="toast">
                <div class="toast-content">
                    <div class="check"><i class="fas"></i></div>
                    <div class="message">
                        <span class="text-1"></span>
                        <span class="text-2"></span>
                    </div>
                </div>
                <i class="fas fa-times close"></i>
                <div class="progress"></div>
            </div>`;
        container.insertAdjacentHTML('beforeend', toastHTML);
        
        // Gắn sự kiện click nút đóng một lần duy nhất
        const closeIcon = document.querySelector(".toast .close");
        if (closeIcon && !closeIcon.hasAttribute('data-listener-added')) {
            closeIcon.setAttribute('data-listener-added', 'true');
            closeIcon.addEventListener("click", () => {
                const toast = document.querySelector(".toast");
                if (toast) {
                    toast.classList.remove("active");
                    clearTimeout(timer1);
                    clearTimeout(timer2);
                }
            });
        }
    }

    function showNotification(type, title, message, duration = 5000) {
        // Đảm bảo DOM đã sẵn sàng
        if (!document.body) {
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', () => {
                    showNotification(type, title, message, duration);
                });
                return;
            }
        }
        
        createNotificationHTML();
        
        const toast = document.querySelector(".toast");
        if (!toast) {
            console.error('Failed to create toast element');
            return;
        }
        
        const progress = document.querySelector(".progress");
        const text1 = toast.querySelector(".text-1");
        const text2 = toast.querySelector(".text-2");
        const icon = toast.querySelector(".check i");

        if (!progress || !text1 || !text2 || !icon) {
            console.error('Toast elements not found:', { progress: !!progress, text1: !!text1, text2: !!text2, icon: !!icon });
            return;
        }

        // 1. Dừng mọi animation đang chạy để reset
        toast.classList.remove("active");
        toast.style.setProperty('--duration', `${duration}ms`);
        
        // 2. Ép trình duyệt nhận diện việc reset (Reflow)
        void toast.offsetWidth; 

        // 3. Cập nhật nội dung và màu sắc
        toast.className = `toast ${type}`;
        text1.innerText = title || '';
        text2.innerText = message || '';
        
        icon.className = "fas " + (type === 'success' ? 'fa-check' : 
                                   type === 'error' ? 'fa-xmark' : 'fa-exclamation');

        // 4. Đảm bảo element hiển thị được
        toast.style.display = 'block';
        toast.style.visibility = 'visible';
        
        // 5. Hiển thị thông báo bằng requestAnimationFrame để mượt mà
        requestAnimationFrame(() => {
            requestAnimationFrame(() => {
                toast.classList.add("active");
            });
        });

        // 6. Thiết lập thời gian tự động
        clearTimeout(timer1);
        clearTimeout(timer2);

        timer1 = setTimeout(() => {
            toast.classList.remove("active");
        }, duration);

        // Đợi animation đóng (0.5s) rồi mới dọn dẹp class nếu cần
        timer2 = setTimeout(() => {
            // Có thể thêm logic xóa hoàn toàn khỏi DOM nếu muốn ở đây
        }, duration + 500);
    }

    // Xuất hàm ra global scope
    window.showNotification = showNotification;

    // Helper functions
    window.showSuccess = (t, m, d) => showNotification('success', t, m, d);
    window.showError = (t, m, d) => showNotification('error', t, m, d);
    window.showWarning = (t, m, d) => showNotification('attention', t, m, d);
    
})();
