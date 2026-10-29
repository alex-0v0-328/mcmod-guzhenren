package net.alex.guzhenren.item.gu.mortal;

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
import net.alex.guzhenren.item.gu.mortal.earth.StoneApertureGuItem;
import net.alex.guzhenren.item.gu.mortal.food.LiquorWormItem;
import net.alex.guzhenren.item.gu.mortal.transformation.ZombieGuItem;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.NotNull;

/**
 * The effects of the state Gu [状态蛊]: the zombie Gu's undead form, the Liquor Worm's distilling, the Stone Aperture
 * Gu's petrified aperture and the Casual and Malicious Thought Gu's flooded mind. Each is the bookkeeping of a state
 * that ends by itself or by command, and the effect ends with it: its strands go the moment it does. Nothing here
 * reads a mark to size anything.
 *
 * <p>Helper and event subscriber over {@link PathEffectService}; the Gu that made a state is found in
 * {@code ModItems}, and its {@code GuSpec} supplies the carrier, path and strands, source the Gu's item id. The
 * thought and distilling effects are the truth of their state and the vanilla effect only their icon, timer and
 * amplifier: milk or {@code /effect clear} costs the icon until the next heartbeat. The zombie form and the petrified
 * aperture stay on {@code BodyData} and {@code Aperture}, so a command that sets them needs no call of its own:
 * {@link #syncZombie} and {@link #syncStone} derive the effect from the state on the change event and on login, and
 * name the Gu by the state's rung or rank.
 *
 * <p>⚠ Only a half-zombie carries a zombie effect. Turning zombie for good dissipates its Transformation strands:
 * {@link #syncZombie} runs the effect out on the spot, so it ends as an expiry and the form stays; a zombie gets none
 * back on login or by command. A half-zombie effect cut or removed takes the undead form with it; one that merely
 * runs out leaves the half-zombie to {@code UndeadService.tickHalfZombie}, which still turns it into a zombie if
 * Death Qi is held. A petrified aperture is never un-petrified by its effect ending; the next aperture write puts
 * the effect back.
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

    //region thought and distilling
    public static void grantThought(@NotNull ServerPlayer player, @NotNull MortalGuItem gu,
                                    @NotNull PathEffectType<TimedBuff> type, @NotNull Holder<MobEffect> effect,
                                    int amplifier, long durationTicks) {
        ResourceLocation source = getId(gu);
        PathEffectService.remove(player, other -> other.type() == type && !other.source().equals(source));
        PathEffectService.apply(player, gu.getSpec().createEffect(type, source,
                PathEffectService.getGameTime(player) + durationTicks, TimedBuff.of(effect, amplifier)));
    }

    public static void grantDistilling(@NotNull ServerPlayer player, @NotNull LiquorWormItem gu, int tier,
                                       long durationTicks) {
        PathEffectService.apply(player, gu.getSpec().createEffect(ModPathEffectTypes.LIQUOR_DISTILLING, getId(gu),
                PathEffectService.getGameTime(player) + durationTicks, tier));
    }
    //endregion

    //region zombie
    @SubscribeEvent
    public static void onPhysiqueChanged(PhysiqueChangedEvent event) { syncZombie(event.getPlayer()); }

    public static void syncZombie(@NotNull ServerPlayer player) {
        PathEffect<Unit> current = PathEffectService.find(player, ModPathEffectTypes.ZOMBIE);
        if (BodyService.isZombie(player)) {
            if (current != null) dissipate(player, current);
            return;
        }
        if (!BodyService.isHalfZombie(player)) {
            if (current != null) PathEffectService.removeType(player, ModPathEffectTypes.ZOMBIE);
            return;
        }
        if (BodyService.hasHalfZombieRunOut(player)) return;

        int rung = Math.max(0, BodyService.get(player).zombieTier());
        ZombieGuItem gu = ModItems.findGu(ZombieGuItem.class, item -> item.getRung() == rung);
        if (gu == null) return;

        long endTick = PathEffectService.getGameTime(player) + BodyService.getHalfZombieTicksLeft(player);
        ResourceLocation source = getId(gu);
        if (current != null && current.source().equals(source)) {
            if (current.endTick() != endTick) {
                PathEffectService.update(player, ModPathEffectTypes.ZOMBIE, source, effect -> effect.withEndTick(endTick));
            }
            return;
        }
        PathEffectService.apply(player, gu.getSpec().createEffect(ModPathEffectTypes.ZOMBIE, source, endTick,
                Unit.INSTANCE));
    }

    // An expiry, unlike a removal, leaves the form standing (see onEnded).
    private static void dissipate(ServerPlayer player, PathEffect<Unit> current) {
        long now = PathEffectService.getGameTime(player);
        PathEffectService.update(player, ModPathEffectTypes.ZOMBIE, current.source(),
                effect -> effect.withEndTick(now));
        PathEffectService.tick(player);
    }
    //endregion

    //region stone aperture
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
        StoneApertureGuItem gu = ModItems.findGu(StoneApertureGuItem.class, item -> item.getRank() == petrified);
        if (gu == null || current != null && current.source().equals(getId(gu))) return;

        PathEffectService.apply(player, gu.getSpec().createEffect(ModPathEffectTypes.STONE_APERTURE, getId(gu),
                PathEffect.PERMANENT, Unit.INSTANCE));
    }
    //endregion

    public static void sync(@NotNull ServerPlayer player) {
        syncZombie(player);
        syncStone(player);
    }

    @SubscribeEvent
    public static void onEnded(PathEffectEndedEvent event) {
        Reason reason = event.getReason();
        if (event.getEffect().type() != ModPathEffectTypes.ZOMBIE || reason != Reason.CUT && reason != Reason.REMOVED) {
            return;
        }
        ServerPlayer player = event.getPlayer();
        BodyService.removePhysique(player, Physique.ZOMBIE);
        BodyService.removePhysique(player, Physique.HALF_ZOMBIE);
    }

    private static ResourceLocation getId(MortalGuItem gu) { return BuiltInRegistries.ITEM.getKey(gu); }
}
