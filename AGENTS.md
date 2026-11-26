> **受众**：所有 AI 代理（Codex、opencode 直读本文件；Claude Code 经 `CLAUDE.md` 导入）。
> **定位**：GUZHENREN 与 GUWORLD 共用常驻约束与入口索引；会话仅从 GUZHENREN 根目录打开，GUWORLD 不单设规则文件。
> **裁决顺序**：Alex 当轮指令 > 常驻技能（`alex-constitution`、`i-have-adhd`）> 本文件 > 按需技能（`.claude/skills/`）> Wiki。

## 项目与路径

| 项 | 值 |
| --- | --- |
| 技术栈 | Minecraft 1.21.1 / NeoForge / Mod 1.0.0（一至五转） |
| 分支 | 三仓均为 `main`（禁自行切分支） |
| 语言 | 回复、Wiki、注释、规则：简体中文；代码、commit、Skills：美式英语<br>游戏内仅简繁中文（手写简体），英文仅限翻译键、注册 ID、类名 |
| MCP | GitHub（只查询）、JetBrains、Context7（只认 NeoForge 1.21.1、GeckoLib 4） |
| `./` | GUZHENREN，`alex-0v0-328/mcmod-guzhenren`（公开）：<br>主玩法、实体、方块/流体/物品/渲染、维度旅行、守卫、聚落 |
| `../mcmod-guworld/` | GUWORLD，`alex-0v0-328/mcmod-guworld`（公开）：mod id `guworld`，<br>包 `net.alex.guzhenrenworld`（维度、地形、群系、自然独特结构、灵泉生成） |
| `../mcmod-wiki-gzr/` | Wiki，`alex-0v0-328/guzhenren-mod-wiki`（私有）：`开发向-guzhenren/`、`开发向-guworld/`、`玩家向/` |
| `../../Temps/` | 临时目录：`guzhenren/`、`guworld/`；任务临时文件与脚本放 `<项目>/<任务唯一目录>/` |
| `../assets/` | 资产源文件（`blockbench-gzr/`、`aseprite-gzr/`），只读，导出进 mod |

## 红线与约束

| 维度 | 必须遵守（DO） | 严禁（DON'T） |
| --- | --- | --- |
| 开发范围 | 仅做凡人蛊师一至五转；宝黄天外观、四域地形原型、狐仙福地按 Wiki 范围处理 | 自行扩展升仙、仙蛊或六至九转 |
| 双仓耦合 | GUWORLD 在 GUZHENREN 后加载，跨仓仅用 ID（`ModDimensions`/`GuzhenrenBlocks`）<br>共用 ID 改名同任务双仓齐改；GUWORLD 先构主模组再跑 `runData`（`-PguzhenrenDir` 可覆写） | 引入双仓相互编译依赖；在 GUWORLD 用 `net.alex.guzhenren` 命名空间 |
| 文件与安全 | 写前重读，保留 Alex 与他会话 WIP；删前核查；无 Git 的 `.env.local` 与临时证据删入回收站<br>改动致旧存档不可用（ID 改名、世界生成变更等）时，推送后直接把 `run/saves` 受影响存档删入回收站<br>文件名与 public 类名大小写一致（仅改大小写用 `git mv`） | 全仓清理、批量格式化、全项目 IDEA 清理<br>迁移或兼容旧存档；改动其余存档、测试证据、`run/mods` 或只读资产源 |
| 数值与规模 | 蛊数值以 `ModItems` `GuProperties` 为准，角色数值以枚举与服务为准<br>克制方块/实体/粒子规模，报告具体数值（格、秒） | 凭印象编造数值；仅用模糊词描述规模 |
| 排障与测试 | 先查源码、日志并复现再改；能写回归先写回归；无法复现时报告证据与不确定项 | 让 Alex 重复已报现象；为通过擅改红色快照断言（归 Alex 裁定） |
| 规则与部署 | 规则只改本文件（`CLAUDE.md` 只放专属段）；改常驻技能或 `.claude/hooks/session_start.py` 后<br>跑 `python -B tools/deploy_harness.py` 并验 `tools/test_deploy_harness.py` | 提交本机文件（`.env.local`、`.mcp.json`、`.claude/settings.local.json`、<br>`CLAUDE.local.md`、`AGENTS.override.md`） |
| 提交与 CI | 经 `python -B tools/commit_push.py` 验证提交；Alex 未指定标题时直选推荐项<br>推送后 CI 变红先 `git revert` 推回绿，再修、复查、重推 | 手工提交绕过验证、强推、带 AI 共同创作者/署名/尾注 |
| Harness | 未注入常驻技能时先读 `.claude/skills/alex-constitution/SKILL.md` 与 `i-have-adhd/SKILL.md`<br>仅在 Alex 授权并选模型后调用子代理 | 用 `.codex/config.toml` 与 `opencode.json` 互相证明对方可用 |

## 流程与门禁

| 阶段 | 必做动作 | 交付 / 验收标准 |
| --- | --- | --- |
| 开工 | 查 `git status`、`git diff`；新机器首会话先完成《维护说明#工具与运行环境》的新机器初始化 | 明确当前 WIP 与环境状态 |
| 方案 | 非小改动先做第一性原理分析、环境审查、实施路径确认；按 `alex-constitution` 仅问未明项 | 目标、边界、约束与阶段计划已对齐 |
| 实现与生成 | 新增蛊/实体/机制先读 `new-content` 与对应 Wiki；改 datagen provider 后先跑 `data` 再跑 `gametest` | 符《维护说明#代码规范》与 `.editorconfig`；`src/generated` 已同步 |
| 检查与锁 | 跑 `python -B tools/local_checks.py` 受影响阶段（`--list` 列阶段，GUWORLD 加 `--project guworld`）<br>手动 Gradle 经 `python -B tools/project_runtime.py gradlew.bat <任务>`<br>持 `../../Temps/guzhenren/gradle.lock` | 自动化检查与 IDEA 同步通过；不删锁；读报后只清己方日志与临时文件<br>视觉与手感交 Alex 验收；通过后仅因新改动/失败/未决项补查 |
| 审查 | 先读 `review-change` | 仅报告带证据的缺陷，不改任何文件 |
| 文档与脚本 | 代码改后/Wiki 编前读 `wiki-sync`；用 `tools/doc_conventions.py` 校验（Obsidian 禁同文件名 H1）<br>新脚本先说明用途：临时脚本放任务目录，长期脚本进 `tools/` 并补用途、测试、入口 | 受影响 Wiki 已同步（按《维护说明#写作与来源》分工）<br>报告改动、检查结果、未验证项与遗留问题 |
| 提交与收尾 | 提交推送读 `commit-push`；结束会话读 `exit-session` | `commit_push.py` 在导出树验证并推绿（不占工作区 Gradle 锁） |

## 入口索引

| 主题 | 权威位置 |
| --- | --- |
| 载体分工与写作规范 | 《维护说明#写作与来源》 |
| 代码规范与架构约束 | 《维护说明#代码规范》 / 《维护说明#代码结构》 |
| Gu World 结构、测试与移植 | 《Gu World 维护说明》 |
| 待办与计划 / 客户端验收 / 待定设计 | 《维护说明#待办与计划》 / 《玩家资料维护#当前悬着项》 / 各专题待定节 |
| Memory 分工 | 仅存 Alex 偏好、跨会话进行中状态与外部指针；设计事实进 Wiki 后仅留指针 |
