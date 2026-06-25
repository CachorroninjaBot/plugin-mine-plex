const API_BASE = window.location.origin;
let authToken = null;
let playerUUID = null;
let isOwner = false;
let isAdmin = false;

document.addEventListener('DOMContentLoaded', () => {
    const savedToken = localStorage.getItem('teams_token');
    const savedUUID = localStorage.getItem('teams_uuid');
    if (savedToken && savedUUID) {
        authToken = savedToken;
        playerUUID = savedUUID;
        loadTeamInfo();
    }

    document.getElementById('btn-generate-code').addEventListener('click', generateCode);
    document.getElementById('btn-verify').addEventListener('click', verifyCode);

    document.querySelectorAll('.tab').forEach(tab => {
        tab.addEventListener('click', () => switchTab(tab.dataset.tab));
    });

    document.getElementById('btn-invite').addEventListener('click', invitePlayer);
    document.getElementById('btn-deposit').addEventListener('click', () => bankAction('deposit'));
    document.getElementById('btn-withdraw').addEventListener('click', () => bankAction('withdraw'));

    document.querySelectorAll('[data-setting]').forEach(btn => {
        btn.addEventListener('click', () => updateSetting(btn.dataset.setting));
    });

    document.getElementById('setting-open').addEventListener('click', () => toggleSetting('open'));
    document.getElementById('setting-pvp').addEventListener('click', () => toggleSetting('pvp'));
});

async function apiFetch(path, options = {}) {
    const headers = { 'Content-Type': 'application/json' };
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;
    const resp = await fetch(`${API_BASE}${path}`, { ...options, headers });
    const data = await resp.json();
    if (!resp.ok) throw new Error(data.error || 'Erro desconhecido');
    return data;
}

async function generateCode() {
    try {
        const data = await apiFetch('/api/auth/link');
        document.getElementById('auth-code').textContent = data.code;
        document.getElementById('code-display').classList.remove('hidden');
        document.getElementById('btn-generate-code').classList.add('hidden');
        showToast('Código gerado! Use no Minecraft.', 'success');
    } catch (e) {
        showToast('Erro ao gerar código: ' + e.message, 'error');
    }
}

async function verifyCode() {
    const code = document.getElementById('verify-code').value.trim().toUpperCase();
    if (!code) {
        showToast('Digite o código.', 'error');
        return;
    }
    try {
        const data = await apiFetch('/api/auth/verify', {
            method: 'POST',
            body: JSON.stringify({ code })
        });
        authToken = data.token;
        playerUUID = data.uuid;
        localStorage.setItem('teams_token', authToken);
        localStorage.setItem('teams_uuid', playerUUID);
        showToast('Conta vinculada com sucesso!', 'success');
        loadTeamInfo();
    } catch (e) {
        showToast('Erro: ' + e.message, 'error');
    }
}

async function loadTeamInfo() {
    showLoading(true);
    try {
        const data = await apiFetch('/api/team/info');
        document.getElementById('auth-section').classList.add('hidden');
        document.getElementById('team-section').classList.remove('hidden');

        isOwner = data.isOwner;
        isAdmin = data.isAdmin;

        updatePermissionUI();

        document.getElementById('team-name').textContent = data.name;
        document.getElementById('team-tag').textContent = `[${data.tag}]`;
        document.getElementById('team-level').textContent = data.level;
        document.getElementById('team-score').textContent = Math.floor(data.score);
        document.getElementById('team-members-count').textContent = data.memberCount;
        document.getElementById('team-money').textContent = `$${data.money.toFixed(2)}`;
        document.getElementById('team-description').textContent = data.description || 'Nenhuma';
        document.getElementById('team-color').textContent = data.color;
        document.getElementById('team-open').textContent = data.open ? 'Sim' : 'Não';
        document.getElementById('team-pvp').textContent = data.pvp ? 'Sim' : 'Não';

        if (data.home) {
            document.getElementById('team-home').textContent =
                `${data.home.world} (${Math.floor(data.home.x)}, ${Math.floor(data.home.y)}, ${Math.floor(data.home.z)})`;
        } else {
            document.getElementById('team-home').textContent = 'Não definida';
        }

        loadMembers();
        loadWarps();
        loadAllies();
        loadBank();
    } catch (e) {
        showToast('Erro ao carregar time: ' + e.message, 'error');
        document.getElementById('auth-section').classList.remove('hidden');
        document.getElementById('team-section').classList.add('hidden');
        authToken = null;
        localStorage.removeItem('teams_token');
    }
    showLoading(false);
}

function updatePermissionUI() {
    document.querySelectorAll('.owner-only').forEach(el => {
        el.style.display = isOwner ? '' : 'none';
    });
    document.querySelectorAll('.owner-admin-only').forEach(el => {
        el.style.display = (isOwner || isAdmin) ? '' : 'none';
    });
}

async function loadMembers() {
    try {
        const data = await apiFetch('/api/team/members');
        const list = document.getElementById('members-list');
        list.innerHTML = '';

        data.members.forEach(member => {
            const item = document.createElement('div');
            item.className = 'member-item';
            const rankClass = member.rank === 'OWNER' ? 'rank-owner' : member.rank === 'ADMIN' ? 'rank-admin' : 'rank-default';
            const onlineBadge = member.isOnline
                ? '<span class="online-badge">Online</span>'
                : '<span class="offline-badge">Offline</span>';

            let actions = '';
            if (member.uuid !== playerUUID) {
                if (isOwner && member.rank !== 'OWNER') {
                    actions += `<button class="btn btn-sm btn-success" onclick="memberAction('promote','${member.uuid}')">Promover</button>`;
                    actions += `<button class="btn btn-sm btn-warning" onclick="memberAction('demote','${member.uuid}')">Rebaixar</button>`;
                    actions += `<button class="btn btn-sm btn-danger" onclick="memberAction('kick','${member.uuid}')">Expulsar</button>`;
                } else if (isAdmin && member.rank === 'DEFAULT') {
                    actions += `<button class="btn btn-sm btn-success" onclick="memberAction('promote','${member.uuid}')">Promover</button>`;
                    actions += `<button class="btn btn-sm btn-danger" onclick="memberAction('kick','${member.uuid}')">Expulsar</button>`;
                }
            }

            item.innerHTML = `
                <div class="member-info">
                    <span class="member-name">${member.name} ${onlineBadge}</span>
                    <span class="member-rank ${rankClass}">${member.rank}</span>
                </div>
                <div class="member-actions">${actions}</div>
            `;
            list.appendChild(item);
        });
    } catch (e) {
        console.error('Erro ao carregar membros:', e);
    }
}

async function loadWarps() {
    try {
        const data = await apiFetch('/api/team/warps');
        const list = document.getElementById('warps-list');
        list.innerHTML = '';

        if (data.warps.length === 0) {
            list.innerHTML = '<p style="color:var(--text-muted);text-align:center;padding:20px;">Nenhuma warp definida.</p>';
            return;
        }

        data.warps.forEach(warp => {
            const item = document.createElement('div');
            item.className = 'warp-item';
            let coords = warp.world ? `${warp.world} (${Math.floor(warp.x)}, ${Math.floor(warp.y)}, ${Math.floor(warp.z)})` : '';
            let actions = '';
            if (isOwner || isAdmin) {
                actions = `<button class="btn btn-sm btn-danger" onclick="deleteWarp('${warp.name}')">Remover</button>`;
            }
            item.innerHTML = `
                <div class="warp-info">
                    <span class="warp-name">${warp.name}</span>
                    <span class="member-rank">${coords}</span>
                </div>
                <div class="warp-actions">${actions}</div>
            `;
            list.appendChild(item);
        });
    } catch (e) {
        console.error('Erro ao carregar warps:', e);
    }
}

async function loadAllies() {
    try {
        const data = await apiFetch('/api/team/allies');
        const list = document.getElementById('allies-list');
        list.innerHTML = '';

        if (data.allies.length === 0) {
            list.innerHTML = '<p style="color:var(--text-muted);text-align:center;padding:20px;">Nenhuma aliança.</p>';
            return;
        }

        data.allies.forEach(ally => {
            const item = document.createElement('div');
            item.className = 'ally-item';
            let actions = '';
            if (isOwner) {
                actions = `<button class="btn btn-sm btn-danger" onclick="allyAction('remove','${ally.id}')">Remover</button>`;
            }
            item.innerHTML = `
                <div class="ally-info">
                    <span class="ally-name">${ally.name}</span>
                </div>
                <div class="ally-actions">${actions}</div>
            `;
            list.appendChild(item);
        });
    } catch (e) {
        console.error('Erro ao carregar aliados:', e);
    }
}

async function loadBank() {
    try {
        const data = await apiFetch('/api/team/bank');
        document.getElementById('bank-balance').textContent = `$${data.balance.toFixed(2)}`;
    } catch (e) {
        console.error('Erro ao carregar banco:', e);
    }
}

async function memberAction(action, target) {
    try {
        await apiFetch('/api/team/members', {
            method: 'POST',
            body: JSON.stringify({ action, target })
        });
        showToast(`Ação "${action}" executada com sucesso!`, 'success');
        loadMembers();
    } catch (e) {
        showToast('Erro: ' + e.message, 'error');
    }
}

async function invitePlayer() {
    const name = prompt('Nome do jogador para convidar:');
    if (!name) return;
    try {
        const playerData = await fetch(`https://api.mojang.com/users/profiles/minecraft/${name}`).then(r => r.json());
        if (playerData.id) {
            const uuid = playerData.id.replace(/(.{8})(.{4})(.{4})(.{4})(.{12})/, '$1-$2-$3-$4-$5');
            await apiFetch('/api/team/members', {
                method: 'POST',
                body: JSON.stringify({ action: 'invite', target: uuid })
            });
            showToast(`Convite enviado para ${name}!`, 'success');
        } else {
            showToast('Jogador não encontrado.', 'error');
        }
    } catch (e) {
        showToast('Erro: ' + e.message, 'error');
    }
}

async function bankAction(action) {
    const amount = parseFloat(document.getElementById('bank-amount').value);
    if (!amount || amount <= 0) {
        showToast('Digite um valor válido.', 'error');
        return;
    }
    try {
        await apiFetch('/api/team/bank', {
            method: 'POST',
            body: JSON.stringify({ action, amount })
        });
        showToast(`${action === 'deposit' ? 'Depositado' : 'Sacado'} $${amount.toFixed(2)}!`, 'success');
        loadBank();
        loadTeamInfo();
    } catch (e) {
        showToast('Erro: ' + e.message, 'error');
    }
}

async function deleteWarp(name) {
    if (!confirm(`Remover warp "${name}"?`)) return;
    try {
        await apiFetch(`/api/team/warps/${name}`, { method: 'DELETE' });
        showToast(`Warp "${name}" removida!`, 'success');
        loadWarps();
    } catch (e) {
        showToast('Erro: ' + e.message, 'error');
    }
}

async function allyAction(action, target) {
    try {
        await apiFetch('/api/team/allies', {
            method: 'POST',
            body: JSON.stringify({ action, target })
        });
        showToast('Aliança removida!', 'success');
        loadAllies();
    } catch (e) {
        showToast('Erro: ' + e.message, 'error');
    }
}

async function updateSetting(setting) {
    const input = document.getElementById(`setting-${setting}`);
    const value = input.value.trim();
    if (!value) {
        showToast('Digite um valor.', 'error');
        return;
    }
    try {
        await apiFetch('/api/team/settings', {
            method: 'POST',
            body: JSON.stringify({ setting, value })
        });
        showToast(`Configuração "${setting}" alterada!`, 'success');
        loadTeamInfo();
    } catch (e) {
        showToast('Erro: ' + e.message, 'error');
    }
}

async function toggleSetting(setting) {
    const currentValue = document.getElementById(`setting-${setting}`).textContent.includes('Sim');
    try {
        await apiFetch('/api/team/settings', {
            method: 'POST',
            body: JSON.stringify({ setting, value: (!currentValue).toString() })
        });
        showToast(`Configuração "${setting}" alterada!`, 'success');
        loadTeamInfo();
    } catch (e) {
        showToast('Erro: ' + e.message, 'error');
    }
}

function switchTab(tabName) {
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
    document.querySelector(`.tab[data-tab="${tabName}"]`).classList.add('active');
    document.getElementById(`tab-${tabName}`).classList.add('active');
}

function showLoading(show) {
    document.getElementById('loading').classList.toggle('hidden', !show);
}

function showToast(message, type) {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = `toast ${type}`;
    toast.classList.remove('hidden');
    setTimeout(() => toast.classList.add('hidden'), 3000);
}
