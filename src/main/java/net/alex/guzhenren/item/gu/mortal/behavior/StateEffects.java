package net.alex.guzhenren.item.gu.mortal.behavior;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.Aperture;
import net.alex.guzhenren.gameplay.aperture.ApertureChangedEvent;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.Physique;
import net.alex.guzhenren.gameplay.body.PhysiqueChangedEvent;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectEndedEvent;
import net.alex.guzhenren.gameplay.path.PathEffectEndedEvent.Reason;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.PathEffectType;
import net.alex.guzhenren.gameplay.path.timed.TimedBuff;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.path.earth.PetrifyBehavior;
import net.alex.guzhenren.item.gu.mortal.path.transformation.ZombieBehavior;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.NotNull;

/**
 * 状态类凡蛊（僵尸形态、酒虫[Liquor Worm]精炼、石窍蛊[Stone Aperture Gu]石化空窍、随意蛊[Casual Gu]与
 * 恶念蛊[Malicious Thought Gu]念头充塞）的流派效果管理与同步工具。
 * <ul>
 *   <li>此类状态随自身计时或命令结束时，对应的 {@link PathEffect} 同步结束并消散道痕[Path Marks]点数，状态本身不按道痕
 *       数量做数值缩放。</li>
 *   <li>付与状态的蛊虫通过扫描 {@code ModItems} 定位，并由其 {@code GuProperties} 提供载体、流派与道痕点数，效果来源取
 *       该蛊物品 ID；念头充塞与精炼的流派效果即状态真值，原版药水效果仅作图标、倒计时与等级投影（{@code /effect clear}
 *       只能让图标暂隐至下一轮心跳，饮用牛奶亦无法清除）。</li>
 *   <li>僵尸形态与石化空窍的真值分别保存在 {@code BodyData} 与 {@code Aperture} 中，命令修改状态时无需额外调用本
 *       类：{@link #syncZombie} 与 {@link #syncStone} 在监听到变更事件及玩家登录时自动由真值推导流派效果（临时僵尸按
 *       {@code zombieTier} 匹配对应转数僵尸蛊，命令未指定档位导致为负时按 0 档处理；石窍按已石化空窍中的最高转数匹配石
 *       窍蛊）。</li>
 *   <li>⚠ 仅临时僵尸化携带变化道流派效果，转为永久僵尸化时必须消散该效果的道痕点数：{@link #syncZombie} 调用
 *       {@link #dissipate} 将效果结束 tick 改为当前时刻并立刻触发到期（按自然到期结束而非移除，从而在 {@link #onEnded}
 *       中保留僵尸形态，此后登录或命令均不再补挂效果）；反之若临时僵尸效果被显式移除
 *       （{@code Reason.REMOVED}），{@link #onEnded} 会一并清除僵尸体质，而自然到期则交由
 *       {@code UndeadService.tickTempZombie} 恢复肉身；石化空窍不随流派效果结束而解封，下次空窍写入时会重新补齐效
 *       果。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class StateEffects {

    private StateEffects() {}

    //region thought and distilling [念与精炼]
    public static void grantThought(@NotNull ServerPlayer player, @NotNull MortalGuItem gu,
                                    @NotNull PathEffectType<TimedBuff> type, @NotNull Holder<MobEffect> effect,
                                    int amplifier, long durationTicks) {
        ResourceLocation source = gu.getEffectSource();
        PathEffectService.remove(player, other -> other.type() == type && !other.source().equals(source));
        PathEffectService.apply(player, gu.createEffect(type, player.level().getGameTime() + durationTicks,
                TimedBuff.of(effect, amplifier)));
    }

    public static void grantDistilling(@NotNull ServerPlayer player, @NotNull MortalGuItem gu, int tier,
                                       long durationTicks) {
        PathEffectService.apply(player, gu.createEffect(ModPathEffectTypes.LIQUOR_DISTILLING,
                player.level().getGameTime() + durationTicks, tier));
    }
    //endregion

    //region zombie [僵尸]
    @SubscribeEvent
    public static void onPhysiqueChanged(PhysiqueChangedEvent event) { syncZombie(event.getPlayer()); }

    public static void syncZombie(@NotNull ServerPlayer player) {
        PathEffect<Unit> current = PathEffectService.find(player, ModPathEffectTypes.ZOMBIE);
        if (BodyService.isZombie(player)) {
            if (current != null) dissipate(player, current);
            return;
        }
        if (!BodyService.isTempZombie(player)) {
            if (current != null) PathEffectService.removeType(player, ModPathEffectTypes.ZOMBIE);
            return;
        }
        if (BodyService.hasTempZombieRunOut(player)) return;

        int rung = Math.max(0, BodyService.get(player).zombieTier());
        MortalGuItem gu = ModItems.findGu(MortalGuItem.class,
                item -> item.getBehavior(ZombieBehavior.class) != null && ZombieBehavior.getRung(item) == rung);
        if (gu == null) return;

        long endTick = player.level().getGameTime() + BodyService.getTempZombieTicksLeft(player);
        ResourceLocation source = gu.getEffectSource();
        if (current != null && current.source().equals(source)) {
            if (current.endTick() != endTick) {
                PathEffectService.update(player, ModPathEffectTypes.ZOMBIE, source,
                        effect -> effect.withEndTick(endTick));
            }
            return;
        }
        PathEffectService.apply(player, gu.createEffect(ModPathEffectTypes.ZOMBIE, endTick, Unit.INSTANCE));
    }

    private static void dissipate(ServerPlayer player, PathEffect<Unit> current) {
        long now = player.level().getGameTime();
        PathEffectService.update(player, ModPathEffectTypes.ZOMBIE, current.source(),
                effect -> effect.withEndTick(now));
        PathEffectService.tick(player);
    }
    //endregion

    //region stone aperture [石窍]
    @SubscribeEvent
    public static void onApertureChanged(ApertureChangedEvent event) { syncStone(event.getPlayer()); }

    public static void syncStone(@NotNull ServerPlayer player) {
        Rank rank = null;
        ApertureData data = ApertureService.get(player);
        for (int i = 0; i < data.count(); i++) {
            Aperture aperture = data.get(i);
            if (aperture.petrified() && (rank == null || aperture.rank().ordinal() > rank.ordinal())) {
                rank = aperture.rank();
            }
        }
        PathEffect<Unit> current = PathEffectService.find(player, ModPathEffectTypes.STONE_APERTURE);
        if (rank == null) {
            if (current != null) PathEffectService.removeType(player, ModPathEffectTypes.STONE_APERTURE);
            return;
        }
        Rank petrified = rank;
        MortalGuItem gu = ModItems.findGu(MortalGuItem.class,
                item -> item.getBehavior(PetrifyBehavior.class) != null && item.getRank() == petrified);
        if (gu == null || current != null && current.source().equals(gu.getEffectSource())) return;

        PathEffectService.apply(player,
                gu.createEffect(ModPathEffectTypes.STONE_APERTURE, PathEffect.PERMANENT, Unit.INSTANCE));
    }
    //endregion

    public static void sync(@NotNull ServerPlayer player) {
        syncZombie(player);
        syncStone(player);
    }

    @SubscribeEvent
    public static void onEnded(PathEffectEndedEvent event) {
        Reason reason = event.getReason();
        if (event.getEffect().type() != ModPathEffectTypes.ZOMBIE || reason != Reason.REMOVED) {
            return;
        }
        ServerPlayer player = event.getPlayer();
        BodyService.removePhysique(player, Physique.ZOMBIE);
        BodyService.removePhysique(player, Physique.TEMP_ZOMBIE);
    }
}
