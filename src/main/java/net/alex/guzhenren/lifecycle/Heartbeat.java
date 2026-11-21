package net.alex.guzhenren.lifecycle;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.AperturePressureService;
import net.alex.guzhenren.gameplay.blissful.BlissfulLands;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ShieldService;
import net.alex.guzhenren.gameplay.body.SkinService;
import net.alex.guzhenren.gameplay.body.UndeadService;
import net.alex.guzhenren.gameplay.clock.Beat;
import net.alex.guzhenren.gameplay.clock.TimeFlow;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.strength.AttackDamageService;
import net.alex.guzhenren.gameplay.secretrealm.SecretRealms;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.alex.guzhenren.item.gu.mortal.path.strength.AutoUse;
import net.alex.guzhenren.item.gu.mortal.path.time.AsBeforeGuItem;
import net.alex.guzhenren.item.gu.tending.GuUpkeep;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.alex.guzhenren.vault.ApertureStorageMenu;
import net.alex.guzhenren.vault.ApertureStorageTick;
import net.minecraft.server.level.ServerPlayer;

/**
 * 服务端玩家每秒执行一次的心跳调度器，按固定先后顺序推进各领域状态（由 {@link PlayerTickEvents} 逐刻调用并自行门控）。
 * <ul>
 *   <li>存活的服务端玩家每刻调用 {@link #beat}，本类在 {@code tickCount % HEARTBEAT_TICKS == 0} 时执行各步；时间流速仅
 *       在本拍开头读取一次并存入 {@link Beat} 供后续按时间缩放的步骤共用，生命周期入口归 {@link PlayerDataService}，具
 *       体业务归各自领域，唯 {@code checkLethalState} 因涉及跨领域死亡优先级而留在本类。</li>
 *   <li>⚠ 下列步骤顺序隐含跨领域数据依赖（多数步骤读取前一步刚写入的状态），严禁随意调换顺序；新增步骤时必须在列表对应
 *       位置补充说明其前置依赖与原因。</li>
 *   <li>⚠ {@code checkLethalState} 必须在心跳末尾执行且首中即返回，死亡判定优先级固定为：空窍压力、寿元耗尽、魂魄崩溃、
 *       脑海爆裂。</li>
 *   <li>⚠ 周期慢于 1 秒的步骤在心跳内按 {@code tickCount % N} 门控时，N 必须为 {@code HEARTBEAT_TICKS} 的整数倍，否则只
 *       会在 {@code lcm(20, N)} 刻触发一次而悄然变慢（例如僵尸念头周期
 *       {@code MindService.ZOMBIE_THOUGHT_INTERVAL_TICKS} 为 100 刻）。</li>
 * </ul>
 * <ol>
 *   <li>{@code BodyService.tickAging} 与 {@code TimeFlow.getRate} 采样存入 {@link Beat}：日钟推进消耗对应天数而不扣减寿
 *       元，心跳中途变更时间倍率将在下一次心跳生效。</li>
 *   <li>{@code GuUpkeep.tickCarried}：按第 (1) 步的天数推进背包内蛊虫的日钟养护。</li>
 *   <li>{@code ApertureStorageTick.tickStored}：按第 (1) 步的天数推进空窍存储与本命槽内蛊虫的日钟养护。</li>
 *   <li>{@code ApertureStorageMenu.refresh}（仅玩家正打开存储界面时）：必须在第 (3) 步之后，防止旧界面视图写回时覆盖刚
 *       完成的养护推进。</li>
 *   <li>{@code VitalLossService.settleOfflineVitalLoss}：紧跟可能令在线本命蛊饿死的第 (2)、(3) 步执行，并保证魂魄与脑海
 *       减半早于第 (15)、(20) 步生效。</li>
 *   <li>{@code PathEffectService.tick}：必须在第 (5) 步之后、后续读取形态或资源池之前摘除到期流派效果并重投剩余图标，使
 *       本拍读取到最新效果状态。</li>
 *   <li>{@code ApertureEssenceService.closeDistilling}：必须在第 (13) 步之前完成，使酒虫精炼的 1:2 真元退还先于自动回复
 *       选池。</li>
 *   <li>{@code UndeadService.tickTempZombie}：可能终止临时僵尸形态；第 (10)、(11) 至 (15) 步均依赖本步确定后的形
 *       态。</li>
 *   <li>{@code SkinService.tickSkin} 与 {@code ShieldService.tickShield}：褪去到期的玉皮与护盾层并将当前穿戴投射为效果
 *       图标，与第 (8) 步同属形态投射（后续无心跳步骤读取皮盾，受击时再实时读取）。</li>
 *   <li>{@code UndeadService.pinHunger}：必须在第 (8) 步之后执行，否则刚结束僵尸形态的玩家仍会被锁定饱食[Hunger]
 *       值。</li>
 *   <li>{@code AttackDamageService.refresh}：必须在第 (8) 步之后执行，因僵尸形态本身是 {@code getBonus} 的组成项之
 *       一。</li>
 *   <li>{@code BodyService.tickLifespan}：必须在第 (8) 步之后（不死之躯免扣寿元）且在第 (20) 步之前执行，供致死检查读取
 *       寿元耗尽状态。</li>
 *   <li>{@code ApertureEssenceService.regenStep}：必须在第 (7)、(8) 步之后按最新形态与精炼池状态回复真元。</li>
 *   <li>{@code ApertureNourishService.tickNourish}：必须在第 (13) 步之后先回复本秒真元再扣除温养[Nourish]消耗。</li>
 *   <li>{@code MindService.regenStep}：必须在第 (8) 步之后执行，以反映僵尸每 5 秒产生一次念头的规则。</li>
 *   <li>{@code AutoUse.tryStart}：必须在第 (13) 步之后执行，使自动催动能够使用本秒刚回复的真元。</li>
 *   <li>{@code SecretRealms.tickPlayer}：天地秘境[Secret Realm]范围内外的每秒钩子（荡魂山内每 10 秒扣一次当前魂魄，山
 *       外清除胆识蛊）；必须在第 (18) 步之前，使快照含本拍扣魂，且在第 (20) 步之前，使扣到 0 当拍按魂魄衰竭处死；之后若开着空窍存储界面再刷新一次，免得旧视图把刚清掉的胆识蛊写回。紧接着
 *       {@code BlissfulLands.tickPlayer} 判定福地[Blissful Land]认主（同属本步，不另编号）。</li>
 *   <li>{@code AsBeforeGuItem.remember}：必须在第 (8)、(12)、(15)、(17) 步之后记录本秒已结算的身体快照（含临时僵尸、寿
 *       元耗尽与念头状态），且必须在第 (20) 步前完成，以便按原样保留濒死瞬间状态。</li>
 *   <li>{@code AperturePressureService.tick}：内部自控每分钟推进与爆炸倒计时，满压后交由第 (20) 步结算。</li>
 *   <li>{@code checkLethalState}：在所有领域状态结算完毕后统一执行致死判定。</li>
 * </ol>
 */

public final class Heartbeat {

    private Heartbeat() {}

    public static final int HEARTBEAT_TICKS = Ticks.SECOND;

    public static void beat(ServerPlayer player) {
        if (player.isRemoved() || player.isDeadOrDying()) return;
        if (player.tickCount % HEARTBEAT_TICKS != 0) return;

        Beat beat = new Beat(BodyService.tickAging(player), TimeFlow.getRate(player), player.getRandom());
        GuUpkeep.tickCarried(player, beat.days());
        ApertureStorageTick.tickStored(player, beat.days());

        if (player.containerMenu instanceof ApertureStorageMenu menu) menu.refresh();
        VitalLossService.settleOfflineVitalLoss(player);

        PathEffectService.tick(player);
        ApertureEssenceService.closeDistilling(player);
        UndeadService.tickTempZombie(player);
        SkinService.tickSkin(player);
        ShieldService.tickShield(player);
        UndeadService.pinHunger(player);
        AttackDamageService.refresh(player);
        BodyService.tickLifespan(player, beat);
        ApertureEssenceService.regenStep(player, beat);
        ApertureNourishService.tickNourish(player, beat);
        MindService.regenStep(player, beat);
        AutoUse.tryStart(player);
        SecretRealms.tickPlayer(player);
        BlissfulLands.tickPlayer(player);
        if (player.containerMenu instanceof ApertureStorageMenu menu) menu.refresh();
        AsBeforeGuItem.remember(player);
        AperturePressureService.tick(player);
        checkLethalState(player);
    }

    private static void checkLethalState(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) return;

        if (AperturePressureService.isFull(player)) {
            if (!ApertureNourishService.convertPetrifiedPressure(player)) AperturePressureService.detonate(player);
            return;
        }
        if (BodyService.get(player).isExhausted()) {
            player.hurt(ModDamageTypes.source(player, ModDamageTypes.LIFESPAN_EXHAUSTED), Float.MAX_VALUE);
            return;
        }
        if (SoulService.get(player).isCollapsed()) {
            player.hurt(ModDamageTypes.source(player, ModDamageTypes.SOUL_COLLAPSE), Float.MAX_VALUE);
            return;
        }
        if (MindService.get(player).isOverflowing()) {
            player.hurt(ModDamageTypes.source(player, ModDamageTypes.MIND_OCEAN_SHATTERED), Float.MAX_VALUE);
        }
    }
}
