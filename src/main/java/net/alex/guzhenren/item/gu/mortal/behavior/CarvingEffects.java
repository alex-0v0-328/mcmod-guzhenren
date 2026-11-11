package net.alex.guzhenren.item.gu.mortal.behavior;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.body.BodyGrade;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.BodyTemperedEvent;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.SkinShedEvent;
import net.alex.guzhenren.gameplay.body.SkinWornEvent;
import net.alex.guzhenren.gameplay.body.Tendon;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectEndedEvent;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.PathEffectType;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * The body effects of the tempering Gu [炼体蛊]: the skin, tendons and bones. Each is the bookkeeping of a grade
 * {@code BodyService} owns and follows its write through the event that write posts, so the grade's writer, a Gu or
 * a command alike, knows nothing of it. The grade follows its effect back ({@link #onEnded}): a skin, tendon or bone
 * effect removed drops the part to its native grade, so there is no grade without its marks. The beast and human
 * strengths are not here: their effects are their store, written by {@code PathStrengthService}.
 *
 * <p>Event subscriber over {@link PathEffectService}; the Gu that made a grade is found in {@code ModItems}, and its
 * {@code GuProperties} supplies the carrier, path and strands, source the Gu's item id. A grade is permanent, one
 * exclusive effect per part, so a new grade takes the old effect and its strands with it.
 *
 * <p>⚠ Every sync compares the effect with the state and writes only on a difference, so a handler running twice
 * is harmless. ⚠ The marks arrive only with the write that
 * sets the state: nothing re-adds them on login, and a state saved before its effect existed stays without. ⚠ A
 * timed skin is not this class's: wearing a jade skin over a permanent one only drops the permanent skin's effect,
 * and {@code onEnded} leaves a part alone once its state names another grade than the removed effect.
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
    public static void onEnded(PathEffectEndedEvent event) {
        if (event.getReason() != PathEffectEndedEvent.Reason.REMOVED) return;

        ServerPlayer player = event.getPlayer();
        Object payload = event.getEffect().payload();
        if (payload instanceof Skin skin) {
            if (BodyService.getSkin(player) == skin) BodyService.shedSkin(player);
        } else if (payload instanceof Tendon tendon) {
            if (BodyService.getTendon(player) == tendon) BodyService.temperTendon(player, Tendon.NATIVE);
        } else if (payload instanceof Bone bone) {
            if (BodyService.getBone(player) == bone) BodyService.temperBone(player, Bone.NATIVE);
        }
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

    private static <G extends Enum<G> & BodyGrade<G>> void syncGrade(
            ServerPlayer player, PathEffectType<G> type, G grade, boolean carries) {
        PathEffect<G> worn = PathEffectService.find(player, type);
        MortalGuItem gu = carries && !grade.isNative()
                ? ModItems.findGuWith(TemperingBehavior.class, behavior -> behavior.getGrade() == grade) : null;
        if (gu == null) {
            if (worn != null) PathEffectService.removeType(player, type);
            return;
        }
        if (worn != null && worn.payload() == grade) return;

        PathEffectService.apply(player, gu.createEffect(type, PathEffect.PERMANENT, grade));
    }
}
