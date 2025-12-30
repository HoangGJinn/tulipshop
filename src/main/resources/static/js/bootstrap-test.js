/**
 * Bootstrap Test Script
 * Kiểm tra xem Bootstrap có load đúng không
 */

document.addEventListener('DOMContentLoaded', function() {
    console.log('🔍 Checking Bootstrap...');
    
    // Check if Bootstrap is loaded
    if (typeof bootstrap !== 'undefined') {
        console.log('✅ Bootstrap is loaded:', bootstrap.Dropdown);
    } else {
        console.error('❌ Bootstrap is NOT loaded!');
        return;
    }
    
    // Check notification dropdown
    const dropdownBtn = document.getElementById('notificationDropdownBtn');
    if (!dropdownBtn) {
        console.error('❌ Notification button NOT found');
        return;
    }
    
    console.log('✅ Notification button found');
    
    let dropdown = null;
    let isDropdownOpen = false;
    
    try {
        // Khởi tạo dropdown với config
        dropdown = new bootstrap.Dropdown(dropdownBtn, {
            autoClose: false
        });
        console.log('✅ Bootstrap Dropdown initialized:', dropdown);
        
        // Ngăn chặn việc đóng dropdown
        dropdownBtn.addEventListener('hide.bs.dropdown', function(e) {
            console.log('⚠️ Attempting to hide dropdown, preventing...');
            if (isDropdownOpen) {
                e.preventDefault();
                e.stopPropagation();
                console.log('🛑 Prevented dropdown from hiding');
                return false;
            }
        });
        
        // Track khi dropdown shown
        dropdownBtn.addEventListener('shown.bs.dropdown', function() {
            console.log('📂 Dropdown shown event fired');
            isDropdownOpen = true;
        });
        
        // Track khi dropdown hidden
        dropdownBtn.addEventListener('hidden.bs.dropdown', function() {
            console.log('📁 Dropdown hidden event fired');
            isDropdownOpen = false;
        });
        
        // Click vào button
        dropdownBtn.addEventListener('click', function(e) {
            console.log('🖱️ Dropdown button clicked');
            e.stopPropagation();
        });
        
        // Đóng dropdown khi click ra ngoài
        document.addEventListener('click', function(event) {
            const dropdownMenu = document.querySelector('.notification-dropdown');
            
            if (!dropdownMenu) return;
            
            const isClickInside = dropdownBtn.contains(event.target) || 
                                 dropdownMenu.contains(event.target);
            
            if (!isClickInside && isDropdownOpen) {
                console.log('🖱️ Click outside, closing dropdown');
                isDropdownOpen = false;
                dropdown.hide();
            }
        });
        
        // Ngăn click vào dropdown menu đóng dropdown
        const dropdownMenu = document.querySelector('.notification-dropdown');
        if (dropdownMenu) {
            dropdownMenu.addEventListener('click', function(e) {
                console.log('🖱️ Click inside dropdown menu');
                // KHÔNG stopPropagation để event có thể bubble lên notification.js
                // e.stopPropagation();
            });
        }
        
    } catch (error) {
        console.error('❌ Error initializing dropdown:', error);
    }
});
