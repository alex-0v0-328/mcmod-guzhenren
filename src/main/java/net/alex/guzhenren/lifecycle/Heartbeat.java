package net.alex.guzhenren.lifecycle;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.AperturePressureService;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ShieldService;
import net.alex.guzhenren.gameplay.body.SkinService;
import net.alex.guzhenren.gameplay.body.UndeadService;
import net.alex.guzhenren.gameplay.clock.Beat;
import net.alex.guzhenren.gameplay.clock.TimeFlow;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.strength.AttackDamageService;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.alex.guzhenren.item.gu.mortal.path.strength.AutoUse;
import net.alex.guzhenren.item.gu.mortal.path.time.AsBeforeGuItem;
import net.alex.guzhenren.item.gu.tending.GuUpkeep;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.alex.guzhenren.vault.ApertureStorageMenu;
import net.alex.guzhenren.vault.ApertureStorageTick;
import net.minecraft.server.level.ServerPlayer;

/**
 * 每秒一次的心跳：玩家多数状态依赖的有序步骤。
 * <ul>
 *   <li>活着的服务端玩家每 tick 调 {@link #beat}，它自行门控：各步都在 {@code tickCount % HEARTBEAT_TICKS == 0} 内，
 *       多数读前步刚写的值；时钟只读一次存入 {@link Beat}，按时间缩放的步骤共用；生命周期归 {@link PlayerDataService}，
 *       节奏归本类，各步归各自领域，唯 {@code checkLethalState} 留此：四种死亡先后是跨领域规则。</li>
 *   <li>⚠ 顺序承重而代码看不出：勿盲目调序；新步骤加一行，注明跟在哪步后、为何。</li>
 *   <li>⚠ {@code checkLethalState} 最后跑、首中即返，死亡优先级固定：空窍压力 → 寿元 → 魂魄 → 脑海。</li>
 *   <li>⚠ 慢于 1 秒的步骤在心跳内按 {@code tickCount % N} 门控，N 须为 {@code HEARTBEAT_TICKS} 倍数，否则只每
 *       lcm(20, N) tick 触发、悄悄变慢；僵尸念头（{@code MindService.ZOMBIE_THOUGHT_INTERVAL_TICKS}，100）照此。</li>
 * </ul>
 * <ol>
 *   <li>{@code BodyService.tickAging}、{@code TimeFlow.getRate} 读入 {@link Beat}：日钟推进花其 {@code days}，不扣寿
 *       元；中途变倍率落下次。</li>
 *   <li>{@code GuUpkeep.tickCarried}：按 (1) 推日钟。</li>
 *   <li>{@code ApertureStorageTick.tickStored}：按 (1) 推日钟。</li>
 *   <li>{@code ApertureStorageMenu.refresh}（打开的存储界面）：(3) 后，否则旧视图存回、盖掉推进。</li>
 *   <li>{@code VitalLossService.settleOfflineVitalLoss}：紧跟当场结算在线本命蛊[Vital Gu]之死的 (2)、(3)；魂魄、脑海减
 *       半在 (15)、(19) 前。</li>
 *   <li>{@code PathEffectService.tick}：(5) 后、读形态或池前；摘到期流派效果、重投余下图标，本次读到本秒效果。</li>
 *   <li>{@code ApertureEssenceService.closeDistilling}：(13) 前，1:2 返还先于回复选池。</li>
 *   <li>{@code UndeadService.tickTempZombie}：可能结束形态；(10)、(11) 至 (15) 读形态。</li>
 *   <li>{@code SkinService.tickSkin}、{@code ShieldService.tickShield}：褪去到期玉皮、护盾层，在穿的投射为效果，同 (8)
 *       属形态投射；后续无步骤读，受击时各自读取。</li>
 *   <li>{@code UndeadService.pinHunger}：(8) 后，否则本次结束的形态仍锁饱食[Hunger]。</li>
 *   <li>{@code AttackDamageService.refresh}：(8) 后，形态是 {@code getBonus} 一项。</li>
 *   <li>{@code BodyService.tickLifespan}：(8) 后，不死之身不扣寿元；(19) 前，(19) 读寿元耗尽。</li>
 *   <li>{@code ApertureEssenceService.regenStep}：(7)、(8) 后。</li>
 *   <li>{@code ApertureNourishService.tickNourish}：(13) 后，先回本秒真元[Essence]后扣温养[Nourish]。</li>
 *   <li>{@code MindService.regenStep}：(8) 后，僵尸每 5 秒思考一次。</li>
 *   <li>{@code AutoUse.tryStart}：(13) 后，自动催动付其回的真元。</li>
 *   <li>{@code AsBeforeGuItem.remember}：(8)、(12)、(15) 后，记本次已结算的身体（含临时僵尸、寿元耗尽、念头）；
 *       (19) 前，将死之身照原样记。</li>
 *   <li>{@code AperturePressureService.tick}：自控每分钟一步与倒计时；满表交 (19)。</li>
 *   <li>{@code checkLethalState}：最后。</li>
 * </ol>
 *
 * @author Alex
 * @version 1.0.0
 * @see BodyService
 * @see PlayerTickEvents
 * @since 1.0.0
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
