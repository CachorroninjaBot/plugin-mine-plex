// ─── Config ─────────────────────────────────────────────────────────────────
const SERVER_IP = 'play.minepex.com';
const API_BASE = window.location.origin;

// ─── State ──────────────────────────────────────────────────────────────────
let currentPlayer = localStorage.getItem('minepex_player') || '';
let currentPurchaseId = null;
let pixTimerInterval = null;

// ─── Store Items (loaded from API or fallback) ──────────────────────────────
const PIX_ITEMS = {
    'vip':     { name: 'Rank VIP',     price: 15.00, icon: '⭐', color: '#00ff88',  features: ['Cor no chat', 'Comando /fly', '2 homes', '10 MobCoins'] },
    'elite':   { name: 'Rank ELITE',   price: 35.00, icon: '💎', color: '#00d4ff',  features: ['Cor no chat', 'Comando /fly', '5 homes', '30 MobCoins', 'Cosméticos especiais'] },
    'ultra':   { name: 'Rank ULTRA',   price: 60.00, icon: '👑', color: '#ff6b6b',  features: ['Todas perks ELITE', '10 homes', '100 MobCoins', 'Tag exclusiva', 'Prioridade na fila'] },
    'media':   { name: 'Rank MÍDIA',   price: 100.00, icon: '🔥', color: '#ffd700', features: ['Todas perks ULTRA', '20 homes', '250 MobCoins', 'Efeitos de partícula', 'Titles exclusivos'] },
    'famous':  { name: 'Rank FAMOSO',  price: 150.00, icon: '🌟', color: '#ff69b4', features: ['Todas perks MÍDIA', '30 homes', '500 MobCoins', 'Cosméticos premium', 'Acesso antecipado'] },
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
    // Restore player name
    if (currentPlayer) {
        document.getElementById('playerName').value = currentPlayer;
        updatePlayerStatus();
    }

    // Render store items
    renderPixStore();
    renderMobcoinsStore();

    // Navbar scroll effect
    window.addEventListener('scroll', updateActiveNav);
    window.addEventListener('scroll', () => {
        const navbar = document.querySelector('.navbar');
        navbar.style.background = window.scrollY > 50
            ? 'rgba(10, 10, 10, 0.98)'
            : 'rgba(15, 15, 15, 0.95)';
    });

    console.log('Minepex Legends store loaded!');
});

// ─── Player Name ────────────────────────────────────────────────────────────
function setPlayerName() {
    const input = document.getElementById('playerName');
    const name = input.value.trim();
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
    if (currentPlayer) {
        status.textContent = `✓ Jogador: ${currentPlayer}`;
        status.classList.add('active');
    } else {
        status.textContent = 'Defina seu nick para comprar';
        status.classList.remove('active');
    }
}

// ─── Render Store Items ─────────────────────────────────────────────────────
function renderPixStore() {
    const grid = document.getElementById('pixStoreGrid');
    grid.innerHTML = '';

    for (const [id, item] of Object.entries(PIX_ITEMS)) {
        grid.innerHTML += `
            <div class="store-item">
                <div class="item-icon">${item.icon}</div>
                <h3>${item.name}</h3>
                <ul class="item-features">
                    ${item.features.map(f => `<li>${f}</li>`).join('')}
                </ul>
                <div class="item-price">
                    <span class="price">R$ ${item.price.toFixed(2)}</span>
                </div>
                <button class="btn-buy" onclick="buyWithPix('${id}')">Comprar via PIX</button>
            </div>
        `;
    }
}

function renderMobcoinsStore() {
    const grid = document.getElementById('mobcoinsStoreGrid');
    grid.innerHTML = '';

    for (const [id, item] of Object.entries(MOBCOINS_ITEMS)) {
        grid.innerHTML += `
            <div class="store-item">
                <div class="item-icon">${item.icon}</div>
                <h3>${item.name}</h3>
                <p class="item-desc">${item.desc}</p>
                <div class="item-price mobcoins-price">
                    <span class="price">${item.cost.toLocaleString()}</span>
                    <span class="coin-icon">🪙</span>
                </div>
                <button class="btn-buy" onclick="buyWithMobCoins('${id}')">Comprar</button>
            </div>
        `;
    }
}

// ─── Copy IP ────────────────────────────────────────────────────────────────
function copyIP() {
    navigator.clipboard.writeText(SERVER_IP).then(() => {
        showToast('IP copiado: ' + SERVER_IP, 'success');
    }).catch(() => {
        const textArea = document.createElement('textarea');
        textArea.value = SERVER_IP;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
        showToast('IP copiado: ' + SERVER_IP, 'success');
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

// ─── Buy with PIX ──────────────────────────────────────────────────────────
async function buyWithPix(itemId) {
    if (!currentPlayer) {
        showToast('Defina seu nick Minecraft primeiro!', 'error');
        document.getElementById('playerName').focus();
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

        // Show PIX modal
        currentPurchaseId = data.purchaseId;
        document.getElementById('pixItemName').textContent = item.name;
        document.getElementById('pixItemPrice').textContent = item.price.toFixed(2);
        document.getElementById('pixPlayerName').textContent = currentPlayer;
        document.getElementById('pixQRCode').src = data.qrCode || '';
        document.getElementById('pixPayload').value = data.pixPayload || '';
        document.getElementById('pixStatus').innerHTML = '<p>Aguardando pagamento...</p><div class="pix-timer" id="pixTimer">30:00</div>';
        document.getElementById('pixModal').classList.remove('hidden');

        // Start timer
        startPixTimer(1800);
        // Start polling for payment status
        pollPixStatus(data.purchaseId);

    } catch (e) {
        console.error('Erro PIX:', e);
        showToast('Erro ao conectar com o servidor', 'error');
    }
}

function closePixModal() {
    document.getElementById('pixModal').classList.add('hidden');
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

            // Still pending
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

function copyPixPayload() {
    const payload = document.getElementById('pixPayload').value;
    navigator.clipboard.writeText(payload).then(() => {
        showToast('Código PIX copiado!', 'success');
    });
}

// ─── Buy with MobCoins ─────────────────────────────────────────────────────
async function buyWithMobCoins(itemId) {
    if (!currentPlayer) {
        showToast('Defina seu nick Minecraft primeiro!', 'error');
        document.getElementById('playerName').focus();
        return;
    }

    const item = MOBCOINS_ITEMS[itemId];
    if (!item) return;

    if (!confirm(`Comprar ${item.name} por ${item.cost.toLocaleString()} MobCoins?`)) {
        return;
    }

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
    if (!currentPlayer) {
        document.getElementById('mobcoinsBalance').textContent = '-';
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/api/mobcoins/${encodeURIComponent(currentPlayer)}`);
        const data = await res.json();
        document.getElementById('mobcoinsBalance').textContent = data.balance?.toLocaleString() || '0';
    } catch (e) {
        document.getElementById('mobcoinsBalance').textContent = '?';
    }
}

// ─── Toast ──────────────────────────────────────────────────────────────────
function showToast(message, type = '') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = 'toast show ' + type;
    setTimeout(() => {
        toast.className = 'toast';
    }, 3000);
}

// ─── Navbar Active State ────────────────────────────────────────────────────
function updateActiveNav() {
    const sections = document.querySelectorAll('section[id]');
    const navTabs = document.querySelectorAll('.nav-tab');
    let current = '';

    sections.forEach(section => {
        if (pageYOffset >= section.offsetTop - 200) {
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

// ─── Simulated Online Count ─────────────────────────────────────────────────
function updateOnlineCount() {
    const count = Math.floor(Math.random() * 50) + 100;
    document.getElementById('onlineCount').textContent = count;
}

updateOnlineCount();
setInterval(updateOnlineCount, 30000);
