# 蛊真人 Guzhenren 模组

简体中文 | [繁體中文](README.zh-TW.md) | [English](README.en.md)

基于 Minecraft 1.21.1（NeoForge）的中国风架空世界模组，围绕凡人蛊师一至五转的修行体系，融合探索、养成与角色扮演玩法。

> 当前处于开发阶段，具体玩法与数值尚未定型，本文仅说明工程结构、运行环境与依赖关系。

## 环境要求

| 项目 | 版本要求 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.265 及以上 |
| Java | JDK 21 |
| 模组版本 | 1.0.0（一至五转） |

## 模组分工

本项目由两个必须同时安装的配套模组组成：

| 模组 | 仓库 | 职责范围 |
|---|---|---|
| 蛊真人 Guzhenren | `mcmod-guzhenren`（本仓库） | 修行体系（空窍、真元、魂魄、肉体、流派）、蛊虫与炼蛊、实体、界面、维度旅行与守卫 |
| [蛊界 Gu World](https://github.com/alex-0v0-328/mcmod-guworld) | `mcmod-guworld` | 全部维度、地形生成、水系、生物群系与自然生成的独特结构 |

单独安装其中任一模组会导致世界生成或核心机制缺失，两个模组的 NeoForge 版本基线也须保持一致。

## 前置依赖

| 模组 | 类型 | 版本范围 | 说明 |
|---|---|---|---|
| [Epic Fight](https://www.curseforge.com/minecraft/mc-mods/epic-fight-mod) | 硬依赖 | `[21.17.3, 21.18)` | 战斗与动作系统 |
| [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) | 硬依赖 | `[4.9, 4.10)` | 蛊虫、野兽与 NPC 骨骼动画 |
| [Photon](https://www.curseforge.com/minecraft/mc-mods/photon) | 硬依赖 | `[2.2.11, 2.3)` | 像素风粒子特效运行库（客户端与服务端均需安装） |
| [LDLib2](https://www.curseforge.com/minecraft/mc-mods/ldlib) | 硬依赖 | `[2.2.46, 2.3)` | Photon 与特效组件基础库（客户端与服务端均需安装） |
| [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) | 可选联动 | `[19, 20)` | 物品与配方查看 |

## 安装与构建

### 游戏安装

1. 安装 Minecraft 1.21.1 与 NeoForge 21.1.265 及以上版本。
2. 将上表中的全部硬依赖模组放入实例的 `mods/` 目录。
3. 同时放入 `guzhenren` 与 `guworld` 两个模组 jar。

### 源码构建

两个仓库按同级目录检出（`mcmod-guzhenren` 与 `../mcmod-guworld`），使用 JDK 21 构建：

```powershell
./gradlew build
```

数据生成与游戏测试：

```powershell
./gradlew runData
./gradlew runGameTestServer
```
