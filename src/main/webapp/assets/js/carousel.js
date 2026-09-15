(function () {
    "use strict";

    // Hero carousel on the catalog page. Progressive enhancement: the first slide is already
    // marked .is-active server-side, so with JS disabled or broken the page still shows a
    // complete hero — this only adds the ability to move between slides.

    var AUTO_ADVANCE_MS = 6000;

    function initCarousel(root) {
        var slides = root.querySelectorAll(".hero-carousel-slide");
        if (slides.length < 2) {
            return; // Nothing to rotate; arrows/dots aren't rendered in this case either.
        }

        var dots = root.querySelectorAll("[data-hero-dot]");
        var index = 0;
        var timer = null;

        // Auto-advance is motion the user didn't ask for, so it stays off when they've said they
        // don't want animation. The arrows and dots keep working either way.
        var prefersReducedMotion = window.matchMedia
            && window.matchMedia("(prefers-reduced-motion: reduce)").matches;

        function show(next) {
            index = (next + slides.length) % slides.length;
            for (var i = 0; i < slides.length; i++) {
                slides[i].classList.toggle("is-active", i === index);
            }
            for (var d = 0; d < dots.length; d++) {
                var isCurrent = d === index;
                dots[d].classList.toggle("is-active", isCurrent);
                if (isCurrent) {
                    dots[d].setAttribute("aria-current", "true");
                } else {
                    dots[d].removeAttribute("aria-current");
                }
            }
        }

        function start() {
            if (prefersReducedMotion || timer) {
                return;
            }
            timer = window.setInterval(function () {
                show(index + 1);
            }, AUTO_ADVANCE_MS);
        }

        function stop() {
            if (timer) {
                window.clearInterval(timer);
                timer = null;
            }
        }

        // Restart the clock after a manual move so a slide the user just chose gets its full turn
        // on screen instead of being swapped out a moment later.
        function goManually(next) {
            stop();
            show(next);
            start();
        }

        var prev = root.querySelector("[data-hero-prev]");
        var next = root.querySelector("[data-hero-next]");
        if (prev) {
            prev.addEventListener("click", function () { goManually(index - 1); });
        }
        if (next) {
            next.addEventListener("click", function () { goManually(index + 1); });
        }

        for (var d = 0; d < dots.length; d++) {
            (function (dot) {
                dot.addEventListener("click", function () {
                    goManually(parseInt(dot.getAttribute("data-hero-dot"), 10) || 0);
                });
            })(dots[d]);
        }

        // Pause while the user is reading or tabbing through it.
        root.addEventListener("mouseenter", stop);
        root.addEventListener("mouseleave", start);
        root.addEventListener("focusin", stop);
        root.addEventListener("focusout", start);

        root.addEventListener("keydown", function (event) {
            if (event.key === "ArrowLeft") {
                event.preventDefault();
                goManually(index - 1);
            } else if (event.key === "ArrowRight") {
                event.preventDefault();
                goManually(index + 1);
            }
        });

        start();
    }

    function init() {
        var carousels = document.querySelectorAll("[data-hero-carousel]");
        for (var i = 0; i < carousels.length; i++) {
            initCarousel(carousels[i]);
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
