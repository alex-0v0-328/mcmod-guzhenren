---
name: wiki-sync
description: Use after GZR code changes or when editing the GZR wiki.
---

# Sync the Wiki

Keep affected documentation consistent with verified behavior and the current design; leave unrelated pages intact.

1. **Trace and update sections.** Trace the diff through code → system → page → design → decision.
   - Read only affected pages and re-read the section before writing; update sections, never regenerate pages.
   - Place each fact by the carrier table in wiki《维护说明#写作与来源》:
     - Dev page: says what and why, and names the owning class once.
     - Class Javadoc: holds implementation invariants.
     - Test coverage: no page lists which test pins what, only behavior with no automated test.
     - Decisions: Alex's calls read 「Alex 定」 without a date.
   - Preserve current decisions, rationale, rejected-idea constraints, and unimplemented design.
   - Do not infer lore from code or bend lore to implementation; mark unconfirmable claims `[NEEDS REVIEW]`.

2. **Verify references and tables.** Verify paths, class names, registry ids, and affected numeric tables.
   - New items and `GuProperties` changes follow the `new-content` skill's table requirements.

3. **Run doc convention checks.** Run `python -B tools/doc_conventions.py` for `links`, `typography`, `carriers`, and `length`; all must be clean.
   - Check touched tables with `tables <file>` and headings with `headings`.
   - If affected pages are pinned by tests, `local_checks.py build` is the final gate; docs-only changes need no new gameplay tests.

4. **Check counts and report.** Check 《玩家向/1.0.0/蛊真人Mod-总表》 for affected counts and acceptance items.
   - Report pages updated, unchanged, or needing review, implementation gaps, conflicts, and what still needs Alex's visual or feel acceptance in `runClient`.
   - Keep plans and client acceptance in the locations named by `AGENTS › Conventions`.

For example, a documentation-only maintenance edit preserves pinned item tables, checks links and formatting, and runs the build gate when that page is pinned; it does not need a game client.
