document.addEventListener('DOMContentLoaded', function () {
    function getAuthHeaders() {
        return {
            'Content-Type': 'application/json'
        };
    }

    function getCookie(name) {
        const value = `; ${document.cookie}`;
        const parts = value.split(`; ${name}=`);
        if (parts.length === 2) return parts.pop().split(';').shift();
        return null;
    }

    let state = {
        page: 0,
        size: 10,
        status: 'PENDING',
        keyword: '',
        dateFrom: '',
        dateTo: '',
        allOrders: []
    };

    let autoRefreshInterval = null;

    init();

    function init() {
        document.querySelectorAll('.tab-item').forEach(t => t.classList.remove('active'));
        const pendingTab = document.querySelector('.tab-item[data-status="PENDING"]');
        if (pendingTab) {
            pendingTab.classList.add('active');
        }
        loadOrders();
        loadStatistics(); // Load statistics lần đầu
        setupEventListeners();
        startAutoRefresh();
    }

    function startAutoRefresh() {
        // Tự động làm mới dữ liệu mỗi 30 giây (AJAX - không reload trang)
        if (autoRefreshInterval) {
            clearInterval(autoRefreshInterval);
        }
        autoRefreshInterval = setInterval(() => {
            loadOrders(true); // silent = true: không hiển thị loading, chỉ update khi có thay đổi
            loadStatistics(); // Cập nhật số liệu thống kê
        }, 30000); // 30 giây
    }

    function loadStatistics() {
        fetch('/v1/api/admin/orders/statistics', {
            method: 'GET',
            headers: getAuthHeaders(),
            credentials: 'include'
        })
            .then(response => response.json())
            .then(stats => {
                updateStatisticsUI(stats);
            })
            .catch(error => {
                console.error('Error loading statistics:', error);
            });
    }

    function updateStatisticsUI(stats) {
        // Cập nhật từng ô thống kê với animation
        const statCards = document.querySelectorAll('.stat-card');
        
        statCards.forEach(card => {
            const numberEl = card.querySelector('.number');
            const heading = card.querySelector('h3').textContent.trim();
            
            let newValue = 0;
            if (heading === 'Chờ xác nhận') {
                newValue = stats.PENDING || 0;
            } else if (heading === 'Đã xác nhận') {
                newValue = stats.CONFIRMED || 0;
            } else if (heading === 'Đang giao') {
                newValue = stats.SHIPPING || 0;
            } else if (heading === 'Hoàn thành') {
                newValue = stats.DELIVERED || 0;
            }
            
            const oldValue = parseInt(numberEl.textContent) || 0;
            if (newValue !== oldValue) {
                // Thêm animation khi số thay đổi
                numberEl.style.transition = 'transform 0.3s ease, color 0.3s ease';
                numberEl.style.transform = 'scale(1.2)';
                numberEl.style.color = '#e74c3c';
                
                setTimeout(() => {
                    numberEl.textContent = newValue;
                    numberEl.style.transform = 'scale(1)';
                    numberEl.style.color = '';
                }, 150);
            }
        });
    }

    function setupEventListeners() {
        document.querySelectorAll('.tab-item').forEach(tab => {
            tab.addEventListener('click', function () {
                // Kiểm tra xem tab này đã active chưa
                if (this.classList.contains('active')) {
                    return; // Nếu đã active thì không làm gì cả
                }
                
                document.querySelectorAll('.tab-item').forEach(t => t.classList.remove('active'));
                this.classList.add('active');
                state.status = this.getAttribute('data-status');
                state.page = 0;
                loadOrders();
            });
        });

        window.applyFilters = function() {
            state.keyword = document.getElementById('searchInput').value;
            state.dateFrom = document.getElementById('dateFrom').value;
            state.dateTo = document.getElementById('dateTo').value;
            state.page = 0;
            loadOrders();
        };

        window.resetFilters = function() {
            document.getElementById('searchInput').value = '';
            document.getElementById('dateFrom').value = '';
            document.getElementById('dateTo').value = '';
            state.keyword = '';
            state.dateFrom = '';
            state.dateTo = '';
            loadOrders();
        };

        document.querySelector('.page-btn.prev').onclick = () => changePage(-1);
        document.querySelector('.page-btn.next').onclick = () => changePage(1);

        document.querySelector('.close-modal').onclick = closeModal;
        document.querySelector('.close-btn').onclick = closeModal;

        window.onclick = function(event) {
            const modal = document.getElementById('orderModal');
            if (event.target == modal) {
                closeModal();
            }
        }
    }

    function loadOrders(silent = false) {
        const tbody = document.getElementById('ordersTableBody');
        
        // Chỉ hiển thị loading khi không phải silent refresh
        if (!silent) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4"><i class="fas fa-spinner fa-spin"></i> Đang tải dữ liệu...</td></tr>';
        }

        const params = new URLSearchParams({
            page: state.page,
            size: state.size,
            status: state.status,
            keyword: state.keyword,
            dateFrom: state.dateFrom,
            dateTo: state.dateTo
        });

        let url = '/v1/api/admin/orders?defaultStatus=ALL';
        
        if (state.status !== 'ALL') {
            url = `/v1/api/admin/orders/status/${state.status}`;
        }
        else if (state.dateFrom) {
            url = `/v1/api/admin/orders/date/${state.dateFrom}`;
        }
        else if (state.keyword && !isNaN(state.keyword)) {
            url = `/v1/api/admin/orders/user/${state.keyword}`;
        }
        
        fetch(url, {
            method: 'GET',
            headers: getAuthHeaders(),
            credentials: 'include'
        })
            .then(response => {
                if (!response.ok) throw new Error('Lỗi tải dữ liệu');
                return response.json();
            })
            .then(orders => {
                // Chỉ update nếu có thay đổi
                const newData = JSON.stringify(orders);
                const oldData = JSON.stringify(state.allOrders);
                
                if (newData !== oldData) {
                    state.allOrders = orders || [];
                    renderTableWithPagination();
                    
                    // Cập nhật statistics sau khi có thay đổi
                    if (!silent) {
                        loadStatistics();
                    }
                }
            })
            .catch(error => {
                console.error('Error:', error);
                if (!silent) {
                    // Hiển thị thông báo không có dữ liệu
                    const tbody = document.getElementById('ordersTableBody');
                    tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4"><i class="fas fa-exclamation-triangle text-warning"></i> Không thể tải dữ liệu. Vui lòng thử lại sau.</td></tr>';
                    state.allOrders = [];
                }
            });
    }

    function renderTableWithPagination() {
        const allOrders = state.allOrders;
        const totalOrders = allOrders.length;
        const totalPages = Math.ceil(totalOrders / state.size);
        
        const startIndex = state.page * state.size;
        const endIndex = startIndex + state.size;
        const ordersForCurrentPage = allOrders.slice(startIndex, endIndex);
        
        renderTable(ordersForCurrentPage);
        
        updatePagination({
            totalPages: totalPages,
            totalElements: totalOrders,
            currentPage: state.page,
            size: state.size
        });
    }
    
    function renderTable(orders) {
        const tbody = document.getElementById('ordersTableBody');
        tbody.innerHTML = '';

        if (!orders || orders.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4">Không tìm thấy đơn hàng nào</td></tr>';
            return;
        }

        orders.forEach(order => {
            const tr = document.createElement('tr');
            
            let paymentWarning = '';
            if (order.status === 'PENDING' && !canBeConfirmed(order)) {
                let warningMsg = '';
                if (order.paymentMethod === 'MOMO' || order.paymentMethod === 'VNPAY') {
                    if (order.paymentStatus !== 'SUCCESS') {
                        warningMsg = 'Chưa thanh toán';
                    } else if (order.paymentExpireAt && new Date(order.paymentExpireAt) < new Date()) {
                        warningMsg = 'Đã hết hạn';
                    }
                }
                if (warningMsg) {
                    paymentWarning = `<div style="margin-top: 5px;"><span class="badge-warning-small" style="background:#fff3cd;color:#856404;padding:3px 8px;border-radius:10px;font-size:11px;"><i class="fas fa-exclamation-triangle"></i> ${warningMsg}</span></div>`;
                }
            }
            
            tr.innerHTML = `
                <td><strong>#${order.orderCode}</strong></td>
                <td>
                    <div class="d-flex flex-column">
                        <span class="fw-bold text-dark">Khách: ${order.userName || 'N/A'}</span>
                        <small class="text-muted">SĐT: ${order.userPhone || 'N/A'}</small>
                        <span class="text-primary mt-1">Nhận: ${order.recipientName}</span>
                        <small class="text-muted">${order.recipientPhone}</small>
                    </div>
                </td>
                <td>${formatDate(order.createdAt)}</td>
                <td class="text-danger fw-bold">${formatCurrency(order.finalPrice)}</td>
                <td>${getPaymentBadge(order.paymentMethod)}${paymentWarning}</td>
                <td>${getStatusBadge(order.status)}</td>
                <td>
                    <div class="d-flex gap-2">
                        <button class="btn-icon view" title="Xem chi tiết" onclick="openDetail(${order.id}, '${order.orderCode}')">
                            <i class="fas fa-eye"></i>
                        </button>
                        ${getActionButtons(order)}
                    </div>
                </td>
            `;
            tbody.appendChild(tr);
        });
    }

    function getActionButtons(order) {
        let btns = '';

        if (order.status === 'PENDING') {
            const canConfirm = canBeConfirmed(order);
            const disabledAttr = canConfirm ? '' : 'disabled';
            const disabledClass = canConfirm ? '' : 'btn-disabled';
            const title = canConfirm ? 'Xác nhận đơn' : 'Đơn hàng chưa thanh toán hoặc đã hết hạn';
            
            btns += `<button class="btn-icon confirm ${disabledClass}" ${disabledAttr} onclick="updateStatus(${order.id}, 'CONFIRMED')" title="${title}">
                        <i class="fas fa-check"></i>
                     </button>`;
            btns += `<button class="btn-icon cancel" onclick="updateStatus(${order.id}, 'CANCELLED')" title="Hủy đơn">
                        <i class="fas fa-times"></i>
                     </button>`;
        }
        else if (order.status === 'CONFIRMED') {
            btns += `<button class="btn-icon shipping" onclick="callShipping(${order.id}, '${order.orderCode}')" title="Bắt đầu giao hàng">
                        <i class="fas fa-shipping-fast"></i>
                     </button>`;
        }

        return btns;
    }
    
    function canBeConfirmed(order) {
        if (order.status !== 'PENDING') {
            return false;
        }
        
        if (order.paymentMethod === 'COD') {
            return true;
        }
        
        if (order.paymentMethod === 'MOMO' || order.paymentMethod === 'VNPAY') {
            if (order.paymentStatus !== 'SUCCESS') {
                return false;
            }
            
            if (order.paymentExpireAt) {
                const expireTime = new Date(order.paymentExpireAt);
                const now = new Date();
                return now <= expireTime;
            }
            
            return true;
        }
        
        return false;
    }

    function getStatusBadge(status) {
        const map = {
            'PENDING': '<span class="badge-status pending">Chờ xác nhận</span>',
            'CONFIRMED': '<span class="badge-status confirmed">Đã xác nhận</span>',
            'SHIPPING': '<span class="badge-status shipping">Đang giao</span>',
            'DELIVERED': '<span class="badge-status delivered">Đã giao</span>',
            'CANCELLED': '<span class="badge-status cancelled">Đã hủy</span>',
            'RETURNED': '<span class="badge-status cancelled">Trả hàng</span>'
        };
        return map[status] || `<span class="badge-status">${status}</span>`;
    }

    function getPaymentBadge(method) {
        if (method === 'COD') return '<span class="badge-pay cod">COD</span>';
        if (method === 'VNPAY') return '<span class="badge-pay vnpay">VNPAY</span>';
        if (method === 'MOMO') return '<span class="badge-pay momo" style="background:#fce4ec; color:#d81b60;">MOMO</span>';
        return method;
    }

    window.updateStatus = function(orderId, newStatus) {
        let msg = newStatus === 'CONFIRMED' ? 'Xác nhận đơn hàng này?' : 'Hủy đơn hàng này?';
        if (!confirm(msg)) return;

        fetch(`/v1/api/admin/orders/${orderId}/confirm`, {
            method: 'POST',
            headers: getAuthHeaders(),
            credentials: 'include'
        })
            .then(res => {
                if (res.ok) {
                    return res.json().then(data => {
                        showSuccess('Thành công', 'Xác nhận đơn hàng thành công!');
                        closeModal();
                        loadOrders();
                        // Refresh lại số liệu thống kê
                        loadStatistics();
                    });
                } else {
                    return res.json().then(data => {
                        showError('Lỗi', data.error || data.message || 'Unknown error');
                    }).catch(() => {
                        return res.text().then(text => {
                            showError('Lỗi', 'Có lỗi xảy ra: ' + (text || 'Unknown error'));
                        });
                    });
                }
            })
            .catch(err => {
                console.error('Fetch error:', err);
                showError('Lỗi kết nối', err.message);
            });
    };

    window.callShipping = function(orderId, orderCode) {
        if (!confirm(`Bắt đầu giao hàng cho đơn ${orderCode}?`)) return;

        fetch(`/v1/api/admin/orders/${orderId}/start-shipping`, {
            method: 'POST',
            headers: getAuthHeaders(),
            credentials: 'include'
        })
            .then(async res => {
                if (res.ok) {
                    showSuccess('Thành công', `Đã gửi yêu cầu giao hàng cho đơn ${orderCode}! Hệ thống vận chuyển đang xử lý.`);
                    loadOrders();
                    // Refresh lại số liệu thống kê
                    loadStatistics();
                } else {
                    const text = await res.text();
                    showError('Lỗi', text);
                }
            })
            .catch(err => {
                console.error(err);
                showError('Lỗi', err.message);
            });
    };

    window.openDetail = function(orderId, orderCode) {
        const modal = document.getElementById('orderModal');
        document.getElementById('modalOrderCode').innerText = '#' + orderCode;

        const bodyContent = document.getElementById('modalBodyContent');
        const modalActions = document.getElementById('modalActions');

        bodyContent.innerHTML = '<div class="text-center py-5"><i class="fas fa-spinner fa-spin fa-2x text-muted"></i><p class="mt-2 text-muted">Đang tải thông tin chi tiết...</p></div>';
        if(modalActions) modalActions.innerHTML = '';

        modal.classList.add('show');

        // Gọi API lấy TẤT CẢ đơn hàng để tìm chi tiết
        fetch(`/v1/api/admin/orders?defaultStatus=ALL`, {
            method: 'GET',
            headers: getAuthHeaders(),
            credentials: 'include'
        })
            .then(res => res.json())
            .then(orders => {
                const order = orders.find(o => o.id === orderId);
                if (!order) throw new Error('Không tìm thấy đơn hàng');

                let paymentWarningHtml = '';
                if (order.status === 'PENDING' && !canBeConfirmed(order)) {
                    let warningMsg = '';
                    if (order.paymentMethod === 'MOMO' || order.paymentMethod === 'VNPAY') {
                        if (order.paymentStatus !== 'SUCCESS') {
                            warningMsg = 'Đơn hàng này chưa thanh toán thành công. Không thể xác nhận.';
                        } else if (order.paymentExpireAt && new Date(order.paymentExpireAt) < new Date()) {
                            warningMsg = 'Đơn hàng này đã hết hạn thanh toán. Không thể xác nhận.';
                        }
                    }
                    if (warningMsg) {
                        paymentWarningHtml = `<div class="alert alert-warning" style="margin-top:15px;padding:12px;background:#fff3cd;color:#856404;border:1px solid #ffc107;border-radius:6px;">
                            <i class="fas fa-exclamation-triangle"></i> <strong>Cảnh báo:</strong> ${warningMsg}
                        </div>`;
                    }
                }

                if(modalActions && typeof getActionButtons === 'function') {
                    modalActions.innerHTML = getActionButtons(order);
                }

                let itemsHtml = order.orderItems.map(item => `
            <div class="product-item">
                <img src="${item.productImage || '/images/flower-logo.png'}" 
                     alt="SP" 
                     onerror="this.src='/images/flower-logo.png'; this.onerror=null;">
                <div class="product-info">
                    <span class="product-name">${item.productName}</span>
                    <span class="product-meta">
                        <span class="badge bg-light text-dark border">Màu: ${item.variantColorName}</span>
                        <span class="badge bg-light text-dark border">Size: ${item.sizeCode}</span>
                        <span class="ms-2">x${item.quantity}</span>
                    </span>
                </div>
                <div class="product-price">
                    ${formatCurrency(item.priceAtPurchase * item.quantity)}
                </div>
            </div>
        `).join('');

                bodyContent.innerHTML = `
            <div class="detail-header">
                <div>
                    <span class="order-id">Mã vận đơn: ${order.orderCode}</span><br>
                    <span class="order-date"><i class="far fa-calendar-alt me-1"></i> Ngày đặt: ${formatDate(order.createdAt)}</span>
                </div>
                <div>
                    ${typeof getStatusBadge === 'function' ? getStatusBadge(order.status) : `<span class="badge bg-secondary">${order.status}</span>`}
                </div>
            </div>

            <div class="info-grid">
                <div class="info-box">
                    <h5><i class="fas fa-user"></i> Người đặt hàng</h5>
                    <p><strong>Tên:</strong> ${order.userName || 'Khách vãng lai'}</p>
                    <p><strong>Email:</strong> ${order.userEmail || '---'}</p>
                    <p><strong>SĐT:</strong> ${order.userPhone || '---'}</p>
                </div>

                <div class="info-box">
                    <h5><i class="fas fa-map-marker-alt"></i> Người nhận</h5>
                    <p><strong>Tên:</strong> ${order.recipientName}</p>
                    <p><strong>SĐT:</strong> ${order.recipientPhone}</p>
                    <p class="mt-2" style="line-height: 1.4; border-top: 1px dashed #eee; padding-top: 5px;">
                        <strong>Địa chỉ:</strong><br> ${order.shippingAddress}
                    </p>
                </div>

                <div class="info-box">
                    <h5><i class="fas fa-credit-card"></i> Thanh toán</h5>
                    <p><strong>Phương thức:</strong> ${typeof getPaymentBadge === 'function' ? getPaymentBadge(order.paymentMethod) : order.paymentMethod}</p>
                    <p><strong>Trạng thái:</strong> 
                        ${order.paymentStatus === 'SUCCESS'
                    ? '<span class="text-success fw-bold"><i class="fas fa-check"></i> Đã thanh toán</span>'
                    : '<span class="text-warning fw-bold"><i class="fas fa-hourglass-half"></i> Chưa thanh toán</span>'}
                    </p>
                    ${order.paymentExpireAt ? `<p><strong>Hết hạn:</strong> <span style="font-size:12px;">${formatDate(order.paymentExpireAt)}</span></p>` : ''}
                </div>
            </div>
            
            ${paymentWarningHtml}

            <h6 class="mb-3 fw-bold text-dark"><i class="fas fa-box-open me-2"></i>Chi tiết sản phẩm</h6>
            <div class="detail-products">
                ${itemsHtml}
            </div>

            <div class="order-summary">
                <div class="summary-box">
                    <div class="summary-row">
                        <span>Tổng tiền hàng:</span>
                        <span>${formatCurrency(order.totalPrice)}</span>
                    </div>
                    <div class="summary-row">
                        <span>Phí vận chuyển:</span>
                        <span>${formatCurrency(order.shippingPrice)}</span>
                    </div>
                    <div class="summary-row total">
                        <span>Thực thu:</span>
                        <span>${formatCurrency(order.finalPrice)}</span>
                    </div>
                </div>
            </div>
        `;
            })
            .catch(err => {
                console.error(err);
                bodyContent.innerHTML = `<div class="alert alert-danger text-center"><i class="fas fa-exclamation-triangle"></i> Lỗi tải chi tiết đơn hàng: ${err.message}</div>`;
            });
    };

    function closeModal() {
        document.getElementById('orderModal').classList.remove('show');
    }

    function formatDate(dateString) {
        if (!dateString) return '';
        return new Date(dateString).toLocaleString('vi-VN');
    }

    function changePage(delta) {
        const newPage = state.page + delta;
        const totalPages = Math.ceil(state.allOrders.length / state.size);
        
        if (newPage >= 0 && newPage < totalPages) {
            state.page = newPage;
            renderTableWithPagination();
        }
    }

    function updatePagination(data) {
        const totalPages = data.totalPages || 1;
        const currentPage = state.page;
        const totalElements = data.totalElements || 0;
        
        const startItem = totalElements > 0 ? (currentPage * state.size + 1) : 0;
        const endItem = Math.min((currentPage + 1) * state.size, totalElements);
        
        const pageInfoEl = document.querySelector('.page-info');
        if (pageInfoEl) {
            pageInfoEl.innerText = `Hiển thị ${startItem}-${endItem} trên ${totalElements} đơn hàng`;
        }

        const prevBtn = document.querySelector('.page-btn.prev');
        if (prevBtn) {
            prevBtn.disabled = currentPage === 0;
            prevBtn.style.opacity = currentPage === 0 ? '0.5' : '1';
            prevBtn.style.cursor = currentPage === 0 ? 'not-allowed' : 'pointer';
        }
        
        const nextBtn = document.querySelector('.page-btn.next');
        if (nextBtn) {
            nextBtn.disabled = (currentPage + 1) >= totalPages;
            nextBtn.style.opacity = (currentPage + 1) >= totalPages ? '0.5' : '1';
            nextBtn.style.cursor = (currentPage + 1) >= totalPages ? 'not-allowed' : 'pointer';
        }
        
        const paginationControls = document.querySelector('.pagination-controls');
        if (paginationControls) {
            const existingPageBtns = paginationControls.querySelectorAll('.page-btn:not(.prev):not(.next)');
            existingPageBtns.forEach(btn => btn.remove());
            
            const maxVisiblePages = 5;
            let startPage = Math.max(0, currentPage - Math.floor(maxVisiblePages / 2));
            let endPage = Math.min(totalPages - 1, startPage + maxVisiblePages - 1);
            
            if (endPage - startPage < maxVisiblePages - 1) {
                startPage = Math.max(0, endPage - maxVisiblePages + 1);
            }
            
            const nextBtn = paginationControls.querySelector('.page-btn.next');
            for (let i = startPage; i <= endPage; i++) {
                const pageBtn = document.createElement('button');
                pageBtn.className = 'page-btn' + (i === currentPage ? ' active' : '');
                pageBtn.textContent = i + 1;
                pageBtn.onclick = () => {
                    state.page = i;
                    renderTableWithPagination();
                };
                paginationControls.insertBefore(pageBtn, nextBtn);
            }
        }
    }
    
    function updateStatistics(orders) {
        // Hàm này không còn dùng nữa, đã được thay bằng loadStatistics() và updateStatisticsUI()
    }

    function formatCurrency(amount) {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
    }

    function formatDate(dateString) {
        if (!dateString) return '';
        const date = new Date(dateString);
        return date.toLocaleString('vi-VN');
    }

    window.filterByStatus = function(status) {
        const tab = document.querySelector(`.tab-item[data-status="${status}"]`);
        if(tab && !tab.classList.contains('active')) {
            tab.click();
        }
    };
});