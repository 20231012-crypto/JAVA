/**
 * Admin-area behaviour. Progressive enhancement only — every admin action is a plain form POST
 * that works with this file absent, and the server re-checks everything this file guards.
 */
(function () {
    "use strict";

    document.addEventListener("DOMContentLoaded", function () {
        initConfirmCodeGate();
    });

    /**
     * Destructive forms (currently the order refund) ask the operator to retype an identifier
     * before the button works. The button ships ENABLED in the HTML and is disabled here instead:
     * shipping it disabled would make the form unusable for anyone without JavaScript, and this is
     * an assistive gate, not the security boundary — OrderRefundServlet compares the same value
     * again server-side and refuses the refund if it does not match.
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
                // The field is only "invalid" once the operator has typed something — flagging an
                // untouched field red is noise, not feedback.
                input.setAttribute("aria-invalid", input.value.length > 0 && !matches ? "true" : "false");
            }

            input.addEventListener("input", sync);
            sync();
        });
    }
})();
