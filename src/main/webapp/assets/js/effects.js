/**
 * Seasonal effects on the student-facing pages — snow, fireworks, falling leaves.
 *
 * Driven entirely by data-effects / data-effects-intensity on <body>, which header.jsp fills in
 * from app_settings. Nothing here runs unless an admin has turned an effect on.
 *
 * Design constraints this file respects, because a decoration that costs a student their battery
 * or their scroll performance is not worth having:
 *   - one canvas, one requestAnimationFrame loop, no DOM nodes per particle
 *   - a hard particle cap (PARTICLE_BUDGET) regardless of screen size
 *   - the loop stops entirely when the tab is hidden, rather than animating into a background tab
 *   - nothing at all when the reader has asked their system for reduced motion
 *   - pointer-events: none, so it can never intercept a tap on a dish
 */
(function () {
    "use strict";

    /** Upper bound on particles at intensity 3. Chosen to stay smooth on a mid-range phone. */
    var PARTICLE_BUDGET = 120;

    document.addEventListener("DOMContentLoaded", function () {
        var mode = document.body.getAttribute("data-effects");
        if (!mode || mode === "NONE") {
            return;
        }
        // Not a preference to be talked out of: someone who has asked for reduced motion gets no
        // animation, and the effect is simply absent rather than degraded.
        if (window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
            return;
        }
        if (!window.requestAnimationFrame) {
            return;
        }

        var intensity = parseInt(document.body.getAttribute("data-effects-intensity"), 10);
        if (isNaN(intensity) || intensity < 1 || intensity > 3) {
            intensity = 2;
        }
        start(mode, intensity);
    });

    function start(mode, intensity) {
        var canvas = document.createElement("canvas");
        canvas.className = "effects-canvas";
        canvas.setAttribute("aria-hidden", "true");
        document.body.appendChild(canvas);

        var ctx = canvas.getContext("2d");
        if (!ctx) {
            canvas.remove();
            return;
        }

        var particles = [];
        var count = Math.round(PARTICLE_BUDGET * (intensity / 3));
        var running = false;
        var frame = null;

        function resize() {
            // Fixed to the viewport, not the document: a full-page canvas on a long menu would be
            // enormous, and the particles only need to cover what is on screen.
            canvas.width = window.innerWidth;
            canvas.height = window.innerHeight;
        }

        function seed() {
            particles = [];
            for (var i = 0; i < count; i++) {
                particles.push(spawn(mode, canvas, true));
            }
        }

        function loop() {
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            for (var i = 0; i < particles.length; i++) {
                step(mode, particles[i], canvas);
                draw(mode, ctx, particles[i]);
            }
            frame = window.requestAnimationFrame(loop);
        }

        function play() {
            if (running) {
                return;
            }
            running = true;
            frame = window.requestAnimationFrame(loop);
        }

        function pause() {
            running = false;
            if (frame !== null) {
                window.cancelAnimationFrame(frame);
                frame = null;
            }
        }

        resize();
        seed();
        play();

        window.addEventListener("resize", function () {
            resize();
            seed();
        });

        // A background tab still gets rAF callbacks in some browsers, and animating one nobody is
        // looking at is pure battery drain.
        document.addEventListener("visibilitychange", function () {
            if (document.hidden) {
                pause();
            } else {
                play();
            }
        });
    }

    /** @param initial true when seeding the first screenful, so particles start scattered rather than all at the top. */
    function spawn(mode, canvas, initial) {
        var p = {
            x: Math.random() * canvas.width,
            y: initial ? Math.random() * canvas.height : -10,
            size: 0,
            speed: 0,
            drift: 0,
            life: 1,
            hue: 0
        };
        if (mode === "SNOW") {
            p.size = 1.5 + Math.random() * 2.5;
            p.speed = 0.4 + Math.random() * 1.1;
            p.drift = (Math.random() - 0.5) * 0.6;
        } else if (mode === "LEAVES") {
            p.size = 5 + Math.random() * 5;
            p.speed = 0.6 + Math.random() * 1.2;
            p.drift = (Math.random() - 0.5) * 1.4;
            p.spin = Math.random() * Math.PI;
            p.spinSpeed = (Math.random() - 0.5) * 0.06;
        } else {
            // Fireworks: a particle is a spark thrown from a burst point, so it carries a velocity
            // rather than falling, and fades over its life.
            var angle = Math.random() * Math.PI * 2;
            var velocity = 0.6 + Math.random() * 2.2;
            p.x = canvas.width * (0.2 + Math.random() * 0.6);
            p.y = canvas.height * (0.15 + Math.random() * 0.4);
            p.vx = Math.cos(angle) * velocity;
            p.vy = Math.sin(angle) * velocity;
            p.size = 1.2 + Math.random() * 1.8;
            p.life = Math.random();
            p.hue = Math.floor(Math.random() * 60) + (Math.random() < 0.5 ? 0 : 180);
        }
        return p;
    }

    function step(mode, p, canvas) {
        if (mode === "FIREWORKS") {
            p.x += p.vx;
            p.y += p.vy;
            p.vy += 0.012; // a little gravity, so sparks arc instead of flying straight
            p.life -= 0.006;
            if (p.life <= 0) {
                Object.assign(p, spawn(mode, canvas, false));
            }
            return;
        }

        p.y += p.speed;
        p.x += p.drift;
        if (mode === "LEAVES") {
            p.spin += p.spinSpeed;
        }
        if (p.y > canvas.height + 12) {
            p.y = -12;
            p.x = Math.random() * canvas.width;
        }
        // Wrap sideways rather than respawn: a particle leaving the right edge reappearing on the
        // left keeps the density even without another allocation.
        if (p.x < -12) {
            p.x = canvas.width + 12;
        } else if (p.x > canvas.width + 12) {
            p.x = -12;
        }
    }

    function draw(mode, ctx, p) {
        if (mode === "SNOW") {
            ctx.fillStyle = "rgba(255, 255, 255, 0.85)";
            ctx.beginPath();
            ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
            ctx.fill();
            return;
        }
        if (mode === "LEAVES") {
            ctx.save();
            ctx.translate(p.x, p.y);
            ctx.rotate(p.spin);
            ctx.fillStyle = "rgba(217, 119, 6, 0.75)";
            ctx.beginPath();
            // An ellipse reads as a leaf at this size without needing a path anyone has to maintain.
            ctx.ellipse(0, 0, p.size, p.size * 0.5, 0, 0, Math.PI * 2);
            ctx.fill();
            ctx.restore();
            return;
        }
        ctx.fillStyle = "hsla(" + p.hue + ", 90%, 60%, " + Math.max(0, p.life) + ")";
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
        ctx.fill();
    }
})();
