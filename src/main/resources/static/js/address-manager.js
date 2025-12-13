// File: src/main/resources/static/js/address-manager.js

var addressModal;

// Khởi tạo Modal khi trang load xong
document.addEventListener("DOMContentLoaded", function() {
    var modalEl = document.getElementById('addressModal');
    if(modalEl) {
        addressModal = new bootstrap.Modal(modalEl);
    }
});

// 1. Mở Modal Thêm mới
function openAddressModal() {
    document.getElementById('addressForm').reset();
    document.getElementById('addrId').value = '';
    document.getElementById('modalTitle').innerText = 'Thêm địa chỉ mới';
    addressModal.show();
}

// 2. Mở Modal Sửa (Lấy dữ liệu từ data attribute)
function editAddress(btn) {
    var id = btn.getAttribute('data-id');
    var name = btn.getAttribute('data-name');
    var phone = btn.getAttribute('data-phone');
    var line = btn.getAttribute('data-line');
    var village = btn.getAttribute('data-village');
    var district = btn.getAttribute('data-district');
    var province = btn.getAttribute('data-province');
    var isDef = btn.getAttribute('data-default') === 'true';

    document.getElementById('addrId').value = id;
    document.getElementById('recipientName').value = name;
    document.getElementById('recipientPhone').value = phone;
    document.getElementById('addressLine').value = line;
    document.getElementById('province').value = province;
    document.getElementById('district').value = district;
    document.getElementById('village').value = village;
    document.getElementById('isDefault').checked = isDef;

    document.getElementById('modalTitle').innerText = 'Cập nhật địa chỉ';
    addressModal.show();
}

// 3. Gọi API Lưu
function saveAddress() {
    var id = document.getElementById('addrId').value;
    var data = {
        recipientName: document.getElementById('recipientName').value,
        recipientPhone: document.getElementById('recipientPhone').value,
        addressLine: document.getElementById('addressLine').value,
        province: document.getElementById('province').value,
        district: document.getElementById('district').value,
        village: document.getElementById('village').value,
        isDefault: document.getElementById('isDefault').checked
    };

    var url = '/v1/api/addresses';
    var method = 'POST';

    if (id) {
        url += '/' + id;
        method = 'PUT';
    }

    var token = $("meta[name='_csrf']").attr("content");
    var header = $("meta[name='_csrf_header']").attr("content");

    $.ajax({
        url: url,
        type: method,
        contentType: 'application/json',
        data: JSON.stringify(data),
        beforeSend: function(xhr) {
            if(header && token) xhr.setRequestHeader(header, token);
        },
        success: function(response) {
            alert("Lưu thành công!");
            location.reload();
        },
        error: function(xhr) {
            if (xhr.responseJSON && typeof xhr.responseJSON === 'object') {
                var msg = "Lỗi:\n";
                for (var key in xhr.responseJSON) {
                    msg += "- " + xhr.responseJSON[key] + "\n";
                }
                alert(msg);
            } else {
                alert("Lỗi: " + xhr.responseText);
            }
        }
    });
}

// 4. Gọi API Xóa
function deleteAddress(id) {
    if(!confirm("Bạn có chắc muốn xóa địa chỉ này?")) return;

    var token = $("meta[name='_csrf']").attr("content");
    var header = $("meta[name='_csrf_header']").attr("content");

    $.ajax({
        url: '/v1/api/addresses/' + id,
        type: 'DELETE',
        beforeSend: function(xhr) {
            if(header && token) xhr.setRequestHeader(header, token);
        },
        success: function() {
            location.reload();
        },
        error: function(xhr) {
            alert("Lỗi xóa: " + xhr.responseText);
        }
    });
}

// 5. Gọi API Đặt mặc định
function setDefault(id) {
    var token = $("meta[name='_csrf']").attr("content");
    var header = $("meta[name='_csrf_header']").attr("content");

    $.ajax({
        url: '/v1/api/addresses/' + id + '/set-default',
        type: 'POST',
        beforeSend: function(xhr) {
            if(header && token) xhr.setRequestHeader(header, token);
        },
        success: function() {
            location.reload();
        },
        error: function(xhr) {
            alert("Lỗi: " + xhr.responseText);
        }
    });
}