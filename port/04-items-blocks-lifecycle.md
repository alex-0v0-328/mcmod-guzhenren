# 04 物品 / 方块 / 流体 / 粒子 / lifecycle 问题清单

范围（主线未搬部分）：`item/` 40 个文件、`block/` 3、`particle/` 2、`lifecycle/` 4、`display/` 2、`advancement/` 1、`gameplay/aperture/storage/ApertureStorageMenu`，加注册 `ModItems`、`ModCreativeTabs`、`ModDataComponents`、`ModBlocks`、`ModFluids`、`ModFluidTypes`、`ModParticles`、`ModMenus`、`ModCriteriaTriggers`。共 62 个类、约 6.4k 行。

## 结构问题

这 62 个类对外只依赖已搬的部分，例外三处：

| # | 问题 | 依据 |
|---|---|---|
| S1 | `HopeGuItem` 依赖命令层 `ModCommandSupport.refreshCommands`——只为重发命令树（`server.getCommands().sendCommands(player)`，原版两版都有） | `item/gu/mortal/human/HopeGuItem.java:80` |
| S2 | lifecycle 依赖兼容层：`PlayerDataService` 在登录、克隆、重生时调 `EpicFightIntegration.refresh`，而 `EpicFightIntegration` 又依赖实体（`WildGuEntity`）、粒子、效果；lifecycle 因此搬不了，除非连兼容层和实体一起搬 | `lifecycle/PlayerDataService.java:66,94,135` |
| S3 | 注册中枢 `ModMenus` 同时注册储物、交易、炼蛊三个菜单；交易、炼蛊在后面的模块 | `registry/menu/ModMenus.java` |
| S4 | `particle/RingConeEmitter` 是服务端的粒子发射器，自带 NeoForge 事件处理器（`PlayerTickEvent.Post`、登出），和模块 3 的 `FlightService` 同样把服务与事件处理写在一个类里 | `particle/RingConeEmitter.java:93,106` |

## API 问题（1.21.1 → 1.20.1）

| # | 主线 | Forge 1.20.1 | 用量 |
|---|---|---|---|
| A1 | 物品数据 `DataComponent`（10 种：`REFINED_GU_STATE`、`VITAL_OWNER`、`VITAL_APERTURE`、`VITAL_TOKEN`、`STORED_STONES`、`REFINED_AT`、`AWAKEN_BASE`、`USED_AT`、`HEAL_BANK`、`LEAF_GROWTH`），有默认值、参与堆叠比较、自动同步 | 物品 NBT：键名、缺省即默认值、空值时删键（否则「有键=默认值」与「无键」不再能堆叠）都要自己守 | 29 处 8 个文件 |
| A2 | `DeferredItem` / `DeferredBlock` / `DeferredHolder`、`DeferredRegister.Items/Blocks` | `RegistryObject` + `DeferredRegister.create(ForgeRegistries.X)` | 125 处 5 个文件 |
| A3 | `appendHoverText(stack, TooltipContext, …)`、`getUseDuration(stack, entity)` | `appendHoverText(stack, Level, …)`、`getUseDuration(stack)` | 4 + 2 处 |
| A4 | `FoodProperties`（1.20.5 重做） | 旧版 builder（`nutrition` / `saturationMod` / `alwaysEat` / `effect(Supplier<MobEffectInstance>, float)`） | 5 处 1 个文件 |
| A5 | 方块 `MapCodec codec()`；`Properties.ofFullCopy`；`FlowerBlock(Holder<MobEffect>, float 秒)` | 删 `codec()`；`Properties.copy`；`FlowerBlock(MobEffect, int 刻)` | 2 个文件 |
| A6 | 流体：`FluidType` 客户端外观走 NeoForge `RegisterClientExtensionsEvent`；`NeoForgeRegistries.FLUID_TYPES` | `FluidType.initializeClient` + `IClientFluidTypeExtensions`；`ForgeRegistries.Keys.FLUID_TYPES`；`ForgeFlowingFluid` | 3 个文件 |
| A7 | 进度触发器：`SimpleCriterionTrigger` + `Codec`、`TRIGGER_TYPES` 注册表 | `SimpleCriterionTrigger` + `createInstance(JsonObject, ContextAwarePredicate, DeserializationContext)`、`CriteriaTriggers.register` | 2 个文件 |
| A8 | 菜单：`IMenuTypeExtension.create` | `IForgeMenuType.create`；开菜单走已有接缝 `ModNetwork.openMenu`（Forge 端要补实现：`NetworkHooks.openScreen`） | 2 个文件 |
| A9 | 创造栏 `withTabsBefore(ResourceLocation)` 指向 `epicfight:items` | 1.20.1 Forge 同样有 `withTabsBefore(ResourceLocation…)`；Epic Fight 20.14 有 `EpicFightCreativeTabs.ITEMS`，id 待开游戏核对 | 1 个文件 |
| A10 | lifecycle 事件：`PlayerTickEvent.Post`、`PlayerWakeUpEvent`、`ItemTossEvent`、`EventPriority.LOW` 的克隆 | `TickEvent.PlayerTickEvent` + `phase`；其余 Forge 同名；克隆顺序要和模块 2 的 `HIGHEST` 拷贝 / `LOWEST` 失效衔接 | 4 个文件 |

## 数据与资源

| # | 问题 | 量 |
|---|---|---|
| D-1 | 生成的数据目录名：1.21 单数（`recipe`、`loot_table`、`advancement`、`tags/item`、`tags/block`、`tags/entity_type`），1.20.1 复数（`recipes`、`loot_tables`、`advancements`、`tags/items`、`tags/blocks`、`tags/entity_types`） | 约 60 个文件 |
| D-2 | JSON 格式：配方结果 `{"id": …}` → `{"item": …}`；战利品表 `set_components` → `set_nbt`；进度的物品谓词格式；NeoForge 全局战利品修饰 `data/neoforge/loot_modifiers` → `data/forge/loot_modifiers`，修饰器 `MapCodec` → `Codec` | 配方 2、战利品 4、进度 1、修饰 2 |
| D-3 | `damage_type` 数据模块 3 已用到（`ModDamageTypes.source`），Forge 端至今没有这些 JSON；运行时一旦造伤害来源会报键缺失 | 6 + 原版标签 8 |
| D-4 | 资源：方块状态、模型、语言表、粒子定义、贴图格式两版相同，可直接复制；GeckoLib 的 `geo/`、`animations/` 属于实体 | 约 120 个文件 |
| D-5 | 生成方式：主线由 datagen（18 个类，2.2k 行）产出 `src/generated`。Forge 端是搬 datagen 让它产出 1.20.1 格式，还是先把主线产物按 D-1/D-2 转换后放进来 | — |

## 已定（Alex）

- D1：三处都按推荐，先在主线改——S1 `HopeGuItem` 直接重发命令树；S2 `PlayerDataService` 处理完后发 `PlayerRefreshedEvent`（经 `ModEvents.post`），`EpicFightIntegration` 订阅，lifecycle 不再认识兼容层；S4 `RingConeEmitter` 的事件处理器拆到 `RingConeEvents`。
- D2 甲：本模块就搬 datagen，Forge 端自己产出 1.20.1 格式。只搬已搬内容对应的 provider 与条目（物品、方块、流体、语言、标签、伤害类型、HopeGu 进度）；实体战利品表、生物群系修饰（刷怪）、炼蛊配方、全局战利品修饰随各自模块补。
- D3：Forge 端 `ModMenus` 本模块只注册储物菜单。

## 原选项

- **D1 S1、S2、S4 的处理**（和前两轮一样先在主线断开再搬）
  - S1：`HopeGuItem` 直接 `server.getCommands().sendCommands(player)`，或者让命令层订阅已有的 `ApertureChangedEvent` 自己重发。推荐前者，一行、无新事件。
  - S2：兼容层自己订阅生命周期（推荐：`PlayerDataService` 处理完后发一个 `ModEvents.post(new PlayerRefreshedEvent(player))`，`EpicFightIntegration` 订阅它；lifecycle 不再认识兼容层）。
  - S4：`RingConeEmitter` 的两个事件处理器拆到 `RingConeEvents`（同 `FlightEvents`）。
- **D2 数据怎么进 Forge 端**
  - 甲：本模块就搬 datagen，Forge 端自己产出 1.20.1 格式（datagen 的 provider API 两版差异大，工作量约等于再搬一个模块）。
  - 乙（推荐）：本模块先写一个一次性的转换脚本，把主线 `src/generated` 按 D-1/D-2 转成 1.20.1 放进 Forge 端，跑通物品与方块；datagen 留到原计划的模块 7，届时以 datagen 产出替换。
- **D3 S3**：Forge 端 `ModMenus` 本模块只注册储物菜单，交易、炼蛊随各自模块补上（注册中枢本来就是逐模块长的）。推荐照此。

## 前置：主线断依赖（已完成）

主线 `d9507fa`（CI 绿）：`HopeGuItem` 直接 `player.server.getCommands().sendCommands(player)`；`PlayerDataService` 在三处派生属性重算后 `ModEvents.post(new PlayerRefreshedEvent(…))`，`EpicFightServerEvents` 订阅它调用 `EpicFightIntegration.refresh`；`RingConeEmitter` 的 tick / 登出 / 换维度处理拆到 `RingConeEvents`。`PackageDependencyTest` 扩为三条（gameplay、item、lifecycle），HEAD 前各有 7 / 1 / 1 处违规，现 0。wiki《维护说明》《力量与战斗》、reference《写作与命名》同步。

## 规格（模块 4）

### 范围

| 搬 | 内容 |
|---|---|
| 物品 | `item/**` 40 个类；`display/` 2；`advancement/HopeGuUsedTrigger` |
| 方块 / 流体 | `block/**` 3 个类；元泉流体类型 |
| 粒子 | `particle/RingConeEmitter`、`RingTrailSpacing`、`RingConeEvents`（服务端发射；客户端粒子渲染随客户端模块） |
| lifecycle | `PlayerDataService`、`PlayerTickEvents`、`VitalLossService`、`PendingVitalPenalties`、`PlayerRefreshedEvent`；主线 `PlayerDataEvents` 的转交逻辑并进 Forge 端已有的同名类 |
| 储物 | `gameplay/aperture/storage/ApertureStorageMenu` |
| 注册 | `ModItems`、`ModCreativeTabs`、`ModDataComponents`、`ModBlocks`、`ModFluids`、`ModFluidTypes`、`ModParticles`、`ModCriteriaTriggers`、`ModMenus`（只储物菜单） |
| 入口 | `Guzhenren` 按主线顺序注册上述各表，接上 `StonePurse`、`StoredGu` |
| datagen | 语言表（含 `TraditionalGlyphs`）、物品模型、方块状态、物品 / 方块 / 实体类型 / 伤害类型 / 生物群系标签、伤害类型（`DatapackBuiltinEntriesProvider`，生物群系修饰留给实体模块）、方块战利品表、粒子定义、HopeGu 进度、Curios 槽位 |
| 资源 | `textures/{item,block,particle,mob_effect}` 原样复制（`textures/entity`、`geo/`、`animations/` 随实体模块） |
| 不搬 | 宝黄天护卫事件、交易、炼蛊、实体、客户端界面与渲染、命令、Epic Fight 兼容、配方 / 实体战利品 / 全局战利品修饰的 datagen |

约 80 个类、约 8k 行（含 datagen 约 1.6k）。

### 新接缝（Forge 端先建，回顾时主线补同名）

| 接缝 | 作用 | Forge 1.20.1 实现 | 主线后续 |
|---|---|---|---|
| `registry/item/ModDataComponents`（键 + 静态读写） | 调用方一律 `ModDataComponents.get / getOrDefault / set / has / remove(stack, ModDataComponents.X, …)`，不碰 `stack.get(…)` 或 NBT | 每个键 = NBT 键名 `guzhenren:<id>` + `Codec`（经 `NbtOps`）；`remove` 删键，标签空了把整个 tag 置空，保证与新物品可堆叠 | 键 = `DataComponentType`，读写转发 `stack.get/set` |
| `item/GuItem` 的提示与使用时长 | 子类覆写与版本无关的 `appendTooltip(stack, tooltip, flag)`；`getUseDuration` 由 `GuItem` 自己按两版签名实现 | `appendHoverText(stack, Level, …)` / `getUseDuration(stack)` | `appendHoverText(stack, TooltipContext, …)` / `getUseDuration(stack, entity)` |
| `ApertureStorage` 的 `VITAL_APERTURE` | 改走 `ModDataComponents.set`，文件两端相同 | — | — |

### 加载器 / 版本差异（只在注册、事件、物品与方块的覆写处）

| 主线 | Forge 1.20.1 |
|---|---|
| `DeferredItem` / `DeferredBlock` / `DeferredHolder`、`DeferredRegister.createItems/Blocks` | `RegistryObject`、`DeferredRegister.create(ForgeRegistries.X, …)` |
| `FoodProperties` 新 builder | 旧 builder（`nutrition` / `saturationMod` / `alwaysEat` / `effect(Supplier, float)`），数值按同一营养与饱和效果换算 |
| 方块 `codec()`、`Properties.ofFullCopy`、`FlowerBlock(Holder, float 秒)` | 删 `codec()`、`Properties.copy`、`FlowerBlock(MobEffect, int 刻)`（秒 × 20） |
| `FluidType` 客户端外观 `RegisterClientExtensionsEvent` | `FluidType` 子类覆写 `initializeClient` |
| `SimpleCriterionTrigger` + `Codec`、`TRIGGER_TYPES` | `createInstance(JsonObject, …)`、`CriteriaTriggers.register` |
| `IMenuTypeExtension.create`；`player.openMenu(provider, buf)` | `IForgeMenuType.create`；Forge 端 `ModNetwork.openMenu` 补上 `NetworkHooks.openScreen` |
| lifecycle 事件（`PlayerTickEvent.Post` 等） | `TickEvent.*` + `phase`；克隆仍是 `HIGHEST` 拷贝 → 主线 `LOW` 结算 → `LOWEST` 失效 |
| datagen：`GatherDataEvent`、`DatapackBuiltinEntriesProvider`、`AdvancementProvider` | Forge 同名类（`net.minecraftforge.common.data.*`、`ForgeAdvancementProvider`），输出 1.20.1 目录与格式 |

### 测试

| 层 | 内容 |
|---|---|
| L1 | 复制主线中现在能编译的：`GuSpecTest`、`TimeFlowTest`、`SecondApertureTest`、`PathEntryTest`、`MealPortionTest`、`LangContractTest`、`TraditionalGlyphsTest`、`HopeGuSpawnTagTest`、`CuriosDataContractTest` 等（以编译和资源为准）；`PackageDependencyTest` 三条全上 |
| L3 复制 | 不依赖实体 / 客户端 / 命令的物品与方块 GameTest（如 `NineLeafVitalityGrassGameTests`、`DecorativeHopperGameTests`、`HoeHarvestGameTests`，及 `ModGameTests` 中能编译的用例） |
| L3 新增 | 物品数据：每个键写入读回相等；`remove` 后与新物品可堆叠（`ItemStack.isSameItemSameTags`）；`VITAL_APERTURE` 缺键视为 `PRIMARY`。数据：6 个伤害类型可解析（`ModDamageTypes.source` 不抛错）。创造栏：四个栏都在、顺序与主线同 |
| datagen | `runData` 输出与提交的 `src/generated` 逐字节一致（同主线的漂移检查） |

### 验收

1. `build`、L1 全过；`runData` 无漂移。
2. `runGameTestServer -PserverRun` 全过。
3. 冒烟：客户端到标题画面 20 s；服务端 `Done` 并 `stop`。
4. 两端一致性：新搬入文件中，接缝与覆写处以外逐字一致（列出例外）。
5. 进游戏手感（物品贴图、提示、创造栏）留给你在 `runClient` 看；客户端渲染与界面不在本模块。

### 计划

1. 主会话：写 Forge 端接缝（`ModDataComponents` 键与读写、`GuItem` 的提示与使用时长、`ModNetwork.openMenu`），定好 lifecycle 克隆顺序与 `PlayerDataEvents` 合并方式。
2. 子代理 A：物品、方块、粒子、lifecycle、储物菜单、注册与入口（约 65 个类），按接缝与差异表改；复制 L1 测试。
3. 子代理 B（与 A 并行，写不同文件）：datagen 13 个 provider + 资源贴图；产出 `src/generated`。
4. 主会话：Forge 专属 GameTest，跑验收与一致性检查，提交 Forge 端。
5. 可移植性回顾：主线补 `ModDataComponents` 读写接缝与 `GuItem.appendTooltip`，调用方改成与 Forge 端相同；其余问题同前几轮。

子代理分配：A、B 两个，模型由你选；B 依赖 A 的注册类名（`ModItems.X` 等）但不依赖其实现，可并行，最后一起编译。

## 验收结果

| 项 | 结果 |
|---|---|
| `build` / L1 | 通过；L1 137 条全过 |
| `runData -PdataRun` | 生成资源与主线 0 处不同；伤害类型、物品 / 方块 / 实体 / 生物群系标签逐字节相同；Curios 槽位只差列表顺序；少一个 `no_knockback` 伤害标签（1.20.1 没有，这 6 种来源无实体，本就不击退） |
| `runGameTestServer -PserverRun` | 21 项全过（原 12 + 复制 `NineLeafVitalityGrassGameTests`、`DecorativeHopperGameTests` + 新 3：物品数据往返与清空后可叠、6 个伤害类型可解析、4 个创造栏） |
| 冒烟 | 客户端 53 个 mod 到标题画面 20 s；服务端 `Done (2.136s)` 并 `stop` |
| 共享文件逐字一致 | 191 个中 125 个 |

## 实施偏差

- 数据生成要加 `-PdataRun`：只加载硬依赖 Epic Fight 与 GeckoLib；整合包的客户端界面 mod（MouseTweaks、ModernUI）在数据生成环境构造即崩。
- 提示接缝改为 `appendTooltip(stack, @Nullable Level level, tooltip, flag)`：两版都能给出 `Level`，子代理的 ThreadLocal 方案去掉。
- 使用时长：1.20.1 的 `getUseDuration(stack)` 拿不到使用者，蛊的蓄力取决于持有者转数。`GuItem.chargeDuration(stack, entity)` 是真实时长，`getUseDuration` 返回占位 20 刻，`item/GuUseEvents`（`LivingEntityUseItemEvent.Start`）在开始使用时写入真实时长。主线回顾时把 `getUseDuration(stack, entity)` 的逻辑改名为 `chargeDuration`。
- `FlowerBlock` 用 1.20.1 的效果供应器构造，时长 `8 * Ticks.SECOND`，与主线 8 秒相同。
- 元泉流体外观暂写在 `ModFluidTypes` 的内部类里（贴图、雾色 `0xFF4FC3F7` 抄自 `ModPalette.APERTURE`），客户端模块换成 `SpiritSpringClientExtensions`。
- 进度触发器：`ModCriteriaTriggers` 在 `FMLCommonSetupEvent.enqueueWork` 里 `CriteriaTriggers.register`；`USED_HOPE_GU.get().trigger(player)` 写法不变；datagen 用 `instance()`。
- `DecorativeHopperBlock.isPathfindable` 是 1.20.1 的四参签名（两版不通用）。
- 测试接缝：`GameTestPlayers`（造假服务端玩家，Forge 端用 1.20.1 的监听器构造）、`GameTestAsserts.assertValueEqual`（1.20.1 的 `GameTestHelper` 没有）；GameTest 的 `@GameTestHolder` 导入两端不同。
- 未搬：`HoeHarvestGameTests`（花瓣来自全局战利品修饰，随它搬）；`CuriosDataContractTest`（见回顾 R-Curios）；`SecondApertureTest`、`ModDimensionsTest`（1.20.1 纯测试里没有引导 Minecraft，读物品类或注册表即报错）。

## 66 个不同文件的归类

| 类 | 文件 | 去向 |
|---|---|---|
| 接缝与注册 | `PlayerData`、`ModNetwork`、`ItemStacks`、`TransientModifiers`、`ModEffects`、`ModDataComponents`、`FlightAbility`、`ModEvents`、`ModSavedData`、`PeriodicEffect`、`AftermathEffect`、`Guzhenren`、10 个注册类 | 保持 |
| 事件基类 / 胶水 | 7 个事件类、`ApertureEvents`、`ServerTickEvents`、`AttributeEvents`、`BodyStateEvents`、`FlightEvents`、`RingConeEvents`、`ItemTossEvents`、`PlayerTickEvents`、`PlayerDataEvents` | 保持 |
| datagen | 14 个 provider | 回顾候选：数据生成接缝 |
| 只差接缝调用，主线补接缝即一致 | `GuItem`、`MortalGuItem`、`TendedGuItem`、`NineLeafVitalityGrassItem`、`GuClock`、`HopeGuItem`、`PrimevalElderGuItem`、`TreasureLotusGuItem`、`PrimevalStoneSupply`、`VitalLossService`、`LiquorItem`、`ApertureStorage` | 回顾 |
| 方块 / 流体 / 触发器 API | `SpiritSpringBlock`、`SpiritSpringFluid`、`DecorativeHopperBlock`、`HopeGuUsedTrigger`、`DragonpillCricketGuEffect` | 部分回顾（`public` 覆写两版通用） |
