# 06 网络意图 / 命令 / Epic Fight 兼容

范围（主线未搬部分）：`network/ClientIntents`、`network/ModPayloads`、`network/payload/` 7 个意图记录；`command/**` 17 个类；`compat/EpicFightIntegration`、`EpicFightServerEvents`。约 2.1k 行。

## 问题

| # | 问题 | 依据 |
|---|---|---|
| S1 | 7 个意图记录实现 NeoForge 的 `CustomPacketPayload`，`StreamCodec` 写在记录里，违反接缝规则；`ModNetwork.sendToServer` 只收 `CustomPacketPayload` | `network/payload/*.java`、`network/ModNetwork.java` |
| A1 | 意图注册：`RegisterPayloadHandlersEvent` + `PayloadRegistrar.playToServer` | Forge：`ModNetwork.CHANNEL.messageBuilder(…, PLAY_TO_SERVER)`，主线程处理，`ctx.getSender()` |
| A2 | 命令注册：NeoForge `RegisterCommandsEvent` | Forge 同名事件（`net.minecraftforge.event`） |
| A3 | Epic Fight：主线 21.x API，Forge 端 20.14.17 | 能力、技能、游戏规则的类名与方法以 20.14 jar 为准 |

## 已定（自动批准：按推荐）

| # | 决定 |
|---|---|
| D1 | 先在主线修 S1：意图记录只剩数据与 `encode`/`decode`（两端同源）；NeoForge 包装放进 `ModPayloads` 的通用 `Carrier`，`ModNetwork.sendToServer(Object)` 按类找类型 —— 主线 `d73e582`（CI 绿） |
| D2 | Forge 端 `ModPayloads` 把同一批记录注册到 `ModNetwork.CHANNEL`，消息号接在 `PlayerDataSyncPacket` 后，由 `ModNetwork.register()` 调用 |
| D3 | 命令与兼容层本模块一起搬；Epic Fight API 不同处只在兼容层两个文件里改 |

## 规格

| 搬 | 做法 |
|---|---|
| 同源 | `ClientIntents`、7 个意图记录、`command/**`（除注册入口）|
| 接缝 | Forge `ModPayloads.register(SimpleChannel, int firstId)`；`ModNetwork.sendToServer(Object)` 已是 Forge 写法 |
| 胶水 | `command/root/ModCommand` 的注册事件；`compat/*` 按 20.14 API |

测试：主线 L2 `PayloadDecodeTest` 只测记录，Forge 端 L2 不跑；Forge 端新增 GameTest：每个意图记录经 `encode`/`decode` 往返相等、伪造序号抛 `DecoderException`；命令树在服务器上能解析 `/gzr`（`Commands.performPrefixedCommand` 对假玩家执行 `gzr info`）。

验收同前：build、L1、GameTest 全过，runData 无漂移，服务器与客户端冒烟。

子代理：命令与兼容层一个（gzr-balanced）；网络由主会话做。

## 验收结果

build、L1 154、GameTest 110（新增意图往返与命令各 1）全过；runData 无漂移；服务端、客户端冒烟正常。命令 15/17 与主线逐字一致（`ModCommand` 事件胶水、`CmdInfo` 模式匹配 switch → `instanceof` 链）；Epic Fight 20.14：全局事件钩子 → 每个玩家的 `PlayerEventListener`（`EntityJoinLevelEvent` 时挂上，同一 UUID 重复挂会替换）、`ComboAttackEvent` → `BasicAttackEvent`、属性修饰键由 `ResourceLocation` 派生 UUID；`LivingIncomingDamageEvent` → `LivingAttackEvent`。
