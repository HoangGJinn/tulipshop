// === TABBED PRODUCT INFO FUNCTIONALITY ===
function switchTab(tabName) {
    // Remove active class from all tab headers
    document.querySelectorAll('.tab-header').forEach(header => {
        header.classList.remove('active');
    });

    // Hide all tab panels
    document.querySelectorAll('.tab-panel').forEach(panel => {
        panel.classList.remove('active');
    });

    // Add active class to clicked tab header
    const activeHeader = document.querySelector(`[data-tab="${tabName}"]`);
    if (activeHeader) {
        activeHeader.classList.add('active');
    }

    // Show corresponding tab panel
    const activePanel = document.getElementById(`tab-${tabName}`);
    if (activePanel) {
        activePanel.classList.add('active');
    }
}

// Make switchTab available globally
window.switchTab = switchTab;
