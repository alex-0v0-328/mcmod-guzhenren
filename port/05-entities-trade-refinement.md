# 05 实体 / 交易 / 炼蛊与配方 问题清单

范围（主线未搬部分）：`entity/` 15 个文件、`entity/ai/` 5、`registry/entity/ModEntityTypes`、`gameplay/trade/` 4、`refinement/` 5、`registry/recipe/ModRecipes`、`gameplay/dimension/TreasureYellowHeavenGuardEvents`、datagen 的 `loot/ModLootModifierProvider`、`loot/WildBoarLootProvider`、`recipe/ModRecipeProvider` 与 `ModDatapackProvider` 里的生物群系修饰。共 36 个类、约 6.0k 行。另有可选的实体渲染（见 D1）：`client/renderer/` 4 个文件、约 300 行。

## 结构问题

| # | 问题 | 依据 |
|---|---|---|
| S1 | `GuRecipe` 一个类装了配方数据、匹配逻辑和 1.21 专属的序列化器（`MapCodec` + 三个 `StreamCodec`），配料用 NeoForge 的 `SizedIngredient`；整个类只能当胶水，匹配逻辑两端各一份 | `refinement/GuRecipe.java:48,204-253` |
| S2 | 配方查询返回 `RecipeHolder<GuRecipe>`（1.20.2 才有），炼蛊菜单 4 处 `.value()` 依赖它 | `refinement/GuRecipe.java:105`、`refinement/RefinementMenu.java:176-196` |
| S3 | `SoulTradeOffer` 的线上读写直接写 `StreamCodec.composite`，绕过已定接缝 `ModByteBufs`（「记录的线上读写是各自的 encode/decode，`StreamCodec` 只在注册处包一层」） | `gameplay/trade/SoulTradeOffer.java:35-39`、`SoulTradeMenu.java:56,82` |
| S4 | `SoulTraderEntity` 直接调 `server.openMenu`，绕过接缝 `ModNetwork.openMenu` | `gameplay/trade/SoulTraderEntity.java:140` |
| S5 | `TreasureYellowHeavenGuardEvents` 9 个处理器里写着守卫规则（`inTyh`、`tradesWith`、取消与伤害判定），违反「事件类只做转交」；事件换名时规则跟着动 | `gameplay/dimension/TreasureYellowHeavenGuardEvents.java:58-146` |
| S6 | 测试 `ModGameTests` 一个类 57 个 GameTest、1.3k 行，跨炼蛊、储物、本命蛊、命令等模块；要等所有模块搬完才能整份同源 | `src/test/game/.../ModGameTests.java` |

## API 问题（1.21.1 → 1.20.1）

| # | 主线 | Forge 1.20.1 | 用量 |
|---|---|---|---|
| A1 | GeckoLib 4.9.2：`software.bernie.geckolib.{animatable,animation}.*`、`animation.PlayState`、`animation.state.BoneSnapshot` | GeckoLib 4.8.4：`geckolib.core.{animatable,animation,object,state}.*`（`GeoEntity`、`GeoBone`、`GeoModel`、`GeckoLibUtil` 包名不变） | 9 个文件只差 import |
| A2 | `defineSynchedData(SynchedEntityData.Builder)` | `defineSynchedData()` + `entityData.define(…)`；`ActionEntity` 注释里关于构造器顺序的警告两版都成立 | 2 个文件 |
| A3 | `RegisterSpawnPlacementsEvent`、`SpawnPlacementTypes.ON_GROUND` | `SpawnPlacementRegisterEvent`、`SpawnPlacements.Type.ON_GROUND`、`Operation.REPLACE` | 1 个文件 |
| A4 | `DeferredHolder<EntityType<?>, …>` | `RegistryObject` + `ForgeRegistries.ENTITY_TYPES` | 22 处 1 个文件 |
| A5 | 配方：`RecipeInput`（`GuRecipeInput`）、`assemble` / `getResultItem(HolderLookup.Provider)`、`RecipeSerializer` 的 `codec()` / `streamCodec()` | `Recipe<Container>`、`assemble(Container, RegistryAccess)`、`getId()`、`canCraftInDimensions`；序列化器 `fromJson` / `fromNetwork` / `toNetwork`；没有 `SizedIngredient` | 3 个文件 |
| A6 | 菜单数据 `RegistryFriendlyByteBuf`、`ItemStack.STREAM_CODEC` | `FriendlyByteBuf`、`writeItem` / `readItem` | 2 个文件 |
| A7 | 战利品：`ResourceKey<LootTable>`、`EnchantedCountIncreaseFunction.lootingMultiplier(registries, …)`、NeoForge 自带 `AddTableLootModifier`（`neoforge:add_table`）、修饰器 `MapCodec` | `ResourceLocation`、`LootingEnchantFunction.lootingMultiplier(…)`；**Forge 1.20.1 没有 add_table**，要自己写一个 `LootModifier` 子类掷子表；修饰器 `Codec` | 2 个文件 |
| A8 | 守卫事件：`LivingIncomingDamageEvent`、`PlayerTickEvent.Post` | `LivingAttackEvent`、`TickEvent.PlayerTickEvent` + `phase`；方块、交互、丢弃事件同名 | 1 个文件 |
| A9 | 生物群系修饰：`BiomeModifiers.AddSpawnsBiomeModifier`、`NeoForgeRegistries.Keys.BIOME_MODIFIERS` | `ForgeBiomeModifiers.AddSpawnsBiomeModifier`、`ForgeRegistries.Keys.BIOME_MODIFIERS` | 12 个修饰 |
| A10 | 配方 datagen：`RecipeOutput.accept(id, recipe, null)` | `Consumer<FinishedRecipe>`，`GuRecipe` 要配一个 `FinishedRecipe` 写 JSON | 1 个文件 |
| A11 | 实体渲染（若纳入）：GeckoLib 4.9 渲染层 `render(…, int colour)` 等签名 | 4.8.4 是 `float red, green, blue, alpha` | 1 个文件（`SoulTraderGeoRenderer` 的渲染层） |

## 数据与资源

| # | 问题 | 量 |
|---|---|---|
| D-1 | 目录：`recipe/refinement` → `recipes/refinement`；`loot_table/entities` → `loot_tables/entities`；`data/neoforge/loot_modifiers` → `data/forge/loot_modifiers`；`guzhenren/neoforge/biome_modifier` → `guzhenren/forge/biome_modifier` | 2 + 1 + 2 + 12 |
| D-2 | JSON：配方结果 `{"id": …}` → `{"item": …}`；配料保持主线的平铺格式 `{"count": n, "item": …}`（自写编解码对齐它）；修饰器类型 `neoforge:add_spawns` → `forge:add_spawns`；`add_table` 换成自写修饰器的 id | 同上 |
| D-3 | 资源：`geo/` 6、`animations/` 6、`textures/entity/` 22，两版格式相同，原样复制 | 34 个文件 |

## 测试

| 层 | 本模块可搬 | 说明 |
|---|---|---|
| L1 | `ActionAnimationControllerTest`、`EntityResourcesTest` | 先确认它们不读 1.21 专属类 |
| L2 | `GuRecipeTest`、`SoulTradeOfferTest` 等 | Forge 端 L2 不跑；`SoulTradeOfferTest:49` 的 `getComponentsPatch()` 随 S3 改 |
| GameTest | `WildBoar` 22、`Bear` 20、`Beetle` 17、`Tiger` 17、`TreasureYellowHeaven` 16、`SoulTrader` 9、`HoeHarvest` 5、`AnchoredTravel` 3 | 共 109 条；`HoeHarvest` 等全局战利品修饰（A7）；`AnchoredTravel` / `TreasureYellowHeaven` 要 Gu World 的维度，Gu World 移植前先跳过还是用占位维度待定 |

## 不在本模块

- `mixin/` 两个客户端 mixin（爆发飞行加速）：随客户端模块。
- 交易、炼蛊的界面（`client/screen`）：随客户端模块；本模块只搬菜单和服务端逻辑。
- `command/`、`compat/`（Epic Fight）、`network/payload`：后续模块。

## 待你决定

| # | 问题 | 选项 |
|---|---|---|
| D1 | 实体渲染放哪个模块。没有渲染器时，客户端一看到这些实体就崩 | 甲（推荐）：本模块带上 4 个渲染文件和 34 个资源，实体在客户端可见；乙：本模块只做服务端，客户端模块再补，期间只跑服务器和 GameTest |
| D2 | 配料类型 | 甲（推荐）：主线也改用自己的 `GuIngredient(Ingredient, int count)`，JSON 格式不变，两端同源；乙：主线留 NeoForge `SizedIngredient`，Forge 端单独写一个 |
| D3 | 全局战利品 add_table | 甲（推荐）：Forge 端自写 `AddTableLootModifier`（Forge 原生写法），主线继续用 NeoForge 自带的；乙：两端都用自写的 `guzhenren:add_table`，同源但主线重复了 NeoForge 已有功能 |
| D4 | 依赖 Gu World 维度的 19 条 GameTest | 甲（推荐）：先不搬，Gu World 移植后再搬；乙：Forge 端测试用占位维度 |
| D5 | 顺序 | 甲（推荐）：同前几轮，先在主线修 S1–S5（拆序列化器、去 `RecipeHolder`、交易走 `ModByteBufs` 和 `openMenu`、守卫规则搬进服务），再写规格；乙：直接写规格，回顾时再修 |

## 已定（Alex）

| # | 决定 |
|---|---|
| D1 | 甲：本模块带上实体渲染和 34 个资源 |
| D2 | 甲：主线也改用自写 `GuIngredient(Ingredient, int count)`，JSON 不变 |
| D3 | 甲：Forge 端自写 `AddTableLootModifier`，主线照旧 |
| D4 | 甲：依赖 Gu World 维度的 19 条 GameTest 等 Gu World 移植后再搬 |
| D5 | 甲：先在主线修 S1–S5（D2 一并做），再写规格 |

## 前置：主线修 S1–S5（已完成）

主线 `0d112c4`、`983550c`、`ad75d38`（CI 绿）：

| 提交 | 改动 |
|---|---|
| `0d112c4` 配方 | 新 `refinement/GuIngredient(Ingredient, int count)` 取代 `SizedIngredient`；`GuRecipe` 只剩数据与匹配，不再实现 `Recipe`；新 `refinement/RefinementRecipe(GuRecipe)` 是唯一碰原版 recipe API 的类（`Recipe<RecipeInput>`、序列化器、按 id 排序的 `known` 返回 `List<GuRecipe>`）；`GuRecipeInput` 不再实现 `RecipeInput`。生成的配方 JSON 不变 |
| `983550c` 交易 | `ItemCost`（1.20.5 才有）换成新 `gameplay/trade/TradeCost(Item, int count)`；报价走自己的 `encode` / `decode` 与 `ItemStacks.writeStack` / `readStack`；菜单数据改 `FriendlyByteBuf`；开菜单走 `ModNetwork.openMenu` |
| `ad75d38` 守卫 | 规则移进新 `gameplay/dimension/TreasureYellowHeavenGuard`，事件类只转交；宝黄天 GameTest 改测服务 |

同时：GameTest 里的 `getFirst()`（Java 21）改 `get(0)`；wiki《维护说明》接缝清单加 `RefinementRecipe` 与 `ItemStacks` 线上读写，Java 17 约束扩到测试。

## 规格（模块 5）

### 范围

| 搬 | 内容 |
|---|---|
| 实体 | `entity/**` 15 个类、`entity/ai/**` 5、`registry/entity/ModEntityTypes`、`EntityRegistrationEvents`（属性、生成规则） |
| 交易 | `gameplay/trade/**` 5 个类（含 `TradeCost`）；`ModMenus` 加交易菜单 |
| 炼蛊 | `refinement/**` 7 个类；`registry/recipe/ModRecipes`；`ModMenus` 加炼蛊菜单 |
| 宝黄天 | `TreasureYellowHeavenGuard`（同源）+ `TreasureYellowHeavenGuardEvents`（Forge 事件） |
| 全局战利品 | Forge 端自写 `loot/AddTableLootModifier` + 注册（D3）；月兰花瓣修饰 |
| 渲染（D1） | `client/renderer/` 4 个文件；资源 `geo/` 6、`animations/` 6、`textures/entity/` 22 |
| datagen | 配方、野猪战利品表、全局战利品修饰与子表、12 个生物群系修饰（`ModDatapackProvider`） |
| 不搬 | 交易与炼蛊界面（`client/screen`，客户端模块）；开菜单时客户端没有界面，只记一条警告，不崩；`mixin/`；命令；Epic Fight 兼容 |

约 40 个类、约 6.5k 行。

### 同源文件（主线原样复制）

`GuRecipe`、`GuIngredient`、`GuRecipeInput`、`RecipeAutofill`、`RefinementMenu`、`RefinementRitual`、`SoulTradeOffer`、`TradeCost`、`SoulTrader`、`SoulTradeMenu`、`TreasureYellowHeavenGuard`、`entity/ai/**`、`BearSpecies`、`HeldDeath`、`WildGuEntity` 等不碰 GeckoLib 与同步数据的实体类。

### 接缝（Forge 端实现）

| 接缝 | Forge 1.20.1 实现 |
|---|---|
| `RefinementRecipe` | `record RefinementRecipe(ResourceLocation id, GuRecipe gu) implements Recipe<Container>`；`matches` 用 `GuRecipeInput.of(container)`；序列化器 `fromJson`：每个配料对象先 `Ingredient.fromJson` 再读 `count`；`toNetwork` / `fromNetwork` 用 `Ingredient.toNetwork` 与 `writeItem`；`known` 按 `getId()` 排序；带一个 `FinishedRecipe` 给 datagen 写 JSON（配料 `ingredient.toJson()` 加 `count`，结果 `{"item", "count"}`） |
| `ItemStacks.writeStack` / `readStack` | `buf.writeItem` / `buf.readItem` |
| `AddTableLootModifier`（仅 Forge 端） | `LootModifier` 子类，`Codec` 带 `table`；`doApply` 用 `context.getResolver().getLootTable(table).getRandomItemsRaw(context, loot::add)`；类型 `guzhenren:add_table`，注册于 `ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS` |

### 加载器 / 版本差异

| 主线 | Forge 1.20.1 | 文件 |
|---|---|---|
| GeckoLib 4.9 `geckolib.animation.*`、`animatable.GeoAnimatable` | 4.8.4 `geckolib.core.animation.*`、`core.animatable.*`、`core.object.PlayState`、`core.state.BoneSnapshot` | 9 个实体 + 渲染，只差 import |
| `defineSynchedData(SynchedEntityData.Builder)` | `defineSynchedData()` + `entityData.define` | `ActionEntity`、`SoulTraderEntity` |
| `RegisterSpawnPlacementsEvent`、`SpawnPlacementTypes.ON_GROUND` | `SpawnPlacementRegisterEvent`、`SpawnPlacements.Type.ON_GROUND`、`Operation.REPLACE` | `EntityRegistrationEvents` |
| `DeferredHolder` | `RegistryObject` + `ForgeRegistries.ENTITY_TYPES` / `RECIPE_TYPES` / `RECIPE_SERIALIZERS` | `ModEntityTypes`、`ModRecipes` |
| `IMenuTypeExtension.create` | `IForgeMenuType.create` | `ModMenus` |
| `LivingIncomingDamageEvent`、`PlayerTickEvent.Post` | `LivingAttackEvent`、`TickEvent.PlayerTickEvent` + `phase` | `TreasureYellowHeavenGuardEvents` |
| GeckoLib 渲染层 `int colour` 签名 | `float red, green, blue, alpha` | `SoulTraderGeoRenderer` |
| `EntityRenderersEvent.RegisterRenderers`（NeoForge） | 同名（`net.minecraftforge.client.event`），`Dist.CLIENT` 的 MOD 总线订阅 | `ModEntityRenderers` |
| datagen：`RecipeOutput`、`EnchantedCountIncreaseFunction`、`ResourceKey<LootTable>`、`BiomeModifiers.AddSpawnsBiomeModifier` | `Consumer<FinishedRecipe>`、`LootingEnchantFunction`、`ResourceLocation`、`ForgeBiomeModifiers.AddSpawnsBiomeModifier` | 4 个 provider |

数据目录与格式按 D-1、D-2：`recipes/refinement`、`loot_tables/entities`、`data/forge/loot_modifiers`、`guzhenren/forge/biome_modifier`；配料 JSON 与主线逐字一致，结果 `id` → `item`。

### 测试

| 层 | 内容 |
|---|---|
| L3 复制 | `WildBoarGameTests` 22、`BearGameTests` 20、`BeetleGameTests` 17、`TigerGameTests` 17、`SoulTraderGameTests` 9、`HoeHarvestGameTests` 5，共 90 条；只换 `@GameTestHolder` 的导入包 |
| L3 推迟（D4） | `TreasureYellowHeavenGameTests` 16、`AnchoredTravelGameTests` 3：等 Gu World；`ModGameTests` 57：跨命令等模块，最后整份搬 |
| L3 新增（仅 Forge 端） | 炼蛊：两张蛊方从 JSON 载入，`RefinementRecipe.known` 按 id 排序；四味酒虫的摆法 `claim` 成功、少一格失败。交易：报价经 `encodeList` / `decodeList` 往返相等 |
| datagen | `runData -PdataRun` 后 `src/generated` 无漂移 |

### 验收

1. `build`、L1 全过；`runData` 无漂移。
2. `runGameTestServer -PserverRun`：原有 21 条 + 复制 90 条 + 新增全过。
3. 冒烟：服务端 `Done` 并 `stop`；客户端到标题画面 20 s。
4. 一致性：同源文件逐字一致；其余文件只差上表所列。
5. 实体在客户端的外观与动画、生成密度留给你在 `runClient` 看。

### 计划

1. 主会话：Forge 端接缝（`RefinementRecipe`、`ItemStacks` 线上读写、`AddTableLootModifier` 与注册）；复制同源文件。
2. 子代理 A：实体、AI、`ModEntityTypes`、`EntityRegistrationEvents`、交易实体、渲染与 34 个资源、宝黄天事件胶水。
3. 子代理 B（与 A 并行，写不同文件）：`ModRecipes`、`ModMenus` 两个菜单、datagen 四处（配方、野猪战利品、全局战利品修饰与子表、生物群系修饰）与 `DataGenerators`，跑 `runData`。
4. 主会话：入口 `Guzhenren` 接上新注册表；复制 90 条 GameTest，写新增用例；跑验收与一致性检查；提交 Forge 端。
5. 可移植性回顾，同前几轮。

子代理分配：A、B 两个，模型由你选。B 依赖 A 的类名（`ModEntityTypes.X`、`SoulTradeMenu`）而不依赖实现，可并行；`Guzhenren.java` 只由主会话改。

## 验收结果

| 项 | 结果 |
|---|---|
| build、L1 | 通过，L1 154 |
| runData | 新增 17 个生成文件，原有生成数据无漂移；与主线 JSON 的差别只有 1.20.1 格式（结果 `item`、`forge:` 类型、`guzhenren:add_table`、`looting_enchant`、熔炼条件少 1.21 的 `smelts_loot` 附魔分支、锄头谓词写法） |
| GameTest | 108 条全过（原 21 + 复制 84 + 新增 3） |
| 冒烟 | 服务端 `Done` 后 `stop`；客户端到标题画面并存活 20 s |
| 一致性 | main 235 个同名文件 158 个一致；L1 30/30；GameTest 11 个只差注解导入包与接缝本体 |

## 实施偏差

- 1.20.1 API 缺口（实体）：`Mob.stopInPlace` 内联其 1.21 实现；`getBaseExperienceReward` → 覆写 `getExperienceReward()`；`MobSpawnType.ignoresLightRequirements` 只对 1.20.1 没有的试炼刷怪笼成立，去掉该分支；`PathType` → `BlockPathTypes`（`LandRestGoal`）；`ignoreExplosion(Explosion)` → `ignoreExplosion()`、`canBeLeashed()` → `canBeLeashed(Player)`；GeckoLib 4.8.4 `stopTriggeredAnim` → `stopTriggeredAnimation`、`GeoBone`/`GeoModel` 在动画处理处是 `CoreGeoBone`/`CoreGeoModel`、`reRender` 颜色为四个 float；`RestingFlyingGuEntity` 也有同步数据。
- GameTest 环境：Forge 的 GameTest 服务器沿用专用服务器的世界（难度来自 `server.properties` 的 easy、默认地形、持久存档），原版 1.21 每次新建平坦 Normal 测试世界并用屏障围住每个结构。Forge 端补：`build.gradle` 每次运行前删世界并设平坦地形；`GameTestServerSetup`（仅 Forge 端）在启动时设 Normal 难度、关刷怪与天气循环，并在第一刻给每个测试结构围一圈屏障（不加顶，对应 1.21 的 `skyAccess`）。修好前 11 条失败：7 条伤害按 easy 折算偏低，4 条被相邻测试的模拟玩家吸引仇恨。
- 主线测试接缝（`src/test`，不进 Git）：新 `GameTestWorld`（`openSky`、`entities`、`enchantLooting`、`ignite`、`leashable`、`arrow`）；`skyAccess = true` 改成测试开头 `GameTestWorld.openSky(helper)`；`TigerGameTests` 的 `DeferredHolder` 参数改 `Supplier`。主线 `check.py build gametest` 通过。
- `ModMenus`、`ModRecipes` 由主会话写（原计划给子代理 B）。
