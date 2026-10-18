---
name: teams
description: Claude Code + Codex team run for GZR — starts only when Alex types /teams. One startup confirmation sets goal, scope, tiers, dispatch count, time limits and auto-resume; the main agent then dispatches Claude tiers and Codex (ask_codex MCP) inside that scope, passes write access back and forth, and closes with checks, wiki sync and commit-push.
disable-model-invocation: true
---

# Teams Skill

Starts only on `/teams`; without it, delegation follows alex-constitution's per-dispatch model pick. The main agent, this session on Opus 5.5 / xhigh, splits, dispatches, integrates and accepts. Codex runs through the `ask_codex` MCP (`tools/ask_codex.py`, settings in `.env.local`); its side of the rules is `AGENTS › Delegated runs`.

## Tiers

| Tier      | Claude agent    | Claude model / effort    | Codex model / effort |
|-----------|-----------------|--------------------------|----------------------|
| economy   | `gzr-economy`   | claude-haiku-5-5 / high  | gpt-6-luna / high    |
| balanced  | `gzr-balanced`  | claude-sonnet-5-5 / high | gpt-6.1-sol / high   |
| strongest | `gzr-strongest` | claude-opus-5-5 / xhigh  | gpt-6-astra / medium |

Model and effort are configured separately, and every result names what actually ran: Codex's `actual` comes from its rollout, a Claude tier's from its transcript. A tier that cannot run is reported, never silently swapped.

- **Adversarial review:** Codex `balanced`, preset `audit`.
- **Competing candidates:** `gzr-strongest` and Codex `balanced` get the same need and criteria, read-only, and each returns a design or patch text; the main agent compares them and implements only the winner in the shared workspace.
- **Simple tasks:** one worker on the confirmed economy or balanced tier; never both models on the same task by default.

## Start

1. Read `git status` and the diff, and `codex_limits`.
2. Propose in one question-tool call: goal, scope (allowed and forbidden paths), the split per worker, tiers, the dispatch cap (default 6), the execution time limit (default 2 h), auto-resume after a quota pause (default off) and the longest quota wait (default 5 h). Alex confirms once.
3. Inside that confirmation the main agent dispatches without asking again; anything outside it goes back to Alex. The authorization ends when the run completes, is cancelled or the session closes.

## Dispatch

- A dispatch states goal, context, allowed and forbidden paths, delivery format and acceptance criteria. A report gives result, evidence (`path:line`), changes, verification, assumptions, blockers and leftovers; the Codex report schema enforces the fields.
- Delivery by type: an errand returns leads, `path:line` and exact sources; produce returns the in-scope changes, the acceptance commands run with their results, and what stays uncovered; an audit, always read-only, returns the trigger, the code line or repro command, the impact and the smallest fix.
- Codex presets: `errand` and `audit` are read-only; `produce` writes the workspace, `Temps/guzhenren` and `~/.gradle` with the network on, `.git` stays read-only, and it runs `tools/check.py build data --headless` at most. The game stages stay with the main agent.
- No recursion: the tier agents have neither the Agent tool nor ask_codex, and Codex is told never to start an agent. No task is dispatched twice.
- `needs_input`: the main agent asks Alex, then continues the same Codex session with `resume_session`.
- Watching Codex: Codex Desktop lists only interactive sessions, so exec dispatches never appear there. A run shows as its `ask_codex` call in the Claude session (progress every 30 s); the whole transcript opens with `/usr/lib/chatgpt/resources/codex resume <session_id>` or sits in the run folder's `events.jsonl`.

## Write handoff

One writer at a time in the shared workspace.

1. Before a write dispatch, a Claude tier that edits or a Codex `produce`, the main agent stops editing.
2. A `produce` run holds `Temps/guzhenren/teams/write.lock`; a second writer gets `busy`. A Claude tier cannot hold that lock, so before its write dispatch `python3 -B tools/ask_codex.py lock` must report it free; a held lock means another session's Codex is writing, and the dispatch waits.
3. Once the worker has stopped and reported, the main agent re-reads the live tree and `git diff`, keeps Alex's live edits and other sessions' WIP, and only then writes again.

## Quota

- `codex_limits`, or `python3 -B tools/ask_codex.py limits`, reads every bucket: used percent and reset time in Australia/Melbourne. Recovery is `ordinaryUsageAllowed` alone, never a percentage or a reset time; a failed read counts as unknown.
- Statuses: `quota_exhausted` (quota spent, also refused before launch), `rate_limited` (short throttle), `auth_failed`, `network_error`. Never switch to a paid API, buy credits or consume a reset credit.
- On `quota_exhausted`: stop dispatching Codex, confirm no write run is alive and the lock is free, keep the changes, and record the session_id with the done and remaining items.
- Auto-resume off: report the pause and stop the Codex work. On: start `python3 -B tools/ask_codex.py wait-quota --max-wait <remaining wait>` as a background shell, which sleeps to the reset and re-reads a limited number of times (exit 0 recovered, 3 out of time, 4 unknown), and keep doing independent Claude work meanwhile.
- After exit 0: re-read the tree, check that the task still needs doing and the authorization still holds, take the write handoff, then resume with `resume_session`. Waiting and execution time are counted separately; the resume spends what is left of the budget.
- Cancel or timeout stops the run's whole process group (`cancelled`, `timeout`); its changes stay for review.
- A resume needs this live main session: with no coordinator, an unclear tree or an expired authorization, notify only and change nothing. A systemd timer that resumes without a session is an open to-do in `rules/state.md`.

## Close

The main agent runs the `tools/check.py` stages the change touches, the wiki-sync chain and the `commit-push` skill; Alex picks every title and stays the only author and committer. This run's folders under `Temps/guzhenren/teams/runs/` are deleted after the report. Gameplay and visual acceptance stay Alex's.
