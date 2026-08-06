(function () {
    // Pages live at varying depths (/products, /admin/products/form, ...), so the service
    // worker must be registered with an absolute, context-path-correct URL — a plain relative
    // "sw.js" would resolve against the current page's URL, not this script's location, and
    // silently 404 on any page below the app root. Derive the app root from this script's own
    // (known) location instead of hardcoding a context path.
    var scriptUrl = document.currentScript && document.currentScript.src;
    var appRoot = scriptUrl ? scriptUrl.replace(/assets\/js\/pwa\.js(\?.*)?$/, "") : "/";

    if ("serviceWorker" in navigator) {
        window.addEventListener("load", function () {
            navigator.serviceWorker.register(appRoot + "sw.js", { scope: appRoot }).catch(function () {
                // Installability is a progressive enhancement — a failed registration
                // (e.g. unsupported browser) should never block the page itself.
            });
        });
    }
})();
