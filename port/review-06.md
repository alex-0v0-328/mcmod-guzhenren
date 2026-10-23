# 可移植性回顾：模块 6

| # | 问题 | 改法 | 结果 |
|---|---|---|---|
| R24 | 主线 `CmdInfo`、`InfoRowRenderer` 用 Java 21 的模式匹配 `switch`，Forge 端 Java 17 编不过 | 两处改 `instanceof` 链并以抛错收尾（失去编译期穷尽检查，注释写明新 `Entry` 要补分支）；wiki《维护说明》的 Java 17 约束扩到语法 | 主线 `2dd5e42`（CI 绿）；`CmdInfo` 两端同源，`InfoRowRenderer` 随客户端模块搬 |

保留的差异：`ModCommand` 的注册事件；Epic Fight 21.x / 20.14 的 API（全局钩子 vs 每玩家监听、`ComboAttackEvent` vs `BasicAttackEvent`、属性修饰键）只在 `compat/` 两个文件里；`LivingIncomingDamageEvent` / `LivingAttackEvent`。

模块 6 主线前置（`d73e582`）：意图记录只剩数据与 `encode`/`decode`，NeoForge 包装集中到 `ModPayloads`。
