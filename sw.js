// Busca a versão nova na internet; sem conexão, usa a última guardada.
const ARQUIVOS = ['./', 'manifest.json', 'icone-192.png', 'icone-512.png'];
self.addEventListener('install', e => e.waitUntil(caches.open('shizu').then(c => c.addAll(ARQUIVOS))));
self.addEventListener('fetch', e => {
  if (e.request.method !== 'GET' || new URL(e.request.url).origin !== location.origin) return;
  e.respondWith(fetch(e.request).then(r => {
    if (r.ok) { const copia = r.clone(); caches.open('shizu').then(c => c.put(e.request, copia)); }
    return r;
  }).catch(() => caches.match(e.request, {ignoreSearch: true})));
});
