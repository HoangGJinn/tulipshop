/**
 * Category Management JavaScript Module
 * Handles all client-side functionality for the category management page with Tree View
 */

// ============================================================================
// MODULE STATE
// ============================================================================

const CategoryModule = {
    // Current category data
    categoryData: [],
    
    // Filter state
    filters: {
        search: ''
    },
    
    // Editing state
    editingCategoryId: null,
    editingParentId: null,
    
    // Tree state
    allExpanded: true
};

// ============================================================================
// INITIALIZATION
// ============================================================================

/**
 * Initialize the category module when DOM is ready
 */
document.addEventListener('DOMContentLoaded', function() {
    console.log('Category module initializing...');
    
    // Load initial data
    loadCategoryData();
    
    // Initialize Lucide icons if available
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }
});

/**
 * Load category data from the server
 */
async function loadCategoryData() {
    try {
        const response = await fetch('/v1/api/admin/categories');
        
        if (!response.ok) {
            throw new Error('Failed to load category data');
        }
        
        const result = await response.json();
        CategoryModule.categoryData = result.data;
        
        // Render the tree
        renderCategoryTree();
        
        // Update counts
        updateCounts();
        
    } catch (error) {
        console.error('Error loading categories:', error);
        showToast('Không thể tải dữ liệu danh mục', 'error');
    }
}

// ============================================================================
// SEARCH AND FILTER FUNCTIONALITY
// ============================================================================

/**
 * Handle search input changes
 */
function handleSearch() {
    const searchInput = document.getElementById('search-input');
    CategoryModule.filters.search = searchInput.value.toLowerCase().trim();
    
    // Re-render tree with filters
    renderCategoryTree();
    
    // Update counts
    updateCounts();
}

/**
 * Filter category data based on current filters
 * @returns {Array} Filtered category data
 */
function getFilteredCategories() {
    if (!CategoryModule.filters.search) {
        return CategoryModule.categoryData;
    }
    
    return CategoryModule.categoryData.filter(item => {
        const searchLower = CategoryModule.filters.search;
        const matchesSearch = 
            item.name.toLowerCase().includes(searchLower) ||
            (item.slug && item.slug.toLowerCase().includes(searchLower)) ||
            (item.parentName && item.parentName.toLowerCase().includes(searchLower));
        
        return matchesSearch;
    });
}

// ============================================================================
// TREE RENDERING
// ============================================================================

/**
 * Render the category tree
 */
function renderCategoryTree() {
    const treeContainer = document.getElementById('category-tree');
    const filteredData = getFilteredCategories();
    
    if (filteredData.length === 0) {
        treeContainer.innerHTML = '<li class="text-center py-20 text-gray-500">Không tìm thấy danh mục nào</li>';
        return;
    }
    
    // Build tree structure
    const rootCategories = filteredData.filter(c => !c.parentId);
    
    if (rootCategories.length === 0) {
        treeContainer.innerHTML = '<li class="text-center py-20 text-gray-500">Không có danh mục gốc</li>';
        return;
    }
    
    treeContainer.innerHTML = rootCategories.map(category => renderCategoryNode(category, filteredData)).join('');
    
    // Reinitialize icons
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }
}

/**
 * Render a single category node recursively
 * @param {Object} category - Category object
 * @param {Array} allCategories - All categories for finding children
 * @returns {string} HTML string
 */
function renderCategoryNode(category, allCategories) {
    const children = allCategories.filter(c => c.parentId === category.id);
    const hasChildren = children.length > 0;
    
    const nodeHtml = `
        <li class="category-node" data-category-id="${category.id}">
            <div class="category-item">
                ${hasChildren ? `
                    <div class="category-toggle" onclick="toggleCategory(${category.id})">
                        <i data-lucide="chevron-down" class="w-4 h-4"></i>
                    </div>
                ` : '<div style="width: 24px;"></div>'}
                
                <div class="category-icon ${hasChildren ? 'has-children' : 'no-children'}">
                    <i data-lucide="${hasChildren ? 'folder' : 'file-text'}" class="w-4 h-4"></i>
                </div>
                
                <div class="category-info">
                    <div class="category-name">${category.name}</div>
                    <div class="category-meta">
                        <span>
                            <i data-lucide="hash" class="w-3 h-3"></i>
                            ID: ${category.id}
                        </span>
                        <span>
                            <i data-lucide="link" class="w-3 h-3"></i>
                            ${category.slug || '-'}
                        </span>
                        ${hasChildren ? `
                            <span>
                                <i data-lucide="folder-tree" class="w-3 h-3"></i>
                                ${children.length} danh mục con
                            </span>
                        ` : ''}
                        <span class="status-badge ${category.hasProducts ? 'status-has-products' : 'status-empty'}">
                            ${category.hasProducts ? 'Có sản phẩm' : 'Trống'}
                        </span>
                    </div>
                </div>
                
                <div class="category-actions">
                    <button class="btn-add" onclick="openCategoryModal(${category.id})" title="Thêm danh mục con">
                        <i data-lucide="plus" class="w-3 h-3 inline"></i>
                        Thêm con
                    </button>
                    <button class="btn-edit" onclick="editCategory(${category.id})" title="Sửa danh mục">
                        <i data-lucide="edit-2" class="w-3 h-3 inline"></i>
                        Sửa
                    </button>
                    <button class="btn-delete" onclick="deleteCategory(${category.id}, '${category.name.replace(/'/g, "\\'")}', ${category.hasProducts}, ${category.childrenCount})" title="Xóa danh mục">
                        <i data-lucide="trash-2" class="w-3 h-3 inline"></i>
                        Xóa
                    </button>
                </div>
            </div>
            
            ${hasChildren ? `
                <div class="category-children" id="children-${category.id}">
                    <ul>
                        ${children.map(child => renderCategoryNode(child, allCategories)).join('')}
                    </ul>
                </div>
            ` : ''}
        </li>
    `;
    
    return nodeHtml;
}

/**
 * Toggle category expansion
 * @param {number} categoryId - Category ID to toggle
 */
function toggleCategory(categoryId) {
    const childrenContainer = document.getElementById(`children-${categoryId}`);
    const toggleBtn = document.querySelector(`[data-category-id="${categoryId}"] .category-toggle`);
    
    if (!childrenContainer || !toggleBtn) return;
    
    const isCollapsed = childrenContainer.classList.contains('collapsed');
    
    if (isCollapsed) {
        // Expand
        childrenContainer.style.maxHeight = childrenContainer.scrollHeight + 'px';
        childrenContainer.classList.remove('collapsed');
        toggleBtn.classList.remove('collapsed');
    } else {
        // Collapse
        childrenContainer.style.maxHeight = '0';
        childrenContainer.classList.add('collapsed');
        toggleBtn.classList.add('collapsed');
    }
    
    // Reinitialize icons
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }
}

/**
 * Toggle all categories (expand/collapse)
 */
function toggleAllCategories() {
    const allChildren = document.querySelectorAll('.category-children');
    const allToggles = document.querySelectorAll('.category-toggle');
    const toggleText = document.getElementById('toggle-all-text');
    
    CategoryModule.allExpanded = !CategoryModule.allExpanded;
    
    allChildren.forEach(container => {
        if (CategoryModule.allExpanded) {
            container.style.maxHeight = container.scrollHeight + 'px';
            container.classList.remove('collapsed');
        } else {
            container.style.maxHeight = '0';
            container.classList.add('collapsed');
        }
    });
    
    allToggles.forEach(toggle => {
        if (CategoryModule.allExpanded) {
            toggle.classList.remove('collapsed');
        } else {
            toggle.classList.add('collapsed');
        }
    });
    
    toggleText.textContent = CategoryModule.allExpanded ? 'Thu gọn tất cả' : 'Mở rộng tất cả';
    
    // Reinitialize icons
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }
}

/**
 * Update count displays
 */
function updateCounts() {
    const filteredData = getFilteredCategories();
    const totalCount = CategoryModule.categoryData.length;
    const visibleCount = filteredData.length;
    
    document.getElementById('visible-count').textContent = visibleCount;
    document.getElementById('total-count').textContent = totalCount;
}

// ============================================================================
// MODAL MANAGEMENT
// ============================================================================

/**
 * Open category modal for adding new category
 * @param {number|null} parentId - Parent category ID (null for root category)
 */
function openCategoryModal(parentId = null) {
    CategoryModule.editingCategoryId = null;
    CategoryModule.editingParentId = parentId;
    
    // Reset form
    document.getElementById('category-form').reset();
    document.getElementById('category-id').value = '';
    
    if (parentId) {
        const parent = CategoryModule.categoryData.find(c => c.id === parentId);
        document.getElementById('modal-title').textContent = `Thêm danh mục con cho "${parent.name}"`;
        document.getElementById('parent-selector-container').style.display = 'none';
    } else {
        document.getElementById('modal-title').textContent = 'Thêm danh mục gốc';
        document.getElementById('parent-selector-container').style.display = 'block';
        populateParentDropdown(null);
    }
    
    // Show modal
    document.getElementById('category-modal').classList.remove('hidden');
    
    // Focus on name input
    document.getElementById('category-name').focus();
    
    // Reinitialize icons
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }
}

/**
 * Close category modal
 */
function closeCategoryModal() {
    document.getElementById('category-modal').classList.add('hidden');
    CategoryModule.editingCategoryId = null;
    CategoryModule.editingParentId = null;
}

/**
 * Populate parent category dropdown
 * @param {number|null} excludeId - Category ID to exclude (when editing, exclude self and children)
 */
function populateParentDropdown(excludeId) {
    const parentSelect = document.getElementById('category-parent');
    
    // Clear existing options except the first one
    while (parentSelect.options.length > 1) {
        parentSelect.remove(1);
    }
    
    // Get list of IDs to exclude (self and all children)
    let excludeIds = [];
    if (excludeId) {
        excludeIds = [excludeId];
        // Add all children recursively
        const addChildren = (parentId) => {
            CategoryModule.categoryData
                .filter(c => c.parentId === parentId)
                .forEach(child => {
                    excludeIds.push(child.id);
                    addChildren(child.id);
                });
        };
        addChildren(excludeId);
    }
    
    // Add category options (excluding self and children)
    CategoryModule.categoryData
        .filter(c => !excludeIds.includes(c.id))
        .forEach(category => {
            const option = document.createElement('option');
            option.value = category.id;
            
            // Add indentation for child categories
            let prefix = '';
            if (category.parentId) {
                prefix = '└─ ';
                // Count depth
                let depth = 1;
                let parent = CategoryModule.categoryData.find(c => c.id === category.parentId);
                while (parent && parent.parentId) {
                    depth++;
                    parent = CategoryModule.categoryData.find(c => c.id === parent.parentId);
                }
                prefix = '  '.repeat(depth - 1) + '└─ ';
            }
            
            option.textContent = prefix + category.name;
            parentSelect.appendChild(option);
        });
}

/**
 * Edit category
 * @param {number} id - Category ID to edit
 */
function editCategory(id) {
    const category = CategoryModule.categoryData.find(c => c.id === id);
    if (!category) {
        showToast('Không tìm thấy danh mục', 'error');
        return;
    }
    
    CategoryModule.editingCategoryId = id;
    
    // Fill form
    document.getElementById('category-id').value = category.id;
    document.getElementById('category-name').value = category.name;
    document.getElementById('modal-title').textContent = 'Chỉnh sửa danh mục';
    
    // Show parent selector
    document.getElementById('parent-selector-container').style.display = 'block';
    
    // Populate parent dropdown (exclude self and children)
    populateParentDropdown(id);
    
    // Set parent value
    if (category.parentId) {
        document.getElementById('category-parent').value = category.parentId;
    }
    
    // Show modal
    document.getElementById('category-modal').classList.remove('hidden');
    
    // Focus on name input
    document.getElementById('category-name').focus();
    
    // Reinitialize icons
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }
}

/**
 * Submit category form (create or update)
 */
async function submitCategory() {
    const id = document.getElementById('category-id').value;
    const name = document.getElementById('category-name').value.trim();
    let parentId = null;
    
    // Determine parent ID
    if (CategoryModule.editingParentId) {
        // Adding child to specific parent
        parentId = CategoryModule.editingParentId;
    } else if (document.getElementById('parent-selector-container').style.display !== 'none') {
        // Using parent selector
        parentId = document.getElementById('category-parent').value || null;
    }
    
    // Validate
    if (!name) {
        showToast('Vui lòng nhập tên danh mục', 'error');
        return;
    }
    
    try {
        const payload = {
            name: name,
            parentId: parentId
        };
        
        let response;
        if (id) {
            // Update
            response = await fetch(`/v1/api/admin/categories/${id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload)
            });
        } else {
            // Create
            response = await fetch('/v1/api/admin/categories', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload)
            });
        }
        
        const result = await response.json();
        
        if (response.ok && result.status === 'success') {
            showToast(result.message, 'success');
            closeCategoryModal();
            
            // Reload data
            await loadCategoryData();
        } else {
            showToast(result.message || 'Có lỗi xảy ra', 'error');
        }
        
    } catch (error) {
        console.error('Error submitting category:', error);
        showToast('Lỗi kết nối. Vui lòng thử lại.', 'error');
    }
}

// ============================================================================
// DELETE FUNCTIONALITY WITH SAFE CHECK
// ============================================================================

/**
 * Delete category with safe check
 * @param {number} id - Category ID to delete
 * @param {string} name - Category name for confirmation
 * @param {boolean} hasProducts - Whether category has products
 * @param {number} childrenCount - Number of children categories
 */
async function deleteCategory(id, name, hasProducts, childrenCount) {
    // Check if has children
    if (childrenCount > 0) {
        showToast('Không thể xóa danh mục đang có danh mục con. Vui lòng xóa danh mục con trước.', 'error');
        return;
    }
    
    // Check if has products by calling API
    try {
        const response = await fetch(`/v1/api/admin/categories/${id}/products`);
        const result = await response.json();
        
        if (response.ok && result.status === 'success') {
            if (result.productCount > 0) {
                // Show warning modal with product list
                showWarningModal(id, name, result.products, result.productCount);
                return;
            }
        }
        
        // No products, proceed with confirmation
        if (!confirm(`Bạn có chắc chắn muốn xóa danh mục "${name}"?`)) {
            return;
        }
        
        performDelete(id);
        
    } catch (error) {
        console.error('Error checking products:', error);
        showToast('Lỗi khi kiểm tra sản phẩm', 'error');
    }
}

/**
 * Show warning modal with product list
 * @param {number} categoryId - Category ID
 * @param {string} categoryName - Category name
 * @param {Array} products - List of products
 * @param {number} productCount - Total product count
 */
function showWarningModal(categoryId, categoryName, products, productCount) {
    const modal = document.getElementById('warning-modal');
    const subtitle = document.getElementById('warning-subtitle');
    const productCountEl = document.getElementById('product-count');
    const productList = document.getElementById('product-list');
    
    subtitle.textContent = `Danh mục "${categoryName}" đang chứa sản phẩm`;
    productCountEl.textContent = productCount;
    
    // Render product list (max 10)
    const displayProducts = products.slice(0, 10);
    productList.innerHTML = displayProducts.map(p => `
        <div class="product-item">
            <img src="${p.thumbnail || '/images/placeholder.png'}" 
                 alt="${p.name}" 
                 class="product-thumbnail">
            <div class="product-info">
                <div class="product-name">${p.name}</div>
                <div class="product-status">
                    <span class="status-badge ${p.status === 'ACTIVE' ? 'status-has-products' : 'status-empty'}">
                        ${p.status}
                    </span>
                    • ${p.categoryName}
                </div>
            </div>
        </div>
    `).join('');
    
    if (products.length > 10) {
        productList.innerHTML += `
            <div class="text-center text-sm text-gray-500 mt-3">
                ... và ${products.length - 10} sản phẩm khác
            </div>
        `;
    }
    
    modal.classList.remove('hidden');
    
    // Reinitialize icons
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }
}

/**
 * Close warning modal
 */
function closeWarningModal() {
    document.getElementById('warning-modal').classList.add('hidden');
}

/**
 * Perform category deletion
 * @param {number} id - Category ID to delete
 */
async function performDelete(id) {
    try {
        const response = await fetch(`/v1/api/admin/categories/${id}`, {
            method: 'DELETE'
        });
        
        const result = await response.json();
        
        if (response.ok && result.status === 'success') {
            showToast(result.message, 'success');
            
            // Reload data
            await loadCategoryData();
        } else {
            showToast(result.message || 'Có lỗi xảy ra', 'error');
        }
        
    } catch (error) {
        console.error('Error deleting category:', error);
        showToast('Lỗi kết nối. Vui lòng thử lại.', 'error');
    }
}
