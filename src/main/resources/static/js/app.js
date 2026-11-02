/* ════════════════════════════════════════════════
   RS Treinamento & Consultoria — Sistema Escolar
   app.js  |  Lógica de negócio + chamadas à API
   ════════════════════════════════════════════════ */

// ── Endereço do servidor backend (Java + Jetty) ──
// Vazio = mesmo servidor que entregou esta página (funciona local e no Cloud Run)

const API_URL = '';

// Rotas da API que exigem login (o token vai no cabeçalho Authorization)
const ROTAS_PROTEGIDAS = /^\/(usuarios|cursos|alunos|relatorio)(\/|$)/;

// Escapa texto antes de colocar no HTML, para evitar XSS
function esc(valor) {
  const mapa = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
  return String(valor ?? '').replace(/[&<>"']/g, c => mapa[c]);
}

// Envia o token em todas as rotas protegidas. Se a sessão expirou, volta ao login.
const fetchOriginal = window.fetch.bind(window);
window.fetch = async (url, opcoes = {}) => {
  const caminho = String(url).replace(API_URL, '');
  if (!ROTAS_PROTEGIDAS.test(caminho)) return fetchOriginal(url, opcoes);

  const usuario = getUsuarioLogado();
  const cabecalhos = new Headers(opcoes.headers || {});
  if (usuario && usuario.token) cabecalhos.set('Authorization', 'Bearer ' + usuario.token);

  const resposta = await fetchOriginal(url, { ...opcoes, headers: cabecalhos });
  if (resposta.status === 401) {
    sessionStorage.removeItem('usuario_logado');
    window.location.href = 'login.html';
  }
  return resposta;
};

// ══════════════════════════════════════════════════
//  AUTENTICAÇÃO
// ══════════════════════════════════════════════════

/** Verifica se existe sessão ativa. Se não tiver, redireciona ao login */
function checkAuth() {
  const usuario = sessionStorage.getItem('usuario_logado');
  if (!usuario) {
    window.location.href = 'login.html';
  }
  return JSON.parse(usuario);
}

/** Retorna o usuário logado ou null */
function getUsuarioLogado() {
  const u = sessionStorage.getItem('usuario_logado');
  return u ? JSON.parse(u) : null;
}

/** Encerra a sessão e volta ao login */
function logout() {
  sessionStorage.removeItem('usuario_logado');
  window.location.href = 'login.html';
}

// ══════════════════════════════════════════════════
//  UTILITÁRIOS DE UI
// ══════════════════════════════════════════════════

/** Mostra mensagem de erro no elemento com id dado */
function showError(id, msg) {
  const el = document.getElementById(id);
  if (!el) return;
  el.textContent = msg;
  el.style.display = 'block';
  setTimeout(() => { el.style.display = 'none'; }, 4000);
}

/** Mostra mensagem de sucesso */
function showSuccess(id, msg) {
  const el = document.getElementById(id);
  if (!el) return;
  el.textContent = msg;
  el.style.display = 'block';
  setTimeout(() => { el.style.display = 'none'; }, 3000);
}

/** Marca qual aba da nav está ativa */
function setActiveNav(page) {
  document.querySelectorAll('.rs-nav-btn').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.page === page);
  });
}

/** Verifica conectividade com a API e atualiza o badge visual */
async function checkApiStatus() {
  const badge = document.getElementById('api-badge');
  if (!badge) return;
  try {
    const res = await fetch(`${API_URL}/health`);
    if (res.ok) {
      badge.textContent = 'API online';
      badge.classList.remove('offline');
    }
  } catch {
    badge.textContent = 'API offline — usando localStorage';
    badge.classList.add('offline');
  }
}

// ══════════════════════════════════════════════════
//  CURSOS  — CRUD via API
// ══════════════════════════════════════════════════

/** Busca todos os cursos */
async function getCursos() {
  try {
    const res = await fetch(`${API_URL}/cursos`);
    return await res.json();          // retorna array de cursos
  } catch {
    console.warn('API indisponível — cursos do localStorage');
    return JSON.parse(localStorage.getItem('cursos') || '[]');
  }
}

/** Cadastra novo curso */
async function postCurso(dados) {
  const res = await fetch(`${API_URL}/cursos`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(dados)
  });
  if (!res.ok) throw new Error(await res.text());
  return await res.json();
}

/** Remove curso por id */
async function deleteCurso(id) {
  const res = await fetch(`${API_URL}/cursos/${id}`, { method: 'DELETE' });
  if (!res.ok) throw new Error('Erro ao excluir curso');
}

// ══════════════════════════════════════════════════
//  ALUNOS  — CRUD via API
// ══════════════════════════════════════════════════

/** Busca todos os alunos */
async function getAlunos() {
  try {
    const res = await fetch(`${API_URL}/alunos`);
    return await res.json();
  } catch {
    console.warn('API indisponível — alunos do localStorage');
    return JSON.parse(localStorage.getItem('alunos') || '[]');
  }
}

/** Cadastra novo aluno */
async function postAluno(dados) {
  const res = await fetch(`${API_URL}/alunos`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(dados)
  });
  if (!res.ok) throw new Error(await res.text());
  return await res.json();
}

/** Remove aluno por id */
async function deleteAluno(id) {
  const res = await fetch(`${API_URL}/alunos/${id}`, { method: 'DELETE' });
  if (!res.ok) throw new Error('Erro ao excluir aluno');
}

// ══════════════════════════════════════════════════
//  USUÁRIOS  — CRUD via API
// ══════════════════════════════════════════════════

/** Busca todos os usuários */
async function getUsuarios() {
  try {
    const res = await fetch(`${API_URL}/usuarios`);
    return await res.json();
  } catch {
    return JSON.parse(localStorage.getItem('usuarios') || '[]');
  }
}

/** Cadastra novo usuário */
async function postUsuario(dados) {
  const res = await fetch(`${API_URL}/usuarios`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(dados)
  });
  if (!res.ok) throw new Error(await res.text());
  return await res.json();
}

/** Remove usuário por id */
async function deleteUsuario(id) {
  const res = await fetch(`${API_URL}/usuarios/${id}`, { method: 'DELETE' });
  if (!res.ok) throw new Error('Erro ao excluir usuário');
}

/** Faz login — POST /login */
async function loginUsuario(email, senha) {
  const res = await fetch(`${API_URL}/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, senha })
  });
  if (!res.ok) throw new Error('Email ou senha inválidos');
  return await res.json();   // retorna o objeto do usuário
}
