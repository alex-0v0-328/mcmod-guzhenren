---
name: new-content
description: Use when adding Gu, entities, items, or mechanics to the GZR mod.
---

> **Trigger**: Adding Gu, entities, items, or mechanics to GZR.
> **Goal**: Deliver complete gameplay path, assets, tests, and aligned docs within approved design (compile pass alone is insufficient).
> **Authority**: `AGENTS.md`, `《维护说明#写作与来源》`, and `wiki-sync` skill.

## Flow & Gates

| Stage | Action | Gate |
| --- | --- | --- |
| Study & classify | Read owning wiki page + existing sibling in same seam (`TendedGuItem`,<br>`ModItems` `GuProperties`, neighboring `ItemRegistrar` chains for names/model/tags/tab)<br>Classify as: one-shot, tended, consumed on use, or entity | Seam and lifecycle class settled before coding |
| Entity contract | Include type, attributes, spawn predicates, biome modifier,<br>renderer, model, texture, animation, lang, loot<br>Server owns writes/damage; client renders synced state<br>Settle cooldown, owner, death, load, dimension change, and target loss upfront | No spawn egg, taming, or breeding added |
| Visual assets | Import models read-only from Blockbench authority;<br>custom renderers preserve invisibility, team visibility, and glow<br>Supply placeholder texture passing `runData`<br>+ exact asset spec (vanilla ref, dimensions, mcmeta, colors, placement) for Alex<br>Armor/clothing: fit both 4 px (wide) and 3 px (slim) skin arms — `*_arm_wide` / `*_arm_slim` bones,<br>Epic Fight `<side>Arm` / `<side>ArmSlim` parts, hide the mismatch per wearer<br>(pattern: `SchoolUniformArmorRenderer`, `EpicFightClientIntegration`) | `runData` passes; asset spec complete;<br>armor checked on slim and wide skins |
| Tests & datagen | Put registry-free JVM logic in L1 `src/test/pure`;<br>registered objects, worlds, and ticks in L2 `src/test/game` via `GameTestPlayers`<br>After provider edits, run `local_checks.py data` and inspect `src/generated` | L1/L2 tests and `data` drift check green |
| Docs & triggers | New item: update wiki gate item groups, player-table row, and homepage count in same task<br>(`GuProperties` change updates player table)<br>Fire advancement triggers only on successful use (never commands)<br>Put implementation invariants in class Javadoc; put spec/rationale in wiki, never both<br>Finish with `wiki-sync` skill and required `AGENTS.md` checks | Wiki gate, `doc_conventions.py`, and `local_checks.py` green |
