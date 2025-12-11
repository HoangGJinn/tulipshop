// Preview màu khi hover chấm tròn
function previewColor(dot) {
    const targetId = dot.getAttribute('data-target');
    const newSrc = dot.getAttribute('data-img');
    const img = document.getElementById(targetId);

    if (img && newSrc) {
        // Nếu ảnh hiện tại không phải là ảnh hover (ảnh hover class .hover), thì mới đổi
        // Ở layout này, ta đổi ảnh gốc (ảnh dưới cùng)
        img.src = newSrc;
    }
}

// Wishlist
function toggleWishlist(btn) {
    const icon = btn.querySelector('i');
    if (icon.classList.contains('far')) {
        icon.classList.remove('far');
        icon.classList.add('fas', 'text-danger');
    } else {
        icon.classList.remove('fas', 'text-danger');
        icon.classList.add('far');
    }
}