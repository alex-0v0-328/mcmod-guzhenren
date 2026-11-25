---
name: commit-push
description: GitHub commit and push for GZR — use when Alex asks to commit, push, 提交并推送 or 走提交流程, when he brings a red GitHub Actions run, or when the exit-session skill finds unshipped work. Runs the whole flow through the push without a question; the agent picks the recommended commit title unless Alex named one.
---

# GitHub Commit and Push Skill

When Alex asks to commit, push, or commit and push ("走提交流程"), go straight into this flow with no step-by-step asks, commit-title questions, or push confirmations. The agent picks the titles and runs `commit_push.py`, which commits, gates, and pushes.

- **Identity:** Commits carry Alex's identity alone — no AI contributor (`Claude`, `Codex`, `opencode`), no `Co-Authored-By` or AI trailer, no generated-with line, no AI author/committer, and no `git config` change; drop any attribution line the harness supplies.
- **Branching:** Each repository ships its main checkout to `main` on origin; open no PR unless Alex asks.
- **Scope & paths:** Paths below are relative to Guzhenren's root. One plan may cover Guzhenren (where every tool lives), Gu World (`../mcmod-guworld`), and the wiki (`../mcmod-wiki-gzr`).
- **Project selection:** Each commit sets `"project"` (`guzhenren`, `guworld`, or `wiki`; default `guzhenren`, or what `--project` names). For Gu World, every `local_checks.py` call adds `--project guworld`.

## Flow

1. **Check the changes.** Start from `git status` and attribute every changed file to the agent's work, Alex's live IDEA edits (his ask means he is done editing), or another session's WIP (leave out unless Alex says otherwise).
   - `commit_push.py` runs tool tests, `build`, `data`, and `gametest` on the planned tree; before shipping, run only what it lacks:
   - `python -B tools/local_checks.py server` for mod code or data changes; add `client multiplayer` when client code, a renderer, or a resource changed.

2. **Prepare the commits.** Group changes by independent purpose; each group carries its own tests, providers, and generated resources, and every commit includes its datagen output. Never ship a shared file into the wrong group, and never commit by hand to work around it.
   - `files`: whole files; a folder with a trailing `/` takes every changed path under it (deletions included), whoever edited it. Preflight spells out any folder entry taking more than 50 paths.
   - `patches`: `git diff` output cut down to this commit's hunks when other hunks belong to another group or session.
   - `contents`: maps a repository path to a file holding exactly what should be committed.

3. **Pick commit messages.** Use Alex's title verbatim if named; otherwise pick one recommended title and proceed without asking (the report shows every title used). Follow Conventional Commits: `<type>[(scope)][!]: <description>`, optional body, optional footers, each after a blank line.

   | Type | Use for |
   | --- | --- |
   | `feat` | New content or behavior |
   | `fix` | Bug fix |
   | `refactor` | Code change altering no behavior |
   | `perf` | Speedup |
   | `test` | Tests only |
   | `docs` | Documentation only |
   | `style` | Formatting only |
   | `build` | Build scripts, Gradle, or dependencies |
   | `ci` | GitHub workflows |
   | `chore` | Anything else (`chore(release): <version>` for a release) |

   - `scope` is optional and lowercase (a package or area, e.g., `aperture`, `refinement`, `worldgen`).
   - Breaking change (save, id, or network-protocol break): add `!` after type or scope and a `BREAKING CHANGE: <what breaks>` footer.
   - Description: lowercase imperative English with no trailing period. Body (when title alone cannot carry it): explains why.
   - Hold only what the commit changes: no process, batch, or decision numbers, no check or CI results, no dates or session notes, and no footer other than `BREAKING CHANGE`. `mod_version` bumps are Alex's call per release.

4. **Commit and push.** Write the plan into this task's own folder (`../../Temps/guzhenren/<task folder>/plan.json`, never a shared file name since sessions ship side by side) and run `python -B tools/commit_push.py <plan>` right after setting titles, without a separate `--dry-run`.
   - Schema: `{"commits": [{"project": "...", "title": "...", "body": "...", "files": [...], "patches": [...], "contents": {...}}]}` in commit order (`project`, `body`, `patches`, `contents` optional).
   - Path rules: repository paths are relative to that repository's root; patch and content sources are relative to the plan's folder.
   - **Preflight:** Checks `main` checked out, remote not ahead, every plan path changed, and every compile jar of `run/mods` downloaded in CI; derives each repository's tree in a scratch index, leaving the checkout and its index alone.
   - **Verify:** Runs off the Gradle lock in one of two parallel slots on an export of that exact tree (`--full` forces every stage):

     | Change scope | Verification run |
     | --- | --- |
     | README and editor files | Nothing |
     | `tools/`, `.claude/`, `AGENTS.md` | Tool tests |
     | Mod code, data, or assets | `build`, `runData` drift, JUnit + wiki gate, GameTests (≈1–2 min) |
     | Wiki pages | `doc_conventions.py links` |

   - **Landing:** Under one short lock, confirms each tree still matches the verified one (else re-verifies because another session landed first), builds commits beside the branch, runs the contributor gate, moves each branch in one step, and pushes in order: Gu World, Guzhenren, wiki.
   - **CI wait:** Polls each pushed SHA's Actions run with no lock held (15-minute default, `--poll-timeout`). Alex's concurrent edits are never reverted and nothing is ever half committed.

5. **Contributor gate.** Before any branch moves, `commit_push.py` reads every commit the push would add: `origin/main..<new tip>` (or every commit not on an origin branch when origin has no `main` yet).
   - Refuses if an author or committer differs from `git config` identity or looks like an AI, or if a message carries a `Co-Authored-By` or generated-with line. Fix the plan and re-run (nothing is committed then).
   - If the offender is a commit already made by hand, fix it (reword, or reset the author on unpushed commits only) and run `commit_push.py --push-only` to verify unpushed commits, re-run the gate, push, and wait for CI.
   - If the configured identity itself is the problem, stop and tell Alex — never change `git config`. Nothing is pushed until the gate passes.

6. **Report.** Read `commit_push-<pid>.log` printed by the run (under `../../Temps/guzhenren/logs/`, or `../../Temps/guworld/logs/` with `--project guworld`).
   - Report each commit's SHA, title, push state, and real CI state as observed; quote the failing step's log lines for a red run.
   - Delete the plan with the log, and delete the shipped work's to-do from wiki《维护说明#待办与计划》.

## Both repositories

- Gu World's check runs `build`, its GameTests (its only local tests), then its datagen drift after building Guzhenren's jar once; its verification runs `build` and `data` drift (as its GitHub workflow does, which builds Guzhenren's `main` first), then `gametest`.
- Each mod is verified against the other exported from its committed tree — or, when one plan ships both, from the tree that plan gives it — never against the other's work tree. The contributor rule and the gate apply unchanged.
- A repository or remote with no commit yet ships too: the first push creates the branch and sets the upstream.
- When a change spans both repositories, put both in one plan: the tool ships Gu World first, so Guzhenren's runs and links already find it.
  - Exception: a change to a jar Guzhenren's build script compiles against (its file name in `build.gradle` and both workflows); Gu World's CI builds Guzhenren's pushed branch, so the plan adds `"order": ["guzhenren", "guworld"]`.
- Gu World carries no rules or harness files: Guzhenren's `AGENTS.md`, skills, subagents, and configs serve both repositories and ship with Guzhenren.

## Wiki

- The wiki (`../mcmod-wiki-gzr`, private `alex-0v0-328/guzhenren-mod-wiki`, `.obsidian/` untracked) ships through the same tool after the mods it documents, in the mods' plan or a plan of its own.
- Use one `"project": "wiki"` commit titled `docs: ...` listing only the pages this task touched (another session's page edits stay out unless Alex says otherwise).
- Its verification is `doc_conventions.py links` on the exported pages; it has no CI to wait for.

## Push rules

- No force push and no rewrite of pushed history; for pushed work, a new commit beats `--amend`. Never use `--no-verify`, `--no-gpg-sign`, or `-c commit.gpgsign=false` — a failing hook is fixed at its cause.
- A preflight, verification, or gate failure stops before anything is committed; fix the cause and re-run. A failed verification keeps its export under `../../Temps/guzhenren/ship/failed-<pid>/`.
- When the remote moved ahead, pull (keep both sides' work) and re-run.
- `python -B tools/commit_push.py --ci <sha>` re-checks a commit's CI by hand (with `--project guworld` for Gu World).
- When one plan ships several repositories and a later push fails, the earlier ones are already pushed: drop their commits from the plan before re-running.
- A push refused with `Invalid username or token` means the stored GitHub credential is wrong (Git Credential Manager answers from Windows Credential Manager): the commits stay local, Alex fixes the credential, then `commit_push.py --push-only` ships them.

## CI red

- When Alex brings a GitHub Actions failure, the loop locate → fix → this flow → green runs without waiting for an explicit commit request — a red CI left unpushed counts as unfixed. Titles follow step 3.

## Traps

- Commit status and Actions check runs are separate queries — an empty status is not "CI green", and neither are "queued", "in progress", or "no run found".
- A hard dependency referenced straight from `run/mods` needs its CI download step; otherwise CI goes red while local stays green.

## After

- When a change moved folders or packages or touched a build script, confirm IDEA's Gradle reload through JetBrains MCP where the session has it; ask Alex for the reload button only when it did not happen (`AGENTS › Checks and shipping`).
- Before a change that rewrites many files (a move or rename), ship any pending WIP first and ask Alex to pause IDEA edits until it lands.
