// Wishlist utilities
async function toggleWishlist(btn) {
    const productId = btn.getAttribute('data-product-id');
    const res = await fetch(`/v1/api/wishlist/toggle?productId=${productId}`, { method: 'POST', credentials: 'include' });
    if (res.ok) {
        const data = await res.json();
        const i = btn.querySelector('i');
        if (data.liked) {
            i.classList.remove('far');
            i.classList.add('fas', 'text-danger');
        } else {
            i.classList.remove('fas', 'text-danger');
            i.classList.add('far');
        }
        // Update wishlist count in header if exists
        const countEl = document.getElementById('wishlist-count');
        if (countEl) countEl.textContent = data.count;
    } else if (res.status === 401) {
        // Not logged in
        window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname);
    } else {
        alert('Không thể cập nhật wishlist');
    }
}

// On page load, set wishlist status for all hearts
document.addEventListener('DOMContentLoaded', async () => {
    const hearts = document.querySelectorAll('.btn-add-wl[data-product-id]');
    for (const btn of hearts) {
        const productId = btn.getAttribute('data-product-id');
        const res = await fetch(`/v1/api/wishlist/check?productId=${productId}`, { credentials: 'include' });
        if (res.ok) {
            const data = await res.json();
            const i = btn.querySelector('i');
            if (data.liked) {
                i.classList.remove('far');
                i.classList.add('fas', 'text-danger');
            }
        }
    }
    // Load wishlist count
    const countRes = await fetch('/v1/api/wishlist/count', { credentials: 'include' });
    if (countRes.ok) {
        const data = await countRes.json();
        const countEl = document.getElementById('wishlist-count');
        if (countEl) countEl.textContent = data.count;
    }
});
