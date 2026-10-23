# 可移植性回顾：模块 4

目标同前：主线补接缝、改调用方，让模块 4 搬进来的文件两端逐字一致；Forge 端跟着改。现状：共享 191 个文件，125 个一致。

## 发现

| # | 问题 | 本轮代价 | 改法（两端同改） | 主线波及 |
|---|---|---|---|---|
| R14 | 物品数据直接用 `stack.get/set/has/remove/getOrDefault(ModDataComponents.X.get())` | 12 个物品与服务文件只差这些调用 | 主线 `ModDataComponents` 补同名静态读写 `get / getOrDefault / set / has / remove(stack, ModDataComponents.X, …)`，键仍是数据组件；调用方改走它 | 29 处 8 个文件 + 测试 |
| R15 | 物品提示与使用时长的覆写签名随版本变（`TooltipContext`、`getUseDuration(stack, entity)`） | `GuItem` 与 3 个子类不同 | 主线 `GuItem` 接管 `appendHoverText`，子类改覆写 `appendTooltip(stack, @Nullable Level, tooltip, flag)`；`getUseDuration(stack, entity)` 的逻辑改名 `chargeDuration`，加载器方法只转发 | 4 个文件 + 1 个测试 |
| R16 | 两版都有的写法没选：`hasInfiniteMaterials()`（1.21 才有）、方块覆写的 `protected`（1.20.1 要 `public`，1.21 两者都行） | 5 + 2 个文件不同 | 主线改 `getAbilities().instabuild`、覆写改 `public` | 7 个文件 |
| R17 | `PlayerTickEvents` 把心跳编排（十几步服务调用的顺序）和 tick 事件处理写在一个类里 | 整个文件只能当胶水，编排逻辑两端各一份 | 编排挪进 `lifecycle/Heartbeat.beat(player)`（两端相同），`PlayerTickEvents` 只转交 | 1 个文件 + 新增 1 个 |
| R18 | 主线 GameTest 用 1.21 才有的 `GameTestHelper.assertValueEqual`（277 处 11 个文件），玩家构造用 1.21 的 `CommonListenerCookie` | GameTest 不能逐字复制，本轮手改 2 个 | 主线测试改走 `GameTestAsserts.assertValueEqual(helper, …)`（主线转发给 helper），玩家一律经 `GameTestPlayers`；测试同时改走 R14/R15 的接缝 | 11 个测试文件 + 新增 1 个 |
| R19 | `CuriosDataContractTest` 断言玩家实体的槽位列表**顺序**；Curios 5 的数据生成器用集合写出，顺序不同。Curios 界面按槽位的 `order` 字段排列，实体列表顺序在游戏里无意义 | Forge 端这条测试红，暂未搬 | 改为断言槽位**集合**相同（顺序无关），两版都过——这是改一条快照断言的意图，需要你点头 | 1 个测试 |
| R20 | 1.20.1 纯测试里没有引导 Minecraft，`SecondApertureTest`（读物品类）、`ModDimensionsTest`（读注册表）报 "Not bootstrapped" | 2 个 L1 不能搬 | Forge 端 `pureTest` 加一个 JUnit 扩展在首次用到时 `SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();`（主线 MDG 的单元测试已自动引导，主线不动） | Forge 端 1 个测试工具类 |

不处理：datagen 14 个文件只差 provider 基类所在的包名（`net.neoforged…` / `net.minecraftforge…`）和 1.20.1 的构造参数，属于加载器胶水；`ModDatapackProvider` / `DataGenerators` 少的生物群系修饰随实体模块补回。

做完 R14–R18，本模块不同文件从 66 降到约 45（只剩接缝、注册、事件胶水、datagen、几处方块与触发器 API）。

## 做法

同前：主线按 `rules/modification.md`、wiki 同步、`check.py`、`commit-push`；Forge 端换成同源文件后跑 `build`、L1、GameTest、`runData` 漂移、冒烟。

## 结果

| 项 | 结果 |
|---|---|
| R14–R16 | 主线 `ModDataComponents` 静态读写、`GuItem.appendTooltip` / `chargeDuration`、`instabuild`、方块覆写 `public`；11 个物品与服务文件从 Forge 端回拷，`ChargeHud` 改读 `chargeDuration` |
| R17 | `lifecycle/Heartbeat.beat` 两端相同；`PlayerTickEvents` 只转交 |
| R18 | 主线 `GameTestAsserts` 转发给 helper，277 处改走它；测试的数据组件与时长走接缝；玩家只经 `GameTestPlayers`（两端 API 相同，只差构造玩家的内部） |
| R19 | `CuriosDataContractTest` 改为比较槽位集合，两端同源 |
| R20 | `PureTestBootstrap`（`LauncherSessionListener`）引导原版；Forge 的 `Bootstrap` 末尾 `NetworkHooks.init` 没有 FML 会失败，只放过这一个错误；`SecondApertureTest`、`ModDimensionsTest` 搬入 |
| 验收 | 主线 `check.py build gametest`：L1 170、L2 136、GameTest 170 全过；Forge 端 `build`、L1 154、GameTest 21、`runData` 零漂移、服务器冒烟全过 |
| 一致率 | main 192 个同名文件 137 个一致（原 125/191）；L1 30/30；GameTest 4 个只差注解导入包与接缝本体 |

留给后续模块：`SoulTradeOfferTest:49` 用了 1.21 才有的 `getComponentsPatch()`，交易模块搬测试时补接缝。
