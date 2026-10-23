# 00 脚手架 规格

目标：空的 `guzhenren` mod 带着 52 个整合包 mod 在 Forge 1.20.1-47.4.10 上启动到标题画面；后续模块直接往里搬代码。

## 问题

| # | 问题 | 依据 |
|---|---|---|
| P1 | MDK 用 ForgeGradle 6 + official 映射；主线用 ModDevGradle 2.0.148 + Parchment，构建脚本和参数名都对不上 | MDK `build.gradle`、主线 `build.gradle` |
| P2 | 1.20.1 是 SRG 混淆环境：本地 jar（Epic Fight、GeckoLib）不能直接 `compileOnly files(...)`，要重映射 | 主线 `build.gradle` 依赖段 |
| P3 | Java 17：mixin `compatibilityLevel` 是 `JAVA_21`，工具链也是 21 | `guzhenren.mixins.json`、主线 `build.gradle` |
| P4 | 元数据文件名 `neoforge.mods.toml` → `mods.toml`，依赖版本区间全要换 | `src/main/templates/META-INF/neoforge.mods.toml` |
| P5 | Curios 1.20.1 是 5.x，maven 坐标是 `curios-forge`；主线是 9.5.1 `curios-neoforge` | 主线 `gradle.properties` |
| P6 | 两个兼容性没验证的 mod：ImmediatelyFast 1.5.5（标的是 1.20.x）、kotlinforforge 4.12.0 | Modrinth 元数据 |
| P7 | 新工程还没有 git，也没有 `.gitignore`、CI、check 工具 | — |
| P8 | ForgeGradle 6 没有 MDG 的 `unitTest`：L2 `test`（要 Minecraft 类和注册表的 JUnit）跑不起来；L1 纯 JUnit 和 L3 GameTest 不受影响 | 主线 `build.gradle` `unitTest { enable() }` |

## 规格

| 项 | 取值 |
|---|---|
| 构建插件 | ForgeGradle `[6.0,6.2)`（MDK 自带），Gradle 8.8 |
| Forge | `1.20.1-47.4.10` |
| 映射 | Parchment `2023.09.03-1.20.1`（`mappings channel: 'parchment'`），插件 `org.parchmentmc.librarian.forgegradle` `1.+` 放在 ForgeGradle 之后；`settings.gradle` 的 `pluginManagement.repositories` 加 `https://maven.parchmentmc.org`；Librarian 给 Minecraft 参数名加 `p` 前缀（`pLevel`），覆写方法时以此为准 |
| Mixin | `org.spongepowered.mixin` `0.7.+` 插件 + `org.spongepowered:mixin:0.8.5:processor`，refmap `guzhenren.refmap.json` |
| Java | 工具链 17，mixin `JAVA_17`；Gradle 自己跑在 JDK 21 上，17 由 foojay 自动下载 |
| mod id / 包名 / 版本 | `guzhenren` / `net.alex.guzhenren` / `1.0.0`，与主线一致（两版不会同时装） |
| 源码集 | `main`、`test`（L2）、`pureTest`（L1）、`gametest`（L3），目录与主线相同，先留空 |
| run 配置 | `client`、`server`、`gameTestServer`、`data`，参数照搬主线（`forge.enabledGameTestNamespaces`） |
| 编译依赖 | Epic Fight 20.14.17、GeckoLib 4.8.4 走 `compileOnly fg.deobf(files(...))`（本地 jar，运行时由 `run/mods` 加载）；Curios `5.14.1+1.20.1` 走 Illusive maven |
| 运行时 | `run/mods` 的 52 个 jar 由游戏自己加载，不进 Gradle 依赖 |
| mods.toml | 必需：forge `[47,)`、minecraft `[1.20.1,1.21)`、epicfight `[20.14,20.15)`、geckolib `[4.8,4.9)`；可选：jei `[15,16)`、curios `[5,6)` |
| 入口类 | 只留 `@Mod` 构造函数、`MOD_ID`、`LOGGER`、`id()`（改成 `new ResourceLocation`） |
| Git | 本地 `git init`，`.gitignore` 照搬主线；远端仓库等你在 GitHub 建好再接 |

## 验收

1. `./gradlew build` 通过。
2. `runClient` 到标题画面并稳定 20 s，日志里 53 个 mod（52 + guzhenren）全部加载，没有崩溃、没有缺少前置的报错。
3. `runServer` 能启动并正常停止（客户端专用 mod 在服务端被跳过也不报错）。

P6 任一失败 → 换成该 mod 更老的 1.20.1 版本再试，或者删掉，结果写进这份规格。

## 不在本步

注册表、网络、业务代码（模块 1 起）；check.py / commit_push.py 移植（模块 7）；CI（接好远端后）。

## 已定

- 构建插件：ForgeGradle 6（Alex 定）。
- `port/` 只留本地，gitignore。

## 待定

- P8：L2 测试到模块 7 再定（改写成 GameTest，或删掉对 Minecraft 的依赖后降为 L1）。

## 计划

1. `settings.gradle`：加 Parchment 仓库，`rootProject.name = 'mcmod-guzhenren-forge'`。
2. `gradle.properties`：mod 信息与主线一致，Parchment、Curios 版本。
3. `build.gradle`：Librarian、MixinGradle、四个源码集、run 配置、依赖、mixin 处理器。
4. `META-INF/mods.toml`、`guzhenren.mixins.json`（空 mixin 列表，`JAVA_17`）、入口类 `net.alex.guzhenren.Guzhenren`；删 MDK 的 example 包。
5. `.gitignore` 照搬主线（加 `/port/`），`git init`。
6. 验收：`build` → `runClient` → `runServer`。Gradle 用 `~/.local/share/jdks/jdk-21.0.12.1+1` 跑。

子代理分配：无，全部在主会话做（文件少、彼此依赖）。

## 实施中的偏差

- 整合包 jar 从 `run/mods` 移到 `pack/mods`（gitignore）。原因：ForgeGradle 开发环境里 Minecraft 用映射名，SRG 名的 mod jar 直接放 `run/mods` 会 `NoSuchMethodError`；改由 `flatDir` + `runtimeOnly fg.deobf("blank:<文件名>")` 逐个重映射后加载，run 配置开 `mixin.env.remapRefMap`。`run/mods` 留空。
- Epic Fight、GeckoLib 的编译依赖同样从 `pack/mods` 经 `fg.deobf` 取，不再用 `files(...)`。
- mixin 配置靠 MixinGradle 写进 jar 清单 `MixinConfigs`（Forge 1.20.1 不认 `mods.toml` 的 `[[mixins]]`）。
- 开发环境排除 `dev_excluded_mods`（`gradle.properties`）：ImmediatelyFast、dummmmmmy、IMBlocker、ModernFix 的 mixin 配置没有 refmap，直接写 SRG 目标，开发环境里注入失败（前三个崩溃，ModernFix 部分注入导致服务端卡在 Preparing start region）；entityculling 内嵌的 TRansition 库不会被 `fg.deobf` 重映射。五个都只在正式实例里验证。
- 服务端排除 `client_only_mods`（`-PserverRun`）：Oculus、JECharacters、RyoamicLights、Better Lock On 在专用服务端加载客户端类崩溃；正式服务器不装它们。
- `runServer` 接 `standardInput`，控制台 `stop` 能传到开发服务端。
- P6 结论：kotlinforforge 4.12.0 正常；ImmediatelyFast 1.5.5 在开发环境无法判定，待正式实例。

## 验收结果

| 项 | 结果 |
|---|---|
| `build` | 通过，jar 清单带 `MixinConfigs: guzhenren.mixins.json` |
| `runClient` | 53 个 mod（含 guzhenren、curios），18.9 s 到标题画面，稳定 20 s |
| `runServer` | 49 个 mod，`Done (2.208s)`，`stop` 后三个维度存盘退出 |

## 遗留

- 正式实例验证：五个开发环境排除的 mod + 整套 52 个，在真实 Forge 1.20.1 客户端/服务端里跑一次。
- MDK 自带的 `CREDITS.txt`、`changelog.txt`、`README.txt`、`LICENSE.txt` 待换成主线的 README / LICENSE。
