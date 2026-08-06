const CACHE_NAME = "canteen-static-v1";
const PRECACHE_URLS = [
    "assets/css/style.css",
    "assets/icons/icon-192.png",
    "assets/icons/icon-512.png",
    "manifest.json"
];

self.addEventListener("install", (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME).then((cache) => cache.addAll(PRECACHE_URLS))
    );
    self.skipWaiting();
});

self.addEventListener("activate", (event) => {
    event.waitUntil(
        caches.keys().then((keys) =>
            Promise.all(keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key)))
        )
    );
    self.clients.claim();
});

// Only cache-first for static assets under /assets/ or manifest.json. Every other request
// (every JSP-rendered page: catalog, cart, orders, admin/sales/store screens) is left to the
// network untouched — this app's HTML is dynamic per-session/per-cart/per-stock, so caching it
// would serve stale prices, stock, or order status instead of speeding anything up.
self.addEventListener("fetch", (event) => {
    const url = new URL(event.request.url);
    const isStaticAsset = event.request.method === "GET" &&
        (url.pathname.includes("/assets/") || url.pathname.endsWith("manifest.json"));

    if (!isStaticAsset) {
        return;
    }

    event.respondWith(
        caches.match(event.request).then((cached) => cached || fetch(event.request))
    );
});
