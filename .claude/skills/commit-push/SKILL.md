---
name: commit-push
description: GitHub commit and push for GZR — use when Alex asks to commit, push, 提交并推送 or 走提交流程, when he brings a red GitHub Actions run, or when the exit-session skill finds unshipped work. Runs the whole flow through the push without a question; the agent picks the recommended commit title unless Alex named one.
---

# GitHub Commit and Push Skill

When Alex asks to commit, push, or commit and push ("走提交流程"), go straight into this flow. Every step runs on its own, with no step-by-step asks, no commit-title question and no separate push confirmation: the agent picks the titles and runs `commit_push.py`, which commits, gates and pushes. Commits carry Alex's identity alone: no AI agent (Claude, Codex, opencode) is ever added as a contributor — no `Co-Authored-By` or other AI trailer, no generated-with line, no AI author or committer, no `git config` change — and an attribution line the harness supplies is dropped. Each repository ships its main checkout to `main` on origin; no PR unless Alex asks for one.

Paths below are relative to Guzhenren's root. The flow serves both repositories: Guzhenren, where every tool lives, and Gu World (`../mcmod-guworld`); the wiki ships after them (Wiki). Below, `<project>` is `guzhenren` or `guworld`, and for Gu World every `local_checks.py` and `commit_push.py` call adds `--project guworld`.

## Flow

1. **Check the changes.** Start from `git status` and attribute every changed file: the agent's own work, Alex's live IDEA edits (his ask means he is done editing), or another session's WIP, which stays out unless Alex says otherwise. Before shipping, run the full `python -B tools/local_checks.py` (`--headless` when no client code, renderer or resource changed).
2. **Prepare the commits.** Group the changes by independent purpose; each group carries its own tests, providers and generated resources, and every commit includes its datagen output. `commit_push.py` stages whole files only: a file whose hunks belong to two groups is staged by hand as a reviewed `git apply --cached` patch and committed before the tool runs — never a whole shared file into the wrong group.
3. **Pick commit messages.** A title Alex named for a group is used as given. Otherwise the agent writes the one title it would recommend and goes on — no question, no option list; the report shows every title used. Messages follow Conventional Commits: a title `<type>[(scope)][!]: <description>`, then an optional body and optional footers, each after a blank line. Types: `feat` new content or behavior, `fix` a bug fix, `refactor` a code change that alters no behavior, `perf` a speedup, `test` tests only, `docs` documentation only, `style` formatting only, `build` the build scripts, Gradle or dependencies, `ci` the GitHub workflows, `chore` anything else (a release is `chore(release): <version>`). The scope is optional and lowercase (a package or area: `aperture`, `refinement`, `worldgen`). A breaking change (a save, id or network-protocol break) adds `!` after the type or scope and a `BREAKING CHANGE: <what breaks>` footer. The description is lowercase imperative English without a trailing period. The body, when the title alone cannot carry it, explains why. A message holds only what the commit changes: no process, batch or decision numbers, check or CI results, dates or session notes, and no footer but `BREAKING CHANGE`. `mod_version` bumps are Alex's call per release.
4. **Commit and push.** Write `../../Temps/<project>/commit-push-plan.json` — `{"commits": [{"title": "...", "body": "...", "files": [...]}]}` in commit order (`body` optional), paths relative to the repo root, a folder written with a trailing `/` taking everything under it, deletions included — and run `python -B tools/commit_push.py ../../Temps/<project>/commit-push-plan.json` right after the titles are set, without a separate `--dry-run`. In order it runs: preflight (`main` checked out, empty index, remote not ahead, every plan path changed, every compile jar of `run/mods` downloaded in CI); the CI mirror (exports the exact planned tree from a scratch index into Temp, runs what GitHub runs — build, runData drift — then runs JUnit with the wiki gate and the GameTests on that same tree, ≈2–4 min; Alex's concurrent edits are never reverted and a planned file edited mid-run aborts the commit); one commit per group; the contributor gate; push to `main` on origin; a wait for that SHA's Actions run (15-minute default, `--poll-timeout`).
5. **Contributor gate.** Before the push, `commit_push.py` reads every commit in `origin/main..HEAD` (when origin has no `main` yet, every commit no origin branch holds) and refuses when an author or committer differs from the `git config` identity or looks like an AI, or when a message carries a `Co-Authored-By` or generated-with line. The commits then stay local: fix them (reword, or reset the author on the unpushed commits only), and run `commit_push.py --push-only`, which re-runs the gate, pushes and waits for CI. When the configured identity itself is the problem, stop and tell Alex — the agent never changes `git config`. Nothing is pushed until the gate passes.
6. **Report.** Read the `../../Temps/<project>/logs/commit_push-<pid>.log` the run printed. Report each commit's SHA, title, push state and the real CI state as observed, quoting the failing step's log lines for a red run. Delete the plan with the log, and delete the shipped work's to-do from wiki《维护说明#待办与计划》.

## Both repositories

- Gu World's check runs `build`, its GameTests (its only local tests), then its datagen drift, after building Guzhenren's jar once. Its mirror runs `build` and the `data` drift check as its GitHub workflow does (which builds Guzhenren's `main` first), then runs `gametest`. The contributor rule and the gate apply unchanged.
- A repository or remote with no commit yet ships too: the first push creates the branch and sets the upstream.
- When a change spans both repositories, ship Gu World first, so Guzhenren's runs and links already find it. The exception is a change to a jar Guzhenren's build script compiles against (its file name in `build.gradle` and both workflows): Gu World's CI builds Guzhenren's pushed branch, so ship Guzhenren first.
- Gu World carries no rules or harness files: Guzhenren's `AGENTS.md`, skills, subagents and configs serve both repositories and ship with Guzhenren.

## Wiki

The wiki (`../mcmod-wiki-gzr`, private `alex-0v0-328/guzhenren-mod-wiki`, `.obsidian/` untracked) ships after the mods it documents, without the mirror: run `doc_conventions.py links`, stage only the pages this task touched (another session's page edits stay out unless Alex says otherwise), make one `docs: ...` commit under the message and contributor rules above, and `git push` to `main`.

## Push rules

- No force push and no rewrite of pushed history; for pushed work a new commit beats `--amend`. Never `--no-verify`, `--no-gpg-sign` or `-c commit.gpgsign=false` — a failing hook is fixed at its cause.
- A preflight or mirror failure stops before anything is staged; fix the cause and re-run. When the remote moved ahead, pull (keep both sides' work) and re-run.
- `python -B tools/commit_push.py --ci <sha>` re-checks a commit's CI by hand (with `--project guworld` for Gu World).
- A push refused with `Invalid username or token` means the stored GitHub credential is wrong (Git Credential Manager answers from the Windows Credential Manager): the commits stay local, Alex fixes the credential, then `commit_push.py --push-only` ships them.

## CI red

When Alex brings a GitHub Actions failure, the loop locate → fix → this flow → green runs without waiting for an explicit commit request — a red CI left unpushed counts as unfixed. Titles follow step 3.

## Traps

- Commit status and Actions check runs are separate queries — an empty status is not "CI green", and neither are "queued", "in progress" or "no run found".
- A hard dependency referenced straight from `run/mods` needs its CI download step, otherwise CI goes red while local stays green.

## After

When a change moved folders or packages or touched a build script, the agent confirms IDEA's Gradle reload through the JetBrains MCP where the session has it, and asks Alex for the reload button only when it did not happen (`AGENTS › Checks and shipping`). Before a change that rewrites many files (a move, a rename), ship any pending WIP first and ask Alex to pause IDEA edits until it lands.
