# 可移植性回顾：模块 0–2

目标：找出主线（NeoForge 1.21.1）里让移植变难、或让一层改动波及其他层的写法；主线与 Forge 端一起改，让以后的移植和架构改动只动一处。

## 发现

| # | 问题 | 本轮代价 | 改法（两端同改） | 主线波及 |
|---|---|---|---|---|
| R1 | 玩家数据直接用加载器 API：15 个文件 `player.getData/setData(ModAttachments.X)` 31 处 + `setIfChanged` 10 处 | Forge 端换成 Capability 后，这 15 个文件以后全要逐个改 | 主线加 `gameplay/data/PlayerData`，公开接口与 Forge 端完全一致（`get(player)`、各值 getter、setter 返回是否变了、`resetCarriedOnDeath()`），内部仍用 attachment；所有服务只经它读写。之后两端的服务层代码相同，换加载器只改 `PlayerData` 内部 | 15 个文件机械替换 + 新增 1 个 |
| R2 | 网络散在各处：5 个客户端文件直接 `PacketDistributor.sendToServer`；`ModPayloads` 的处理把校验逻辑和 `IPayloadContext` 写在一起 | 5 个客户端文件 + 7 个处理都要随加载器重写 | 主线加 `ModNetwork.sendToServer(…)`（Forge 端已有同名）；7 个请求的校验与执行搬到不依赖加载器的 `ClientIntents`（参数 `ServerPlayer` + 请求记录），`ModPayloads` 只剩注册与转发 | 5 个客户端文件 + `ModPayloads` + 新增 1 个 |
| R3 | 线上格式写在数据记录里：12 个记录各带 NeoForge 的 `STREAM_CODEC` | 本轮子代理逐个改写 12 个文件 | 主线记录改为静态 `encode(FriendlyByteBuf, T)` / `decode(FriendlyByteBuf)`（1.21 的 `RegistryFriendlyByteBuf` 就是 `FriendlyByteBuf`），需要 `StreamCodec` 的地方用 `StreamCodec.of(encode, decode)` 在注册处包一层。两端记录文件完全相同 | 12 个记录 + `ModAttachments` |
| R4 | 只在新 Java / 新原版里有的写法混在领域代码里（主线共 40 处 30 个文件）：`Math.clamp`、`Math.ceilDiv`、`List.getLast/removeLast/addFirst`、`ItemStack.isSameItemSameComponents`、`ItemStack.OPTIONAL_CODEC` | 20 个文件里零散改 | Java 部分改成 17 也有的写法（主线 Java 21 照样能编）；两处原版物品 API 收进一个小类 `core/ItemStacks`（`sameItemSameData`、`OPTIONAL_CODEC`），两端各自实现。之后这 20 个文件两端相同 | 约 20 个文件 + 新增 1 个 |
| R5 | 生命周期靠 NeoForge 的隐式顺序：主线 `LOW` 的 `onClone` 依赖 NeoForge 先自动拷贝 attachment | Forge 端要自己拆成 `HIGHEST`/`LOWEST` 才对上 | 主线 `PlayerDataEvents` 的类注释写明这个顺序约定（已有一半），R1 之后拷贝规则也集中在 `PlayerData` 里，两端各自满足同一约定 | 1 处注释 |
| R6 | 用 attachment「存不存在」表示状态：`BURST` 以 `hasData` 判断爆发、`removeData` 结束；`HEALTH_CARRY` 以 `removeData` 的返回值区分有没有存过 | Forge 端第一版把 `burst` 建模成默认 `true`，语义反了（已由 GameTest 发现并更正） | `PlayerData` 门面写成显式方法：`burst()` / `setBurst(boolean)`、`setHealthCarry` / `takeHealthCarry()`；主线内部仍用存在与否，调用方看不到 | 2 个文件 |

下一轮（效果模块）预告：`ModEffects.X` 被当 `Holder<MobEffect>` 用了 58 处 14 个文件，同类问题，留到那一轮回顾。

## 做法

- 主线改动按主线规则走：`rules/modification.md`、wiki 同步、`check.py`、`commit-push`。
- Forge 端同步：R1 已是目标形态；R2–R4 在 Forge 端对应改，保证改完后这些文件两端逐字一致（加载器专属的只剩 `PlayerData` 内部、`ModNetwork`、`ItemStacks`）。
- 量级：主线约 55 个文件、Forge 端约 25 个文件；主线全量 `check.py` 一次约 3–4 分钟。

## 已定（Alex）

R1–R4 全做（R5 注释、R6 随 R1 一起）。

## 接缝（两端各自实现，其余文件两端逐字一致）

| 接缝 | 主线（NeoForge 1.21.1） | Forge 1.20.1 |
|---|---|---|
| `core/ModByteBufs` | 两端同一份源码：参数改用 `BiConsumer<FriendlyByteBuf, V>` / `Function<FriendlyByteBuf, V>`，集合与映射手写 VarInt 长度 + 逐项读写；替代主线 `ModStreamCodecs` | 同左 |
| `core/ItemStacks` | `sameItemSameData` → `isSameItemSameComponents`；`OPTIONAL_CODEC` → `ItemStack.OPTIONAL_CODEC` | `isSameItemSameTags`；`Codec.either(空 CompoundTag, ItemStack.CODEC)` |
| `gameplay/data/PlayerData` | 门面：`get(player)` 包一层，读写转到 `ModAttachments`；setter 内部即 `setIfChanged` | Capability 本体 |
| `network/ModNetwork` | `sendToServer(payload)`、`openMenu(player, provider, Consumer<FriendlyByteBuf>)` | 同名，`SimpleChannel` / `NetworkHooks.openScreen` |
| 线上读写 | 记录只有 `encode/decode`；`ModAttachments`、`ModDataComponents` 注册处用 `StreamCodec.of(X::encode, X::decode)` | `PlayerDataField` 直接调 `encode/decode` |

## 执行

1. 主会话：写四个接缝（主线版 `ModByteBufs`、`ItemStacks`、`PlayerData`、`ModNetwork`）与 `ClientIntents`。
2. 子代理（主线）：15 个服务改走 `PlayerData`；5 个客户端文件改走 `ModNetwork`；`ModPayloads` 处理改转发到 `ClientIntents`；36 个数据模型以外的 Java 18+ 写法改 17 写法；相应测试。
3. 主会话：20 个数据模型文件由 Forge 端回拷到主线（两端相同），注册处包 `StreamCodec.of`，删 `ModStreamCodecs`。
4. 主线：`check.py` 全量、wiki 同步、`commit-push`。
5. Forge 端：换上同源 `ModByteBufs` 与 `ItemStacks`，`build` + GameTest + 冒烟，提交。

## 结果

- 主线 `b2d2371`（已推送，CI 绿）：`PlayerData` 门面、`ModNetwork` + `ClientIntents`、记录 `encode/decode`、`ModByteBufs`、`ItemStacks`、Java 17 写法；删 `ModStreamCodecs`。`check.py` 全部 5 阶段通过（JUnit 167 + 136、GameTest 170）。wiki 改 6 页：维护说明（版本接缝、附件继承、意图校验）、生命周期、空窍、飞行与疾飞、宝黄天玩法、力量与战斗。
- Forge 端：换上同源 `ModByteBufs`、本端 `ItemStacks`；`build`、GameTest 6/6、冒烟通过。
- 两端逐字一致的共享文件：46 个中 45 个。剩下 `ApertureStorage` 一行（写物品上的 `VITAL_APERTURE`），等物品模块定下物品数据接缝再并。
- 遗留：主线 `Guzhenren` 仍直接注册 `ModAttachments`（注册类属于接缝，合理）；`SoulTradeOffer`、`GuRecipe`、`SoulTradeMenu` 仍有各自的 `STREAM_CODEC`，到交易和炼蛊模块一起改。
