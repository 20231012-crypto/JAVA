---
name: canteen-dev-flow
description: Development discipline for the EAUT canteen web app — analyze and critique before coding, then build, then test and report real results. Use for every feature/task worked on in this repository.
---

# Canteen dev flow

Every task in this project (BTL_JAVA / EAUT canteen web app) goes through three phases, in order. Do not skip a phase or collapse them together.

## 1. Read & critique

Before writing any code:
- Restate the requirement in your own words to confirm you understood it.
- Check it against the approved plan and the existing schema/architecture (`sql/schema.sql`, the `model`/`dao`/`controller` packages) — does it fit cleanly, or does it need a schema/architecture change?
- Actively look for problems: ambiguous scope, missing edge cases (empty states, concurrent access, invalid input), conflicts with the MVC layering rules or the two-tier inventory / order-state-machine invariants already established, anything that contradicts an existing decision.
- If something is genuinely ambiguous or risky, say so and ask or flag it — don't silently guess on a consequential decision.

## 2. Build

- Follow the strict MVC layering: `model`/`dao`/`dao.impl` stay HTTP-unaware; `controller` only orchestrates (request → DAO → forward); JSPs under `WEB-INF/views` only display via JSTL/EL, no scriptlet business logic, no DAO/DB calls.
- All user-facing text is Vietnamese (labels, buttons, messages, status names) — no English UI strings.
- Reuse existing conventions rather than inventing new ones: the guarded-`UPDATE` pattern for stock/status changes, `Connection conn` as the first DAO parameter, try-with-resources for every JDBC resource.

## 3. Test & report

- Compile (`./mvnw clean package`) and fix any errors before calling the task done.
- Actually exercise the change — deploy and click through it, or run a targeted check — rather than asserting it "should work."
- Report what you actually verified and what you didn't (e.g. "compiled and manually tested the happy path; did not test the concurrent-order race condition"). Never report a task as complete if it only compiled but was never run.
