# 02 玩家数据 + 网络 问题清单

原则（Alex 定）：正视加载器与游戏版本的差异，用 Forge 1.20.1 原生机制做出与主线相同的效果；不照搬 1.21 的结构。

## 主线的效果（要复刻的行为）

| # | 效果 | 主线怎么做到的 |
|---|---|---|
| E1 | 12 份玩家数据随存档保存；2 份（`ESSENCE_CARRY`、`HEALTH_CARRY`）只在内存、不存档 | `AttachmentType.serialize(codec)` |
| E2 | 取不到就是默认值；`removeData` 回到默认值 | `AttachmentType.builder(() -> DEFAULT)` |
| E3 | 死亡和从末地回来时，11 份数据带到新玩家实体上；`BURST` 不带（回到默认 `true`） | `copyOnDeath()`；`PlayerDataService.onClone` 在 `LOW` 优先级、拷贝之后再按「死亡且未开死亡不掉落」重置 |
| E4 | 9 份数据只同步给本人：服务端一改，本人客户端立刻拿到新值；登录、重生、换维度后客户端也是最新的 | `sync(OWNER_ONLY, streamCodec)`，NeoForge 自动在改动和进入时发 |
| E5 | 客户端读玩家数据和服务端用同一入口（各 `XxxService.get(player)`） | 客户端实体上同样有 attachment |
| E6 | 7 个客户端请求（打开空窍储物、选副道、打开炼蛊、温养、冲击窍壁、冲撞、切换爆发）只在服务端执行，先校验：不在宝黄天、空窍下标合法、数值范围、`Float.isFinite` | `PayloadRegistrar.playToServer`，协议版本 `"1"` |
| E7 | 打开菜单时把空窍下标带给客户端菜单 | `player.openMenu(provider, buf -> buf.writeVarInt(i))` |
| E8 | 坏包（越界枚举等）断开发送者并给出可读原因 | `ModStreamCodecs.byOrdinal` 抛 `DecoderException` |

## 问题

| # | 问题 | 依据 |
|---|---|---|
| P1 | Forge 1.20.1 没有 Attachment：E1–E5 全要自己用 Capability 搭——挂载（`AttachCapabilitiesEvent<Entity>`）、存档（`ICapabilitySerializable<CompoundTag>`，Codec 经 `NbtOps`）、默认值、拷贝、同步 | `registry/attachment/ModAttachments.java` 14 份 |
| P2 | `PlayerEvent.Clone` 时旧实体的 Capability 已失效，读之前要 `original.reviveCaps()`，读完 `invalidateCaps()`；漏了就拿到空值，死亡后数据全丢 | Forge 1.20.1 Capability 生命周期 |
| P3 | 同步不会自动发：每次改动、登录、重生、换维度都要手动发包。重生和换维度时客户端会新建玩家实体，旧数据跟着没了，漏发就会显示默认值 | E4 |
| P4 | 同步包到客户端时本地玩家可能还没建好（登录早期），处理要排进主线程并判空 | Forge `NetworkEvent.Context.enqueueWork` |
| P5 | 没有 `StreamCodec` / `ByteBufCodecs`：9 份同步数据的 `STREAM_CODEC`（`Aperture`、`BodyData` 等）改成 `FriendlyByteBuf` 读写，按字段顺序原样保留 | 10 处 `StreamCodec.composite` 与手写编解码 |
| P6 | 网络通道：NeoForge `RegisterPayloadHandlersEvent` / `CustomPacketPayload` → Forge `SimpleChannel`（协议版本、编号、方向）；客户端发包 `PacketDistributor.sendToServer` → `CHANNEL.sendToServer` | `network/ModPayloads.java`、`client/**` 5 处发包 |
| P7 | 带数据打开菜单：`player.openMenu(provider, buf)` → `NetworkHooks.openScreen(player, provider, buf)`；菜单类型 `IMenuTypeExtension.create` → `IForgeMenuType.create` | E7 |
| P8 | `ApertureStorage` 用 `ItemStack.OPTIONAL_CODEC`（1.20.5 才有，空格子也能编码），1.20.1 只有 `ItemStack.CODEC`；空格子要能原样存回，否则储物格位置错乱 | `gameplay/aperture/storage/ApertureStorage.java:50,52` |
| P9 | Java 21 写法：`getLast()` / `removeLast()` | `ApertureStorage.java:62,65` |
| P10 | 存档不跨版本：1.21.1 的世界在 1.20.1 打不开（原版就不支持降级），玩家数据格式不需要和主线兼容，只需要 1.20.1 自身前后一致 | — |

## 依赖与边界

- 玩家数据模型（10 份数据记录 + 它们用到的枚举与值类型）共 36 个文件、约 2600 行，只依赖 `core/` 和 `ModDataComponents`（`ApertureStorage` 用 1 处 `VITAL_APERTURE`，经 `RefinedGuState`、`LeafGrowth` 两个小类收口）；可以整体放进本模块。
- 读写数据的各 `XxxService`（`ApertureService`、`BodyService` 等）依赖链很长（效果、物品、维度时钟），不在本模块。
- 7 个客户端请求的处理都调用 gameplay 服务和菜单，随各自功能模块搬；本模块只建通道和同步包。
- `ModDataComponents` 在 D1 下是物品 NBT，物品模块才定写法；`ApertureStorage` 那 1 处要先有个落脚点。

## 已定（Alex）

- D1 甲：一个玩家 Capability 装全部 14 份数据。
- D2 值变就发，照主线 `setIfChanged`。
- D3 `VITAL_APERTURE` 先作为 NBT 键写在 `ApertureStorage` 旁边，物品模块再收进统一写法。

## 原选项

- **D1 Capability 粒度**
  - 甲（推荐）：一个玩家 Capability 装全部 14 份数据，一次挂载、一次存档、一次拷贝；同步按份发包。Forge 模组里常见，生命周期只写一遍。
  - 乙：每份数据一个 Capability（14 个），各自挂载、存档、拷贝。和主线一份一份的划分最像，重复代码多。
- **D2 同步频率**：主线每次 `setData` 改动就同步（`setIfChanged` 只在值变了才写）。照搬即「值变就发」；另一种是每 tick 末尾合并成一个包。推荐照搬，行为一致、实现简单。
- **D3 `ApertureStorage` 的 `VITAL_APERTURE`**：本模块先把这一个物品 NBT 键写在 `ApertureStorage` 旁边，物品模块再收进统一写法；或者这一处推迟到物品模块再搬。推荐前者，数据模型能一次搬完。

## 规格（模块 2）

### 数据字段

| 字段（主线 attachment） | 类型 | 存档键 | 存档 | 死亡带走 | 末地返回带走 | 同步本人 |
|---|---|---|---|---|---|---|
| `aperture`（`APERTURE`） | `ApertureData` | `aperture_data` | 是 | 是 | 是 | 是 |
| `essenceCarry`（`ESSENCE_CARRY`） | `float[MAX_APERTURES]` | — | 否 | 否 | 否 | 否 |
| `apertureStorage`（`APERTURE_STORAGE`） | `ApertureStorage` | `aperture_storage` | 是 | 是 | 是 | 否 |
| `nourish`（`APERTURE_NOURISH`） | `ApertureNourishData` | `nourish_data` | 是 | 是 | 是 | 是 |
| `body`（`BODY`） | `BodyData` | `body_data` | 是 | 是 | 是 | 是 |
| `soul`（`SOUL`） | `SoulData` | `soul_data` | 是 | 是 | 是 | 是 |
| `path`（`PATH`） | `PathData` | `path_data` | 是 | 是 | 是 | 是 |
| `strength`（`PATH_STRENGTH`） | `PathStrengthData` | `strength_data` | 是 | 是 | 是 | 是 |
| `qi`（`PATH_QI`） | `PathQiData` | `qi_data` | 是 | 是 | 是 | 是 |
| `mind`（`MIND`） | `MindData` | `mind_data` | 是 | 是 | 是 | 是 |
| `dimensionReturn`（`DIMENSION_RETURN`） | `DimensionReturnData` | `dimension_return` | 是 | 是 | 是 | 否 |
| `burst`（`BURST`） | `boolean`，默认 `false`（主线：attachment 存在即在爆发） | `burst_flag` | 是 | **否** | 是 | 是 |
| `born`（`BORN`） | `boolean`，默认 `false` | `born_flag` | 是 | 是 | 是 | 否 |
| `healthCarry`（`HEALTH_CARRY`） | `Float`，可空（主线：`removeData` 取出并清空） | — | 否 | 否 | 否 | 否 |

合计：存档 12、死亡带走 11、末地返回带走 12、同步 9。规则核对自 NeoForge 21.1.252 `AttachmentInternals.copyEntityAttachments`：死亡只拷 `copyOnDeath`，非死亡拷全部可存档的。

### 结构

| 文件 | 内容 |
|---|---|
| `gameplay/data/PlayerData` | Capability 实现：14 个字段、各字段 getter；setter 返回「是否变了」，变了且挂在 `ServerPlayer` 上就发同步包（替代 `setData` + `setIfChanged`）；`reset(field)` 回默认值（替代 `removeData`）；`copyFrom(PlayerData, boolean wasDeath)` 按上表；`serializeNBT` / `deserializeNBT` 用各记录的 `Codec` 经 `NbtOps`，单个字段解码失败记日志并用默认值；`static PlayerData get(Player)` 统一入口 |
| `gameplay/data/PlayerDataField` | 枚举，9 个同步字段各一项，带该字段的 `FriendlyByteBuf` 写和读；同步包按它编号 |
| `registry/capability/ModCapabilities` | `CapabilityToken` 取 `PLAYER_DATA`；`RegisterCapabilitiesEvent` 注册；`AttachCapabilitiesEvent<Entity>` 给每个 `Player`（含客户端 `LocalPlayer`、假玩家）挂 `guzhenren:player_data` |
| `lifecycle/PlayerDataEvents`（本模块只放数据部分） | `PlayerEvent.Clone`：`original.reviveCaps()` → `copyFrom` → `original.invalidateCaps()`，`HIGH` 优先级，保证主线 `LOW` 的重置逻辑在拷贝之后；登录、重生、换维度后整份同步 9 个字段 |
| `network/ModNetwork` | `SimpleChannel` `guzhenren:main`，协议 `"1"`，两端版本须相同；消息编号按注册顺序；本模块注册 `PlayerDataSyncPacket` |
| `network/packet/PlayerDataSyncPacket` | 服务端 → 客户端：`PlayerDataField` 序号（越界即坏包）+ 该字段数据；客户端 `enqueueWork` 里取 `Minecraft.getInstance().player`，为空丢弃；客户端处理类经 `DistExecutor` 隔离，服务端不加载 |
| 数据模型 36 个文件 | 复制；每个 `STREAM_CODEC` 改成静态 `write(FriendlyByteBuf, T)` / `read(FriendlyByteBuf)`，字段顺序不变；`ModStreamCodecs` 调用改 `ModByteBufs`；`getLast()` / `removeLast()` 改下标写法；`Codec` 原样 |
| `ApertureStorage` | `ItemStack.OPTIONAL_CODEC` → 本类私有的可空物品 `Codec`：空格子编码为空 `CompoundTag`，读回 `ItemStack.EMPTY`，其余走 `ItemStack.CODEC`；`VITAL_APERTURE` 写成 NBT 键常量 `"guzhenren:vital_aperture"`，`copy.getOrCreateTag().putInt(...)` |

入口 `Guzhenren`：注册 `ModCapabilities`、`ModNetwork.register()`（在构造器里，早于任何发包）。

### 不在本模块

各 `XxxService`（读写入口）、7 个客户端请求的包与处理、菜单打开——随功能模块搬，届时都走 `PlayerData.get(player)` 与 `ModNetwork`。

### 测试（GameTest，本模块起建）

`gametest` 源码集首次启用：模板 `empty9x9x9.nbt` 从主线复制到 `data/guzhenren/structures/`（1.20.1 是复数目录），`@GameTestHolder(Guzhenren.MOD_ID)` + `@PrefixGameTestTemplate(false)`。

| 测试 | 断言 |
|---|---|
| 存档往返 | 改 12 个存档字段 → `serializeNBT` → 新实例 `deserializeNBT` → 全部相等；2 个不存档字段回默认 |
| 死亡拷贝 | `copyFrom(old, true)` 后 `burst` 回 `true`、两个不存档字段回默认、其余 11 个相等 |
| 末地返回拷贝 | `copyFrom(old, false)` 后 12 个存档字段相等 |
| 储物空格子 | `[A, 空, B]` 往返后仍是 3 格、中间为空 |
| 同步字节往返 | 9 个同步字段各 `write` → `read` 相等 |
| 坏包 | 越界的 `PlayerDataField` 序号读出时抛 `DecoderException` |

### 验收

1. `build` 通过。
2. `runGameTestServer` 6 项全过。
3. 冒烟：`runClient` 到标题画面 20 s；`runServer -PserverRun` 到 `Done` 并 `stop`。
4. 进游戏的手动验证（死亡、末地返回、重进后数值不变）留到有服务和界面之后；本模块靠上面的 GameTest 覆盖。

### 计划

1. 复制数据模型 36 个文件，改 `STREAM_CODEC` / `getLast` / `OPTIONAL_CODEC` / `VITAL_APERTURE`。
2. 写 `PlayerData`、`PlayerDataField`、`ModCapabilities`。
3. 写 `ModNetwork`、`PlayerDataSyncPacket`、客户端处理。
4. 写 `PlayerDataEvents`（拷贝与三种整份同步）。
5. 接入 `Guzhenren`。
6. 建 GameTest 源码集与 6 项测试。
7. `build` → `runGameTestServer` → 冒烟。

子代理分配：第 1 步是机械改写（36 个文件），可交给一个子代理，模型由你选；其余在主会话做。第 1 步与第 2–4 步写不同文件，可并行。

## 验收结果

| 项 | 结果 |
|---|---|
| `build` | 通过 |
| `runGameTestServer -PserverRun` | `All 6 required tests passed` |
| `runClient` | 到标题画面并稳定 20 s |
| `runServer -PserverRun` | `Done (2.299s)`，`stop` 后所有维度存盘 |

## 实施偏差

- 克隆拆成两个处理：`HIGHEST` 恢复旧实体 Capability 并拷贝，`LOWEST` 才让旧实体失效——主线 `LOW` 优先级的 `onClone` 还要读旧玩家（`from.getData(APERTURE)`），在它之前失效会读空。
- `PlayerData` 用具体字段与 getter/setter；同步字段的线上读写集中在 `PlayerDataField` 枚举。setter 返回是否变了，替代 `setData` + `setIfChanged`；`resetCarriedOnDeath()` 替代按 `getInherited()` 逐个 `removeData`。
- `gametest` 源码集的编译与运行类路径要显式加上 `main` 的类路径（ForgeGradle 不像 MDG 自动带）。
- `runGameTestServer` 也要带 `-PserverRun`（它和专用服务端一样加载不了纯客户端 mod）；它崩溃时 Gradle 仍报 `BUILD SUCCESSFUL`，以日志里的 `All N required tests passed` 为准。
- 1.20.1 的 `GameTestHelper` 没有 `assertValueEqual`，测试类自带 `assertEqual`。
- 数据模型改写（子代理）：`Math.clamp`（Java 21）→ `Mth.clamp` 或 `Math.min/max`；`Math.ceilDiv`（Java 18）→ `-Math.floorDiv(-a, r)`；`addFirst` → `add(0, …)`；`ByteBufCodecs.list(MAX_APERTURES)` 的上限检查手写，报错文字与主线一致；`ItemStack.isSameItemSameComponents` → `isSameItemSameTags`；`ApertureStorage` 的可空物品 Codec 用 `Codec.either(空 CompoundTag, ItemStack.CODEC)`，空格子存为 `{}`。
- 遗留：部分 Javadoc 仍提到 data component / `OWNER_ONLY`，跟物品模块与回顾一起改；读 `guzhenren:vital_aperture` 的一方要把缺键当 `PRIMARY`。
