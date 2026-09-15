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

        function videoIn(slide) {
            return slide ? slide.querySelector("video") : null;
        }

        function show(next) {
            var leaving = videoIn(slides[index]);
            if (leaving) {
                leaving.pause();
                leaving.currentTime = 0; // Next time this slide comes round it starts from the top.
            }

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

            var arriving = videoIn(slides[index]);
            if (arriving && !prefersReducedMotion) {
                // play() rejects when the browser blocks autoplay; the slide just stays on its
                // first frame in that case, which is an acceptable outcome for a banner.
                var played = arriving.play();
                if (played && typeof played.catch === "function") {
                    played.catch(function () { /* autoplay blocked — leave the poster frame up */ });
                }
            }
        }

        // A video slide holds the screen until the clip finishes instead of being cut off by the
        // fixed interval, so a 12-second promo is not swapped out after 6.
        function start() {
            if (prefersReducedMotion || timer) {
                return;
            }
            var current = videoIn(slides[index]);
            if (current && !current.ended) {
                var resumed = current.play(); // Picks up where a hover/focus pause left off.
                if (resumed && typeof resumed.catch === "function") {
                    resumed.catch(function () { /* autoplay blocked — leave the frame up */ });
                }
                return; // The "ended" listener advances this one; no interval needed.
            }
            timer = window.setInterval(function () {
                if (videoIn(slides[index])) {
                    stop(); // Handed over to the video's own "ended" event.
                    return;
                }
                show(index + 1);
            }, AUTO_ADVANCE_MS);
        }

        // Stopping means "hold this slide where it is", so a playing clip pauses too rather than
        // running on (and finishing, and advancing) while the user is reading it.
        function stop() {
            if (timer) {
                window.clearInterval(timer);
                timer = null;
            }
            var current = videoIn(slides[index]);
            if (current) {
                current.pause();
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

        // When a clip finishes, move on — this is what keeps the rotation going across video
        // slides, since the interval timer steps aside for them.
        var videos = root.querySelectorAll("video");
        for (var v = 0; v < videos.length; v++) {
            videos[v].addEventListener("ended", function () {
                if (!prefersReducedMotion) {
                    show(index + 1);
                    start();
                }
            });
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
