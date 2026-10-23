# 07 客户端

范围：`GuzhenrenClient`、`client/**` 33 个类（HUD 4、界面 13、图标 3、输入 3、粒子 2、事件 3、流体外观 1、`GuiBatch`、`ModKeyMappings`、`ModPalette`）、`mixin/` 2 个（爆发飞行加速，MixinExtras 注解）。约 3.7k 行。实体渲染已随模块 5 搬过。

## 问题

| # | 主线 | Forge 1.20.1 |
|---|---|---|
| S1 | HUD 直接实现 1.21 的 `LayeredDraw.Layer`、读 `DeltaTracker` | 已在主线修：HUD 只有 `render(GuiGraphics)`，图层由 `ClientEvents` 包一层 —— `b10090a`（CI 绿） |
| A1 | `RegisterGuiLayersEvent` + `VanillaGuiLayers` | `RegisterGuiOverlaysEvent` + `VanillaGuiOverlay`（`registerAbove(… .id(), name, (gui, g, partialTick, w, h) -> …)`） |
| A2 | `RegisterMenuScreensEvent` | `FMLClientSetupEvent.enqueueWork` 里 `MenuScreens.register` |
| A3 | `IClientMobEffectExtensions`（NeoForge 签名）、效果图标注册用 `Holder` | Forge 1.20.1 同名接口，`MobEffect.initializeClient` 或注册时挂扩展；签名以 1.20.1 为准 |
| A4 | 粒子 `VertexConsumer.addVertex/setColor/setUv` | `vertex().color().uv().uv2().endVertex()` |
| A5 | `RegisterClientExtensionsEvent`（流体外观） | Forge 端流体类型已用 `initializeClient`（模块 4），`SpiritSpringClientExtensions` 的外观并入 |
| A6 | MixinExtras（`@ModifyExpressionValue`、`@ModifyReturnValue`） | Forge 1.20.1 需自带 MixinExtras（`annotationProcessor` + `jarJar`）或确认 Forge 已带；mixin 配置要 refmap |
| A7 | 事件包名 | `net.minecraftforge.client.event.*`，客户端订阅写 `Dist.CLIENT` 与总线 |

## 已定（自动批准：按推荐）

D1 主线先修 S1（已完成）。D2 粒子顶点、效果图标签名差异留在各自文件（客户端渲染 API 无法同源），回顾时再看能否收窄。D3 两个子代理并行：C1 界面与 HUD，C2 入口、事件、输入、图标、粒子、流体外观、mixin 与构建。

## 验收

build、L1、GameTest、runData 无漂移；服务端冒烟；客户端冒烟到标题画面 20 s。界面与 HUD 的外观、按键、粒子手感留给你在 `runClient` 看。

## 验收结果

| 项 | 结果 |
|---|---|
| build、L1 | 通过，L1 170（与主线同数，34 个文件全部同源） |
| GameTest | 190 全过：主线全部 170 条（含推迟的宝黄天 16、锚定传送 3、`ModGameTests` 57）+ Forge 端专属 20 条 |
| runData | 无漂移 |
| 冒烟 | 服务端 `Done` 后 `stop`；客户端到标题画面存活 20 s，无 mixin 报错 |
| 一致性 | main 299 个同名文件 205 个一致（只有 `ModAttachments` 不搬）；GameTest 16 个文件只差注解导入或是接缝本体 |

## 实施偏差与本轮修掉的 Forge 端缺陷

- 客户端：HUD 两处 1.20.1 API（`ForgeGui.leftHeight/rightHeight`、`options.renderDebug`）；`mouseScrolled` 三参、`renderBackground(graphics)`；HUD 图层走 `RegisterGuiOverlaysEvent`，界面在 `FMLClientSetupEvent` 里 `MenuScreens.register`；粒子顶点链式 API 加 `end()` 恢复混合；效果图标：Forge 只在 `MobEffect` 构造时读 `initializeClient`，加客户端 mixin `MobEffectMixin` 在 `getEffectRendererInternal` 返回空时给出 `GuzhenrenClient` 的映射；MixinExtras 0.5.3 以 jar-in-jar 自带（Forge 47.4.10 不带）。
- 推迟的测试暴露三个 Forge 端缺陷，已修：玩家经 `teleportTo` 换维度后能力的 `LazyOptional` 失效且 `reviveCaps` 救不回 → provider 读取时重建；Epic Fight 20.14 的 `SetTargetEvent` 不可取消且无条件赋值 → 新 mixin `ServerPlayerPatchMixin` 在 `setAttackTarget` 开头拒绝野生蛊；1.20.1 原版 shift 合并只读无物品参数的格子上限 → 主线与 Forge 端同改 `ApertureStorageMenu.moveItemStackTo`（主线 `da667a5`）。
- GameTest 环境：Gu World Forge jar 经 flatDir + `fg.deobf` 进运行类路径；屏障墙每批都围（1.20.1 每批 100 条）；GameTest 运行排除 FTB Quests（它的换维度处理对模拟玩家空指针）；`GameTestWorld.FLIGHT_IS_RAW_MAYFLY`：1.20.1 没有创造飞行属性，飞行授予就是原版 `mayfly` 字段。
- `enterReturnsFalseWhenTargetLevelMissing` 改指向确实不存在的维度键（两端同改），不再依赖测试世界恰好没有宝黄天。
