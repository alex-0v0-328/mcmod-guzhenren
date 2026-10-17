# 交接：分支 `claude/blissful-tesla-4gax8y`

本文件只服务于这条分支的交接：按第二节把 wiki 更新完、第一节的本地检查通过后，删除本文件。它不是规则，规则仍以 `CLAUDE.md` 为准。Gu World 的同名分支有自己的一份 `HANDOFF.md`，两边一起处理。

## 一、本地先跑的检查

云端会话只能做下面这些，已全部通过：

- 两个仓库编译通过；中间提交 `0f37809` 单独检出后也能编译。
- `runData` 与改动前逐字节一致：语言表合并、注册类精简都不改生成结果。只新增了 `data/minecraft/tags/damage_type/bypasses_invulnerability.json`（四种致死伤害无视图腾）。
- 开服冒烟（GZR 加 Gu World，临时游戏目录）：三个维度加载、南疆强制加载 81 个区块、`/guzhenren` 命令树完整、正常停服，没有报错。
- 不带 Gu World 开服：`/guworld enter guworld:southern_border` 回"未在此服务器加载"，`/guworld enter minecraft:the_nether` 回"不是锚定维度"，都不再抛异常。

云端跑不了：L1 / L2 JUnit、GameTest、客户端。本地按顺序做：

1. 两个仓库都检出 `claude/blissful-tesla-4gax8y`（Gu World 那条已包含 `claude/pensive-bell-rwd5he` 的全部提交）。
2. 运行 `python3 -B tools/check.py`，再运行 `python3 -B tools/check.py --project guworld`。
3. `src/test` 不在 Git 里，下面这些改名和移动要在本地测试里跟进：
   - `gameplay.path.time.PathTimeFlowService` 改为 `gameplay.clock.TimeFlow`。
     - `getRate(Player)` 返回 `double`（自身 × 世界）；原来的整数倍率是 `getSelfRate(Player)`。
     - 纯算术重载改为 `shortenWait(double, int)`、`scale(double, long, RandomSource)`、`scale(double, double)`、`getSteps(double, RandomSource)`。
     - `TimeRateContributor` 也移到 `gameplay.clock`。
   - 心跳步骤多了一个 `Beat` 参数（`gameplay.clock.Beat`）：`BodyService.tickLifespan`、`ApertureEssenceService.regenStep`、`ApertureNourishService.tickNourish`、`MindService.regenStep`。
   - `ModDimensions.AnchoredDimension` 多了第五个组件 `timeRate`。
   - `GuCooldowns.stampCooldownLeft` 现在承认"未来"的冷却戳（时间变慢时），上限是 `TimeFlow.MIN_RATE` 拉长后的窗口。
   - 附件：
     - `PlayerDataService.copy` 删除。克隆继承改由 `ModAttachments.getInherited()` 加 `copyOnDeath` 决定，`onClone` 以 `LOW` 优先级运行。
     - `ApertureStorage.copy()` 删除。
   - 旧存档迁移删除：
     - `Aperture` 去掉 `legacyExtremePhysique`，剩 13 个组件；12 参数便利构造器、`clearLegacyExtremePhysique` 一并删除。
     - `BodyData` 的 `life_form` 解码删除。
     - `PlayerDataService.migratePhysique` 删除。
   - `BodyData.with(Physique)` / `without(Physique)` 新增，`BodyService.copyPhysiques` 删除。
   - 包移动：
     - `gameplay.lifecycle.*` → `lifecycle.*`
     - `gameplay.refinement.*` → `refinement.*`
     - 全部 `Cmd*` 和 `AwakenedGate` → `command.*`
   - 空窍温养付元石改走 `gameplay.aperture.StonePurse`，由 `Guzhenren` 构造器调用 `ApertureNourishService.setStonePurse` 接上。L2 测试如果不经过模组构造器，需要在测试准备阶段自己设置。
   - 语言：`EnUsLanguageProvider`、`ZhCnLanguageProvider`、`ZhTwLanguageProvider` 合并为 `datagen.lang.ModLanguageProvider(output, Language)`。测试里按 provider 类名找语言的地方要改。
   - `MindPool.burstAt()` 不再低于 `max`；`MindData.isOverflowing()` 只看可爆发的池。
   - `core.TossedItems.stow` 新增；`TreasureYellowHeavenGuardEvents.stowTossed` 删除。
   - `TendedGuItem.spendAndGrant` 新增；`PrimevalElderGuItem` 不再覆盖 `feedUnits`、`holdingFood`。
   - `GuItem.isForeignVital` 新增。
   - `ModDataComponents`、`ModEntityTypes`、`ModCreativeTabs` 的注册写法变了，常量名不变。
   - `/guworld enter` 的目标维度未加载时改用新键 `guzhenren.command.travel.missing_dimension`，不再借用 `unknown_dimension`。
4. 把结果报告给 Alex，等 Alex 合并。

## 二、wiki 修改建议

云端看不到 wiki，下面按主题写，请对应到实际页面。数值仍以代码为准，wiki 只引用。

1. **时间流速（宙道 / 自身时间）**
   - 总倍率 = 自身倍率 × 世界倍率。
   - 自身倍率：两更、三更等宙道效果相加，最低 1。
   - 世界倍率：维度的 `timeRate` × 区域 `TimeFlowArea`，可以低于 1（时间变慢）。目前所有维度都是 1，区域只留了接口，离线补算也只留了接口。
   - 总倍率下限 `TimeFlow.MIN_RATE` = 0.125（八分之一）。
   - 小数倍率用随机进位，期望值精确；整数倍率不掷随机，结果与以前完全一致。
   - 心跳每秒只读一次时钟（`Beat`），真元、念、寿元、温养都用这一次的读数。
2. **寿元**
   - 只扣在线时间：登录时重新锚定计费。
   - 蛊的按天饮食照旧按离线天数补算。
   - 僵尸三段规则：常人 → 半僵时寿元冻结、不改数值，半僵 → 常人只解冻；常人直接变彻底僵尸、或半僵 → 彻底僵尸时，寿元设为 1 年；彻底僵尸 → 常人时，这 1 年保留。
3. **致死判定**：寿尽、魂崩、脑碎、压力自爆这四种无视不死图腾，创造模式照旧免疫；十绝之灾和本命蛊丢失不受影响。
4. **空窍压力自爆半径**：一转 32 格、五转 96 格，大力真武体再加 16 格，最大 112 格。
5. **脑海**：意志、情绪池满了不再判定脑海炸裂；思绪池的爆发阈值不低于上限，所以池满不会致死。
6. **僵尸蛊复发**：半僵结束后 5 分钟内再用僵尸蛊，会永久变僵（这个窗口以前实际不生效）。
7. **本命蛊**：背包满时丢弃，放不下的本命蛊悬停在原地，并且永不消失。
8. **蛊的喂食**：元老蛊取石时不再吃掉取出的元石；闲情蛊、恶念蛊这类一次性蛊被消耗前不再吃副手食物。
9. **攻击力刷新**：药水效果增减后当 tick 生效，不再要等下一次心跳。
10. **存档**：空窍体质和 `life_form` 的旧格式迁移已删除。
11. **维度传送命令** `/guworld enter`：目标不是锚定维度时提示"不是锚定维度"；目标是锚定维度但本服没有加载（没装 Gu World）时提示"未在此服务器加载"。
12. **维护说明**
    - 包结构：`lifecycle`、`refinement` 成为顶层包；命令全部平铺在 `command`；时钟在 `gameplay.clock`；`core.TossedItems`；`StonePurse`。
    - 依赖方向：gameplay 不再依赖 command、lifecycle、refinement；对 item 的依赖只剩空窍储物一处。
    - 语言表：一个文件、三列，繁中留空就自动转换；只有用词不同时才手写繁中（如"服务器加载"写成"伺服器載入"）。转换表 `TraditionalGlyphs` 遇到没收录的字会让 `runData` 失败，新文案里的新字要先加进表里。
    - 附件继承：在 `ModAttachments` 注册处声明。

## 三、待办

1. 空窍储物 `gameplay.aperture.storage` 与 `item` 的双向依赖，留到本地有测试时拆：首个空窍插入改为事件，储物的逐秒结算和菜单上移到组合层。
2. 审查发现、本分支没做的（多数需要客户端或本地测试验证）：
   - GUI 公共控件基类，四个容器界面共用，约少 120–160 行。
   - 实体：`RestGoal` / `GrazeGoal` 合并、`strikeTarget` 提取、冷却改为服务端私有状态（现在每 tick 同步给所有追踪者）、追击寻路节流。
   - 超过 300 行的类拆分：`TendedGuItem`（504 行）、`ModItems`（570 行）、`AperturePressureExplosionTask`（319 行，顺便核对每 tick 方块预算）。
   - `ApertureStorageTick` 可能的 O(N²) 复制。
   - 崩溃后残留的 `HEALTH_CARRY` 可能回血。
   - 切换十绝体命令可反复刷人道气；`awaken(0)` 会生成坏空窍。
   - 命名规则 3：大量读方法不以动词开头。
3. Gu World 的语言表也可以改成同样的三列单表，与 GZR 一致。
