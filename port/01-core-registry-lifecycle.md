# 01 core / registry / lifecycle 问题清单

范围：主线 `core/`（5 个文件，220 行）、`registry/`（18 个文件，1685 行）、`lifecycle/`（5 个文件，483 行）。

## 结构问题

| # | 问题 | 依据 |
|---|---|---|
| S1 | registry 不是底层：18 个注册类里 9 个 import gameplay / item / entity / block / effect / refinement / advancement，`ModItems` 一个就引 41 个物品类；单独搬过来编译不过 | `registry/**` 跨包 import |
| S2 | lifecycle 依赖 gameplay / item / compat / registry，属于上层，排在模块 1 搬不了 | `lifecycle/PlayerDataService.java` 等 |
| S3 | 1.21 的四套基础设施（Attachment、DataComponent、StreamCodec、Holder 式效果）在 1.20.1 都不存在，用处散在全代码库；改法一旦定下，后面每个模块都照它改 | 见下表用量 |

## API 问题（按影响面排序）

| # | 1.21.1 主线 | 1.20.1 Forge | 用量 |
|---|---|---|---|
| A1 | `AttachmentType`（14 个：自动存档、`copyOnDeath`、`sync(OWNER_ONLY)` 自动同步） | 无；Capability + `AttachCapabilitiesEvent`、`PlayerEvent.Clone` 手动拷贝、手动发同步包 | `getData/setData` 31 处 15 个文件；`setIfChanged` 10 处 |
| A2 | `DataComponentType`（10 个，物品上的 UUID / long / int / 记录） | 无；`ItemStack` NBT | `stack.get/set/has(...)` 30 处 9 个文件 |
| A3 | `StreamCodec` / `ByteBufCodecs` / `RegistryFriendlyByteBuf` | 无；`FriendlyByteBuf` 读写方法 | `ModStreamCodecs` 20 处 9 个文件；各数据记录的 `STREAM_CODEC` 约 26 个文件 |
| A4 | `DeferredHolder<MobEffect>` 本身是 `Holder`，`MobEffectInstance`、`hasEffect` 收 `Holder<MobEffect>` | `RegistryObject`，API 收 `MobEffect`，要 `.get()` | `ModEffects.X` 当 Holder 用 58 处 14 个文件；`ModEffects.instance` 12 处 |
| A5 | `DeferredItem` / `DeferredBlock` / `DeferredRegister.Blocks/Items/DataComponents` | `RegistryObject` + 普通 `DeferredRegister` | 签名里 160 处 7 个文件（多在 `ModItems`） |
| A6 | 进度触发器注册表 `TRIGGER_TYPES` | 不是注册表；`CriteriaTriggers.register(...)`，触发器用 JSON 不用 Codec | `ModCriteriaTriggers` 1 个 |
| A7 | `SavedData.Factory` | `computeIfAbsent(load, create, name)` | `PendingVitalPenalties` |
| A8 | `PlayerTickEvent.Post`、`@EventBusSubscriber`（neoforged） | `TickEvent.PlayerTickEvent` + `phase`、`@Mod.EventBusSubscriber` | lifecycle 3 个文件 |
| A9 | 零散改名：`Entity.registryAccess()`、`Properties.ofFullCopy`、`IMenuTypeExtension.create`、`FlowerBlock(Holder, float)`、配方序列化器 `id -> ...` 注册 | `entity.level().registryAccess()`、`Properties.copy`、`IForgeMenuType.create`、`FlowerBlock(MobEffect, int)`、`() -> ...` | 各 1–7 处 |

不变、可直接复制：`core/` 的 `NamedEnum`、`Ticks`、`WeightedPick`、`TossedItems`；Mojang `Codec`（41 个文件，DFU 在 1.20.1 一样）；各类 `TagKey`、`ModDimensions`；`ResourceLocation.fromNamespaceAndPath` / `withDefaultNamespace` / `parse`（Forge 47.4 已补回）；`FluidType`（Forge 1.20.1 也有）。

## 已定

- D1：方案乙，Forge 原生写法（Alex 定）。不做仿 1.21 兼容层；`ModStreamCodecs` 改成 `FriendlyByteBuf` 读写帮手。
- D2：同意。模块 1 = core + 注册基础设施 + 不依赖上层的注册；内容注册跟各自模块走；lifecycle 挪到 gameplay 之后。

## 原选项

- **D1 A1–A4 的改法**
  - 方案甲（推荐）：在 1.20.1 里写一层薄的仿 1.21 接口——`StreamCodec`/`ByteBufCodecs` 的最小子集、基于 Capability 的 `AttachmentType`（存档、死亡拷贝、只同步给本人）、基于 NBT 的 `DataComponentType`。gameplay 代码只改调用入口（如 `player.getData(X)` → `ModAttachments.get(player, X)`），记录类和 Codec 基本原样。两版同步成本最低。
  - 方案乙：全部改成 Forge 原生写法（每个数据一个 Capability、手写 NBT 和包读写）。更地道，但每个数据类都要重写，以后同步主线改动要逐行翻译。
- **D2 模块边界**：把模块 1 改成「core + 注册基础设施 + 方案甲的兼容层 + 不依赖上层的注册（标签、伤害类型、维度键、粒子、流体类型）」；各内容的注册条目（物品、方块、实体、效果、菜单、配方、触发器）跟着各自内容模块搬；lifecycle 挪到 gameplay 之后。

## 规格（模块 1）

目标：搬入不依赖上层的底座，定下后续模块共用的注册与读写约定。

| 文件 | 处理 |
|---|---|
| `core/NamedEnum`、`Ticks`、`WeightedPick`、`TossedItems` | 原样复制，含 Javadoc |
| `core/ModStreamCodecs` → `core/ModByteBufs` | `FriendlyByteBuf` 读写帮手，替换主线五种用法：`writeEnum/readEnum`（越界抛 `DecoderException`，原版 `readEnum` 不查）、`writeNullableEnum/readNullableEnum`（0 = 空，其余 = 序号 + 1）、`writeEnumMap/readEnumMap`、`writeEnumSet/readEnumSet`。线上格式与主线一致（VarInt 序号） |
| `registry/world/ModBiomeTags`、`ModDimensions` | 原样复制（Forge 47.4 已补回 `ResourceLocation.fromNamespaceAndPath`） |
| `registry/damage/ModDamageTypes` | 复制；`entity.registryAccess()` → `entity.level().registryAccess()`（1.20.1 的 `Entity` 没有前者） |
| `registry/entity/ModEntityTypeTags`、`registry/item/ModItemTags` | 原样复制 |
| `Guzhenren` | 构造器改为 `Guzhenren(FMLJavaModLoadingContext context)`，取 `context.getModEventBus()`；去掉临时的 `Guzhenren loaded` 日志；类注释照主线，先不列还没搬的注册表 |

挪出模块 1（跟内容走）：`ModParticles`（客户端粒子）、`ModFluidTypes` / `ModFluids` / `ModBlocks`（元泉与方块）、`ModEffects`、`ModItems` / `ModCreativeTabs` / `ModDataComponents`（物品）、`ModEntityTypes`、`ModMenus`、`ModRecipes`、`ModCriteriaTriggers`、`ModAttachments`（玩家数据）。

### 后续模块共用的约定

| 主线 | Forge 1.20.1 |
|---|---|
| `DeferredHolder<R, T>` / `DeferredItem<T>` / `DeferredBlock<T>` | `RegistryObject<T>` |
| `DeferredRegister.create(Registries.X, MOD_ID)`、`createBlocks/createItems` | `DeferredRegister.create(ForgeRegistries.X, MOD_ID)`；没有 Forge 封装的用 `Registries.X` |
| 每个注册类 `register(IEventBus)`，入口按主线顺序调用 | 不变 |
| `ModEffects.X` 当 `Holder<MobEffect>` 用 | `ModEffects.X.get()`；`ModEffects.instance(MobEffect, ...)` |
| Attachment / DataComponent / StreamCodec | 各自模块里按 D1 方案乙写成 Capability / NBT / `FriendlyByteBuf`，`ModByteBufs` 是共用读写帮手 |

### 验收

1. `build` 通过。
2. 冒烟：`runClient` 到标题画面 20 s、`runServer -PserverRun` 到 `Done` 并正常 `stop`。
3. 不新增测试：`ModByteBufs` 的往返测试在模块 2（网络）随首个使用者一起写，那时 GameTest 框架也在。

### 计划

1. 复制 core 四个类与五个无依赖注册类，改 `ResourceLocation`。
2. 写 `ModByteBufs`。
3. 改 `Guzhenren` 构造器。
4. `build` → 客户端冒烟 → 服务端冒烟。

子代理分配：无，文件少且机械。

## 验收结果

| 项 | 结果 |
|---|---|
| `build` | 通过，无警告 |
| `runClient` | 53 个 mod，到标题画面并稳定 20 s |
| `runServer -PserverRun` | `Done (2.030s)`，`stop` 后所有维度存盘 |

实施偏差：`ResourceLocation` 保留主线写法（见上表）；`ModDamageTypes` 一处改为 `entity.level().registryAccess()`。
