# 03 物品 / 方块 / 效果 / 粒子 问题清单

范围（主线）：`item/` 3595 行、`block/` 370、`effect/` 563、`particle/` 216，加注册 `ModItems`、`ModCreativeTabs`、`ModDataComponents`、`ModBlocks`、`ModFluids`、`ModFluidTypes`、`ModEffects`、`ModParticles`（约 1100 行）。

## 结构问题：这一层排不到 gameplay 前面

包级依赖（去掉注册类、只看代码引用）：

| 包 | 依赖 |
|---|---|
| `item.gu`（2972 行） | `gameplay.aperture / attribute / body / clock / mind / path / soul`、`effect.timed`、`display`、**`command`** |
| `item.material` | `gameplay.aperture / body / path` |
| `effect.pool / timed` | `gameplay.attribute / body / clock / mind / path` |
| `gameplay.*` | 经 `ModEffects` 反过来引用全部效果类；`gameplay.aperture.storage` 引用 `GuItem` / `MortalGuItem` |
| `block` | 只依赖 `core` 和注册（`ModItems`、`ModFluids`） |
| `particle` | 只依赖注册 |

| # | 问题 | 依据 |
|---|---|---|
| S1 | 物品和效果都调用 gameplay 服务（`ApertureService`、`BodyService`、`PathQiService` 等），gameplay 不先到，这一层编译不过 | 上表 |
| S2 | 去掉注册枢纽后，全代码库只剩一个跨包环：`gameplay.aperture.storage.ApertureStorageService` → `GuItem` / `MortalGuItem` → `ApertureService` / `ApertureEssenceService`（14 个类）；加上注册类，整个 gameplay + item + effect + registry 是一个 97 个类、约 1.19 万行的大环 | Tarjan 强连通分量 |
| S3 | gameplay 服务与效果类经 `ModEffects` 互相引用（服务读效果、效果调服务），两者只能同一轮到 | `gameplay.* → registry.effect`、`effect.* → gameplay.*` |
| S4 | `item.gu.mortal.human.HopeGuItem` 依赖 `command.ModCommandSupport`，物品层反向依赖命令层 | 包级依赖 |

## API 问题（1.21.1 → 1.20.1）

| # | 主线 | Forge 1.20.1 | 用量 |
|---|---|---|---|
| A1 | 物品数据 `DataComponent`（`stack.get/set/has(ModDataComponents.X)`），自动同步 | 物品 NBT（`getOrCreateTag`），随物品同步；键名与默认值要自己定 | 29 处 8 个文件；`ModDataComponents` 10 种 |
| A2 | `ModEffects.X` 当 `Holder<MobEffect>` 用（`hasEffect`、`new MobEffectInstance`、`getEffect().value()`） | `RegistryObject`，API 收 `MobEffect` | 38 处 10 个文件（gameplay 内）+ 物品里同类用法 |
| A3 | `MobEffect.applyEffectTick` 返回 `boolean`、`shouldApplyEffectTickThisTick`、`onEffectStarted` | `void applyEffectTick`、`isDurationEffectTick(duration, amplifier)`、无 `onEffectStarted`（用 `MobEffectEvent.Added`） | 6 + 2 + 1 处 |
| A4 | 属性修饰器用 `ResourceLocation` 作 id，`Holder<Attribute>` | `UUID` 作 id，直接 `Attribute` | 3 + 1 处（`TransientModifiers` 等） |
| A5 | `appendHoverText(stack, TooltipContext, …)`、`getUseDuration(stack, entity)` | `appendHoverText(stack, Level, …)`、`getUseDuration(stack)` | 4 + 1 处 |
| A6 | `FoodProperties`（1.20.5 重做：`alwaysEdible`、`nutrition` 等） | 旧版 `FoodProperties.Builder`（`alwaysEat`） | 5 处 1 个文件 |
| A7 | 方块 `codec()`（`MapCodec`，1.20.5 起必须） | 没有；删掉即可 | 2 处 1 个文件 |
| A8 | `NeoForgeMod.CREATIVE_FLIGHT` 属性授予飞行 | **没有对应属性**：要改用 `Abilities.mayfly` 并自己记账，还要和其他模组的飞行授予共存 | 3 处（`FlightService`、`DimensionTravelService`） |
| A9 | NeoForge 事件：`LivingBreatheEvent`、`LivingIncomingDamageEvent`、`PlayerTickEvent.Post`、`ServerTickEvent`、自定义事件（`Event`、`ICancellableEvent`、`NeoForge.EVENT_BUS`） | 1.20.1 没有 `LivingBreatheEvent`（要 tick 里自己处理或 mixin）；`LivingAttackEvent` / `LivingHurtEvent`；`TickEvent.*` + `phase`；`@Cancelable` + `MinecraftForge.EVENT_BUS` | 33 个文件里 85 个 neoforged import |
| A10 | `FluidType` 客户端外观走 NeoForge `RegisterClientExtensionsEvent` | Forge 1.20.1 `FluidType.initializeClient` | 1 处 |
| A11 | `HopeGuUsedTrigger` 按 1.21 的触发器结构（Codec、`TRIGGER_TYPES` 注册） | `SimpleCriterionTrigger` + JSON 反序列化，`CriteriaTriggers.register` | 1 个类 |
| A12 | `DeferredItem` / `DeferredBlock` / `DeferredHolder` | `RegistryObject` | `ModItems` 等签名里约 160 处 |

`particle/`：`SimpleParticleType`、`ParticleProvider` 两版一致，只换注册写法。

## 下一轮可移植性回顾的候选（先记下）

- A1 物品数据和 A2 效果：做法同上轮的 `PlayerData`，主线加一个物品数据门面和一个效果门面，调用方两端逐字一致。
- S2 唯一的跨包环：让 `gameplay.aperture.storage` 不再认识具体物品类（由物品层提供判断），gameplay 才能整体先于 item 搬。
- S4：`HopeGuItem` 对 `command` 的依赖挪走。
- A8 飞行授予：主线也收进一个飞行接缝（授予 / 收回 / 是否可飞），NeoForge 用属性、Forge 用 abilities。

## 已定（Alex）

- D1 甲：模块 3 = gameplay 服务 + 效果（本清单的 S1–S4、A2、A3、A4、A8、A9、A11 落在这一轮）；模块 4 = 物品 / 方块 / 流体 / 粒子 / 创造栏 / 物品数据 / 空窍储物 / lifecycle（A1、A5、A6、A7、A10、A12）。
- D2：搬之前先在主线断开 S2（空窍储物不再引用具体物品类），主线与 Forge 端一起改。

## 原选项

- **D1 模块顺序**
  - 甲（推荐）：模块 3 改为「gameplay 服务 + 效果」（gameplay 共 7.4k 行，扣掉已搬的数据模型约 2.6k，余约 4.8k；效果 0.6k；含 `ModEffects`，含 A3、A4、A8、A9）；模块 4 才是物品 / 方块 / 流体 / 粒子 / 创造栏 / 物品数据（含空窍储物和 lifecycle）。按依赖方向走，每轮都能单独编译验收。
  - 乙：gameplay + 物品 + 效果 + 方块 + 粒子合成一个大模块（约 1.2 万行），一轮做完。
- **D2 先修结构再搬**：是否在搬之前就先在主线把 S2（空窍储物引用具体物品类）断开——断开后 gameplay 可以完全不带物品类进 Forge 端；不断开则模块 3 要带上 `GuItem` / `MortalGuItem` 的最小骨架。

## 前置：主线断环（已完成）

主线 `c33a94e`（CI 绿）：空窍储物经 `gameplay/aperture/storage/StoredGu` 认识蛊，物品层 `item.gu.StoredGuItems` 实现，`Guzhenren` 构造器 `ApertureStorageService.setStoredGu` 接上（同 `StonePurse`）。新 L1 测试 `PackageDependencyTest` 钉住「gameplay 不 import item / command / lifecycle / refinement」（HEAD 前 7 处违规，现 0）。reference《写作与命名》《数据模型》、wiki《维护说明》同步更正（含上一轮漏改的 `Math.clamp` 规则与 `ModStreamCodecs`）。拆环后 gameplay 对上层零依赖。

## 规格（模块 3）

### 范围

| 搬 | 文件 |
|---|---|
| gameplay 服务 | `aperture`（`ApertureService`、`ApertureEssenceService`、`ApertureNourishService`、`AperturePressureService`、`AperturePressureExplosions`、`AperturePressureExplosionTask`、`CraterChunkTicket`、`CraterShape`、`ApertureStatus`、`StonePurse`、`ApertureChangedEvent`、`ApertureEvents`、`ServerTickEvents`）；`aperture/storage`（`ApertureStorageService`、`ApertureStorageTick`、`StoredGu`）；`attribute` 全部；`body`（`BodyService`、`BodyStrengthService`、`UndeadService`、`BodyStateEvents`、两个 `*ChangedEvent`）；`clock` 全部；`dimension`（`DimensionTravelService`、`TreasureYellowHeavenGuardEvents`）；`flight` 全部；`mind/MindService`；`path`（`PathService`、`PathQiService`、`PathStrengthService`、`StrengthChangedEvent`）；`soul/SoulService` |
| 效果 | `effect/**` 15 个类 + `registry/effect/ModEffects` |
| 不搬（后续模块） | `aperture/storage/ApertureStorageMenu`（随菜单与界面）、`trade/**`（随实体与物品）|

约 50 个文件、约 5.4k 行。

### 两端相同 / 只在 Forge 端不同

搬入时按「接缝外逐字一致」：调用方写法直接用下表的接缝名，下一轮回顾只需在主线补同名接缝。

| 接缝（新） | 作用 | Forge 1.20.1 实现 | 主线后续（回顾） |
|---|---|---|---|
| `registry/effect/ModEffects.has(LivingEntity, X)` / `instance(X, …)` / `effect(X)` | 效果判定与实例，调用方不碰 `Holder` / `MobEffect` 差异 | `RegistryObject<MobEffect>`，内部 `.get()` | 内部取 `Holder` |
| `gameplay/flight/FlightAbility.grant(player, source)` / `revoke(player, source)` / `has(player, source)` | 按来源授予/收回飞行 | `Abilities.mayfly` + 每人一组来源（只在内存）；有来源且原本不能飞时打开并记「是我们打开的」，最后一个来源撤销时只关自己打开的，创造/旁观不动 | `CREATIVE_FLIGHT` 属性修饰器 |
| `gameplay/attribute/TransientModifiers`（已有类） | 加/换/删临时属性修饰器 | `ResourceLocation` → `UUID.nameUUIDFromBytes(id)`，直接用 `Attribute` | 不变 |
| 自定义事件基类与发事件 | `ApertureChangedEvent` 等 5 个 | 继承 Forge `Event`，可取消的加 `@Cancelable`，经 `MinecraftForge.EVENT_BUS.post` | 不变 |

### 加载器差异（只在事件处理文件里，不进服务）

| 主线 | Forge 1.20.1 |
|---|---|
| `@EventBusSubscriber(modid)` | `@Mod.EventBusSubscriber(modid)` |
| `PlayerTickEvent.Post` / `ServerTickEvent.Post` | `TickEvent.PlayerTickEvent` / `TickEvent.ServerTickEvent`，只在 `phase == END` 处理 |
| `LivingBreatheEvent`（不死之躯可呼吸） | 玩家 tick 末尾：不死之躯时 `setAirSupply(getMaxAirSupply())` |
| `LivingIncomingDamageEvent`（宝黄天护卫） | `LivingAttackEvent`（同样在减伤前、可取消） |
| `MobEffect.applyEffectTick` 返回 `boolean`、`shouldApplyEffectTickThisTick` | `void applyEffectTick`、`isDurationEffectTick(duration, amplifier)`；原 `return false` 提前结束的效果改为调用方移除 |
| `onEffectStarted` | `MobEffectEvent.Added` |
| 属性修饰器 `ResourceLocation` id、`Holder<Attribute>` | `UUID`（见上）、`Attribute` |

其余按编译报错逐个对：区块票据 `TicketType`、`Level.explode` 重载、`AttributeModifier.Operation` 枚举名（`ADD_VALUE` → `ADDITION` 等）。

### 测试

| 层 | 内容 |
|---|---|
| L1 | 主线 L1 里只依赖本模块代码的测试原样复制（如 `ApertureNourishTest`、`BodyDataTest`、`TimeFlowTest`、`FlightGrantTest`、`PressureExplosionMathTest`、`ApertureTalentMarksTest`、`MindDataTest`、`PathEntryTest`、`QiKindTest`、`TalentTest`、`TitleTest`、`WeightedPickTest`、`RaceTalentTest`、`PathStrengthRampTest`、`SecondApertureTest` 等，以编译为准）；`PackageDependencyTest` 同样复制 |
| L3 新增（Forge 专属机制） | 飞行：Gu 来源授予/撤销；两个来源叠加只撤一个仍能飞；原本创造模式时撤销不关飞行。不死之躯水下 5 s 氧气不降。宝黄天护卫取消伤害。每个效果的 tick 节奏与主线一致（按 `isDurationEffectTick` 次数）。属性修饰器同一 `ResourceLocation` 两次得同一 `UUID`，替换不叠加 |
| L3 复制 | 主线 GameTest 中不涉及物品/实体的用例（以编译为准，如 `FlightGameTests`、`TreasureYellowHeavenGameTests` 的一部分） |

### 验收

1. `build` 通过；L1 全过。
2. `runGameTestServer -PserverRun` 全过。
3. 冒烟：客户端到标题画面 20 s；服务端 `Done` 并 `stop`。
4. 两端逐字一致检查：本模块搬入文件中，接缝与事件处理文件以外的全部与主线逐字一致（列出例外清单）。

### 计划

1. 主会话：写 Forge 端接缝（`ModEffects` 帮助方法、`FlightAbility`、`TransientModifiers` 的 UUID 版）和 5 个自定义事件。
2. 子代理：复制约 45 个服务与效果文件，调用改走接缝名，按上表改事件处理文件与效果回调；复制 L1 测试。
3. 主会话：写 Forge 专属 L3 测试，跑验收。
4. 可移植性回顾：主线补同名接缝（`ModEffects` 帮助方法、`FlightAbility`），调用方改成与 Forge 端相同；其余问题同上轮流程。

子代理分配：第 2 步一个子代理（模型由你选）；第 1、3 步主会话；第 1 步与第 2 步有先后（子代理要用接缝名）。

## 验收结果

| 项 | 结果 |
|---|---|
| `build` | 通过 |
| L1 `pureTest` | 17 类 97 条全过（逐字复制主线；跳过的见下） |
| `runGameTestServer -PserverRun` | 12 项全过（原 6 + 新 6：单来源授予/收回飞行、两来源叠加、不收回创造飞行、不死之躯呼吸、龙丸蛐蛐蛊跳跃、最大生命修饰器不叠加） |
| `runClient` / `runServer -PserverRun` | 到标题画面 20 s；`Done (2.344s)` 并 `stop` |
| 共享文件逐字一致 | 108 个中 71 个 |

## 实施偏差

- 不死之躯呼吸：Forge 1.20.1 有 `LivingBreatheEvent`（`ForgeHooks.onLivingBreathe` 发出），保留事件写法，不改 tick。
- 宝黄天护卫 `TreasureYellowHeavenGuardEvents` 引用交易实体 `SoulTraderEntity`，挪到模块 4 与交易一起搬。
- 龙丸蛐蛐蛊：1.20.1 的 `Attributes.JUMP_STRENGTH` 是马的属性，玩家没有；改为 `LivingJumpEvent` 起跳时加 `JUMP_BONUS ×（等级）` 的竖直速度（`effect/timed/DragonpillCricketJump`，Forge 端独有）。普通地面与主线同量，蜂蜜块等改跳跃系数的方块上不乘系数。
- 新增接缝：`FlightAbility.mayFly(player)`（主线 `player.mayFly()` 含属性，Forge 端读 `mayfly`）、`ModEffects.VANILLA_WEAKNESS`（原版效果在 1.21 是 `Holder`、1.20.1 是 `MobEffect`）、`core/ModEvents.post`。
- `AperturePressureExplosions` 的存档数据用 1.20.1 的 `computeIfAbsent(load, create, name)` 与 `save(CompoundTag)`。
- `ApertureNourishService`：`hasInfiniteMaterials()` → `getAbilities().instabuild`（两版都有）。
- 测试：1.20.1 的 `makeMockServerPlayerInLevel` 在 Forge 下建不出来（连接无通道）且恒为创造模式，Forge 专属测试用 `FakePlayerFactory`。`pureTest` 源码集补上 `main` 的类路径与 `gzr.projectDir`。
- L1 跳过：`GuSpecTest`、`TimeFlowTest`、`SecondApertureTest`（依赖物品）；`PathEntryTest`、`SecondApertureTest` 用了 1.20.1 DFU 没有的无参 `getOrThrow()`；`ModDimensionsTest`（1.20.1 未引导时 `Level.OVERWORLD` 报错）；其余依赖物品、客户端、命令、datagen 或语言表，随各自模块。

## 37 个不同文件的归类

| 类 | 文件 | 去向 |
|---|---|---|
| 接缝本身（两端永远不同） | `PlayerData`、`ModNetwork`、`ItemStacks`、`TransientModifiers`、`ModEffects`、`Guzhenren`、`lifecycle/PlayerDataEvents` | 保持 |
| 事件基类 / 事件处理 | 4 个 `*ChangedEvent`；`ApertureEvents`、`ServerTickEvents`、`AttributeEvents`、`BodyStateEvents` | 保持（加载器胶水） |
| 效果回调签名 | `AftermathEffect`、`PeriodicEffect`、`LifeQiEffect`、`CasualThoughtEffect`、`MaliciousThoughtEffect`、`SelfRelianceGuEffect`、`VitalityLeafEffect`、`DragonpillCricketGuEffect` | 回顾候选：效果基类接缝 |
| 只差接缝调用，主线补接缝即可一致 | `ApertureEssenceService`、`ApertureNourishService`、`ApertureService`、`AttackDamageService`、`BodyService`、`BodyStrengthService`、`UndeadService`、`TimeFlow`、`PathQiService`、`PathStrengthService`、`DimensionTravelService`、`FlightService` | 回顾 |
| 存档 API | `AperturePressureExplosions` | 回顾候选 |
| 物品数据 | `ApertureStorage` | 模块 4 |
