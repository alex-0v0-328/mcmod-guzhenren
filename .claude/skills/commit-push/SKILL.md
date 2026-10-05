---
name: commit-push
description: GitHub commit and push for GZR — use when Alex asks to commit, push, 提交并推送 or 走提交流程, when he brings a red GitHub Actions run, or when the session-close skill finds unshipped work. Runs the whole flow; picking the commit message is the only question.
---

# GitHub Commit and Push Skill

When Alex asks to commit, push, or commit and push ("走提交流程"), go straight into this flow. Picking the commit message is the only step that waits for him; every other step runs on its own, with no step-by-step asks and no separate push confirmation. Commits carry Alex's identity alone: Claude is never added as a contributor — no `Co-Authored-By` or other AI trailer, no generated-with line, no AI author or committer, no `git config` change — and an attribution line the harness supplies is dropped. The branch stays `main`; no PR unless Alex asks for one.

## Flow

1. **Check the changes.** Start from `git status` and attribute every changed file: the agent's own work, or Alex's live IDEA edits (his ask means he is done editing). Before shipping, run the full `python tools/check.py` (`--headless` when no client code, renderer or resource changed).
2. **Prepare the commits.** Group the changes by independent purpose; each group carries its own tests, providers and generated resources, and every commit includes its datagen output. `commit_push.py` stages whole files only: a file whose hunks belong to two groups is staged by hand as a reviewed `git apply --cached` patch and committed before the tool runs — never a whole shared file into the wrong group.
3. **Offer commit messages.** For each group, offer 2..3 English titles through the question tool, never as plain chat text, the recommended one first, marked `(Recommended)` with a one-line reason; then wait for his pick — never default, never pick for him. Title shape `[PREFIX] <english body>`: `[UPDATE]` bugfixes and small changes, `[TEST]` test releases, `[ALPHA]` second-to-last before a release, `[BETA]` last before a release, `[RELEASE]` official release. A message holds only what the commit changes: no process, batch or decision numbers, check or CI results, dates or session notes, and no body beyond the title. `mod_version` bumps are Alex's call per release.
4. **Commit and push.** Write `C:\workspace\Dev\Projects\_Temp\guzhenren\commit-push-plan.json` — `{"commits": [{"title": "...", "files": [...]}]}` in commit order, paths relative to the repo root, a folder written with a trailing `/` taking everything under it, deletions included — and run `python tools/commit_push.py C:\workspace\Dev\Projects\_Temp\guzhenren\commit-push-plan.json` right after the pick, without a separate `--dry-run`. In order it runs: preflight (branch `main`, empty index, remote not ahead, every plan path changed, every `run/mods` jar downloaded in CI); the CI mirror (exports the exact planned tree from a scratch index into Temp, runs what GitHub runs — build, runData drift — then copies in the local-only `src/test` and runs JUnit with the wiki gate and GameTests on that same code, ≈2–4 min; Alex's concurrent edits are never reverted and a planned file edited mid-run aborts the commit); one commit per group; the contributor gate; push to `origin main`; a wait for that SHA's Actions run (15-minute default, `--poll-timeout`).
5. **Contributor gate.** Before the push, `commit_push.py` reads every commit in `origin/main..HEAD` and refuses when an author or committer differs from the `git config` identity or looks like an AI, or when a message carries a `Co-Authored-By` or generated-with line. The commits then stay local: fix them (reword, or reset the author on the unpushed commits only), and run `python tools/commit_push.py --push-only`, which re-runs the gate, pushes and waits for CI. When the configured identity itself is the problem, stop and tell Alex — the agent never changes `git config`. Nothing is pushed until the gate passes.
6. **Report.** Read `C:\workspace\Dev\Projects\_Temp\guzhenren\logs\commit_push.log`. Report each commit's SHA, title, push state and the real CI state as observed, quoting the failing step's log lines for a red run. Delete the plan with the log, and delete the shipped work's to-do from `rules/state.md`.

## Sibling repositories

Gu World (`../guworld`, remote `alex-0v0-328/mcmod-guworld`) ships through the same flow and the same tools with `--project guworld`: check with `python tools/check.py --project guworld` (build with its local JUnit, its GameTests, then its datagen drift; each stage first builds Guzhenren's jar), write the plan to `C:\workspace\Dev\Projects\_Temp\guworld\commit-push-plan.json`, run `python tools/commit_push.py <plan> --project guworld`, and read `C:\workspace\Dev\Projects\_Temp\guworld\logs\commit_push.log`. Its mirror runs `build` as its GitHub workflow does, then copies in its local-only `src/test` and runs `build`, `gametest` and the `data` drift check its CI cannot run. The contributor rule and the gate apply unchanged. A repository or remote with no commit yet ships too: the first push creates `main` and sets the upstream. When a change spans both repositories, ship Gu World first, so Guzhenren's runs and links already find it.

Camera Shift (`../camera-shift`, remote `alex-0v0-328/mcmod-camera-shift`) ships the same way with `--project camerashift`: plan at `C:\workspace\Dev\Projects\_Temp\camerashift\commit-push-plan.json`; check runs `build` with its local JUnit suite, and the mirror runs `build` as its GitHub workflow does, then again with the gitignored `src/test` copied in. Read each sibling's `CLAUDE.md` whole before touching that repository.

Burst Flight (`../burst-flight`, public remote `alex-0v0-328/mcmod-burst-flight`) ships the same way with `--project burstflight`: plan at `C:\workspace\Dev\Projects\_Temp\burstflight\commit-push-plan.json`; check runs `build` with its local JUnit suite and `gametest`, and the mirror runs `build` as its GitHub workflow does, then both with the gitignored `src/test` copied in.

Every sibling tracks the same `.claude/` cloud files as this repository (`settings.json`, `hooks/session_start.py`, the two always-on skills). They are copies: edit them here, run `python .claude/hooks/session_start.py --deploy`, and ship each sibling's refreshed copy with that sibling.

## Push rules

- No force push and no rewrite of pushed history; for pushed work a new commit beats `--amend`. Never `--no-verify`, `--no-gpg-sign` or `-c commit.gpgsign=false` — a failing hook is fixed at its cause.
- A preflight or mirror failure stops before anything is staged; fix the cause and re-run. When the remote moved ahead, pull (keep both sides' work) and re-run. Interactive flags (`-i`) don't work in agent shells.
- `python tools/poll_actions_ci.py --sha <sha>` re-checks CI by hand.

## CI red

When Alex brings a GitHub Actions failure, the loop locate → fix → this flow → green runs without waiting for an explicit commit request — a red CI left unpushed counts as unfixed. The commit-message pick still applies.

## Traps

- Line-ending noise stays out of commits (mechanics in reference《环境、构建与测试》).
- Gradle lock conflicts between parallel runs: retry or serialize.
- Commit status and Actions check runs are separate queries — an empty status is not "CI green", and neither are "queued", "in progress" or "no run found".
- A hard dependency referenced straight from `run/mods` needs its CI download step, otherwise CI goes red while local stays green (reference《依赖与兼容》).

## After

When a change moved folders or packages or touched a build script, the agent confirms IDEA's Gradle reload through the JetBrains MCP and asks Alex for the reload button only when it did not happen (`CLAUDE.md` › Checks and shipping). Before a change that rewrites many files (a move, a rename), ship any pending WIP first, and ask Alex to pause IDEA edits until it lands.
