# 交接：分支 `claude/blissful-tesla-4gax8y`

本文件只服务于这条分支的交接：按第二节把 wiki 更新完、第一节的本地检查通过后，删除本文件。它不是规则，规则仍以 `CLAUDE.md` 为准。Gu World 的同名分支有自己的一份 `HANDOFF.md`，两边一起处理。

## 一、本地先跑的检查

云端会话只能做下面这些，已全部通过：

- 两个仓库编译通过；中间提交 `0f37809` 单独检出后也能编译。
- `runData` 与改动前逐字节一致：语言表合并、注册类精简都不改生成结果。只新增了 `data/minecraft/tags/damage_type/bypasses_invulnerability.json`（四种致死伤害无视图腾）。
- 开服冒烟（GZR 加 Gu World，临时游戏目录）：三个维度加载、南疆强制加载 81 个区块、`/guzhenren` 命令树完整、正常停服，没有报错。
- 不带 Gu World 开服：`/guworld enter guworld:southern_border` 回"未在此服务器加载"，`/guworld enter minecraft:the_nether` 回"不是锚定维度"，都不再抛异常。
- 第二轮重构（界面共用件、实体、长类拆分、两个 bug 修复）编译通过，`runData` 逐字节不变。

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
   - `MaxHealthService.restoreCarried` 恢复后移除 `HEALTH_CARRY`（`removeData`），不再保留到下次登录。
   - `ApertureService.awaken(player, 0)` 改为随机掷一次资质，不再开出底数为 0 的空窍。
   - 实体：
     - `ActionEntity.Cooldown` 变为 `(maxTicks, saveKey)`；攻击冷却不再是 `SynchedEntityData`，存档键不变。
     - `ActionEntity.strikeTarget(reach, damage, knockback, upward)` 新增，取代 `BeastEntity.performSwipe`、`WildBoarEntity.performToss` 和熊的重击判定。
     - `BeastEntity.RestGoal`、`WildBoarEntity.GrazeGoal` 删除，改用 `entity.ai.StandStillGoal`。
     - `CombatGoal` 追击按原版近战节奏重算路径（见第二节）。
   - 空窍压力爆炸：
     - 按列的计算移到 `gameplay.aperture.CraterShape`：`getColumnJitter`、`getColumnRadius`、`getColumnFloorY`、`getIceTier`、`getGoldTier`、`isLavaColumn`、`isGoldColumn`、`isPowderSnowColumn`。
     - `ringOuterRadius` 改名为 `getRingOuterRadius`，新增 `getRingReach`。
     - `BLOCK_BUDGET` 改为 16,384。
   - 客户端界面：
     - 新增 `Widgets`（面板、格子、按钮、返回键、命中判定、容器按钮）、`RecipeGhosts`、`PoolBars`。
     - `RefinementScreen.result` / `option` 改为 `RecipeGhosts.getResult` / `getOption`。
     - `RefinementScreen.CELL` 改为 `Widgets.CELL`，`LEGEND_TEXT` 移到 `ModPalette`。
     - `ModPalette` 新增 `MODAL_FILL`、`LEGEND_TEXT`、`SHORT`、`SHORT_OUTLINE`。
     - `InfoRowRenderer.getRows`、`getApertureGroups` 接手 `PlayerInfoScreen` 原来的行构建。
   - `RefinementMenu` 的自动填充移到 `refinement.RecipeAutofill.fill(recipe, input, player)`。
   - `DataGenerators.gatherData` 拆成客户端、服务端两组，顺序不变。
4. 进客户端看四个界面（B 面板各标签、空窍储物翻页和返回键、炼蛊的按钮、虚影和池条、魂魄交易）和两个弹窗。界面改成共用 `Widgets` 后应当像素不变，云端看不了。
5. 把结果报告给 Alex，等 Alex 合并。

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
12. **离线血量**：超过 20 的血量只在正常退出到下次登录之间保留；会话中途崩溃时，读档最多恢复到 20，不会再被更早一次退出时的血量补满。
13. **希望蛊**：没掷出底数就使用时，开窍时补掷一次资质。
14. **野兽追击**：熊、虎、野猪追击时按原版近战怪的节奏重算路径：目标在动时每 4–10 tick 一次，超过 16 格再加 5、超过 32 格再加 10，寻路失败再加 15；做完动作停下后立即重算。
15. **空窍压力自爆挖坑**：每 tick 每阶段最多处理 16,384 个方块位置，半径 112 的坑（约 590 万个位置）约 18 秒成形，不再卡服。
16. **维护说明**
    - 包结构：`lifecycle`、`refinement` 成为顶层包；命令全部平铺在 `command`；时钟在 `gameplay.clock`；`core.TossedItems`；`StonePurse`。
    - 依赖方向：gameplay 不再依赖 command、lifecycle、refinement；对 item 的依赖只剩空窍储物一处。
    - 语言表：一个文件、三列，繁中留空就自动转换；只有用词不同时才手写繁中（如"服务器加载"写成"伺服器載入"）。转换表 `TraditionalGlyphs` 遇到没收录的字会让 `runData` 失败，新文案里的新字要先加进表里。
    - 附件继承：在 `ModAttachments` 注册处声明。
    - 界面：所有界面共用 `client.screen.Widgets`；两个以上界面共用的颜色才进 `ModPalette`。
    - 实体：休息、吃草这类"站着不动"的姿势共用 `entity.ai.StandStillGoal`；命中帧打击走 `ActionEntity.strikeTarget`；攻击冷却只在服务端。
    - 超过 300 行而不拆的类，在类 Javadoc 里写明原因：`ModItems`、`ModLanguageProvider`（数据表）、`RefinementScreen`、`PlayerInfoScreen`、`RefinementMenu`、`ApertureNourishService`、`TendedGuItem`、`ActionEntity`、`BeastEntity`。

## 三、待办

1. 空窍储物 `gameplay.aperture.storage` 与 `item` 的双向依赖，留到本地有测试时拆：首个空窍插入改为事件，储物的逐秒结算和菜单上移到组合层。同一处的 `ApertureStorageTick` 每改一格就复制整页列表（O(N²)），拆的时候一起改成整页写回一次，但要保留"蛊自己的维护可能写入储物"那条约束。
2. 命名规则 3（方法名以动词开头）：约两三百个名词或形容词开头的方法（如 `maxEssence`、`talent`、`refilled`、`action`、`cooldown`），在本地用 IDEA 的重命名重构处理，测试会一起跟着改。记录类自动生成的组件访问器不算。
