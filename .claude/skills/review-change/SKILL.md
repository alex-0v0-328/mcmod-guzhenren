---
name: review-change
description: Use when reviewing GZR changes or explicitly confirming that a behavior works.
---

> **Trigger**: Reviewing GZR changes or confirming a behavior works.
> **Mode**: Read-only — a review request alone authorizes zero file edits.
> **Output**: Actionable findings ordered by severity (`trigger`, `path:line`, `impact`, `smallest fix`), separated from pre-existing defects and undecided design.

## Review Checklist

| Dimension | Check | Boundary |
| --- | --- | --- |
| Scope | Read `git diff HEAD` and relevant untracked files | Preserve unrelated WIP; bulk rename/reformat is not cleanup |
| Structure | Verify reuse over duplication, single-job classes, one-way package deps (`《维护说明#代码结构》`),<br>and named steps/constants over nested expressions | Respect `⚠` class Javadocs and wiki 待定 sections as intentional limits |
| Numerics | Evaluate full numeric expressions at `0`, negatives, `MIN`/`MAX`<br>(saturated add does not protect later multiply; pre-add clamp does not prevent overflow) | Flag real overflow/underflow paths with exact expression |
| Lifecycle & sync | Trace interruption, pathing failure, target death, rejected damage, NBT restore, late tracking<br>(e.g., assign → serialize → transmit → client use; payload class alone is not proof)<br>For "confirm X works", trace both write and read paths with `path:line`; recount wiki totals | Every stop/interrupt leaves a reachable state |
