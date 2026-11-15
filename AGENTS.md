GZR 规则正文：读 `AGENTS.md` 的工具（Codex、opencode 等）直接读本文件，Claude Code 经 `CLAUDE.md` 导入。会话只从 GUZHENREN 根目录打开；GUWORLD 不设规则文件，其约束见本文件 GUWORLD 一节。

## Overview

- 项目：Minecraft Mod 开发；游戏版本 1.21.1，加载器 NeoForge，模组版本 1.0.0（一至五转）。
- 语言：回复与 wiki 用简体中文（zh-CN）；代码、commit、Skills 用美式英语；注释、规则用简体中文。
- 分支：三个仓库均用 main，禁止自行切换分支。
- 工具：GitHub MCP（只查询）、JetBrains MCP、Context7 MCP（只认 NeoForge 1.21.1、GeckoLib 4）。

## Map

路径相对 GUZHENREN 根目录；新机器按同一布局 clone。

| 路径                              | 内容                                                                                               |
|-----------------------------------|----------------------------------------------------------------------------------------------------|
| `./`                              | GUZHENREN MOD，`alex-0v0-328/mcmod-guzhenren`（公开）                                              |
| `../mcmod-guworld/`               | GUWORLD MOD，`alex-0v0-328/mcmod-guworld`（公开）                                                  |
| `../mcmod-wiki-gzr/`              | wiki，`alex-0v0-328/guzhenren-mod-wiki`（私有）：`开发向-guzhenren/`、`开发向-guworld/`、`玩家向/` |
| `../../Temps/`                    | 临时目录：`guzhenren/`、`guworld/`                                                                 |
| `../assets/`                      | 资产源文件，只读                                                                                   |

## Boundaries

- 仅开发凡人蛊师一至五转，不自行扩展升仙、仙蛊或六至九转；宝黄天外观维度、四地区地形原型按 wiki 范围处理。
- GUWORLD：维度、地形、生物群系、自然生成的独特结构；GUZHENREN：其余内容、维度旅行、守卫、聚落。
- 灵泉世界生成归 GUWORLD；方块、流体、物品、渲染归 GUZHENREN。
- GUWORLD 依赖 GUZHENREN 并在其后加载；无相互编译依赖，跨模组只用 ID，共用 ID 修改检查两仓库。
- 写前重读，保留 Alex 及其他会话改动；删前核查；禁止全仓清理、改动存档和测试证据；资产源文件（`../assets/blockbench-gzr/`、`../assets/aseprite-gzr/`）只读，导出进 mod。
- 两个 mod 与 wiki 靠 Git 回滚；无 Git 的 `.env.local` 和临时目录证据，删除时移入回收站；不碰 `run/mods`。
- 文件名与 public 类名大小写一致；只改大小写用 `git mv`，Windows 文件系统不区分大小写。

## GUWORLD

- mod id `guworld`，包 `net.alex.guzhenrenworld`，不得用 `net.alex.guzhenren` 命名空间。
- GUZHENREN 只按 level key 引用维度（`ModDimensions`），GUWORLD 只按 id 引用方块（`GuzhenrenBlocks`）；改名两仓库同任务改。
- 运行与 `runData` 从 `../mcmod-guzhenren` 加载其 jar 和 `run/mods`（`-PguzhenrenDir` 可改），先构建 GUZHENREN。
- 运行读已提交的 `src/generated`：改 provider 后先跑 `data` 再跑 `gametest`。
- 代码规范同《维护说明#代码规范》；结构、测试与移植见《Gu World 维护说明》。
- 本仓库 `tools/` 同时驱动 GUWORLD，检查、提交均加 `--project guworld`。

## Workflows

技能：`.claude/skills/<技能名>/SKILL.md`。

1. SessionStart 注入 `alex-constitution`、`i-have-adhd`；未正确注入则先读对应技能。
2. 开工查 `git status`、`git diff`，确认环境和现有改动；新机器首个会话先做完《维护说明#待办与计划》的「新机器」条目。
3. 非小改动先做第一性原理分析、环境审查、实施路径确认；计划分阶段自查，按 `alex-constitution` 向 Alex 提问，仅问未明确的目标、约束、选择。
4. 新增蛊、实体、机制先读 `new-content` 和对应 wiki；审查或确认行为先读 `review-change`，仅审查不改文件。
5. 修复先查源码、日志与复现，定位原因后改；无法复现则报告证据与不确定项，不让 Alex 重复已报现象。能写回归先写回归；红色快照断言由 Alex 裁定，不为通过而改。
6. 以模组作者和整合包玩家视角判断；方块、实体、粒子规模克制，报告具体数值（格、秒）。

## Checks and shipping

改后用 `python -B tools/local_checks.py` 检查受影响阶段；`--list` 列阶段，GUWORLD 加 `--project guworld`。数据生成 provider 改动必跑 `data`；通过后仅因新改动、失败或未决问题补查。

你负责 Gradle、自动客户端/多人检查、IDEA 同步验证；Alex 验收视觉与手感。两仓库共用 `../../Temps/guzhenren/gradle.lock`；手动 Gradle 也须持锁（`python -B tools/project_runtime.py gradlew.bat <任务>`），禁止删锁。

日志按运行隔离；读取、报告后只清理自己的日志，保留诊断证据。
代码修改后、wiki 编辑前读 `wiki-sync`；完成时同步受影响 wiki，报告改动、检查结果、未验证项、遗留问题。

【原则】commit 标题 Alex 指定则照用，否则代理直接选推荐项，不再询问；禁止 AI 共同创作者、署名及尾注。
提交推送先读 `commit-push`，mod 用 `python -B tools/commit_push.py`，wiki 按该技能直接提交推送；Alex 要求结束会话时读 `exit-session`。推送后 CI 变红：先 git revert 该提交并推送，使 main 回绿，再修复、复查、重新推送；禁止强推。

## Conventions

- 遵循现有代码、术语、`.editorconfig`；注释只解释不明显原因，禁止批量格式化或全项目 IDEA 清理。蛊数值以 `ModItems` 的 `GuSpec` 为准，角色数值以枚举和服务为准。
- 临时文件放临时目录的 `<项目>/<本任务唯一目录>/`，验证后只清理自己的文件；构建产物、Gradle 缓存位置不变。新脚本先说明用途；临时脚本放任务目录，长期脚本放 `tools/`，补用途、测试、地图入口。
- 载体分工见《维护说明#写作与来源》：开发向写是什么与为什么，类级 Javadoc 写怎么做与小心什么，玩家向记版本快照，测试不在 wiki 列清单；规则与 Skills 存代理指令；文档只写当前决定、理由、约束、待办，历史留给 Git。
- 待办：《维护说明#待办与计划》；客户端验收：《玩家资料维护#当前悬着项》；待定设计：对应页面待定部分。Markdown 沿用表格、标题、引用规范，用 `tools/doc_conventions.py` 检查；Obsidian 不另设与页面名相同的 H1。
- 规则只改本文件，`CLAUDE.md` 只放 Claude Code 专属段。本机文件不进 Git：`.env.local`、`.mcp.json`、`.claude/settings.local.json`、`CLAUDE.local.md`、`AGENTS.override.md`。改常驻技能或 `.claude/hooks/session_start.py` 后，用 `python -B tools/deploy_harness.py` 部署，`tools/test_deploy_harness.py` 检查副本。
- memory 只存 Alex 偏好、跨会话进行中状态与外部指针；设计事实进 wiki 后 memory 只留指针。

## Codex 与 opencode

- Codex 读 `.codex/config.toml`（hook、MCP），opencode 读 `opencode.json`（常驻技能、MCP）；一个 harness 的配置不证明另一个可用。
- 常驻技能未注入时，先读 `.claude/skills/alex-constitution/SKILL.md`、`.claude/skills/i-have-adhd/SKILL.md`。
- 仅在 Alex 授权时用本运行时的子代理，模型由 Alex 选。
