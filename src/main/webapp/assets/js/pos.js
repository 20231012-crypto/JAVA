/**
 * The till's behaviour: keyboard shortcuts, instant search, and updating the order without
 * reloading the page.
 *
 * Every form on this screen is a real form with a real action, and the server renders the whole
 * screen on a normal POST. This file only intercepts them. That ordering matters more here than
 * anywhere else in the app: a till that stops working because a script failed to load is a closed
 * canteen, so the fallback has to be the thing that is actually built and the enhancement has to be
 * the thing layered on top.
 */
(function () {
    "use strict";

    var SHELL_ID = "pos-shell";

    document.addEventListener("DOMContentLoaded", function () {
        bind();
        focusSearch();
    });

    function ctx() {
        return document.body.getAttribute("data-context-path") || "";
    }

    /**
     * Re-binds everything after the shell is replaced. Called on load and after each swap, because
     * the new markup's elements are different objects from the ones listeners were attached to.
     */
    function bind() {
        interceptForms();
        initProductSearch();
        initCustomerSearch();
        initQuantityInputs();
        initNoteAndDiscount();
        initCustomModal();
        initPrint();
    }

    // ---- posting without a reload -------------------------------------------------------------

    /**
     * Submits a form in the background and swaps in the shell the server renders back.
     *
     * The server returns the entire screen rather than a JSON delta, so there is exactly one place
     * that knows how an order looks — the JSP. A delta format would be a second renderer to keep in
     * step with the first.
     */
    function submitAsync(form, extra) {
        var body = new URLSearchParams(new FormData(form));
        if (extra) {
            Object.keys(extra).forEach(function (key) { body.set(key, extra[key]); });
        }

        return fetch(form.action, {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded",
                "X-Requested-With": "fetch"
            },
            body: body.toString()
        })
            .then(function (resp) { return resp.ok ? resp.text() : Promise.reject(); })
            .then(function (html) {
                var parsed = new DOMParser().parseFromString(html, "text/html");
                var fresh = parsed.getElementById(SHELL_ID);
                var current = document.getElementById(SHELL_ID);
                if (!fresh || !current) {
                    // Something unexpected came back — fall back to a real navigation rather than
                    // leaving the cashier looking at a stale order.
                    window.location.reload();
                    return;
                }
                current.replaceWith(fresh);
                bind();
                focusSearch();
            })
            .catch(function () {
                // The till must never silently fail to record something. A full submit makes the
                // outcome visible, whatever it is.
                form.submit();
            });
    }

    function interceptForms() {
        document.querySelectorAll("#" + SHELL_ID + " form").forEach(function (form) {
            form.addEventListener("submit", function (event) {
                event.preventDefault();
                submitAsync(form);
            });
        });
    }

    // ---- product search (F3) ------------------------------------------------------------------

    function initProductSearch() {
        var input = document.querySelector("[data-product-search]");
        var results = document.querySelector("[data-product-results]");
        if (!input || !results) {
            return;
        }
        var timer = null;

        input.addEventListener("input", function () {
            window.clearTimeout(timer);
            // One request per pause in typing. A cashier types fast and a request per keystroke
            // would have answers arriving out of order.
            timer = window.setTimeout(function () { search(input.value.trim()); }, 140);
        });

        input.addEventListener("keydown", function (event) {
            if (event.key === "Escape") {
                hide(results);
                return;
            }
            if (event.key === "ArrowDown") {
                event.preventDefault();
                var first = results.querySelector("button:not(:disabled)");
                if (first) {
                    first.focus();
                }
            }
            if (event.key === "Enter") {
                event.preventDefault();
                var top = results.querySelector("button:not(:disabled)");
                if (top) {
                    top.click();
                }
            }
        });

        document.addEventListener("click", function (event) {
            if (!results.contains(event.target) && event.target !== input) {
                hide(results);
            }
        });

        function search(query) {
            if (!query) {
                hide(results);
                return;
            }
            fetch(ctx() + "/pos/search?q=" + encodeURIComponent(query))
                .then(function (resp) { return resp.ok ? resp.json() : Promise.reject(); })
                .then(render)
                .catch(function () { hide(results); });
        }

        function render(items) {
            if (!items.length) {
                results.innerHTML = "<p class=\"pos-suggest-empty\">Không tìm thấy món nào.</p>";
                show(results);
                return;
            }
            results.innerHTML = "";
            items.forEach(function (item) {
                var btn = document.createElement("button");
                btn.type = "button";
                btn.disabled = !item.sellable;
                btn.innerHTML = "<span>" + escapeHtml(item.name) + "</span>"
                    + "<span class=\"pos-suggest-hint\">" + formatMoney(item.price) + "₫"
                    + (item.sellable ? " · còn " + item.stock : " · hết hàng") + "</span>";
                btn.addEventListener("click", function () {
                    addProduct(item.id);
                    input.value = "";
                    hide(results);
                });
                results.appendChild(btn);
            });
            show(results);
        }
    }

    function addProduct(productId) {
        var form = document.getElementById("pos-add-form");
        var field = document.getElementById("pos-add-product");
        if (!form || !field) {
            return;
        }
        field.value = productId;
        submitAsync(form);
    }

    // ---- customer search ----------------------------------------------------------------------

    function initCustomerSearch() {
        var input = document.querySelector("[data-customer-search]");
        var results = document.querySelector("[data-customer-results]");
        if (!input || !results) {
            return;
        }
        var timer = null;

        input.addEventListener("input", function () {
            window.clearTimeout(timer);
            timer = window.setTimeout(function () {
                var query = input.value.trim();
                if (query.length < 2) {
                    hide(results);
                    return;
                }
                fetch(ctx() + "/pos/customers?q=" + encodeURIComponent(query))
                    .then(function (resp) { return resp.ok ? resp.json() : Promise.reject(); })
                    .then(function (items) {
                        if (!items.length) {
                            results.innerHTML = "<p class=\"pos-suggest-empty\">Không tìm thấy khách hàng.</p>";
                            show(results);
                            return;
                        }
                        results.innerHTML = "";
                        items.forEach(function (item) {
                            var btn = document.createElement("button");
                            btn.type = "button";
                            btn.innerHTML = "<span>" + escapeHtml(item.name) + "</span>"
                                + "<span class=\"pos-suggest-hint\">" + escapeHtml(item.hint) + "</span>";
                            btn.addEventListener("click", function () {
                                var form = document.getElementById("pos-customer-form");
                                document.getElementById("pos-customer-id").value = item.id;
                                submitAsync(form);
                            });
                            results.appendChild(btn);
                        });
                        show(results);
                    })
                    .catch(function () { hide(results); });
            }, 200);
        });
    }

    // ---- inline edits -------------------------------------------------------------------------

    /** Quantity commits on blur or Enter, not on every keystroke — 12 must not pass through 1. */
    function initQuantityInputs() {
        document.querySelectorAll("[data-qty-input]").forEach(function (input) {
            var original = input.value;
            input.addEventListener("keydown", function (event) {
                if (event.key === "Enter") {
                    event.preventDefault();
                    input.blur();
                }
            });
            input.addEventListener("blur", function () {
                if (input.value !== original) {
                    submitAsync(input.form);
                }
            });
        });
    }

    function initNoteAndDiscount() {
        document.querySelectorAll("[data-note-input], [data-discount-input]").forEach(function (input) {
            var original = input.value;
            input.addEventListener("keydown", function (event) {
                if (event.key === "Enter") {
                    event.preventDefault();
                    input.blur();
                }
            });
            input.addEventListener("blur", function () {
                if (input.value !== original) {
                    submitAsync(input.form);
                }
            });
        });
    }

    // ---- custom product dialog (F2) -----------------------------------------------------------

    function initCustomModal() {
        var modal = document.getElementById("pos-custom-modal");
        if (!modal) {
            return;
        }
        var open = document.querySelector("[data-open-custom]");
        var close = modal.querySelector("[data-close-custom]");

        if (open) {
            open.addEventListener("click", openModal);
        }
        if (close) {
            close.addEventListener("click", closeModal);
        }
        modal.addEventListener("click", function (event) {
            if (event.target === modal) {
                closeModal();
            }
        });
        modal.querySelector("form").addEventListener("submit", function (event) {
            event.preventDefault();
            submitAsync(event.target);
            closeModal();
        });
    }

    function openModal() {
        var modal = document.getElementById("pos-custom-modal");
        if (!modal) {
            return;
        }
        modal.hidden = false;
        var first = modal.querySelector("input[type=text]");
        if (first) {
            first.focus();
        }
    }

    function closeModal() {
        var modal = document.getElementById("pos-custom-modal");
        if (modal) {
            modal.hidden = true;
            modal.querySelector("form").reset();
        }
    }

    // ---- printing -----------------------------------------------------------------------------

    function initPrint() {
        var draft = document.querySelector("[data-print-draft]");
        if (draft) {
            draft.addEventListener("click", function () { window.print(); });
        }
        // After a completed sale the server re-renders with the order code shown; printing then is
        // what "In hóa đơn tự động" means.
        var autoprint = document.querySelector("[data-autoprint]");
        var receipt = document.querySelector(".alert-success.pos-alert");
        if (receipt && autoprint && autoprint.checked) {
            window.setTimeout(function () { window.print(); }, 150);
        }
    }

    // ---- shortcuts ----------------------------------------------------------------------------

    document.addEventListener("keydown", function (event) {
        // Only the function keys, and only when no modifier is held — a cashier's Ctrl+F should
        // still be the browser's.
        if (event.ctrlKey || event.altKey || event.metaKey) {
            return;
        }
        switch (event.key) {
            case "F3":
                event.preventDefault();
                focusSearch();
                break;
            case "F2":
                event.preventDefault();
                openModal();
                break;
            case "F6":
                event.preventDefault();
                focus("[data-discount-input]");
                break;
            case "F9": {
                event.preventDefault();
                var payForm = document.querySelector("[data-pay-form]");
                var payBtn = payForm ? payForm.querySelector("button[type=submit]") : null;
                if (payBtn && !payBtn.disabled) {
                    submitAsync(payForm);
                }
                break;
            }
            case "F10": {
                event.preventDefault();
                var autoprint = document.querySelector("[data-autoprint]");
                if (autoprint) {
                    autoprint.checked = !autoprint.checked;
                }
                break;
            }
            case "Escape":
                closeModal();
                break;
            default:
                break;
        }
    });

    function focusSearch() {
        focus("[data-product-search]");
    }

    function focus(selector) {
        var el = document.querySelector(selector);
        if (el) {
            el.focus();
            if (typeof el.select === "function") {
                el.select();
            }
        }
    }

    // ---- helpers ------------------------------------------------------------------------------

    function show(el) {
        el.hidden = false;
    }

    function hide(el) {
        el.hidden = true;
    }

    function formatMoney(value) {
        return String(value).replace(/\B(?=(\d{3})+(?!\d))/g, ".");
    }

    function escapeHtml(value) {
        return String(value == null ? "" : value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }
})();
