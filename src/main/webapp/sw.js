// Bump this on every deploy that changes anything under /assets/ or manifest.json — activate()
// deletes any cache key that doesn't match, which is what actually forces old installs to drop
// stale precached files. (v1 -> v2: fixed the fetch handler below, which had no such bump path —
// see the note there.)
const CACHE_NAME = "canteen-static-v2";
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

// Network-first (falling back to cache only when offline) for static assets under /assets/ or
// manifest.json. Every other request (every JSP-rendered page: catalog, cart, orders, admin/
// sales/store screens) is left to the network untouched — this app's HTML is dynamic per-session/
// per-cart/per-stock, so caching it would serve stale prices, stock, or order status instead of
// speeding anything up.
//
// v1 served cache-first with no revalidation at all: once installed, style.css/manifest.json were
// frozen forever, since a matching CACHE_NAME (no version bump) never triggers activate()'s
// cleanup and there was no other path back to the network. Every CSS/icon change after a user's
// first visit was invisible to them until they manually cleared site data — this is what made
// redesigns look like they "didn't take" on a real device even though the server was serving the
// new file correctly. Network-first means a deploy is visible on the very next load while online;
// the cache is only a fallback for when the network request itself fails.
self.addEventListener("fetch", (event) => {
    const url = new URL(event.request.url);
    const isStaticAsset = event.request.method === "GET" &&
        (url.pathname.includes("/assets/") || url.pathname.endsWith("manifest.json"));

    if (!isStaticAsset) {
        return;
    }

    event.respondWith(
        fetch(event.request)
            .then((response) => {
                const responseCopy = response.clone();
                caches.open(CACHE_NAME).then((cache) => cache.put(event.request, responseCopy));
                return response;
            })
            .catch(() => caches.match(event.request))
    );
});
