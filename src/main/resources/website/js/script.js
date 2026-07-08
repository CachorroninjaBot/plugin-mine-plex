// ─── Config ─────────────────────────────────────────────────────────────────
const SERVER_IP = 'play.minepex.com';
const API_BASE = 'https://minepex.minehost.com.br:3000';

// ─── State ──────────────────────────────────────────────────────────────────
let currentPlayer = localStorage.getItem('minepex_player') || '';
let currentPurchaseId = null;
let pixTimerInterval = null;

// ─── Store Items ────────────────────────────────────────────────────────────
const PIX_ITEMS = {
    'vip':     { name: 'Rank VIP',     price: 15.00, icon: '⭐', color: '#00ff88',  features: ['Prefixo [VIP] colorido', 'Kit VIP semanal', 'Comandos exclusivos'] },
    'elite':   { name: 'Rank ELITE',   price: 35.00, icon: '💎', color: '#00d4ff',  features: ['Tudo do VIP', 'Kit Elite semanal', 'Voo em sobrevivência'] },
    'ultra':   { name: 'Rank ULTRA',   price: 60.00, icon: '👑', color: '#ff6b6b',  features: ['Tudo do Elite', 'Kit Ultra diário', 'Regiões exclusivas'] },
    'media':   { name: 'Rank MÍDIA',   price: 100.00, icon: '🔥', color: '#b388ff', features: ['Tag [Mídia] no chat', 'Kit exclusivo de Mídia', 'Para criadores de conteúdo'] },
    'famous':  { name: 'Rank FAMOSO',  price: 150.00, icon: '🌟', color: '#ffd700', features: ['Tudo do Ultra', 'Tag [Famoso] dourada', 'Kit Famoso premium diário'] },
};

const MOBCOINS_ITEMS = {
    'nether_star':     { name: 'Estrela do Nether',      cost: 50000,  icon: '⭐', desc: 'Item raro do Nether' },
    'heavy_core':      { name: 'Núcleo Pesado',          cost: 100000, icon: '📦', desc: 'Componente valioso' },
    'totem':           { name: 'Totem da Imortalidade',  cost: 50000,  icon: '🛡️', desc: 'Proteção contra a morte' },
    'elytra':          { name: 'Elytra',                 cost: 5000,   icon: '🦋', desc: 'Voos pelos céus' },
    'trident':         { name: 'Tridente',               cost: 10000,  icon: '🔱', desc: 'Arma poderosa' },
    'enchanted_apple': { name: 'Maçã Dourada Encantada', cost: 40000,  icon: '🍎', desc: 'Regeneração suprema' },
    'lucky_card':      { name: 'Carta da Sorte',         cost: 600,    icon: '🍀', desc: 'Gire a roda da fortuna' },
    'super_stick':     { name: 'Bastão Super (64)',      cost: 12000,  icon: '💡', desc: '64 bastões encantados' },
    'flare':           { name: 'Sinalizador de Comboio', cost: 8000,   icon: '🎆', desc: 'Sinal especial' },
    'vip_30d':         { name: 'VIP (30 dias)',          cost: 50000,  icon: '⭐', desc: 'Rank VIP por 30 dias' },
    'elite_30d':       { name: 'ELITE (30 dias)',        cost: 100000, icon: '💎', desc: 'Rank ELITE por 30 dias' },
    'ultra_30d':       { name: 'ULTRA (30 dias)',        cost: 150000, icon: '👑', desc: 'Rank ULTRA por 30 dias' },
    'media_30d':       { name: 'MÍDIA (30 dias)',        cost: 200000, icon: '🔥', desc: 'Rank MÍDIA por 30 dias' },
    'famous_30d':      { name: 'FAMOSO (30 dias)',       cost: 250000, icon: '🌟', desc: 'Rank FAMOSO por 30 dias' },
};

// ─── Init ───────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    updateActiveNav();
    setupNavbarScroll();

    if (currentPlayer) {
        const input = document.getElementById('playerName');
        if (input) input.value = currentPlayer;
        updatePlayerStatus();
    }

    refreshMobCoins();
    updateOnlineCount();
    setInterval(updateOnlineCount, 30000);
});

// ─── Navbar Active State ────────────────────────────────────────────────────
function updateActiveNav() {
    const pathname = window.location.pathname;
    document.querySelectorAll('.nav-tab').forEach(tab => {
        tab.classList.remove('active');
        const href = tab.getAttribute('href');
        if (href === pathname || (pathname === '/' && href === '/')) {
            tab.classList.add('active');
        }
    });
}

function setupNavbarScroll() {
    window.addEventListener('scroll', () => {
        const navbar = document.querySelector('.navbar');
        if (!navbar) return;
        navbar.style.background = window.scrollY > 50
            ? 'rgba(10, 6, 0, 0.98)'
            : 'rgba(10, 6, 0, 0.85)';
    });
}

// ─── Copy IP ────────────────────────────────────────────────────────────────
function copyIP() {
    navigator.clipboard.writeText(SERVER_IP).then(() => {
        showToast('IP copiado: ' + SERVER_IP, 'success');
    }).catch(() => {
        const ta = document.createElement('textarea');
        ta.value = SERVER_IP;
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        document.body.removeChild(ta);
        showToast('IP copiado: ' + SERVER_IP, 'success');
    });
}

function copyPix() {
    const key = document.querySelector('.pix-key');
    if (!key) return;
    navigator.clipboard.writeText(key.textContent).then(() => {
        showToast('Chave Pix copiada!', 'success');
    });
}

function copyPixPayload() {
    const payload = document.getElementById('pixPayload');
    if (!payload) return;
    navigator.clipboard.writeText(payload.value).then(() => {
        showToast('Código PIX copiado!', 'success');
    });
}

// ─── Store Tab Switching ────────────────────────────────────────────────────
function switchStoreTab(tab) {
    document.getElementById('realMoneyStore').classList.toggle('hidden', tab !== 'real');
    document.getElementById('mobcoinsStore').classList.toggle('hidden', tab !== 'mobcoins');
    document.querySelectorAll('.store-tab').forEach((t, i) => {
        t.classList.toggle('active', (tab === 'real' && i === 0) || (tab === 'mobcoins' && i === 1));
    });
}

// ─── Mobile Menu ────────────────────────────────────────────────────────────
function toggleMenu() {
    document.getElementById('mobileNav').classList.toggle('active');
}

function closeMenu() {
    document.getElementById('mobileNav').classList.remove('active');
}

// ─── Player Name ────────────────────────────────────────────────────────────
function setPlayerName() {
    const input = document.getElementById('playerName');
    if (!input) return;
    const name = input.value.trim().replace(/[^a-zA-Z0-9_]/g, '').substring(0, 16);
    if (!name) {
        showToast('Digite seu nick Minecraft!', 'error');
        return;
    }
    currentPlayer = name;
    localStorage.setItem('minepex_player', name);
    updatePlayerStatus();
    refreshMobCoins();
    showToast(`Nick definido: ${name}`, 'success');
}

function updatePlayerStatus() {
    const status = document.getElementById('playerStatus');
    if (!status) return;
    if (currentPlayer) {
        status.textContent = `✓ Jogador: ${currentPlayer}`;
        status.classList.add('active');
    } else {
        status.textContent = 'Defina seu nick para comprar';
        status.classList.remove('active');
    }
}

// ─── Buy with PIX ──────────────────────────────────────────────────────────
async function buyWithPix(itemId) {
    if (!currentPlayer) {
        showToast('Defina seu nick Minecraft primeiro!', 'error');
        const input = document.getElementById('playerName');
        if (input) input.focus();
        return;
    }

    const item = PIX_ITEMS[itemId];
    if (!item) return;

    try {
        const res = await fetch(`${API_BASE}/api/pix/create`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ playerName: currentPlayer, itemId })
        });

        const data = await res.json();
        if (!res.ok) {
            showToast(data.error || 'Erro ao criar pagamento', 'error');
            return;
        }

        currentPurchaseId = data.purchaseId;
        const modal = document.getElementById('pixModal');
        const body = document.getElementById('pixModalBody');

        body.innerHTML = `
            <div class="pix-modal-item">
                <h3>${item.name}</h3>
                <span class="price">R$ ${item.price.toFixed(2)}</span>
                <p>Jogador: ${currentPlayer}</p>
            </div>
            ${data.qrCode ? `<div class="pix-qr-section"><img src="${data.qrCode}" alt="QR Code Pix"></div>` : ''}
            <div class="pix-payload-section">
                <label>Código Pix (copia e cola):</label>
                <textarea class="pix-payload-input" id="pixPayload" rows="3" readonly>${data.pixPayload || ''}</textarea>
                <button class="btn-copy-payload" onclick="copyPixPayload()">📋 Copiar Código</button>
            </div>
            <div id="pixStatus">
                <p>Aguardando pagamento...</p>
                <div class="pix-timer" id="pixTimer">30:00</div>
            </div>
        `;

        modal.classList.add('active');
        startPixTimer(1800);
        pollPixStatus(data.purchaseId);

    } catch (e) {
        console.error('Erro PIX:', e);
        showToast('Erro ao conectar com o servidor', 'error');
    }
}

function showPixModal(itemId) {
    buyWithPix(itemId);
}

function closePixModal() {
    const modal = document.getElementById('pixModal');
    if (modal) modal.classList.remove('active');
    if (pixTimerInterval) clearInterval(pixTimerInterval);
    currentPurchaseId = null;
}

function startPixTimer(seconds) {
    if (pixTimerInterval) clearInterval(pixTimerInterval);
    let remaining = seconds;

    pixTimerInterval = setInterval(() => {
        remaining--;
        const min = Math.floor(remaining / 60);
        const sec = remaining % 60;
        const timerEl = document.getElementById('pixTimer');
        if (timerEl) {
            timerEl.textContent = `${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}`;
        }
        if (remaining <= 0) {
            clearInterval(pixTimerInterval);
            closePixModal();
            showToast('Tempo de pagamento expirado', 'error');
        }
    }, 1000);
}

async function pollPixStatus(purchaseId) {
    const check = async () => {
        try {
            const res = await fetch(`${API_BASE}/api/pix/status/${purchaseId}`);
            const data = await res.json();

            if (data.status === 'delivered') {
                document.getElementById('pixStatus').innerHTML = '<p style="color: #00ff88;">✅ Pagamento confirmado! Item entregue!</p>';
                setTimeout(closePixModal, 3000);
                showToast('Pagamento confirmado! Item entregue!', 'success');
                return;
            }

            if (data.status === 'paid') {
                document.getElementById('pixStatus').innerHTML = '<p style="color: #00d4ff;">⏳ Pagamento confirmado. Processando entrega...</p>';
                setTimeout(check, 2000);
                return;
            }

            if (currentPurchaseId === purchaseId) {
                setTimeout(check, 5000);
            }
        } catch (e) {
            console.warn('Erro ao verificar status:', e);
            if (currentPurchaseId === purchaseId) {
                setTimeout(check, 10000);
            }
        }
    };

    setTimeout(check, 5000);
}

// ─── Buy with MobCoins ─────────────────────────────────────────────────────
async function buyWithMobCoins(itemId) {
    if (!currentPlayer) {
        showToast('Defina seu nick Minecraft primeiro!', 'error');
        const input = document.getElementById('playerName');
        if (input) input.focus();
        return;
    }

    const item = MOBCOINS_ITEMS[itemId];
    if (!item) return;

    if (!confirm(`Comprar ${item.name} por ${item.cost.toLocaleString()} MobCoins?`)) return;

    try {
        const res = await fetch(`${API_BASE}/api/mobcoins/buy`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ playerName: currentPlayer, itemId })
        });

        const data = await res.json();
        if (!res.ok) {
            showToast(data.error || 'Erro na compra', 'error');
            return;
        }

        showToast(data.message || 'Compra realizada!', 'success');
        refreshMobCoins();
    } catch (e) {
        console.error('Erro MobCoins:', e);
        showToast('Erro ao conectar com o servidor', 'error');
    }
}

// ─── MobCoins Balance ───────────────────────────────────────────────────────
async function refreshMobCoins() {
    const balanceEl = document.getElementById('mobcoinsBalance');
    if (!balanceEl) return;

    if (!currentPlayer) {
        balanceEl.textContent = '-';
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/api/mobcoins/${encodeURIComponent(currentPlayer)}`);
        const data = await res.json();
        balanceEl.textContent = data.balance?.toLocaleString() || '0';
    } catch (e) {
        balanceEl.textContent = '?';
    }
}

async function checkMobCoins() {
    const input = document.getElementById('mcUsername');
    if (!input) return;
    const name = input.value.trim().replace(/[^a-zA-Z0-9_]/g, '').substring(0, 16);
    if (!name) {
        showToast('Digite seu nick!', 'error');
        return;
    }

    currentPlayer = name;
    localStorage.setItem('minepex_player', name);
    updatePlayerStatus();
    refreshMobCoins();
    showToast(`Saldo atualizado para ${name}`, 'success');
}

// ─── Online Count ───────────────────────────────────────────────────────────
function updateOnlineCount() {
    const el = document.getElementById('onlineCount');
    if (!el) return;

    fetch(`${API_BASE}/api/haiz/status`)
        .then(r => r.json())
        .then(data => {
            if (data.playerCount !== undefined) {
                el.textContent = data.playerCount;
            }
        })
        .catch(() => {
            el.textContent = Math.floor(Math.random() * 50) + 100;
        });
}

// ─── Toast ──────────────────────────────────────────────────────────────────
function showToast(message, type = '') {
    const toast = document.getElementById('toast');
    if (!toast) return;
    toast.textContent = message;
    toast.className = 'toast show ' + type;
    setTimeout(() => {
        toast.className = 'toast';
    }, 3000);
}

// ─── Modal close on overlay click ──────────────────────────────────────────
document.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal-overlay')) {
        closePixModal();
    }
});
