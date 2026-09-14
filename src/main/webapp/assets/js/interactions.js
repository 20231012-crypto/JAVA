/**
 * Site-wide UI/UX motion: scroll-reveal, the hero banner carousel, the add-to-cart fly
 * animation + cart badge bounce, a real-data "recent activity" toast, and a loading spinner
 * for slow form submits. Progressive enhancement only — every feature this touches (add to
 * cart, checkout) already works via a plain form submit without JavaScript.
 */
(function () {
    "use strict";

    document.addEventListener("DOMContentLoaded", function () {
        initScrollReveal();
        initHeroCarousel();
        initQuickAddToCart();
        initSpinnerOnSubmit();
        initRecentActivityToast();
    });

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

    function initHeroCarousel() {
        var carousel = document.querySelector(".hero-banner-carousel");
        if (!carousel) {
            return;
        }
        var slides = carousel.querySelectorAll(".hero-slide");
        if (slides.length < 2) {
            return;
        }
        var dotsWrap = carousel.querySelector(".hero-carousel-dots");
        var dots = [];
        slides.forEach(function (_, i) {
            var dot = document.createElement("button");
            dot.type = "button";
            dot.className = "hero-carousel-dot" + (i === 0 ? " is-active" : "");
            dot.setAttribute("aria-label", "Xem banner " + (i + 1));
            dot.addEventListener("click", function () { goTo(i); });
            dotsWrap.appendChild(dot);
            dots.push(dot);
        });

        var current = 0;
        var intervalMs = parseInt(carousel.getAttribute("data-autoplay"), 10) || 5000;
        var timer = null;

        function goTo(index) {
            slides[current].classList.remove("is-active");
            dots[current].classList.remove("is-active");
            current = (index + slides.length) % slides.length;
            slides[current].classList.add("is-active");
            dots[current].classList.add("is-active");
        }

        function start() {
            timer = window.setInterval(function () { goTo(current + 1); }, intervalMs);
        }

        carousel.addEventListener("mouseenter", function () { window.clearInterval(timer); });
        carousel.addEventListener("mouseleave", start);
        start();
    }

    function initQuickAddToCart() {
        var buttons = document.querySelectorAll("[data-quick-add]");
        if (!buttons.length) {
            return;
        }
        var contextPath = document.body.getAttribute("data-context-path") || "";
        buttons.forEach(function (btn) {
            btn.addEventListener("click", function (event) {
                event.preventDefault();
                event.stopPropagation();
                if (btn.classList.contains("is-loading")) {
                    return;
                }
                var productId = btn.getAttribute("data-product-id");
                var sourceImg = btn.closest(".product-card-media").querySelector("img, .product-image-placeholder");
                btn.classList.add("is-loading");

                fetch(contextPath + "/cart/add", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/x-www-form-urlencoded",
                        "X-Requested-With": "fetch"
                    },
                    body: "productId=" + encodeURIComponent(productId) + "&quantity=1"
                })
                    .then(function (resp) { return resp.ok ? resp.json() : Promise.reject(); })
                    .then(function (data) {
                        flyToCart(sourceImg);
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

    function initSpinnerOnSubmit() {
        var forms = document.querySelectorAll("form.show-spinner-on-submit");
        var overlay = document.getElementById("page-spinner");
        if (!forms.length || !overlay) {
            return;
        }
        forms.forEach(function (form) {
            form.addEventListener("submit", function () {
                overlay.hidden = false;
            });
        });
    }

    var lastSeenActivityKey = null;

    function initRecentActivityToast() {
        var page = document.querySelector("main[data-page='catalog']");
        if (!page) {
            return;
        }
        var contextPath = document.body.getAttribute("data-context-path") || "";

        function poll() {
            fetch(contextPath + "/products/recent-activity")
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
