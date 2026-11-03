// ════════════════════════════════════════════════════════
//  RS Sistema Escolar — Service Worker (PWA)
//  sw.js  |  Cache offline para as páginas e assets
// ════════════════════════════════════════════════════════

const CACHE_NAME = 'rs-escola-v4';

// Caminhos da API: sempre vão à rede, nunca ao cache
const API_PREFIXOS = ['/health', '/login', '/usuarios', '/cursos', '/alunos', '/relatorio'];

// Arquivos que serão armazenados em cache (disponíveis offline)
const ARQUIVOS_PARA_CACHE = [
  '/',
  '/index.html',
  '/login.html',
  '/usuario.html',
  '/aluno.html',
  '/curso.html',
  '/css/style.css',
  '/js/app.js',
  '/manifest.json',
  '/icons/icon-192.png',
  '/icons/icon-512.png',
];

// ── Evento: install ────────────────────────────────────
// Ocorre quando o SW é instalado pela primeira vez
self.addEventListener('install', (event) => {
  console.log('[SW] Instalando…');
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => {
        console.log('[SW] Cacheando arquivos estáticos');
        return cache.addAll(ARQUIVOS_PARA_CACHE);
      })
  );
});

// ── Evento: activate ──────────────────────────────────
// Remove caches antigos quando uma nova versão é instalada
self.addEventListener('activate', (event) => {
  console.log('[SW] Ativando…');
  event.waitUntil(
    caches.keys().then(keys =>
      Promise.all(
        keys
          .filter(key => key !== CACHE_NAME)
          .map(key => {
            console.log('[SW] Removendo cache antigo:', key);
            return caches.delete(key);
          })
      )
    )
  );
});

// ── Evento: fetch ──────────────────────────────────────
// Estratégia: Network First para a API, Cache First para assets
self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);

  // Requisições para a API — sempre tenta a rede
  if (API_PREFIXOS.some(p => url.pathname.startsWith(p))) {
    event.respondWith(
      fetch(event.request)
        .catch(() => new Response(
          JSON.stringify({ erro: 'Sem conexão com a API' }),
          { headers: { 'Content-Type': 'application/json' } }
        ))
    );
    return;
  }

  // Assets estáticos — Cache First (mais rápido)
  event.respondWith(
    caches.match(event.request)
      .then(cached => cached || fetch(event.request))
  );
});
