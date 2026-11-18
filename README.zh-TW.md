# 蠱真人 Guzhenren 模組

[简体中文](README.md) | 繁體中文 | [English](README.en.md)

基於 Minecraft 1.21.1（NeoForge）的中國風架空世界模組，圍繞凡人蠱師一至五轉的修行體系，融合探索、養成與角色扮演玩法。

> 目前處於開發階段，具體玩法與數值尚未定型，本文僅說明工程結構、執行環境與依賴關係。

本文譯自[簡體中文版](README.md)；各版本不一致時，以簡體中文版為準。

## 環境要求

| 項目 | 版本要求 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.265 及以上 |
| Java | JDK 21 |
| 模組版本 | 1.0.0（一至五轉） |

## 模組分工

本專案由兩個必須同時安裝的配套模組組成：

| 模組 | 儲存庫 | 職責範圍 |
|---|---|---|
| 蠱真人 Guzhenren | `mcmod-guzhenren`（本儲存庫） | 修行體系（空竅、真元、魂魄、肉體、流派）、蠱蟲與煉蠱、實體、介面、維度旅行與守衛 |
| [蠱界 Gu World](https://github.com/alex-0v0-328/mcmod-guworld) | `mcmod-guworld` | 全部維度、地形生成、水系、生態域與自然生成的獨特結構 |

單獨安裝其中任一模組會導致世界生成或核心機制缺失，兩個模組的 NeoForge 版本基線也須保持一致。

## 前置依賴

| 模組 | 類型 | 版本範圍 | 說明 |
|---|---|---|---|
| [Epic Fight](https://www.curseforge.com/minecraft/mc-mods/epic-fight-mod) | 硬依賴 | `[21.17.3, 21.18)` | 戰鬥與動作系統 |
| [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) | 硬依賴 | `[4.9, 4.10)` | 蠱蟲、野獸與 NPC 骨骼動畫 |
| [Photon](https://www.curseforge.com/minecraft/mc-mods/photon) | 硬依賴 | `[2.2.11, 2.3)` | 像素風粒子特效執行庫（用戶端與伺服端均需安裝） |
| [LDLib2](https://www.curseforge.com/minecraft/mc-mods/ldlib) | 硬依賴 | `[2.2.46, 2.3)` | Photon 與特效組件基礎庫（用戶端與伺服端均需安裝） |
| [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) | 可選聯動 | `[19, 20)` | 物品與配方查看 |

## 安裝與建置

### 遊戲安裝

1. 安裝 Minecraft 1.21.1 與 NeoForge 21.1.265 及以上版本。
2. 將上表中的全部硬依賴模組放入實例的 `mods/` 目錄。
3. 同時放入 `guzhenren` 與 `guworld` 兩個模組 jar。

### 原始碼建置

兩個儲存庫按同級目錄檢出（`mcmod-guzhenren` 與 `../mcmod-guworld`），使用 JDK 21 建置：

```powershell
./gradlew build
```

資料生成與遊戲測試：

```powershell
./gradlew runData
./gradlew runGameTestServer
```
