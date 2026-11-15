---
name: review-change
description: Use when reviewing GZR changes or explicitly confirming that a behavior works.
---

# Review Changes

Report actionable defects with evidence; distinguish them from existing defects and undecided design.

1. Read `git diff HEAD` and relevant untracked files, preserving unrelated WIP. Each finding states the trigger, `path:line`, impact and smallest fix, ordered by severity.
2. Check reuse before duplication, one job per class and one-way package dependencies (wiki《维护说明#代码结构》). Prefer named steps and constants to nested expressions.
3. Evaluate whole numeric expressions at zero, negatives and MIN/MAX. Saturating an addition does not protect a later multiplication; clamping before addition does not prevent overflow.
4. Trace interruption, pathing failure, target death, rejected damage, NBT restore and late tracking. Stopping must leave a reachable state. For “confirm X works,” trace both the write and read paths with line evidence; recount wiki totals.
5. Respect retained design: a `⚠` class Javadoc and wiki 待定 sections describe intentional limits. Bulk renaming or formatting is not cleanup. A review request alone authorizes no edits.

For example, an ownership sync review traces assignment, serialization, transmission and client use, including a player who starts tracking late; the presence of a payload class alone is not proof.
