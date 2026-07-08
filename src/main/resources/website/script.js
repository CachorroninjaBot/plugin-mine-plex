// Server IP
const SERVER_IP = 'play.minepex.com';

// Copy IP Function
function copyIP() {
    navigator.clipboard.writeText(SERVER_IP).then(() => {
        showToast('IP copiado com sucesso!');
    }).catch(() => {
        // Fallback for older browsers
        const textArea = document.createElement('textarea');
        textArea.value = SERVER_IP;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
        showToast('IP copiado com sucesso!');
    });
}

// Toast Notification
function showToast(message) {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.classList.add('show');
    setTimeout(() => {
        toast.classList.remove('show');
    }, 2000);
}

// Mobile Menu Toggle
function toggleMenu() {
    const mobileNav = document.getElementById('mobileNav');
    mobileNav.classList.toggle('active');
}

function closeMenu() {
    const mobileNav = document.getElementById('mobileNav');
    mobileNav.classList.remove('active');
}

// Store Tab Switching
function switchStoreTab(tab) {
    const realMoneyStore = document.getElementById('realMoneyStore');
    const mobcoinsStore = document.getElementById('mobcoinsStore');
    const tabs = document.querySelectorAll('.store-tab');

    tabs.forEach(t => t.classList.remove('active'));

    if (tab === 'real') {
        realMoneyStore.classList.remove('hidden');
        mobcoinsStore.classList.add('hidden');
        tabs[0].classList.add('active');
    } else {
        realMoneyStore.classList.add('hidden');
        mobcoinsStore.classList.remove('hidden');
        tabs[1].classList.add('active');
    }
}

// Navbar Active State on Scroll
function updateActiveNav() {
    const sections = document.querySelectorAll('section[id]');
    const navTabs = document.querySelectorAll('.nav-tab');
    let current = '';

    sections.forEach(section => {
        const sectionTop = section.offsetTop;
        const sectionHeight = section.clientHeight;
        if (pageYOffset >= sectionTop - 200) {
            current = section.getAttribute('id');
        }
    });

    navTabs.forEach(tab => {
        tab.classList.remove('active');
        if (tab.getAttribute('href') === `#${current}`) {
            tab.classList.add('active');
        }
    });
}

window.addEventListener('scroll', updateActiveNav);

// Simulated Online Count (Replace with real API call)
function updateOnlineCount() {
    // This would normally be an API call to your server
    // For demo, we show a random number
    const count = Math.floor(Math.random() * 50) + 100;
    document.getElementById('onlineCount').textContent = count;
}

// Update count on load and every 30 seconds
updateOnlineCount();
setInterval(updateOnlineCount, 30000);

// Smooth scroll for anchor links
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
        e.preventDefault();
        const target = document.querySelector(this.getAttribute('href'));
        if (target) {
            target.scrollIntoView({
                behavior: 'smooth',
                block: 'start'
            });
        }
    });
});

// Navbar background on scroll
window.addEventListener('scroll', function() {
    const navbar = document.querySelector('.navbar');
    if (window.scrollY > 50) {
        navbar.style.background = 'rgba(10, 10, 10, 0.98)';
    } else {
        navbar.style.background = 'rgba(15, 15, 15, 0.95)';
    }
});

// Pix Modal
function showPixModal(itemName, price) {
    const modal = document.getElementById('pixModal');
    const itemInfo = document.getElementById('pixItemInfo');
    itemInfo.innerHTML = `
        <p style="color: var(--text-white); font-weight: 700; font-size: 1.2rem;">${itemName}</p>
        <p style="color: var(--secondary); font-size: 1.5rem; font-weight: 700; margin-top: 0.5rem;">${price}</p>
    `;
    modal.classList.add('active');
}

function closePixModal() {
    const modal = document.getElementById('pixModal');
    modal.classList.remove('active');
}

function copyPixKey() {
    const pixKey = document.getElementById('pixKey').textContent;
    navigator.clipboard.writeText(pixKey).then(() => {
        showToast('Chave Pix copiada!');
    }).catch(() => {
        const textArea = document.createElement('textarea');
        textArea.value = pixKey;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
        showToast('Chave Pix copiada!');
    });
}

// Close modal on overlay click
document.addEventListener('click', function(e) {
    if (e.target.classList.contains('modal-overlay')) {
        closePixModal();
    }
});

// Render Pix Store Items
function renderPixStore() {
    const buyButtons = document.querySelectorAll('.btn-buy');
    buyButtons.forEach(btn => {
        btn.addEventListener('click', function() {
            const storeItem = this.closest('.store-item');
            if (!storeItem) return;
            const itemName = storeItem.querySelector('h3')?.textContent || 'Item';
            const price = storeItem.querySelector('.price')?.textContent || 'R$ 0,00';
            showPixModal(itemName, price);
        });
    });
}

// Initialize
document.addEventListener('DOMContentLoaded', function() {
    console.log('Minepex Legends website loaded!');
    renderPixStore();
});
