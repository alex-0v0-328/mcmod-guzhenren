# 可移植性回顾：模块 5

| # | 问题 | 改法（两端同改） | 结果 |
|---|---|---|---|
| R21 | `ActionEntity` 调 1.21 才有的 `Mob.stopInPlace()` | 主线内联它的四个调用（`getNavigation().stop()`、`setXxa`、`setYya`、`setSpeed`），两版都有 | 主线 `9a1816e`；`ActionEntity` 只剩同步数据与经验覆写两处差异 |
| R22 | 生成规则谓词写在事件类里，且用了 1.21 的 `MobSpawnType.ignoresLightRequirements`（只对 1.20.1 没有的试炼刷怪笼成立，这些生物从不由它生成） | 谓词移进新的同源 `entity/SpawnRules`，去掉该分支；`EntityRegistrationEvents` 只做注册 | 主线 `9a1816e`；事件类只差事件名与导入 |
| R23 | GameTest 用 1.21 专属 API：`skyAccess`、`helper.getEntities(type)`、附魔 Holder、`igniteForSeconds`、`canBeLeashed()`、`Arrow` 四参构造、`DeferredHolder` 参数 | 新测试接缝 `GameTestWorld`（两端各一份）；`skyAccess` 改为测试开头 `openSky`；参数改 `Supplier` | 主线 `src/test`（不进 Git），`check.py build gametest` 通过 |

保留的版本差异（无法同源）：同步数据定义（3 个文件）、`getExperienceReward` / `ignoreExplosion` / `canBeLeashed` 的覆写签名、`LandRestGoal` 的 `PathType` / `BlockPathTypes`、GeckoLib 4.8.4 的包名与 `stopTriggeredAnimation` / `CoreGeoBone` / `reRender` 颜色参数、注册表与事件胶水、datagen 的 provider 与 `RegistryObject`。

验收：主线完整 headless check 通过（GameTest 170）、CI 绿；Forge 端 build、GameTest 108 全过。
