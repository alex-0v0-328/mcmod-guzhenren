---
name: grill-me
description: Grill Alex relentlessly about a plan, design or decision until every branch is settled. Runs only when Alex types /grill-me or says "grill me"; never on the agent's own initiative.
disable-model-invocation: true
license: MIT
metadata:
  source: "mattpocock/skills@7739eae, skills/productivity/grilling and grill-me, merged and adapted"
---

> **Trigger**: Only when Alex types `/grill-me` or says `"grill me"` (never self-initiated).
> **Language**: Questions and recommendations in `zh-CN`.
> **Done**: Frontier is empty (every branch visited, nothing silently assumed) → list settled decisions → wait for Alex to say `「开始」` before acting.

## Rules

| Dimension | DO | DON'T |
| --- | --- | --- |
| Design tree | Map subject as a decision tree; compute the frontier<br>(all open decisions whose prerequisites are settled) each round | Ask a question whose answer depends on another still-open question this round |
| Rounds | Ask the whole frontier in one round (overrides `i-have-adhd` 5-item list cap)<br>Word every question so `"是"` accepts your recommendation<br>Recompute frontier after answers; reopen branches if an answer invalidates an earlier one | Split settled-prerequisite questions across unnecessary turns |
| Facts vs decisions | Verify facts yourself (code, wiki, logs, tools); for wide search dispatch subagent<br>per `alex-constitution` (offer model first) while asking independent frontier questions now | Ask Alex for verifiable facts; answer your own decision question |
| Delivery | Put round in closing message after `mcp__ccd_turn__wrap_up` with ask `"按推荐全部接受？"`<br>and buttons `「全部接受」` / `「逐条回复」`<br>Without `wrap_up` (Codex/opencode/CLI): send round as full reply (`"全部接受"` works same) | Act before Alex confirms `「开始」` |

## Question Format

```
❓ **Q1 · <标题>**：<问题正文，可含 A/B/C 选项>

➡️ <推荐答案>

---
```

Alex replies by number (e.g., `"1 是，3 选 B，5 不，因为…"`) or clicks `「全部接受」`.
