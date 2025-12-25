/**
 * Inventory Management JavaScript Module
 * Handles all client-side functionality for the inventory management page
 */

// ============================================================================
// MODULE STATE
// ============================================================================

const InventoryModule = {
    // Current inventory data
    inventoryData: [],
    
    // Pagination state
    currentPage: 1,
    itemsPerPage: 20,
    
    // Filter state
    filters: {
        search: '',
        status: '',
        category: ''
    },
    
    // Editing state
    editingStockId: null,
    originalValue: null,
    
    // Constants
    LOW_STOCK_THRESHOLD: 10
};

// ============================================================================
// INITIALIZATION
// ============================================================================

/**
 * Initialize the inventory module when DOM is ready
 */
document.addEventListener('DOMContentLoaded', function() {
    console.log('Inventory module initializing...');
    
    // Load initial data
    loadInventoryData();
    
    // Load alerts
    loadAlerts();
    
    // Initialize Lucide icons if available
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }
});

/**
 * Load inventory data from the server
 */
async function loadInventoryData() {
    try {
        const response = await fetch('/api/admin/inventory');
        
        if (!response.ok) {
            throw new Error('Failed to load inventory data');
        }
        
        InventoryModule.inventoryData = await response.json();
        
        // Populate category filter
        populateCategoryFilter();
        
        // Render the table
        renderInventoryTable();
        
        // Update counts
        updateCounts();
        
    } catch (error) {
        console.error('Error loading inventory:', error);
        showToast('Không thể tải dữ liệu kho hàng', 'error');
    }
}

/**
 * Load alert data from the server
 */
async function loadAlerts() {
    try {
        const response = await fetch('/api/admin/inventory/alerts');
        
        if (!response.ok) {
            throw new Error('Failed to load alerts');
        }
        
        const alerts = await response.json();
        
        // Update uninitialized alert
        if (alerts.uninitializedCount > 0) {
            document.getElementById('uninitialized-count').textContent = alerts.uninitializedCount;
            document.getElementById('uninitialized-alert').classList.remove('hidden');
        }
        
        // Update low stock alert
        if (alerts.lowStockCount > 0) {
            document.getElementById('low-stock-count').textContent = alerts.lowStockCount;
            document.getElementById('low-stock-alert').classList.remove('hidden');
        }
        
    } catch (error) {
        console.error('Error loading alerts:', error);
    }
}

/**
 * Populate the category filter dropdown with unique categories
 */
function populateCategoryFilter() {
    const categoryFilter = document.getElementById('category-filter');
    const categories = [...new Set(InventoryModule.inventoryData.map(item => item.categoryName))];
    
    // Clear existing options except the first one
    while (categoryFilter.options.length > 1) {
        categoryFilter.remove(1);
    }
    
    // Add category options
    categories.forEach(category => {
        const option = document.createElement('option');
        option.value = category;
        option.textContent = category;
        categoryFilter.appendChild(option);
    });
}

// ============================================================================
// PLACEHOLDER FUNCTIONS (to be implemented in subtasks)
// ============================================================================

// ============================================================================
// SEARCH AND FILTER FUNCTIONALITY
// ============================================================================

/**
 * Handle search input changes
 */
function handleSearch() {
    const searchInput = document.getElementById('search-input');
    InventoryModule.filters.search = searchInput.value.toLowerCase().trim();
    
    // Reset to first page when searching
    InventoryModule.currentPage = 1;
    
    // Re-render table with filters
    renderInventoryTable();
    
    // Update counts
    updateCounts();
}

/**
 * Handle filter dropdown changes
 */
function handleFilter() {
    const statusFilter = document.getElementById('status-filter');
    const categoryFilter = document.getElementById('category-filter');
    
    InventoryModule.filters.status = statusFilter.value;
    InventoryModule.filters.category = categoryFilter.value;
    
    // Reset to first page when filtering
    InventoryModule.currentPage = 1;
    
    // Re-render table with filters
    renderInventoryTable();
    
    // Update counts
    updateCounts();
}

/**
 * Filter inventory data based on current filters
 * @returns {Array} Filtered inventory data
 */
function getFilteredInventory() {
    return InventoryModule.inventoryData.filter(item => {
        // Search filter (product name or SKU)
        if (InventoryModule.filters.search) {
            const searchLower = InventoryModule.filters.search;
            const matchesSearch = 
                item.productName.toLowerCase().includes(searchLower) ||
                item.sku.toLowerCase().includes(searchLower);
            
            if (!matchesSearch) return false;
        }
        
        // Status filter
        if (InventoryModule.filters.status && item.status !== InventoryModule.filters.status) {
            return false;
        }
        
        // Category filter
        if (InventoryModule.filters.category && item.categoryName !== InventoryModule.filters.category) {
            return false;
        }
        
        return true;
    });
}

/**
 * Filter by alert type (from alert banner click)
 * @param {string} type - Alert type ('uninitialized' or 'low-stock')
 */
function filterByAlert(type) {
    // Clear other filters
    document.getElementById('search-input').value = '';
    document.getElementById('category-filter').value = '';
    InventoryModule.filters.search = '';
    InventoryModule.filters.category = '';
    
    // Set status filter
    if (type === 'low-stock') {
        document.getElementById('status-filter').value = 'LOW_STOCK';
        InventoryModule.filters.status = 'LOW_STOCK';
    } else if (type === 'uninitialized') {
        // For uninitialized, we'd need to filter differently
        // This would require backend support to list uninitialized variants
        console.log('Uninitialized filter not yet implemented');
        return;
    }
    
    // Reset to first page
    InventoryModule.currentPage = 1;
    
    // Re-render table
    renderInventoryTable();
    updateCounts();
}

// ============================================================================
// TOAST NOTIFICATIONS
// ============================================================================

/**
 * Show a toast notification
 * @param {string} message - The message to display
 * @param {string} type - The type of toast ('success', 'error', 'warning', 'info')
 */
function showToast(message, type = 'success') {
    const toastContainer = document.getElementById('toast-container');
    
    // Create toast element
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    // Create icon
    const iconWrapper = document.createElement('div');
    iconWrapper.className = 'toast-icon';
    const icon = document.createElement('i');
    const iconName = {
        'success': 'check-circle',
        'error': 'alert-circle',
        'warning': 'alert-triangle',
        'info': 'info'
    }[type] || 'check-circle';
    icon.setAttribute('data-lucide', iconName);
    iconWrapper.appendChild(icon);
    
    // Create content
    const content = document.createElement('div');
    content.className = 'toast-content';
    
    const messageText = document.createElement('div');
    messageText.className = 'toast-message';
    messageText.textContent = message;
    content.appendChild(messageText);
    
    // Create close button
    const closeBtn = document.createElement('button');
    closeBtn.className = 'toast-close';
    closeBtn.onclick = () => removeToast(toast);
    const closeIcon = document.createElement('i');
    closeIcon.setAttribute('data-lucide', 'x');
    closeBtn.appendChild(closeIcon);
    
    // Create progress bar
    const progress = document.createElement('div');
    progress.className = 'toast-progress';
    progress.style.color = getComputedStyle(toast).borderLeftColor;
    
    // Assemble toast
    toast.appendChild(iconWrapper);
    toast.appendChild(content);
    toast.appendChild(closeBtn);
    toast.appendChild(progress);
    
    // Add to container
    toastContainer.appendChild(toast);
    
    // Initialize icons
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }
    
    // Auto-dismiss after 3 seconds
    setTimeout(() => {
        removeToast(toast);
    }, 3000);
}

/**
 * Remove a toast notification with animation
 * @param {HTMLElement} toast - The toast element to remove
 */
function removeToast(toast) {
    toast.classList.add('removing');
    
    setTimeout(() => {
        if (toast.parentNode) {
            toast.parentNode.removeChild(toast);
        }
    }, 300);
}

function exportToExcel() {
    console.log('Export handler - to be implemented');
}

// ============================================================================
// STOCK HISTORY MODAL
// ============================================================================

/**
 * Open stock history modal and load history data
 * @param {string} stockId - The stock ID to show history for
 */
async function openHistoryModal(stockId) {
    const modal = document.getElementById('history-modal');
    const tableBody = document.getElementById('history-table-body');
    const productNameEl = document.getElementById('history-product-name');
    
    // Find the product name
    const item = InventoryModule.inventoryData.find(i => i.stockId == stockId);
    if (item) {
        productNameEl.textContent = `${item.productName} - ${item.colorName} / ${item.sizeName}`;
    }
    
    // Show modal
    modal.classList.remove('hidden');
    
    // Show loading state
    tableBody.innerHTML = '<tr><td colspan="6" class="p-8 text-center text-gray-500">Đang tải...</td></tr>';
    
    try {
        // Fetch history data
        const response = await fetch(`/api/admin/inventory/${stockId}/history`);
        
        if (!response.ok) {
            throw new Error('Failed to load history');
        }
        
        const history = await response.json();
        
        // Render history table
        if (history.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="6" class="p-8 text-center text-gray-500">Chưa có lịch sử thay đổi</td></tr>';
        } else {
            tableBody.innerHTML = history.map(record => `
                <tr class="border-b border-gray-100 hover:bg-gray-50">
                    <td class="px-4 py-3 text-xs text-gray-600">${formatDateTime(record.timestamp)}</td>
                    <td class="px-4 py-3 text-sm font-medium">${record.previousQuantity}</td>
                    <td class="px-4 py-3 text-sm font-medium">${record.newQuantity}</td>
                    <td class="px-4 py-3 text-sm font-bold ${record.changeAmount >= 0 ? 'text-green-600' : 'text-red-600'}">
                        ${record.changeAmount >= 0 ? '+' : ''}${record.changeAmount}
                    </td>
                    <td class="px-4 py-3 text-xs text-gray-600">${record.adminUsername || 'System'}</td>
                    <td class="px-4 py-3 text-xs text-gray-500">${record.reason || '-'}</td>
                </tr>
            `).join('');
        }
        
    } catch (error) {
        console.error('Error loading history:', error);
        tableBody.innerHTML = '<tr><td colspan="6" class="p-8 text-center text-red-500">Không thể tải lịch sử</td></tr>';
    }
}

/**
 * Close stock history modal
 */
function closeHistoryModal() {
    const modal = document.getElementById('history-modal');
    modal.classList.add('hidden');
}

/**
 * Format date and time for display
 * @param {string} timestamp - ISO timestamp string
 * @returns {string} Formatted date and time
 */
function formatDateTime(timestamp) {
    const date = new Date(timestamp);
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    
    return `${day}/${month}/${year} ${hours}:${minutes}`;
}

// ============================================================================
// EXPORT FUNCTIONALITY
// ============================================================================

/**
 * Export inventory data to Excel
 */
async function exportToExcel() {
    const exportBtn = event.target.closest('button');
    const originalContent = exportBtn.innerHTML;
    
    try {
        // Show loading indicator
        exportBtn.disabled = true;
        exportBtn.innerHTML = '<i data-lucide="loader-2" class="w-4 h-4 animate-spin"></i><span>Đang xuất...</span>';
        
        if (typeof lucide !== 'undefined') {
            lucide.createIcons();
        }
        
        // Fetch Excel file
        const response = await fetch('/api/admin/inventory/export');
        
        if (!response.ok) {
            throw new Error('Export failed');
        }
        
        // Get the blob
        const blob = await response.blob();
        
        // Create download link
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        
        // Get filename from Content-Disposition header or use default
        const contentDisposition = response.headers.get('Content-Disposition');
        let filename = 'inventory-export.xlsx';
        if (contentDisposition) {
            const filenameMatch = contentDisposition.match(/filename="?(.+)"?/);
            if (filenameMatch) {
                filename = filenameMatch[1];
            }
        }
        
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        
        // Cleanup
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        
        showToast('Xuất Excel thành công', 'success');
        
    } catch (error) {
        console.error('Error exporting:', error);
        showToast('Không thể xuất Excel', 'error');
    } finally {
        // Restore button
        exportBtn.disabled = false;
        exportBtn.innerHTML = originalContent;
        
        if (typeof lucide !== 'undefined') {
            lucide.createIcons();
        }
    }
}

// ============================================================================
// BULK INITIALIZATION MODAL
// ============================================================================

/**
 * Open bulk initialization modal and load uninitialized variants
 */
async function openBulkInitModal() {
    const modal = document.getElementById('bulk-init-modal');
    const listContainer = document.getElementById('bulk-init-list');
    
    // Show modal
    modal.classList.remove('hidden');
    
    // Show loading state
    listContainer.innerHTML = '<div class="p-8 text-center text-gray-500">Đang tải...</div>';
    
    try {
        // Fetch uninitialized variants
        const response = await fetch('/api/admin/inventory/uninitialized');
        
        if (!response.ok) {
            throw new Error('Failed to load uninitialized variants');
        }
        
        const variants = await response.json();
        
        if (variants.length === 0) {
            listContainer.innerHTML = '<div class="p-8 text-center text-gray-500">Không có SKU nào cần khởi tạo</div>';
        } else {
            listContainer.innerHTML = `
                <div class="space-y-3">
                    ${variants.map(variant => `
                        <div class="flex items-center gap-4 p-4 border border-gray-200 rounded-lg hover:bg-gray-50">
                            <img src="${variant.imageUrl || '/images/placeholder.png'}" 
                                 class="w-12 h-16 object-cover border border-black shadow-sm" 
                                 alt="${variant.productName}">
                            <div class="flex-1">
                                <div class="font-bold text-sm">${variant.productName}</div>
                                <div class="text-xs text-gray-500">${variant.colorName} / ${variant.sizeName}</div>
                                <div class="text-xs text-gray-400 font-mono mt-1">SKU: ${variant.sku}</div>
                            </div>
                            <div class="flex items-center gap-2">
                                <label class="text-xs font-medium text-gray-600">Số lượng:</label>
                                <input type="number" 
                                       min="0" 
                                       value="0"
                                       class="w-20 px-2 py-1 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-black"
                                       data-variant-id="${variant.variantId}"
                                       data-size-id="${variant.sizeId}">
                            </div>
                        </div>
                    `).join('')}
                </div>
            `;
        }
        
    } catch (error) {
        console.error('Error loading uninitialized variants:', error);
        listContainer.innerHTML = '<div class="p-8 text-center text-red-500">Không thể tải danh sách SKU</div>';
    }
}

/**
 * Close bulk initialization modal
 */
function closeBulkInitModal() {
    const modal = document.getElementById('bulk-init-modal');
    modal.classList.add('hidden');
}

/**
 * Submit bulk initialization request
 */
async function submitBulkInit() {
    const listContainer = document.getElementById('bulk-init-list');
    const inputs = listContainer.querySelectorAll('input[type="number"]');
    
    // Collect variant-quantity pairs where quantity > 0
    const requests = [];
    inputs.forEach(input => {
        const quantity = parseInt(input.value);
        if (quantity > 0) {
            requests.push({
                variantId: parseInt(input.dataset.variantId),
                sizeId: parseInt(input.dataset.sizeId),
                initialQuantity: quantity
            });
        }
    });
    
    if (requests.length === 0) {
        showToast('Vui lòng nhập số lượng cho ít nhất một SKU', 'error');
        return;
    }
    
    try {
        // Send POST request
        const response = await fetch('/api/admin/inventory/bulk-init', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requests)
        });
        
        if (!response.ok) {
            throw new Error('Bulk initialization failed');
        }
        
        const result = await response.json();
        
        // Close modal
        closeBulkInitModal();
        
        // Show success message
        showToast(`Đã khởi tạo ${result.count || requests.length} SKU thành công`, 'success');
        
        // Reload inventory data
        await loadInventoryData();
        
        // Reload alerts
        await loadAlerts();
        
    } catch (error) {
        console.error('Error initializing stock:', error);
        showToast('Không thể khởi tạo kho', 'error');
    }
}

function dismissAlert(alertId) {
    const alert = document.getElementById(alertId);
    if (alert) {
        alert.classList.add('dismissing');
        setTimeout(() => {
            alert.classList.add('hidden');
            alert.classList.remove('dismissing');
        }, 300);
    }
}

function openHistoryModal(stockId) {
    console.log('Open history modal - to be implemented');
}

function closeHistoryModal() {
    const modal = document.getElementById('history-modal');
    modal.classList.add('hidden');
}

function previousPage() {
    console.log('Previous page - to be implemented');
}

function nextPage() {
    console.log('Next page - to be implemented');
}

function showToast(message, type) {
    console.log('Toast notification - to be implemented');
}

// ============================================================================
// INLINE EDITING FOR PHYSICAL STOCK
// ============================================================================

/**
 * Make a stock cell editable
 * @param {HTMLElement} cell - The stock cell element
 */
function makeStockEditable(cell) {
    // Prevent multiple edits
    if (InventoryModule.editingStockId) {
        return;
    }
    
    const stockId = cell.dataset.stockId;
    const currentPhysical = parseInt(cell.dataset.physical);
    const currentReserved = parseInt(cell.dataset.reserved);
    
    // Store editing state
    InventoryModule.editingStockId = stockId;
    InventoryModule.originalValue = currentPhysical;
    
    // Create input field
    const input = document.createElement('input');
    input.type = 'number';
    input.value = currentPhysical;
    input.min = '0';
    input.className = 'w-20 px-2 py-1 border border-black rounded text-sm font-bold focus:outline-none focus:ring-2 focus:ring-black';
    input.id = 'stock-input-' + stockId;
    
    // Create button container
    const buttonContainer = document.createElement('div');
    buttonContainer.className = 'flex gap-2 mt-2';
    
    // Create save button
    const saveBtn = document.createElement('button');
    saveBtn.innerHTML = '<i data-lucide="check" class="w-4 h-4"></i>';
    saveBtn.className = 'px-2 py-1 bg-green-600 text-white rounded hover:bg-green-700 transition-colors';
    saveBtn.onclick = () => saveStockEdit(stockId, input.value, currentReserved);
    
    // Create cancel button
    const cancelBtn = document.createElement('button');
    cancelBtn.innerHTML = '<i data-lucide="x" class="w-4 h-4"></i>';
    cancelBtn.className = 'px-2 py-1 bg-gray-300 text-gray-700 rounded hover:bg-gray-400 transition-colors';
    cancelBtn.onclick = () => cancelStockEdit(cell);
    
    buttonContainer.appendChild(saveBtn);
    buttonContainer.appendChild(cancelBtn);
    
    // Replace cell content
    cell.innerHTML = '';
    cell.appendChild(input);
    cell.appendChild(buttonContainer);
    
    // Focus input
    input.focus();
    input.select();
    
    // Reinitialize icons
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }
    
    // Handle real-time calculation on input change
    input.addEventListener('input', () => {
        updateAvailableStockPreview(stockId, input.value, currentReserved);
    });
    
    // Handle Enter key
    input.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            saveStockEdit(stockId, input.value, currentReserved);
        }
    });
    
    // Handle Escape key
    input.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            cancelStockEdit(cell);
        }
    });
}

/**
 * Cancel stock editing and restore original value
 * @param {HTMLElement} cell - The stock cell element
 */
function cancelStockEdit(cell) {
    const stockId = InventoryModule.editingStockId;
    const originalValue = InventoryModule.originalValue;
    
    // Restore original display
    cell.innerHTML = originalValue;
    cell.className = 'editable-stock cursor-pointer hover:bg-gray-100 px-2 py-1 rounded';
    
    // Clear any warning styling from available stock cell
    const row = cell.closest('tr');
    if (row) {
        const availableCell = row.querySelector('.available-stock');
        if (availableCell) {
            availableCell.classList.remove('text-red-600', 'font-bold');
            // Restore original available stock value
            const item = InventoryModule.inventoryData.find(i => i.stockId == stockId);
            if (item) {
                availableCell.textContent = item.availableStock;
            }
        }
    }
    
    // Clear editing state
    InventoryModule.editingStockId = null;
    InventoryModule.originalValue = null;
}

/**
 * Update available stock preview in real-time as user types
 * @param {string} stockId - The stock ID being edited
 * @param {string} newPhysicalValue - The new physical stock value from input
 * @param {number} currentReserved - The current reserved stock
 */
function updateAvailableStockPreview(stockId, newPhysicalValue, currentReserved) {
    const newPhysical = parseInt(newPhysicalValue) || 0;
    const newAvailable = newPhysical - currentReserved;
    
    // Find the row and available stock cell
    const row = document.querySelector(`tr[data-stock-id="${stockId}"]`);
    if (!row) return;
    
    const availableCell = row.querySelector('.available-stock');
    const input = document.getElementById('stock-input-' + stockId);
    
    if (!availableCell || !input) return;
    
    // Update available stock display
    availableCell.textContent = newAvailable;
    
    // Show warning if negative
    if (newAvailable < 0) {
        availableCell.classList.add('text-red-600', 'font-bold');
        input.classList.add('border-red-500', 'ring-2', 'ring-red-500');
        input.classList.remove('border-black', 'ring-black');
    } else {
        availableCell.classList.remove('text-red-600', 'font-bold');
        input.classList.remove('border-red-500', 'ring-2', 'ring-red-500');
        input.classList.add('border-black');
    }
}

/**
 * Save stock edit via AJAX
 * @param {string} stockId - The stock ID being edited
 * @param {string} newValue - The new physical stock value
 * @param {number} currentReserved - The current reserved stock
 */
async function saveStockEdit(stockId, newValue, currentReserved) {
    const newPhysical = parseInt(newValue);
    
    // Validate input
    if (isNaN(newPhysical) || newPhysical < 0) {
        showToast('Số lượng không hợp lệ', 'error');
        return;
    }
    
    // Check if would result in negative available stock
    const newAvailable = newPhysical - currentReserved;
    if (newAvailable < 0) {
        showToast(`Không thể đặt tồn kho thành ${newPhysical}. Đã đặt ${currentReserved}, sẽ dẫn đến kho khả dụng âm.`, 'error');
        return;
    }
    
    try {
        // Send PUT request
        const response = await fetch(`/api/admin/inventory/${stockId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                stockId: stockId,
                newPhysicalStock: newPhysical,
                reason: 'Manual update from inventory page'
            })
        });
        
        if (response.ok) {
            // Success - update UI
            const updatedItem = await response.json();
            
            // Update inventory data
            const index = InventoryModule.inventoryData.findIndex(item => item.stockId == stockId);
            if (index !== -1) {
                InventoryModule.inventoryData[index] = updatedItem;
            }
            
            // Re-render the table
            renderInventoryTable();
            
            // Show success toast
            showToast('Cập nhật tồn kho thành công', 'success');
            
            // Clear editing state
            InventoryModule.editingStockId = null;
            InventoryModule.originalValue = null;
            
        } else if (response.status === 409) {
            // Concurrent modification - reload current value
            showToast('Kho đang được chỉnh sửa bởi người khác. Đang tải lại giá trị hiện tại...', 'error');
            
            // Reload inventory data
            await loadInventoryData();
            
            // Clear editing state
            InventoryModule.editingStockId = null;
            InventoryModule.originalValue = null;
            
        } else {
            // Other error
            const errorData = await response.json();
            showToast(errorData.message || 'Không thể cập nhật tồn kho', 'error');
        }
        
    } catch (error) {
        console.error('Error updating stock:', error);
        showToast('Lỗi kết nối. Vui lòng thử lại.', 'error');
    }
}

/**
 * Initialize click handlers for editable stock cells
 */
function initializeStockEditHandlers() {
    document.querySelectorAll('.editable-stock').forEach(cell => {
        cell.addEventListener('click', function() {
            makeStockEditable(this);
        });
    });
}

// ============================================================================
// TABLE RENDERING AND PAGINATION
// ============================================================================

/**
 * Render the inventory table with current filters and pagination
 */
function renderInventoryTable() {
    const tableBody = document.getElementById('inventory-table-body');
    const filteredData = getFilteredInventory();
    
    // Calculate pagination
    const startIndex = (InventoryModule.currentPage - 1) * InventoryModule.itemsPerPage;
    const endIndex = startIndex + InventoryModule.itemsPerPage;
    const pageData = filteredData.slice(startIndex, endIndex);
    
    // Render table rows
    if (pageData.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="10" class="p-20 text-center text-gray-500">Không tìm thấy sản phẩm nào</td></tr>';
    } else {
        tableBody.innerHTML = pageData.map(item => `
            <tr class="hover:bg-gray-50/50 transition-colors border-b border-gray-50 last:border-none" 
                data-stock-id="${item.stockId}"
                data-category="${item.categoryName}"
                data-status="${item.status}">
                <td class="p-6">
                    <img src="${item.imageUrl || '/images/placeholder.png'}" 
                         class="w-12 h-16 object-cover border border-black shadow-sm" 
                         alt="${item.productName}">
                </td>
                <td class="p-6">
                    <div class="font-black uppercase text-xs tracking-wider">${item.productName}</div>
                    <div class="text-[9px] text-gray-400 uppercase mt-1">${item.categoryName}</div>
                </td>
                <td class="p-6 text-[10px] font-mono text-gray-600">${item.sku}</td>
                <td class="p-6">
                    <div class="text-xs">${item.colorName}</div>
                    <div class="text-[10px] text-gray-500">${item.sizeName}</div>
                </td>
                <td class="p-6 font-black italic tracking-tighter">${formatPrice(item.price)}</td>
                <td class="p-6">
                    <span class="editable-stock cursor-pointer hover:bg-gray-100 px-2 py-1 rounded"
                          data-stock-id="${item.stockId}"
                          data-physical="${item.physicalStock}"
                          data-reserved="${item.reservedStock}">
                        ${item.physicalStock}
                    </span>
                </td>
                <td class="p-6 text-gray-600">${item.reservedStock}</td>
                <td class="p-6">
                    <span class="available-stock font-bold">${item.availableStock}</span>
                </td>
                <td class="p-6">
                    ${getStatusBadge(item.status)}
                </td>
                <td class="p-6 text-right">
                    <div class="flex justify-end gap-3">
                        <button onclick="openHistoryModal(${item.stockId})" 
                                class="text-[10px] font-black uppercase tracking-widest hover:underline">
                            Lịch sử
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');
    }
    
    // Initialize click handlers for editable cells
    initializeStockEditHandlers();
    
    // Update pagination controls
    updatePaginationControls(filteredData.length);
}

/**
 * Update count displays
 */
function updateCounts() {
    const filteredData = getFilteredInventory();
    const totalCount = InventoryModule.inventoryData.length;
    const visibleCount = filteredData.length;
    
    document.getElementById('visible-count').textContent = visibleCount;
    document.getElementById('total-count').textContent = totalCount;
}

/**
 * Update pagination controls
 * @param {number} totalItems - Total number of filtered items
 */
function updatePaginationControls(totalItems) {
    const totalPages = Math.ceil(totalItems / InventoryModule.itemsPerPage);
    const currentPage = InventoryModule.currentPage;
    
    // Update page info
    const startIndex = (currentPage - 1) * InventoryModule.itemsPerPage + 1;
    const endIndex = Math.min(currentPage * InventoryModule.itemsPerPage, totalItems);
    
    document.getElementById('page-start').textContent = totalItems > 0 ? startIndex : 0;
    document.getElementById('page-end').textContent = endIndex;
    document.getElementById('page-total').textContent = totalItems;
    
    // Update prev/next buttons
    const prevBtn = document.getElementById('prev-btn');
    const nextBtn = document.getElementById('next-btn');
    
    prevBtn.disabled = currentPage <= 1;
    nextBtn.disabled = currentPage >= totalPages;
    
    // Update page numbers
    const pageNumbersContainer = document.getElementById('page-numbers');
    pageNumbersContainer.innerHTML = '';
    
    // Show max 5 page numbers
    const maxPages = 5;
    let startPage = Math.max(1, currentPage - Math.floor(maxPages / 2));
    let endPage = Math.min(totalPages, startPage + maxPages - 1);
    
    if (endPage - startPage < maxPages - 1) {
        startPage = Math.max(1, endPage - maxPages + 1);
    }
    
    for (let i = startPage; i <= endPage; i++) {
        const pageBtn = document.createElement('button');
        pageBtn.textContent = i;
        pageBtn.className = `px-4 py-2 border rounded-lg text-sm font-medium ${
            i === currentPage 
                ? 'bg-black text-white border-black' 
                : 'border-gray-300 hover:bg-gray-50'
        }`;
        pageBtn.onclick = () => goToPage(i);
        pageNumbersContainer.appendChild(pageBtn);
    }
}

/**
 * Go to a specific page
 * @param {number} page - Page number to navigate to
 */
function goToPage(page) {
    InventoryModule.currentPage = page;
    renderInventoryTable();
}

/**
 * Format price for display
 * @param {number} price - Price value
 * @returns {string} Formatted price
 */
function formatPrice(price) {
    return new Intl.NumberFormat('vi-VN').format(price) + '₫';
}

/**
 * Get status badge HTML
 * @param {string} status - Stock status
 * @returns {string} Badge HTML
 */
function getStatusBadge(status) {
    const badges = {
        'IN_STOCK': '<span class="px-2 py-1 text-xs font-bold uppercase rounded bg-green-100 text-green-800">Còn hàng</span>',
        'LOW_STOCK': '<span class="px-2 py-1 text-xs font-bold uppercase rounded bg-yellow-100 text-yellow-800">Sắp hết</span>',
        'OUT_OF_STOCK': '<span class="px-2 py-1 text-xs font-bold uppercase rounded bg-red-100 text-red-800">Hết hàng</span>'
    };
    
    return badges[status] || '';
}

