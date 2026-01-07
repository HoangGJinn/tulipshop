/**
 * Bootstrap Test Script
 * Kiểm tra xem Bootstrap có load đúng không
 * DISABLED - Test file không dùng trong production
 */

// File này đã được disable để tránh console logs
// Uncomment code bên dưới nếu cần debug Bootstrap dropdown

/*
document.addEventListener('DOMContentLoaded', function() {
    // Check if Bootstrap is loaded
    if (typeof bootstrap === 'undefined') {
        return;
    }
    
    const dropdownBtn = document.getElementById('notificationDropdownBtn');
    if (!dropdownBtn) return;
    
    try {
        const dropdown = new bootstrap.Dropdown(dropdownBtn, {
            autoClose: false
        });
        
        let isDropdownOpen = false;
        
        dropdownBtn.addEventListener('hide.bs.dropdown', function(e) {
            if (isDropdownOpen) {
                e.preventDefault();
                e.stopPropagation();
                return false;
            }
        });
        
        dropdownBtn.addEventListener('shown.bs.dropdown', function() {
            isDropdownOpen = true;
        });
        
        dropdownBtn.addEventListener('hidden.bs.dropdown', function() {
            isDropdownOpen = false;
        });
        
        dropdownBtn.addEventListener('click', function(e) {
            e.stopPropagation();
        });
        
        document.addEventListener('click', function(event) {
            const dropdownMenu = document.querySelector('.notification-dropdown');
            if (!dropdownMenu) return;
            
            const isClickInside = dropdownBtn.contains(event.target) || 
                                 dropdownMenu.contains(event.target);
            
            if (!isClickInside && isDropdownOpen) {
                isDropdownOpen = false;
                dropdown.hide();
            }
        });
        
    } catch (error) {
        // Dropdown initialization failed
    }
});
*/

