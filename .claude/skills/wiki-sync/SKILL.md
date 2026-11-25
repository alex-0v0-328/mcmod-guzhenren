---
name: wiki-sync
description: Use after GZR code changes or when editing the GZR wiki.
---

> **Trigger**: After GZR code changes or before/during GZR wiki edits.
> **Scope**: Update only affected sections (re-read before writing; never regenerate whole pages); leave unrelated pages intact.
> **Authority**: Carrier split in `《维护说明#写作与来源》`, plans/acceptance paths in `AGENTS.md`.

## Flow & Gates

| Stage | Action | Gate |
| --- | --- | --- |
| Trace & edit | Trace diff: `code → system → page → design → decision`<br>Place facts by carrier: dev page = what/why + owning class named once; class Javadoc = invariants;<br>test coverage = list untested behavior only (no test-class lists);<br>Alex's decisions = `「Alex 定」` without date<br>Preserve decisions, rationale, rejected-idea constraints, and unimplemented design;<br>mark unconfirmable claims `[NEEDS REVIEW]` | Never infer lore from code or bend lore to implementation |
| Verify refs | Verify file paths, class names, registry IDs, and affected numeric tables<br>Apply `new-content` table rules for new items or `GuProperties` changes | Zero stale IDs, paths, or values |
| Check conventions | Run `python -B tools/doc_conventions.py` for `links`, `typography`, `carriers`, `length`,<br>`headings`, and `tables <file>` on touched tables<br>Run `local_checks.py build` when affected pages are pinned by tests<br>(docs-only edits need no new gameplay tests) | All convention checks and pinned build gate green |
| Counts & report | Check `《玩家向/1.0.0/蛊真人Mod-总表》` for affected counts and acceptance items<br>Report: pages updated/unchanged/needing review, implementation gaps, conflicts,<br>and items needing Alex's `runClient` visual/feel acceptance | Counts reconciled; report complete |
