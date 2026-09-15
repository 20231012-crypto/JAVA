/**
 * Admin-area behaviour. Progressive enhancement only — every admin action is a plain form POST
 * that works with this file absent, and the server re-checks everything this file guards.
 */
(function () {
    "use strict";

    document.addEventListener("DOMContentLoaded", function () {
        initConfirmCodeGate();
        initRailToggle();
        initCommandPalette();
        initNewOrderAlert();
        initSelectAll();
    });

    function contextPath() {
        return document.body.getAttribute("data-context-path") || "";
    }

    /**
     * Destructive forms (currently the order refund) ask the operator to retype an identifier
     * before the button works. The button ships ENABLED in the HTML and is disabled here instead:
     * shipping it disabled would make the form unusable without JavaScript, and this is an
     * assistive gate, not the security boundary — OrderRefundServlet compares the same value
     * server-side and refuses the refund if it does not match.
     */
    function initConfirmCodeGate() {
        document.querySelectorAll("form[data-confirm-code]").forEach(function (form) {
            var expected = form.getAttribute("data-confirm-code") || "";
            var input = form.querySelector("[data-confirm-input]");
            var submit = form.querySelector("[data-confirm-submit]");
            if (!input || !submit) {
                return;
            }

            function sync() {
                var matches = input.value.trim().toUpperCase() === expected.trim().toUpperCase();
                submit.disabled = !matches;
                // Only flag the field once something has been typed — an untouched field shown red
                // is noise, not feedback.
                input.setAttribute("aria-invalid", input.value.length > 0 && !matches ? "true" : "false");
            }

            input.addEventListener("input", sync);
            sync();
        });
    }

    /**
     * The rail is expanded by default and collapsible. The old hover-only behaviour did not
     * survive the menu growing past a dozen entries: a 64px icon strip with sixteen
     * indistinguishable glyphs is not navigation. The choice is remembered per browser.
     */
    function initRailToggle() {
        var toggle = document.querySelector("[data-rail-toggle]");
        if (!toggle) {
            return;
        }

        function apply(collapsed) {
            document.body.classList.toggle("rail-collapsed", collapsed);
            toggle.setAttribute("aria-expanded", collapsed ? "false" : "true");
        }

        var stored = null;
        try {
            stored = window.localStorage.getItem("adminRailCollapsed");
        } catch (e) {
            // Private mode, or site data blocked. The rail just starts expanded every time.
        }
        apply(stored === "1");

        toggle.addEventListener("click", function () {
            var collapsed = !document.body.classList.contains("rail-collapsed");
            apply(collapsed);
            try {
                window.localStorage.setItem("adminRailCollapsed", collapsed ? "1" : "0");
            } catch (e) {
                // Not worth telling the user about; the toggle still worked for this page.
            }
        });
    }

    /**
     * Ctrl+K / Cmd+K command palette over dishes, orders and students. The server filters each
     * group by permission, so what comes back is already what this account may open.
     */
    function initCommandPalette() {
        var palette = document.getElementById("command-palette");
        if (!palette) {
            return;
        }
        var input = palette.querySelector("[data-palette-input]");
        var results = palette.querySelector("[data-palette-results]");
        var lastFocused = null;
        var debounce = null;

        function open() {
            lastFocused = document.activeElement;
            palette.hidden = false;
            input.value = "";
            results.innerHTML = "";
            input.focus();
        }

        function close() {
            palette.hidden = true;
            // Returning focus where it was is what makes this usable by keyboard: otherwise focus
            // lands back at the top of the document and the operator loses their place.
            if (lastFocused && typeof lastFocused.focus === "function") {
                lastFocused.focus();
            }
        }

        document.addEventListener("keydown", function (event) {
            if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
                event.preventDefault();
                if (palette.hidden) {
                    open();
                } else {
                    close();
                }
                return;
            }
            if (event.key === "Escape" && !palette.hidden) {
                close();
                return;
            }
            // Focus trap. A dialog that lets Tab wander off into the page behind it is a dialog a
            // keyboard user cannot get out of coherently.
            if (event.key === "Tab" && !palette.hidden) {
                var focusable = palette.querySelectorAll("input, a[href], button");
                if (!focusable.length) {
                    return;
                }
                var first = focusable[0];
                var last = focusable[focusable.length - 1];
                if (event.shiftKey && document.activeElement === first) {
                    event.preventDefault();
                    last.focus();
                } else if (!event.shiftKey && document.activeElement === last) {
                    event.preventDefault();
                    first.focus();
                }
            }
        });

        // The header button opens the same palette, so the feature is discoverable without
        // knowing the shortcut.
        document.querySelectorAll("[data-palette-open]").forEach(function (btn) {
            btn.addEventListener("click", open);
        });

        palette.addEventListener("click", function (event) {
            if (event.target === palette || event.target.closest("[data-palette-close]")) {
                close();
            }
        });

        input.addEventListener("input", function () {
            // One request per pause in typing, not per keystroke.
            window.clearTimeout(debounce);
            debounce = window.setTimeout(function () {
                search(input.value.trim());
            }, 180);
        });

        function search(query) {
            if (query.length < 2) {
                results.innerHTML = "";
                return;
            }
            fetch(contextPath() + "/admin/search?q=" + encodeURIComponent(query))
                .then(function (resp) { return resp.ok ? resp.json() : Promise.reject(); })
                .then(function (data) { render(data, query); })
                .catch(function () {
                    results.innerHTML = "<p class=\"palette-empty\">Không tìm được, thử lại.</p>";
                });
        }

        function render(data, query) {
            var groups = [
                {title: "Món ăn", items: data.products},
                {title: "Đơn hàng", items: data.orders},
                {title: "Khách hàng", items: data.customers}
            ];
            var html = "";
            groups.forEach(function (group) {
                if (!group.items || !group.items.length) {
                    return;
                }
                html += "<div class=\"palette-group\"><h3>" + escapeHtml(group.title) + "</h3><ul>";
                group.items.forEach(function (item) {
                    html += "<li><a href=\"" + contextPath() + escapeHtml(item.url) + "\">"
                        + "<span class=\"palette-label\">" + escapeHtml(item.label) + "</span>"
                        + "<span class=\"palette-hint\">" + escapeHtml(item.hint) + "</span>"
                        + "</a></li>";
                });
                html += "</ul></div>";
            });
            results.innerHTML = html || "<p class=\"palette-empty\">Không có kết quả cho “"
                + escapeHtml(query) + "”.</p>";
        }
    }

    /**
     * Polls for new PENDING orders and top-up requests, and says so visibly plus audibly.
     *
     * Browsers refuse to play sound until the user has interacted with the page, so the audio is
     * deliberately the secondary channel: the badge and the toast always work, and the AudioContext
     * is created lazily on the first real interaction. If it is still suspended when an alert
     * fires, the toast says so rather than the alert silently doing nothing.
     */
    function initNewOrderAlert() {
        var badge = document.getElementById("admin-alert-badge");
        if (!badge) {
            return;
        }

        var seenOrderAt = null;
        var seenTopupAt = null;
        var audioContext = null;
        var soundEnabled = true;
        var pollMs = 20000;
        var timer = null;

        // Created on a real gesture so the browser lets it start; before that there is nothing to
        // unlock and creating one would only produce a suspended context.
        function unlockAudio() {
            if (audioContext) {
                return;
            }
            var Ctor = window.AudioContext || window.webkitAudioContext;
            if (!Ctor) {
                return;
            }
            try {
                audioContext = new Ctor();
            } catch (e) {
                audioContext = null;
            }
        }
        document.addEventListener("click", unlockAudio, {once: true});
        document.addEventListener("keydown", unlockAudio, {once: true});

        /** A two-note chime from an oscillator — no asset to ship, no <audio> element to be blocked. */
        function chime() {
            if (!soundEnabled || !audioContext || audioContext.state !== "running") {
                return false;
            }
            [880, 1320].forEach(function (freq, i) {
                var osc = audioContext.createOscillator();
                var gain = audioContext.createGain();
                osc.type = "sine";
                osc.frequency.value = freq;
                gain.gain.setValueAtTime(0.0001, audioContext.currentTime);
                gain.gain.exponentialRampToValueAtTime(0.18, audioContext.currentTime + 0.01 + i * 0.12);
                gain.gain.exponentialRampToValueAtTime(0.0001, audioContext.currentTime + 0.28 + i * 0.12);
                osc.connect(gain);
                gain.connect(audioContext.destination);
                osc.start(audioContext.currentTime + i * 0.12);
                osc.stop(audioContext.currentTime + 0.4 + i * 0.12);
            });
            return true;
        }

        function showBadge(pendingOrders, pendingTopups) {
            var total = pendingOrders + pendingTopups;
            badge.textContent = total;
            badge.hidden = total === 0;
            badge.title = pendingOrders + " đơn chờ xác nhận, " + pendingTopups + " yêu cầu nạp ví";
        }

        function announce(message) {
            var played = chime();
            if (window.showAdminToast) {
                window.showAdminToast(played ? message : message + " (bấm vào trang để bật âm báo)");
            }
        }

        function poll() {
            fetch(contextPath() + "/admin/events")
                .then(function (resp) { return resp.ok ? resp.json() : Promise.reject(); })
                .then(function (data) {
                    soundEnabled = data.soundEnabled;
                    if (data.pollSeconds > 0 && data.pollSeconds * 1000 !== pollMs) {
                        pollMs = data.pollSeconds * 1000;
                        window.clearInterval(timer);
                        timer = window.setInterval(poll, pollMs);
                    }
                    showBadge(data.pendingOrders, data.pendingTopups);

                    // The first response establishes the baseline. Announcing what was already
                    // waiting when the page opened would fire an alert on every navigation.
                    if (seenOrderAt === null) {
                        seenOrderAt = data.newestOrderAt;
                        seenTopupAt = data.newestTopupAt;
                        return;
                    }
                    if (data.newestOrderAt > seenOrderAt) {
                        seenOrderAt = data.newestOrderAt;
                        announce("Có đơn hàng mới cần xác nhận");
                    }
                    if (data.newestTopupAt > seenTopupAt) {
                        seenTopupAt = data.newestTopupAt;
                        announce("Có yêu cầu nạp ví mới");
                    }
                })
                .catch(function () { /* a dropped poll is not worth reporting; the next one retries */ });
        }

        poll();
        timer = window.setInterval(poll, pollMs);
    }

    /**
     * Select-all for the bulk tables. Purely a convenience: the checkboxes and the submit buttons
     * are plain form controls that work without this, so all it does is tick the boxes and keep
     * the header box's indeterminate state honest.
     */
    function initSelectAll() {
        document.querySelectorAll("[data-select-all]").forEach(function (master) {
            var group = master.getAttribute("data-select-all");
            var items = document.querySelectorAll('[data-select-item="' + group + '"]');
            if (!items.length) {
                return;
            }

            master.addEventListener("change", function () {
                items.forEach(function (item) { item.checked = master.checked; });
                master.indeterminate = false;
            });

            items.forEach(function (item) {
                item.addEventListener("change", function () {
                    var checked = 0;
                    items.forEach(function (i) { if (i.checked) { checked++; } });
                    master.checked = checked === items.length;
                    // Neither all nor none: the header box says "some", rather than lying either way.
                    master.indeterminate = checked > 0 && checked < items.length;
                });
            });
        });
    }

    /** Shared with the palette's result rendering — both build HTML from server strings. */
    function escapeHtml(value) {
        return String(value == null ? "" : value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }

    /** Reuses the site toast container from interactions.js when the admin page has one. */
    window.showAdminToast = function (message) {
        var container = document.getElementById("toast-container");
        if (!container) {
            return;
        }
        var toast = document.createElement("div");
        toast.className = "toast toast-info";
        toast.setAttribute("role", "status");
        toast.textContent = message;
        container.appendChild(toast);
        requestAnimationFrame(function () { toast.classList.add("is-visible"); });
        window.setTimeout(function () {
            toast.classList.remove("is-visible");
            window.setTimeout(function () { toast.remove(); }, 300);
        }, 6000);
    };
})();
