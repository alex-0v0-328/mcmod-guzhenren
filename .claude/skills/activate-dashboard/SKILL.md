---
name: activate-dashboard
description: Dashboard session for GZR — use when Alex asks for the dashboard (看板) on a task. Clears the previous board, prepares this session's data, then launches the live progress board (进度 / 已定目标 / 输出) drawn by the dashboard-builder subagent.
---

# Activate Dashboard Skill

An on-demand session skill. The board is `C:\workspace\Dev\Projects\Minecraft-ModDev\.dashboard\dashboard.html`, drawn only by the `dashboard-builder` subagent (`.claude/agents/dashboard-builder.md`); the main agent gathers every piece of data and never writes the board itself. Every launch is a new dashboard session: nothing carries over from the previous board.

## Launch, strictly in this order

Never skip steps 1 and 2 to launch early.

1. **Clear.** Delete the previous `dashboard.html` (regenerable output). Keep `.dashboard\memory\style.md`: it holds Alex's standing style preferences, not session data.
2. **Prepare.** Gather this session's data: the settled goals, one entry each; the progress they imply; the outputs so far; `now` from the real clock (`Get-Date -Format 'yyyy-MM-dd HH:mm:ss'`). Ask Alex for the builder's model in one question-tool call (alex-constitution › SubAgent collaboration); if `style.md` is missing, the same call also asks for theme (dark or light), density (dense or spacious) and primary color, and the answers go with the `BUILD`.
3. **Launch.** Dispatch `BUILD` to the builder in the background on the picked model, then open the board once with `Start-Process '<board path>'`. The page reloads itself every 10 s.

## The three areas

- **进度** — completion of the current task, computed from 已定目标 so the two always agree. While a question waits for Alex it shows a 等你回答 state; the question itself is asked in chat only.
- **已定目标** — the final, confirmed goals, each on its own; a finished goal is struck through and 进度 updates with it. This replaces the old "我的要求 + AGENT 理解" areas.
- **输出** — the current stage's actual results: what was run, what it produced, script output, tool results, failures included.

The alex-constitution skill is injected into every session, so every board also shows `Alex Constitution Skill：已启用`.

## Updates

- After every finished step, every question for Alex and every blocker, continue the same builder instance (`SendMessage` to its ID) with an `UPDATE` delta; spawn a fresh one on the same model only if continuing fails.
- Every dispatch carries the mode (`BUILD`, `UPDATE`, `FEEDBACK`), `now` from the real clock, `alex-constitution: active`, and the data: goals with their status, progress and outputs, each with its real time. Alex's comments on the board go to the builder as `FEEDBACK` and land in `style.md`.
- The builder replies in one line (`done: …` or `blocked: …`); relay what Alex needs.
