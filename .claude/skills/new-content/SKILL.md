---
name: new-content
description: Use when adding Gu, entities, items, or mechanics to the GZR mod.
---

# New Content

Deliver the complete gameplay path, assets, tests and affected documentation within the approved design.

1. Read the owning wiki page and an existing implementation in the same seam: `TendedGuItem`, the `GuProperties` chain in `ModItems`, and neighboring items' `ItemRegistrar` chains, which carry each item's names, model, tags and tab. Classify the content as one-shot, tended, consumed on use or entity before choosing structure.
2. For an entity, include type, attributes, spawn predicates, biome modifier, renderer, model, texture, animation, language and loot. Add no spawn egg, taming or breeding. The server owns writes and damage; the client displays synced state. Settle cooldown, owner, death, load, dimension change and target loss before implementation.
3. Import models read-only from the Blockbench authority. Custom renderers retain invisibility, team visibility and glow. Alex draws final textures; supply a placeholder that passes `runData` and an exact asset spec: vanilla reference, dimensions, mcmeta, colors and placement.
4. Put registry-free JVM logic in L1 `src/test/pure`; registered objects, worlds and ticks belong in L2 `src/test/game` using `GameTestPlayers`. Provider changes require `local_checks.py data` and review of `src/generated`.
5. A new item updates the wiki gate's item groups, its player-table row and the homepage count in the same task. A `GuProperties` change also updates the player table. Fire advancement triggers only at successful use, never from commands. Implementation invariants go in the class Javadoc, spec and rationale in the wiki page, never both (wiki《维护说明#写作与来源》). Finish with the wiki-sync skill and the required checks in `AGENTS › Checks and shipping`.

For example, adding a tended Gu starts from a neighboring tended item and its registration, then verifies feeding/use behavior, generated resources and the matching player-table entry. A compile pass alone does not establish complete delivery.
