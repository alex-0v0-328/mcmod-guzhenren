---
name: grill-me
description: Grill Alex relentlessly about a plan, design or decision until every branch is settled. Runs only when Alex types /grill-me or says "grill me"; never on the agent's own initiative.
disable-model-invocation: true
license: MIT
metadata:
  source: "mattpocock/skills@7739eae, skills/productivity/grilling and grill-me, merged and adapted"
---

# Grill Me Skill

Interview Alex until you share one understanding of the subject. Questions and recommendations are in zh-CN.

## Design tree

Map the subject as a design tree: every decision branches into the decisions that hang off it. The frontier is every open decision whose prerequisites are settled, the questions you can ask now without guessing at answers you have not heard.

## Rounds

1. Ask the whole frontier in one round, however many questions it holds; the `i-have-adhd` list cap does not apply here.
2. A question whose answer depends on another question still open this round waits for a later round.
3. Word each question so "是" accepts your recommendation.
4. After Alex answers, recompute the frontier from scratch and ask the next round. If he says an answer changes an earlier one, reopen that branch.

## Format

The round goes in the closing message, after `mcp__ccd_turn__wrap_up`; the box's ask is "按推荐全部接受？" with the buttons 「全部接受」 and 「逐条回复」. Each question:

```
❓ **Q1 · <标题>**：<问题正文，可含 A/B/C 选项>

➡️ <推荐答案>

---
```

Alex answers by number ("1 是，3 选 B，5 不，因为…"); 「全部接受」 accepts every recommendation in the round. Without `wrap_up` (Codex, opencode, the CLI) the round is the whole reply, and "全部接受" typed back works the same.

## Facts and decisions

- Facts are yours, never Alex's: read the code, wiki, logs and tools yourself. For a wide search, dispatch a subagent per `alex-constitution` (offer the model first); the questions that depend on it wait for it, the rest of the frontier is asked now.
- Decisions are Alex's: put each one to him and wait. Never answer your own decision question.

## Done

The session ends when the frontier is empty: every branch visited, nothing silently assumed. Then show the settled decisions as one short list and wait for Alex to say 「开始」 before acting on any of it.
