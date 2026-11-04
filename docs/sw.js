/* 전자근로계약서 서비스 워커: 앱 파일을 캐시해 두어 재방문 시 빠르게 열리고, 인터넷이 잠깐 끊겨도 화면은 뜹니다.
   네트워크가 되면 항상 최신 파일을 먼저 받고 캐시를 갱신합니다. */
var CACHE = 'ngk-v2';
var APP_FILES = ['./', './index.html', './manifest.webmanifest', './icon-192.png', './icon-512.png', './icon-maskable-512.png', './apple-touch-icon.png', './lib/pako.min.js', './lib/html2canvas.min.js', './lib/jspdf.umd.min.js'];
self.addEventListener('install', function(e){
  e.waitUntil(caches.open(CACHE).then(function(c){ return c.addAll(APP_FILES); }).then(function(){ return self.skipWaiting(); }));
});
self.addEventListener('activate', function(e){
  e.waitUntil(caches.keys().then(function(keys){ return Promise.all(keys.filter(function(k){ return k !== CACHE; }).map(function(k){ return caches.delete(k); })); }).then(function(){ return self.clients.claim(); }));
});
self.addEventListener('fetch', function(e){
  var req = e.request;
  if (req.method !== 'GET') return;
  var url = new URL(req.url);
  var sameOrigin = url.origin === self.location.origin;
  var isLib = /cdnjs\.cloudflare\.com|fonts\.(googleapis|gstatic)\.com|kakaocdn\.net/.test(url.host);
  if (!sameOrigin && !isLib) return; // Firestore 등 데이터 요청은 캐시하지 않음
  e.respondWith(
    fetch(req).then(function(res){
      if (res && res.ok && (sameOrigin || res.type === 'basic' || res.type === 'cors')) { var copy = res.clone(); caches.open(CACHE).then(function(c){ c.put(req, copy); }); }
      return res;
    }).catch(function(){
      return caches.match(req, {ignoreSearch: sameOrigin}).then(function(hit){ return hit || (sameOrigin && req.mode === 'navigate' ? caches.match('./index.html') : undefined); });
    })
  );
});
