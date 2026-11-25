---
name: commit-push
description: GitHub commit and push for GZR — use when Alex asks to commit, push, 提交并推送 or 走提交流程, when he brings a red GitHub Actions run, or when the exit-session skill finds unshipped work. Runs the whole flow through the push without a question; the agent picks the recommended commit title unless Alex named one.
---

> **Trigger**: Alex asks to commit/push ("走提交流程"), brings a red CI run, or `exit-session` finds unshipped work.
> **Scope**: `guzhenren` (`./`), `guworld` (`../mcmod-guworld`), and `wiki` (`../mcmod-wiki-gzr`) via `tools/commit_push.py`.
> **Execution**: Run straight through push with zero step-by-step asks, title questions, or push confirmations; pick recommended titles unless Alex named one.

## Hard Rules

| Dimension | DO | DON'T |
| --- | --- | --- |
| Identity | Ship under Alex's `git config` identity alone;<br>stop and notify Alex if configured identity itself fails gate | Add AI contributors (`Claude`, `Codex`, `opencode`), `Co-Authored-By`,<br>generated-with lines, or alter `git config` |
| History & hooks | Push directly to `origin/main`; use a new commit for pushed fixes; fix failing hooks at cause | Open unasked PRs, force-push, rewrite pushed history, `--amend` pushed commits,<br>or pass `--no-verify` / `--no-gpg-sign` / `-c commit.gpgsign=false` |
| Multi-repo order | Put cross-repo changes in one plan (default push order: `guworld` → `guzhenren` → `wiki`)<br>Add `"order": ["guzhenren", "guworld"]` only when changing a jar<br>compiled by `build.gradle` + both CI workflows | Ship shared files into wrong group;<br>commit by hand to bypass tool |
| CI & traps | Verify Actions check runs separately from commit status;<br>add CI download step for any hard dep in `run/mods`<br>When Alex brings red CI, run locate → fix → this flow → green automatically | Treat empty status, `queued`, `in progress`, or `no run found` as green |

## Flow & Gates

| Stage | Action | Gate / Output |
| --- | --- | --- |
| Check changes | Inspect `git status`; attribute each file to agent work,<br>Alex's live IDEA edits (ask = done), or other-session WIP (exclude unless asked)<br>Run only checks `commit_push.py` lacks: `local_checks.py server` for mod code/data;<br>add `client multiplayer` for client/renderer/resource changes (`--project guworld` for Gu World) | Clean attribution; required runtime smoke checks pass |
| Group & plan | Group by independent purpose (each carries own tests, providers, generated output)<br>Write unique plan at `../../Temps/guzhenren/<task>/plan.json`:<br>`{"commits":[{"project":"...","title":"...","body":"...","files":[...],"patches":[...],`<br>`"contents":{...}}]}`<br>Paths: repo-relative; `patches`/`contents` sources: plan-folder-relative;<br>trailing `/` grabs all changed paths under folder (>50 triggers preflight list) | Zero mixed-purpose commits; shared files split via `patches` or `contents` |
| Commit titles | Use Alex's title verbatim if given, else pick recommended Conventional Commit:<br>`<type>[(scope)][!]: <lowercase imperative English, no period>`<br>Breaking save/id/protocol change: add `!` + `BREAKING CHANGE: <what breaks>` footer;<br>optional body explains why | No batch/process numbers, check/CI results, dates,<br>session notes, extra footers, or unasked `mod_version` bumps |
| Run & verify | Run `python -B tools/commit_push.py <plan>` directly (no separate `--dry-run`)<br>Tool runs preflight (`main` checked out, remote not ahead, changed paths, CI jar check)<br>→ scratch-tree export verify off Gradle lock → contributor gate → atomic branch move<br>→ push → CI poll (15m default, `--poll-timeout`) | Exported tree verified (partner mod built from committed/planned tree, never live worktree)<br>Failure stops pre-commit and keeps `../../Temps/guzhenren/ship/failed-<pid>/` |
| Recovery | Remote ahead: pull keeping both sides and re-run;<br>partial multi-repo failure: drop already-pushed repos from plan and re-run<br>Hand-committed gate failure: fix unpushed commits then run `commit_push.py --push-only`;<br>`Invalid username or token`: Alex fixes Windows Credential Manager then `--push-only`<br>Manual CI re-check: `python -B tools/commit_push.py --ci <sha>` (`--project guworld`) | Contributor gate (`origin/main..<tip>`) and CI green |
| Report & post | Read `../../Temps/<project>/logs/commit_push-<pid>.log`; report each SHA, title, push state,<br>observed CI state (quote failing log lines if red)<br>Delete plan + log; remove shipped to-do from wiki《维护说明#待办与计划》<br>If folders/packages/build script changed, verify IDEA Gradle reload via JetBrains MCP<br>(ask Alex to click reload only if missed) | Before mass file move/rename: ship pending WIP first and ask Alex to pause IDEA edits |

## Quick References

| Commit Type | Scope |
| --- | --- |
| `feat` / `fix` / `refactor` / `perf` | New behavior / bug fix / no-behavior refactor / speedup |
| `test` / `docs` / `style` | Tests only / docs only / formatting only |
| `build` / `ci` / `chore` | Gradle & deps / GitHub workflows / other (`chore(release): <version>`) |

| Changed Scope | `commit_push.py` Verification (`--full` forces all) |
| --- | --- |
| README / editor files | None |
| `tools/`, `.claude/`, `AGENTS.md` | Tool tests |
| Mod code, data, or assets | `build`, `runData` drift, JUnit + wiki gate, `gametest`<br>(≈1–2 min; Gu World: `build`, `data` drift, `gametest`) |
| Wiki pages (`project: "wiki"`, `docs: ...`) | `doc_conventions.py links` on exported pages (no CI wait; `.obsidian/` untracked) |
