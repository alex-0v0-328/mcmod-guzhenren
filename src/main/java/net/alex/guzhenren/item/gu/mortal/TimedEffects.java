package net.alex.guzhenren.item.gu.mortal;

import java.util.HashMap;
import java.util.Map;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.effect.timed.TimeRateUpEffect;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.MugwortShieldDownEvent;
import net.alex.guzhenren.gameplay.body.MugwortShieldRaisedEvent;
import net.alex.guzhenren.gameplay.body.MugwortShieldService;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.SkinShedEvent;
import net.alex.guzhenren.gameplay.body.SkinWornEvent;
import net.alex.guzhenren.gameplay.path.DaoMarks;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectEndedEvent;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.PathEffectType;
import net.alex.guzhenren.gameplay.path.timed.TimedBuff;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem.Amplification;
import net.alex.guzhenren.item.gu.mortal.water.HeavenlyMugwortGuItem;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The body effects of the timed Gu [限时蛊]: the buffs (beast, crash, hardship strength, self reliance, all-out
 * effort), the jade skins, the Heavenly Mugwort shield, the Vitality Leaf and the Watch Gu forms. Each is the
 * bookkeeping of a state that ends by itself, and the effect ends with it: its strands go the moment it does.
 * Nothing here reads a mark to size anything.
 *
 * <p>Helper and event subscriber over {@link PathEffectService}; the Gu that made a state is found in
 * {@code ModItems}, and its {@code GuSpec} supplies the carrier, path and strands, source the Gu's item id. The
 * buff, leaf and watch effects are the truth of their state and the vanilla effect only their icon, timer and
 * amplifier: milk or {@code /effect clear} costs the icon until the next heartbeat, and the rules read
 * {@link #findBuff} and its kin. A jade skin and the shield keep their state on {@code BodyData} and
 * {@code MugwortShieldData}; their effects follow the events those post, and an effect cut, removed or run out
 * takes the state with it.
 *
 * <p>Using a Gu again while its effect runs lengthens the timer and adds no strands; a watch form stacks one layer
 * of strands per use up to its limit. A buff of the same vanilla effect from another Gu (a crash Gu of a higher
 * rank) takes the old effect and its strands with it. An Amplify Effect Gu [功倍蛊] that paid for the use adds its
 * Rule strands to the effect it amplified, once: {@link #markAmplified} tops the effect up to the amplifier's
 * strands and never past them.
 *
 * <p>⚠ Grants write only the effect; the state the effect projects is the caller's. ⚠ A skin or shield effect
 * ending for any reason but being replaced ends the state too, guarded by the state still being the effect's own,
 * so the events it posts do not loop.
 *
 * @author Alex
 * @version 1.0.0
 * @see PathEffectService
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class TimedEffects {

    private TimedEffects() {}

    //region buffs
    public static @Nullable PathEffect<TimedBuff> findBuff(@NotNull Player player,
                                                           @NotNull Holder<MobEffect> effect) {
        PathEffect<TimedBuff> found = null;
        for (PathEffect<?> candidate : PathEffectService.getEffects(player)) {
            if (candidate.type() != ModPathEffectTypes.BUFF) continue;

            @SuppressWarnings("unchecked")
            PathEffect<TimedBuff> buff = (PathEffect<TimedBuff>) candidate;
            if (buff.payload().is(effect) && (found == null
                    || buff.payload().amplifier() > found.payload().amplifier())) found = buff;
        }
        return found;
    }

    public static boolean hasBuff(@NotNull Player player, @NotNull Holder<MobEffect> effect) {
        return findBuff(player, effect) != null;
    }

    public static int getBuffTicksLeft(@NotNull Player player, @NotNull Holder<MobEffect> effect) {
        PathEffect<TimedBuff> buff = findBuff(player, effect);
        return buff == null ? 0 : (int) Math.min(Integer.MAX_VALUE,
                buff.getTicksLeft(PathEffectService.getGameTime(player)));
    }

    public static void grantBuff(@NotNull ServerPlayer player, @NotNull MortalGuItem gu,
                                 @NotNull Holder<MobEffect> effect, int amplifier, long durationTicks,
                                 @NotNull Amplification amplification) {
        ResourceLocation source = getId(gu);
        TimedBuff payload = TimedBuff.of(effect, amplifier);
        long endTick = PathEffectService.getGameTime(player) + durationTicks;
        PathEffect<TimedBuff> own = PathEffectService.find(player, ModPathEffectTypes.BUFF, source);
        if (own != null && own.payload().equals(payload)) {
            PathEffectService.update(player, ModPathEffectTypes.BUFF, source, buff -> buff.withEndTick(endTick));
        } else {
            PathEffectService.remove(player, other -> other.type() == ModPathEffectTypes.BUFF
                    && other.payload() instanceof TimedBuff buff && buff.effect().equals(payload.effect()));
            PathEffectService.apply(player, gu.getSpec().createEffect(ModPathEffectTypes.BUFF, source, endTick,
                    payload));
        }
        markAmplified(player, ModPathEffectTypes.BUFF, source, amplification);
    }
    //endregion

    //region leaf and watch
    public static boolean hasLeaf(@NotNull Player player) {
        return PathEffectService.getEffects(player).stream().anyMatch(
                effect -> effect.type() == ModPathEffectTypes.VITALITY_LEAF);
    }

    public static void grantLeaf(@NotNull ServerPlayer player, @NotNull MortalGuItem gu, long durationTicks,
                                 @NotNull Amplification amplification) {
        ResourceLocation source = getId(gu);
        long endTick = PathEffectService.getGameTime(player) + durationTicks;
        PathEffectService.apply(player, gu.getSpec().createEffect(
                ModPathEffectTypes.VITALITY_LEAF, source, endTick, Unit.INSTANCE));
        markAmplified(player, ModPathEffectTypes.VITALITY_LEAF, source, amplification);
    }

    public static void grantWatch(@NotNull ServerPlayer player, @NotNull MortalGuItem gu,
                                  @NotNull Holder<MobEffect> form, long durationTicks,
                                  @NotNull Amplification amplification) {
        ResourceLocation source = getId(gu);
        TimeRateUpEffect rate = (TimeRateUpEffect) form.value();
        long endTick = PathEffectService.getGameTime(player) + durationTicks;
        PathEffect<ResourceLocation> own = PathEffectService.find(player, ModPathEffectTypes.WATCH_RATE, source);
        if (own == null) {
            PathEffectService.apply(player, gu.getSpec().createEffect(
                    ModPathEffectTypes.WATCH_RATE, source, endTick, form.unwrapKey().orElseThrow().location()));
        } else {
            boolean stacks = own.layers() < rate.getNextAmplifier(own.layers() - 1) + 1;
            Map<GuPath, DaoMarks> layer = gu.getSpec().getEffectPaths();
            PathEffectService.update(player, ModPathEffectTypes.WATCH_RATE, source, effect -> stacks
                    ? effect.addLayer(layer).withEndTick(endTick)
                    : effect.withEndTick(Math.max(effect.endTick(), endTick)));
        }
        markAmplified(player, ModPathEffectTypes.WATCH_RATE, source, amplification);
    }
    //endregion

    //region amplification
    public static <P> void markAmplified(@NotNull ServerPlayer player, @NotNull PathEffectType<P> type,
                                         @NotNull ResourceLocation source, @NotNull Amplification amplification) {
        if (amplification.gu() == null) return;

        Map<GuPath, DaoMarks> wanted = amplification.gu().getSpec().getEffectPaths();
        PathEffectService.update(player, type, source, effect -> {
            Map<GuPath, DaoMarks> missing = new HashMap<>();
            wanted.forEach((path, marks) -> missing.put(path, marks.cut(effect.getMarks(path))));
            return effect.addMarks(missing);
        });
    }
    //endregion

    //region jade skin
    @SubscribeEvent
    public static void onSkinWorn(SkinWornEvent event) { syncSkin(event.getPlayer()); }

    @SubscribeEvent
    public static void onSkinShed(SkinShedEvent event) { syncSkin(event.getPlayer()); }

    public static void markSkinAmplified(@NotNull ServerPlayer player, @NotNull Amplification amplification) {
        PathEffect<Skin> worn = PathEffectService.find(player, ModPathEffectTypes.TIMED_SKIN);
        if (worn != null) markAmplified(player, ModPathEffectTypes.TIMED_SKIN, worn.source(), amplification);
    }

    private static void syncSkin(ServerPlayer player) {
        Skin skin = BodyService.getSkin(player);
        SkinGuItem gu = skin.isTimed() ? ModItems.findGu(SkinGuItem.class, item -> item.getGrade() == skin) : null;
        if (gu == null) {
            PathEffectService.removeType(player, ModPathEffectTypes.TIMED_SKIN);
            return;
        }
        long endTick = PathEffectService.getGameTime(player) + BodyService.getSkinTicksLeft(player);
        PathEffect<Skin> worn = PathEffectService.find(player, ModPathEffectTypes.TIMED_SKIN);
        if (worn != null && worn.payload() == skin) {
            if (worn.endTick() != endTick) {
                PathEffectService.update(player, ModPathEffectTypes.TIMED_SKIN, worn.source(),
                        effect -> effect.withEndTick(endTick));
            }
            return;
        }
        PathEffectService.apply(player, gu.getSpec().createEffect(ModPathEffectTypes.TIMED_SKIN, getId(gu), endTick,
                skin));
    }
    //endregion

    //region mugwort shield
    @SubscribeEvent
    public static void onShieldRaised(MugwortShieldRaisedEvent event) {
        ServerPlayer player = event.getPlayer();
        HeavenlyMugwortGuItem gu = ModItems.findGu(HeavenlyMugwortGuItem.class, item -> true);
        if (gu == null) return;

        ResourceLocation source = getId(gu);
        long endTick = MugwortShieldService.get(player).endTick();
        if (PathEffectService.find(player, ModPathEffectTypes.MUGWORT_SHIELD, source) != null) {
            PathEffectService.update(player, ModPathEffectTypes.MUGWORT_SHIELD, source,
                    effect -> effect.withEndTick(endTick));
        } else {
            PathEffectService.apply(player, gu.getSpec().createEffect(
                    ModPathEffectTypes.MUGWORT_SHIELD, source, endTick, Unit.INSTANCE));
        }
    }

    @SubscribeEvent
    public static void onShieldDown(MugwortShieldDownEvent event) {
        PathEffectService.removeType(event.getPlayer(), ModPathEffectTypes.MUGWORT_SHIELD);
    }

    public static void markShieldAmplified(@NotNull ServerPlayer player, @NotNull Amplification amplification) {
        HeavenlyMugwortGuItem gu = ModItems.findGu(HeavenlyMugwortGuItem.class, item -> true);
        if (gu != null) markAmplified(player, ModPathEffectTypes.MUGWORT_SHIELD, getId(gu), amplification);
    }
    //endregion

    @SubscribeEvent
    public static void onEnded(PathEffectEndedEvent event) {
        if (event.getReason() == PathEffectEndedEvent.Reason.REPLACED) return;

        ServerPlayer player = event.getPlayer();
        PathEffect<?> effect = event.getEffect();
        if (effect.type() == ModPathEffectTypes.TIMED_SKIN && effect.payload() == BodyService.getSkin(player)) {
            BodyService.shedSkin(player);
        } else if (effect.type() == ModPathEffectTypes.MUGWORT_SHIELD && MugwortShieldService.isUp(player)) {
            MugwortShieldService.dismiss(player);
        }
    }

    private static ResourceLocation getId(MortalGuItem gu) { return BuiltInRegistries.ITEM.getKey(gu); }
}
