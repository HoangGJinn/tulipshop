// Header Scripts - User Dropdown và Mobile Menu
// Wrap trong IIFE để tránh global scope pollution và duplicate execution
(function() {
    'use strict';
    
    // Kiểm tra xem đã được khởi tạo chưa
    if (window.headerScriptsInitialized) {
        return;
    }
    window.headerScriptsInitialized = true;

function initUserDropdown() {
    const userMenuToggle = document.getElementById('user-menu-toggle');
    const userDropdownMenu = document.getElementById('user-dropdown-menu');
    
    if (!userMenuToggle || !userDropdownMenu) {
        return false;
    }
    
    // Chỉ đăng ký event listener một lần
    if (userMenuToggle.dataset.listenerAttached === 'true') {
        return true;
    }
    
    // Gắn event listener cho toggle - click vào icon hoặc tên đều hiện dropdown
    userMenuToggle.addEventListener('click', function(e) {
        e.preventDefault();
        e.stopPropagation();
        userDropdownMenu.classList.toggle('active');
    });

    // Cho phép click vào các dropdown-item hoạt động bình thường
    // Không cần stopPropagation trên menu vì các link sẽ tự navigate

    // Đóng dropdown khi click bên ngoài (chỉ đăng ký một lần)
    if (!window.userDropdownOutsideClickHandler) {
        window.userDropdownOutsideClickHandler = function(e) {
            const menu = document.getElementById('user-dropdown-menu');
            const toggle = document.getElementById('user-menu-toggle');
            if (menu && toggle && !toggle.contains(e.target) && !menu.contains(e.target)) {
                menu.classList.remove('active');
            }
        };
        document.addEventListener('click', window.userDropdownOutsideClickHandler);
    }
    
    // Đánh dấu đã đăng ký
    userMenuToggle.dataset.listenerAttached = 'true';
    
    return true;
}

function initMobileMenu() {
    const menuToggle = document.getElementById('menu-toggle');
    const mobileMenu = document.getElementById('mobile-menu');
    const closeMenu = document.getElementById('close-menu');
    
    if (menuToggle && mobileMenu) {
        menuToggle.onclick = function() {
            mobileMenu.classList.add('active');
        };
    }
    
    if (closeMenu && mobileMenu) {
        closeMenu.onclick = function() {
            mobileMenu.classList.remove('active');
        };
    }
}

// Khởi tạo khi DOM sẵn sàng
function initHeaderScripts() {
    initMobileMenu();
    initUserDropdown();
}

// Chạy ngay nếu DOM đã sẵn sàng
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initHeaderScripts);
} else {
    initHeaderScripts();
}

// Thử lại sau một khoảng thời gian để đảm bảo Spring Security đã render
setTimeout(initUserDropdown, 100);

// Hero Slider functionality - Chỉ khởi tạo nếu có slides
(function initSlider() {
    const slides = document.querySelectorAll('.slider-item');
    const dots = document.querySelectorAll('.dot');
    
    if (slides.length === 0) {
        return; // Không có slider thì không khởi tạo
    }
    
    let currentSlideIndex = 0;

    function showSlide(index) {
        if (index >= slides.length) {
            currentSlideIndex = 0;
        } else if (index < 0) {
            currentSlideIndex = slides.length - 1;
        } else {
            currentSlideIndex = index;
        }

        // Hide all slides
        slides.forEach(slide => slide.classList.remove('active'));
        dots.forEach(dot => dot.classList.remove('active'));

        // Show current slide
        if (slides[currentSlideIndex]) {
            slides[currentSlideIndex].classList.add('active');
        }
        if (dots[currentSlideIndex]) {
            dots[currentSlideIndex].classList.add('active');
        }
    }

    function changeSlide(direction) {
        showSlide(currentSlideIndex + direction);
    }

    function currentSlide(index) {
        showSlide(index - 1);
    }

    // Make functions global for onclick handlers
    window.changeSlide = changeSlide;
    window.currentSlide = currentSlide;

    // Auto-play slider
    setInterval(function() {
        changeSlide(1);
    }, 5000); // Change slide every 5 seconds

    // Initialize first slide
    showSlide(0);
})();

// Search Panel Toggle
function toggleSearchPanel() {
    const panel = document.getElementById('searchOverlay');
    const input = document.getElementById('searchInput');
    
    if (panel) {
        const isOpen = panel.classList.contains('open');
        
        if (isOpen) {
            // Đóng panel
            panel.classList.remove('open');
            document.body.style.overflow = '';
            // Reset scroll position
            document.body.style.position = '';
            document.body.style.top = '';
        } else {
            // Mở panel
            panel.classList.add('open');
            document.body.style.overflow = 'hidden';
            // Focus vào input sau khi animation hoàn tất
            setTimeout(() => {
                if (input) {
                    input.focus();
                }
            }, 300);
        }
    }
}

// Make toggleSearchPanel global
window.toggleSearchPanel = toggleSearchPanel;

// Header Scroll Effect - Thêm shadow khi scroll
function initHeaderScrollEffect() {
    const header = document.getElementById('site-header');
    if (!header) return;
    
    const scrollThreshold = 10; // Scroll 10px thì kích hoạt
    
    function handleScroll() {
        const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
        
        if (scrollTop > scrollThreshold) {
            header.classList.add('scrolled');
            document.body.classList.add('header-scrolled');
        } else {
            header.classList.remove('scrolled');
            document.body.classList.remove('header-scrolled');
        }
    }
    
    // Throttle scroll event để tối ưu performance
    let ticking = false;
    window.addEventListener('scroll', function() {
        if (!ticking) {
            window.requestAnimationFrame(function() {
                handleScroll();
                ticking = false;
            });
            ticking = true;
        }
    }, { passive: true });
    
    // Kiểm tra ngay khi load nếu đã scroll
    if (window.pageYOffset > scrollThreshold) {
        header.classList.add('scrolled');
        document.body.classList.add('header-scrolled');
    }
}

// Khởi tạo scroll effect
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initHeaderScrollEffect);
} else {
    initHeaderScrollEffect();
}

})(); // End IIFE

