# 可移植性回顾：模块 3

目标同上轮：主线补接缝、改调用方，让模块 3 搬进来的文件两端逐字一致；Forge 端跟着改。

## 发现

| # | 问题 | 本轮代价 | 改法（两端同改） | 主线波及 |
|---|---|---|---|---|
| R7 | 效果句柄直接用加载器类型：`player.hasEffect(ModEffects.X)`、`getEffect().value()`、参数型 `Holder<MobEffect>`，原版效果 `MobEffects.WEAKNESS` 在 1.21 是 `Holder` | 本模块 12 个服务文件因此不同；模块 4 物品、客户端还有同类 | 主线 `ModEffects` 补同名帮助方法 `has` / `get` / `remove` / `effectOf` / `instance(Supplier, …)` 与 `VANILLA_WEAKNESS`、`VANILLA_CONFUSION`；调用方一律改走它们，效果句柄类型写 `Supplier<? extends MobEffect>` | 58 处 14 个文件 |
| R8 | 飞行授予写死在 `NeoForgeMod.CREATIVE_FLIGHT` 属性上，还用 `player.mayFly()`（含属性） | `FlightService`、`DimensionTravelService` 两端不同 | 主线加 `gameplay/flight/FlightAbility`（`has` / `grant` / `revoke` / `mayFly`），内部仍是属性修饰器；两个服务改调用它 | 2 个文件 + 新增 1 个 |
| R9 | 服务里直接 `NeoForge.EVENT_BUS.post` | 4 个服务文件只差这一行 | 主线加 `core/ModEvents.post`，服务改调它 | 5 处（含物品层 1 处） |
| R10 | 效果的 tick 回调签名随版本变（1.21 `boolean applyEffectTick` + `shouldApplyEffectTickThisTick`；1.20.1 `void` + `isDurationEffectTick`），5 个周期效果子类各自覆写 | 8 个效果文件不同 | `PeriodicEffect`、`AftermathEffect` 作接缝：由它们覆写加载器回调，子类只覆写一个与版本无关的方法（如 `onPeriod(LivingEntity, int amplifier)`）。之后 5 个子类两端相同 | 7 个文件 |
| R11 | 两版都有的写法没选：`hasInfiniteMaterials()`（1.21 才有）而不是 `getAbilities().instabuild` | 1 个文件不同 | 主线改 `getAbilities().instabuild` | 1 个文件 |
| R12 | 存档数据 API 随版本变（1.21 `SavedData.Factory`、`save(tag, HolderLookup.Provider)`） | `AperturePressureExplosions` 不同；模块 4 的 `PendingVitalPenalties` 同样 | 加 `core/ModSavedData` 接缝基类：子类只写与版本无关的 `write(CompoundTag)` / 静态 `read`，取用走 `ModSavedData.get(level, name, read, create)` | 2 个文件 + 新增 1 个 |
| R13 | 主线测试用了 1.21 DFU 才有的无参 `getOrThrow()` | `PathEntryTest`、`SecondApertureTest` 不能逐字复制 | 主线测试改 `result().orElseThrow()`（两版都有） | 2 个测试文件 |

做完 R7–R12，本模块不同文件从 37 个降到 18 个（只剩接缝本身、事件基类与事件处理、`DragonpillCricketGuEffect` 与 `ApertureStorage`）。

## 做法

同上轮：主线按 `rules/modification.md`、wiki 同步、`check.py`、`commit-push`；Forge 端换成同源文件后跑 `build`、L1、GameTest、冒烟。reference《写作与命名》补上接缝清单。

## 结果

- 主线 `096d7f0`（已推送，CI 绿）：`ModEffects` 帮助方法与 `VANILLA_WEAKNESS` / `VANILLA_CONFUSION`、`FlightAbility`、`FlightEvents`（tick 处理器从 `FlightService` 拆出）、`ModEvents`、`ModSavedData`（`AperturePressureExplosions`、`PendingVitalPenalties` 改到它上面）、`PeriodicEffect.onPulse` / `isPulseTick` 与 `AftermathEffect` 接管 tick 回调、`instabuild`；调用方全部迁移；`PathEntryTest`、`SecondApertureTest` 改 `result().orElseThrow()`，`MaliciousThoughtEffectTest` 改 `isPulseTick`。`check.py` 5 阶段全过（JUnit 168 + 136、GameTest 170）。wiki 改《维护说明》接缝清单、《维度旅行》《飞行与疾飞》；reference《写作与命名》补接缝规则。
- 发现并处理的新问题：`FlightService` 自带 tick 事件处理器，事件注解两端不同 → 拆出 `FlightEvents`，服务两端相同。
- Forge 端：换上 `PeriodicEffect` / `AftermathEffect` / `ModSavedData` / `FlightEvents` / `VANILLA_CONFUSION`；`build`、L1 97 条、GameTest 12 项、冒烟通过。
- 两端逐字一致：共享 112 个文件中 89 个。其余 23 个是接缝与胶水（`PlayerData`、`ModNetwork`、`ItemStacks`、`TransientModifiers`、`ModEffects`、`FlightAbility`、`FlightEvents`、`ModEvents`、`ModSavedData`、`PeriodicEffect`、`AftermathEffect`、`Guzhenren`、`lifecycle/PlayerDataEvents`、4 个事件基类、4 个事件处理类）和两个已知例外（`DragonpillCricketGuEffect` 的跳跃、`ApertureStorage` 的物品数据，后者在模块 4）。
- 遗留给模块 4：`GuzhenrenClient` 注册效果图标用 `Holder`（客户端扩展，随客户端模块定）；模块化测试里还有 4 个用无参 `getOrThrow()` 的 L2 测试，Forge 端 L2 本就不跑。
