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
 * 状态蛊的流派效果。
 * <ul>
 *   <li>状态：僵尸蛊的僵尸形态、酒虫[Liquor Worm]精炼、石窍蛊[Stone Aperture Gu]石化空窍[Aperture]、随意蛊[Casual Gu]
 *       与恶念蛊[Malicious Thought Gu]念头充塞；自行或经命令结束，效果随之结束、丝即消散；不读道痕[Path Marks]定
 *       量。</li>
 *   <li>{@link PathEffectService} 上的工具与事件订阅者；造状态的蛊查 {@code ModItems}，其 {@code GuProperties} 给载体、
 *       道[Path]、丝，来源为蛊物品 id；念与精炼效果是状态真值，原版效果只是图标、计时、等级：{@code /effect clear}
 *       只让图标消失到下次心跳，牛奶全解不掉。</li>
 *   <li>僵尸形态、石化空窍存于 {@code BodyData}、{@code Aperture}，命令设置无需另调：{@link #syncZombie}、
 *       {@link #syncStone} 在变更事件与登录时由状态推出效果，按档或转找蛊。</li>
 *   <li>⚠ 仅临时僵尸化带僵尸效果；永久僵尸化消散其变化道丝：{@link #syncZombie} 当场令效果到期，按到期结束、形态保留，
 *       登录或命令都不再给；临时僵尸效果被摘则连形态带走，仅到期则交 {@code UndeadService.tickTempZombie} 恢复；
 *       石化空窍不因效果结束解除，下次空窍写入补回效果。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see PathEffectService
 * @see TimedEffects
 * @since 1.0.0
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

    // An expiry, unlike a removal, leaves the form standing (see onEnded).
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
