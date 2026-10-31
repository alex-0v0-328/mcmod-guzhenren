package net.alex.guzhenren.item.gu.mortal;

import java.util.Map;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.body.BodyGrade;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.BodyTemperedEvent;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.SkinShedEvent;
import net.alex.guzhenren.gameplay.body.SkinWornEvent;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.DaoMarks;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.PathEffectType;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthChangedEvent;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.strength.BeastStrengthModule;
import net.alex.guzhenren.item.gu.mortal.strength.HumanStrengthModule;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * The body effects of the channeled dao mark carving Gu [灌注蛊]: the tempering Gu's skin, tendons and bones, the
 * beast strengths and the human strength layers. Each is the bookkeeping of a state another service owns -- the
 * body's grade, the strength store -- and follows its write through the event that write posts, so the state's
 * writer, a Gu or a command alike, knows nothing of it. Nothing here reads a mark to size anything.
 *
 * <p>Event subscriber over {@link PathEffectService}; the Gu that made a state is found in {@code ModItems}, and its
 * {@code GuSpec} supplies the carrier, path and strands, source the Gu's item id. A grade is permanent, one
 * exclusive effect per part, so a new grade takes the old effect and its strands with it. A beast strength is one
 * effect per species. A human strength is one effect per kind whose layers follow the stored count.
 *
 * <p>⚠ Every sync compares the effect with the state and writes only on a difference, so a handler running twice
 * is harmless and a partial cut survives a write that changes nothing. ⚠ The marks arrive only with the write that
 * sets the state: nothing re-adds them on login, and a state saved before its effect existed stays without. ⚠ A
 * timed skin is not this class's: wearing a jade skin over a permanent one only drops the permanent skin's effect.
 * ⚠ Lowering human strength cuts each dropped layer's strands from the effect, so marks added to it by other
 * means stay.
 *
 * @author Alex
 * @version 1.0.0
 * @see PathEffectService
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class CarvingEffects {

    private CarvingEffects() {}

    @SubscribeEvent
    public static void onSkinWorn(SkinWornEvent event) { syncSkin(event.getPlayer()); }

    @SubscribeEvent
    public static void onSkinShed(SkinShedEvent event) { syncSkin(event.getPlayer()); }

    @SubscribeEvent
    public static void onTempered(BodyTemperedEvent event) {
        syncTendon(event.getPlayer());
        syncBone(event.getPlayer());
    }

    @SubscribeEvent
    public static void onStrengthChanged(StrengthChangedEvent event) {
        ServerPlayer player = event.getPlayer();
        for (BeastStrength beast : BeastStrength.values()) syncBeast(player, beast);
        for (HumanStrength kind : HumanStrength.values()) alignHuman(player, kind);
    }

    private static void syncSkin(ServerPlayer player) {
        Skin skin = BodyService.getSkin(player);
        syncGrade(player, ModPathEffectTypes.BODY_SKIN, skin, !skin.isTimed());
    }

    private static void syncTendon(ServerPlayer player) {
        syncGrade(player, ModPathEffectTypes.BODY_TENDON, BodyService.getTendon(player), true);
    }

    private static void syncBone(ServerPlayer player) {
        syncGrade(player, ModPathEffectTypes.BODY_BONE, BodyService.getBone(player), true);
    }

    private static void syncBeast(ServerPlayer player, BeastStrength beast) {
        MortalGuItem gu = ModItems.findGuWith(BeastStrengthModule.class, module -> module.beast() == beast);
        if (gu == null) return;

        ResourceLocation source = getId(gu);
        boolean worn = PathEffectService.find(player, ModPathEffectTypes.BEAST_STRENGTH, source) != null;
        if (PathStrengthService.has(player, beast) == worn) return;

        if (worn) {
            PathEffectService.remove(player, effect -> isOf(effect, ModPathEffectTypes.BEAST_STRENGTH, source));
        } else {
            PathEffectService.apply(player, gu.getSpec().createEffect(
                    ModPathEffectTypes.BEAST_STRENGTH, source, PathEffect.PERMANENT, beast));
        }
    }

    private static void alignHuman(ServerPlayer player, HumanStrength kind) {
        MortalGuItem gu = ModItems.findGuWith(HumanStrengthModule.class, module -> module.kind() == kind);
        if (gu == null) return;

        ResourceLocation source = getId(gu);
        int layers = PathStrengthService.getHumanStrength(player, kind);
        PathEffect<HumanStrength> worn = PathEffectService.find(player, ModPathEffectTypes.HUMAN_STRENGTH, source);
        if (layers <= 0) {
            if (worn != null) {
                PathEffectService.remove(player, effect -> isOf(effect, ModPathEffectTypes.HUMAN_STRENGTH, source));
            }
            return;
        }
        Map<GuPath, DaoMarks> layer = gu.getSpec().getEffectPaths();
        if (worn == null) {
            PathEffectService.apply(player, withLayers(gu.getSpec().createEffect(
                    ModPathEffectTypes.HUMAN_STRENGTH, source, PathEffect.PERMANENT, kind), layer, layers));
        } else if (worn.layers() != layers) {
            PathEffectService.update(player, ModPathEffectTypes.HUMAN_STRENGTH, source,
                    effect -> withLayers(effect, layer, layers));
        }
    }

    private static <G extends Enum<G> & BodyGrade<G>> void syncGrade(
            ServerPlayer player, PathEffectType<G> type, G grade, boolean carries) {
        PathEffect<G> worn = PathEffectService.find(player, type);
        MortalGuItem gu = carries && !grade.isNative()
                ? ModItems.findGuWith(TemperingModule.class, module -> module.getGrade() == grade) : null;
        if (gu == null) {
            if (worn != null) PathEffectService.removeType(player, type);
            return;
        }
        if (worn != null && worn.payload() == grade) return;

        PathEffectService.apply(player, gu.getSpec().createEffect(type, getId(gu), PathEffect.PERMANENT, grade));
    }

    private static <P> PathEffect<P> withLayers(PathEffect<P> effect, Map<GuPath, DaoMarks> layer, int layers) {
        PathEffect<P> next = effect;
        while (next.layers() < layers) next = next.addLayer(layer);
        int dropped = next.layers() - layers;
        for (int i = 0; i < dropped; i++) {
            for (Map.Entry<GuPath, DaoMarks> entry : layer.entrySet()) next = next.cut(entry.getKey(), entry.getValue());
        }
        return new PathEffect<>(next.type(), next.carrier(), next.source(), next.paths(), next.endTick(), layers,
                next.payload());
    }

    private static boolean isOf(PathEffect<?> effect, PathEffectType<?> type, ResourceLocation source) {
        return effect.type() == type && effect.source().equals(source);
    }

    private static ResourceLocation getId(MortalGuItem gu) { return BuiltInRegistries.ITEM.getKey(gu); }
}
