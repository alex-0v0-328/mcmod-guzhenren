# Guzhenren Mod

[简体中文](README.md) | [繁體中文](README.zh-TW.md) | English

A Chinese-style fantasy world mod for Minecraft 1.21.1 (NeoForge) centered on mortal Gu Master cultivation (Ranks 1–5), blending exploration, progression, and role-playing.

> Currently in development. Gameplay mechanics and balance are not yet final; this document covers project structure, environment requirements, and dependencies only.

This is a translation of [README.md](README.md); where the versions differ, the Simplified Chinese one prevails.

## Requirements

| Component | Requirement |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.265 or newer |
| Java | JDK 21 |
| Mod Version | 1.0.0 (Ranks 1–5) |

## Module Split

This project consists of two companion mods that must be installed together:

| Mod | Repository | Scope |
|---|---|---|
| Guzhenren | `mcmod-guzhenren` (this repo) | Cultivation systems (Aperture, Primeval Essence, Soul, Body, Paths), Gu worms and refinement, entities, UI, dimension travel, and guards |
| [Gu World](https://github.com/alex-0v0-328/mcmod-guworld) | `mcmod-guworld` | All dimensions, terrain generation, waterways, biomes, and natural structures |

Installing only one of the two mods leaves world generation or core mechanics incomplete. Both mods must also run on the same NeoForge version baseline.

## Dependencies

| Mod | Type | Version Range | Purpose |
|---|---|---|---|
| [Epic Fight](https://www.curseforge.com/minecraft/mc-mods/epic-fight-mod) | Required | `[21.17.3, 21.18)` | Combat and animation system |
| [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) | Required | `[4.9, 4.10)` | Skeletal animations for Gu worms, beasts, and NPCs |
| [Photon](https://www.curseforge.com/minecraft/mc-mods/photon) | Required | `[2.2.11, 2.3)` | Pixel-art particle effects runtime (required on both client and server) |
| [LDLib2](https://www.curseforge.com/minecraft/mc-mods/ldlib) | Required | `[2.2.46, 2.3)` | Core library for Photon and effect components (required on both client and server) |
| [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) | Optional | `[19, 20)` | Item and recipe viewing |

## Installation & Building

### Player Installation

1. Install Minecraft 1.21.1 with NeoForge 21.1.265 or newer.
2. Place all required dependency mods listed above into your instance's `mods/` folder.
3. Place both the `guzhenren` and `guworld` mod jars into `mods/`.

### Building from Source

Check out both repositories as sibling directories (`mcmod-guzhenren` and `../mcmod-guworld`) and build with JDK 21:

```powershell
./gradlew build
```

Data generation and GameTests:

```powershell
./gradlew runData
./gradlew runGameTestServer
```
