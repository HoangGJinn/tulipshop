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
async function toggleWishlist(btn) {
    const icon = btn.querySelector('i');
    const productId = btn.getAttribute('data-product-id');
    if (!productId) return;

    const wasLiked = icon.classList.contains('fas');

    try {
        const res = await fetch(`/v1/api/wishlist/toggle?productId=${encodeURIComponent(productId)}`, {
            method: 'POST',
            credentials: 'include'
        });

        if (res.status === 401) {
            const redirectUrl = window.location.pathname + window.location.search;
            window.location.href = `/login?redirect=${encodeURIComponent(redirectUrl)}`;
            return;
        }

        if (!res.ok) {
            throw new Error('Request failed');
        }

        const data = await res.json();
        const liked = !!data.liked;

        if (liked) {
            icon.classList.remove('far');
            icon.classList.add('fas', 'text-danger');
            icon.classList.remove('text-dark');
        } else {
            icon.classList.remove('fas', 'text-danger');
            icon.classList.add('far');
            icon.classList.add('text-dark');
        }
    } catch (e) {
        console.error(e);
        // rollback UI
        if (wasLiked) {
            icon.classList.remove('far');
            icon.classList.add('fas', 'text-danger');
            icon.classList.remove('text-dark');
        } else {
            icon.classList.remove('fas', 'text-danger');
            icon.classList.add('far');
            icon.classList.add('text-dark');
        }
        alert('Không thể cập nhật wishlist lúc này');
    }
}