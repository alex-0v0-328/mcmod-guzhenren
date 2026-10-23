# 可移植性回顾：模块 7

| # | 问题 | 改法 | 结果 |
|---|---|---|---|
| R25 | HUD 实现 1.21 的 `LayeredDraw.Layer`、读 `DeltaTracker` | HUD 只有 `render(GuiGraphics)`，图层在 `ClientEvents` 包一层 | 主线 `b10090a`；4 个 HUD 文件中 2 个同源 |
| R26 | 储物菜单依赖原版 shift 合并读哪种上限 | 自写 `moveItemStackTo`，按每叠上限 | 主线 `da667a5`；文件两端同源 |
| R27 | 测试依赖 1.21 的 Epic Fight 事件、存档 API、`player.mayFly()`、事件总线、附件复制、测试世界里恰好没有的维度 | 测试接缝 `GameTestWorld` / `GameTestEpicFight` 各一份；用例改走 `FlightAbility.mayFly`；缺失维度用确定不存在的键 | 主线 `src/test`（不进 Git）`check.py` 全过；全部 GameTest 文件两端只差注解导入 |

保留的差异：客户端渲染与事件 API（粒子顶点、效果图标扩展、图层与界面注册、`mouseScrolled` 等签名）、`HotbarHud`/`PlayerStatsHud` 的 1.20.1 字段；这些只在客户端各自文件里。
