/**
 * Site-wide UI/UX motion: scroll-reveal, the add-to-cart fly animation + cart badge bounce, a
 * real-data "recent activity" toast, and a loading spinner for slow form submits. The hero
 * banner is a static sticky 3-panel layout (see style.css .hero-banner-stack) — no JS needed.
 * Progressive enhancement only — every feature this touches (add to cart, checkout) already
 * works via a plain form submit without JavaScript.
 */
(function () {
    "use strict";

    document.addEventListener("DOMContentLoaded", function () {
        initScrollReveal();
        initQuickAddToCart();
        initFavoriteToggle();
        initQuickView();
        initMegaMenu();
        initSpinnerOnSubmit();
        initRecentActivityToast();
    });

    /** The CSRF token SecurityFilter expects on every POST, published on <body> by header.jsp. */
    function csrfToken() {
        return document.body.getAttribute("data-csrf") || "";
    }

    function contextPath() {
        return document.body.getAttribute("data-context-path") || "";
    }

    function initScrollReveal() {
        var targets = document.querySelectorAll(".reveal-on-scroll");
        if (!targets.length) {
            return;
        }
        if (!("IntersectionObserver" in window)) {
            targets.forEach(function (el) { el.classList.add("is-visible"); });
            return;
        }
        var observer = new IntersectionObserver(function (entries, obs) {
            entries.forEach(function (entry) {
                if (entry.isIntersecting) {
                    entry.target.classList.add("is-visible");
                    obs.unobserve(entry.target);
                }
            });
        }, { threshold: 0.1, rootMargin: "0px 0px -40px 0px" });
        targets.forEach(function (el) { observer.observe(el); });
    }

    // Delegated on document (not querySelectorAll+forEach at load time) because the quick-view
    // modal injects its own copies of these buttons after the page has already loaded.
    function initQuickAddToCart() {
        document.addEventListener("click", function (event) {
            var btn = event.target.closest("[data-quick-add]");
            if (!btn) {
                return;
            }
            event.preventDefault();
            event.stopPropagation();
            if (btn.classList.contains("is-loading")) {
                return;
            }
            var productId = btn.getAttribute("data-product-id");
            var card = btn.closest(".product-card");
            var sourceImg = card ? card.querySelector(".product-card-media img, .product-card-media .product-image-placeholder") : null;
            btn.classList.add("is-loading");

            fetch(contextPath() + "/cart/add", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded",
                    "X-Requested-With": "fetch"
                },
                body: "productId=" + encodeURIComponent(productId) + "&quantity=1"
                    + "&csrfToken=" + encodeURIComponent(csrfToken())
            })
                .then(function (resp) { return resp.ok ? resp.json() : Promise.reject(); })
                .then(function (data) {
                    if (sourceImg) {
                        flyToCart(sourceImg);
                    }
                    updateCartBadge(data.count);
                    showToast((btn.getAttribute("data-product-name") || "Món") + " đã thêm vào giỏ hàng", "success");
                })
                .catch(function () {
                    showToast("Không thêm được vào giỏ hàng, vui lòng thử lại.", "error");
                })
                .finally(function () {
                    btn.classList.remove("is-loading");
                });
        });
    }

    function initFavoriteToggle() {
        document.addEventListener("click", function (event) {
            var btn = event.target.closest("[data-favorite-toggle]");
            if (!btn) {
                return;
            }
            event.preventDefault();
            event.stopPropagation();
            if (btn.classList.contains("is-loading")) {
                return;
            }
            var productId = btn.getAttribute("data-product-id");
            btn.classList.add("is-loading");

            fetch(contextPath() + "/favorites/toggle", {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: "productId=" + encodeURIComponent(productId)
                    + "&csrfToken=" + encodeURIComponent(csrfToken())
            })
                .then(function (resp) { return resp.ok ? resp.json() : Promise.reject(); })
                .then(function (data) {
                    // A product usually appears twice on the catalog page (its own card, and
                    // possibly the quick-view modal open on top of it) — keep every instance in sync.
                    document.querySelectorAll('[data-favorite-toggle][data-product-id="' + productId + '"]').forEach(function (el) {
                        el.textContent = data.favorited ? "♥" : "♡";
                        el.classList.toggle("is-favorited", data.favorited);
                    });
                })
                .catch(function () {
                    showToast("Không thực hiện được, vui lòng thử lại.", "error");
                })
                .finally(function () {
                    btn.classList.remove("is-loading");
                });
        });
    }

    function initQuickView() {
        var modal = document.getElementById("quick-view-modal");
        var content = document.getElementById("quick-view-content");
        if (!modal || !content) {
            return;
        }

        function open(productId) {
            content.innerHTML = "";
            modal.hidden = false;
            fetch(contextPath() + "/products/quick-view?id=" + encodeURIComponent(productId))
                .then(function (resp) { return resp.ok ? resp.text() : Promise.reject(); })
                .then(function (html) { content.innerHTML = html; })
                .catch(function () {
                    content.innerHTML = "<p>Không tải được thông tin món ăn.</p>";
                });
        }

        function close() {
            modal.hidden = true;
        }

        document.addEventListener("click", function (event) {
            var trigger = event.target.closest("[data-quick-view]");
            if (trigger) {
                event.preventDefault();
                event.stopPropagation();
                open(trigger.getAttribute("data-product-id"));
                return;
            }
            if (event.target === modal || event.target.closest("[data-modal-close]")) {
                close();
            }
        });

        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape" && !modal.hidden) {
                close();
            }
        });
    }

    function flyToCart(sourceEl) {
        var cartLink = document.getElementById("nav-cart-link");
        if (!sourceEl || !cartLink) {
            return;
        }
        var from = sourceEl.getBoundingClientRect();
        var to = cartLink.getBoundingClientRect();

        var clone = sourceEl.tagName === "IMG" ? sourceEl.cloneNode(true) : document.createElement("div");
        clone.className = "fly-to-cart-clone";
        clone.style.position = "fixed";
        clone.style.left = from.left + "px";
        clone.style.top = from.top + "px";
        clone.style.width = from.width + "px";
        clone.style.height = from.height + "px";
        clone.style.zIndex = "9999";
        document.body.appendChild(clone);

        var deltaX = (to.left + to.width / 2) - (from.left + from.width / 2);
        var deltaY = (to.top + to.height / 2) - (from.top + from.height / 2);

        requestAnimationFrame(function () {
            clone.style.transform = "translate(" + deltaX + "px, " + deltaY + "px) scale(0.15)";
            clone.style.opacity = "0.3";
        });

        window.setTimeout(function () {
            clone.remove();
            cartLink.classList.add("cart-bump");
            window.setTimeout(function () { cartLink.classList.remove("cart-bump"); }, 300);
        }, 550);
    }

    function updateCartBadge(count) {
        var badge = document.getElementById("cart-badge");
        if (!badge) {
            return;
        }
        badge.textContent = count;
        badge.hidden = !count || count <= 0;
    }

    function initMegaMenu() {
        var trigger = document.querySelector("[data-mega-menu-toggle]");
        var panel = document.getElementById("mega-menu-panel");
        if (!trigger || !panel) {
            return;
        }
        var loaded = false;

        function open() {
            panel.hidden = false;
            if (loaded) {
                return;
            }
            loaded = true;
            fetch(contextPath() + "/products/category-menu")
                .then(function (resp) { return resp.ok ? resp.text() : Promise.reject(); })
                .then(function (html) { panel.innerHTML = html; })
                .catch(function () {
                    loaded = false;
                    panel.innerHTML = "<p>Không tải được danh mục.</p>";
                });
        }

        function close() {
            panel.hidden = true;
        }

        trigger.addEventListener("click", function (event) {
            event.stopPropagation();
            if (panel.hidden) {
                open();
            } else {
                close();
            }
        });

        document.addEventListener("click", function (event) {
            if (!panel.hidden && !panel.contains(event.target) && event.target !== trigger) {
                close();
            }
        });

        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape" && !panel.hidden) {
                close();
            }
        });
    }

    function initSpinnerOnSubmit() {
        var overlay = document.getElementById("page-spinner");
        if (!overlay) {
            return;
        }

        // Bfcache safety net: if the browser restores this exact page (e.g. the user hits
        // "Back" right after submitting) it can restore the DOM as it was the instant we
        // navigated away — spinner still showing, blocking every click on a page that isn't
        // actually loading anything. Always force it hidden whenever this page becomes visible,
        // whether that's a fresh load or a bfcache restore.
        window.addEventListener("pageshow", function () {
            overlay.hidden = true;
        });

        var forms = document.querySelectorAll("form.show-spinner-on-submit");
        if (!forms.length) {
            return;
        }
        forms.forEach(function (form) {
            form.addEventListener("submit", function () {
                overlay.hidden = false;
                // Safety net for a submit that never actually navigates away (e.g. a network
                // error) — never leave the whole page unclickable indefinitely.
                window.setTimeout(function () { overlay.hidden = true; }, 15000);
            });
        });
    }

    var lastSeenActivityKey = null;

    function initRecentActivityToast() {
        var page = document.querySelector("main[data-page='catalog']");
        if (!page) {
            return;
        }

        function poll() {
            fetch(contextPath() + "/products/recent-activity")
                .then(function (resp) { return resp.ok ? resp.json() : Promise.reject(); })
                .then(function (items) {
                    if (!items || !items.length) {
                        return;
                    }
                    var latest = items[0];
                    var key = latest.product + "|" + latest.building + "|" + latest.minutesAgo;
                    if (lastSeenActivityKey === null) {
                        // First poll after page load just establishes the baseline — showing a
                        // toast immediately for an order placed 20 minutes ago wouldn't feel "live".
                        lastSeenActivityKey = key;
                        return;
                    }
                    if (key !== lastSeenActivityKey && latest.minutesAgo <= 2) {
                        lastSeenActivityKey = key;
                        showToast("🛎️ Một bạn ở " + latest.building + " vừa đặt " + latest.product, "info");
                    }
                })
                .catch(function () { /* silent — this is a nice-to-have, not core functionality */ });
        }

        poll();
        window.setInterval(poll, 25000);
    }

    function showToast(message, variant) {
        var container = document.getElementById("toast-container");
        if (!container) {
            return;
        }
        var toast = document.createElement("div");
        toast.className = "toast toast-" + (variant || "info");
        toast.textContent = message;
        container.appendChild(toast);

        requestAnimationFrame(function () { toast.classList.add("is-visible"); });

        window.setTimeout(function () {
            toast.classList.remove("is-visible");
            window.setTimeout(function () { toast.remove(); }, 300);
        }, 4000);
    }
})();
