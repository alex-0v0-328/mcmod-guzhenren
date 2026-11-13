package net.alex.guzhenren.compat;

import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.alex.guzhenren.gameplay.plunge.PlungeService;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.event.types.entity.ModifyPlayerLivingMotionEvent;
import yesman.epicfight.api.client.event.types.render.PrepareModelEvent;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.animation.InitAnimatorEvent;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;

/**
 * The client half of the Epic Fight bridge: a second-stage flier [二段飞行] leans into the flight, a plunging
 * player [坠击] holds the stomp dive, a meditating player [打坐] sits cross-legged and a player refining a Gu [炼化]
 * holds it before the chest; client only.
 *
 * <p>Epic Fight draws an elytra flier lying along the flight and tilts the model by the negated pitch about its X
 * axis. A second-stage flier gets the same picture from this mod's own flight loop
 * ({@code biped/living/second_stage_flight_loop}, {@link EpicFightIntegration}): {@link #onInitAnimator} maps
 * {@link EpicFightLivingMotions#SECOND_STAGE_FLIGHT_LOOP} to it on every client player's animator, before Epic Fight
 * takes the animator's motions as its defaults, and {@link #onLivingMotion} sets that motion in place of the
 * creative flight, unless an animation holds the living motion (an action, the crash clip) or the flier is dying.
 * {@link #onPrepareModel} adds the tilt, scaled
 * by the flier's lean: it fills over {@link #LEAN_TICKS} while the mark is on and empties as fast once it goes
 * ({@link #onClientTick}), interpolated between ticks. The mark reaches every client that sees the flier, so others
 * see the lean too; the flier's own first-person view stays untilted. Epic Fight's own motion blend eases the
 * animation change.
 *
 * <p>A plunging player gets {@link EpicFightLivingMotions#PLUNGE_FALL}, mapped to the fall loop
 * ({@code biped/living/plunge_fall}) the same way, for as long as the plunge mark is on: from the start to the
 * landing or to whatever else ends the plunge. The mark reaches every client that sees the player too. The plunge
 * comes first: starting it ends the second stage, and the two marks may cross on the way to a client.
 *
 * <p>A player who nourishes an aperture [温养空窍], is held still by the awakening ritual [开窍仪式] or pours
 * essence into a channeled Gu [灌注]
 * ({@link EpicFightIntegration#isMeditating}) gets {@link EpicFightLivingMotions#MEDITATE_LOOP}, mapped to the seated
 * loop ({@code biped/living/meditate_loop}) the same way, after the plunge and the flight. All three states reach every
 * client that sees the player. The sit-down and stand-up clips come from the server and hold the living motion
 * while they run, so the loop takes over when the sit-down ends and the idle when the stand-up lets go.
 *
 * <p>A player refining a wild Gu [炼化] ({@code TendedGuItem#isRefining}: the held use of a tended Gu not yet
 * refined, which every client reads off the player's use item) gets {@link EpicFightLivingMotions#REFINE_LOOP} as
 * the composite living motion ({@link #onCompositeMotion}), mapped to the refining hold
 * ({@code biped/living/refine_loop}): the clip's data file puts it on a composite layer under the
 * {@code chest_arms} mask, so the right hand comes up to the chest and the head bows toward it while the legs keep the
 * base layer's motion, and the hold ends with the use, refined or let go. An animation that holds the living motion
 * (an action, a landing) clears the composite motion, so the hold gives way to it.
 *
 * <p>{@link #getLockOnTarget} reads Epic Fight's lock-on [锁定目标] for the moon blade Gu [月刃]: the entity its camera
 * is locked on, while the lock is held, read off {@code EpicFightCameraAPI} (the local player patch's own target is
 * that same focused entity).
 *
 * <p>{@link #initialize} registers the four hooks; client setup calls it once.
 *
 * @author Alex
 * @version 1.0.0
 * @see EpicFightIntegration
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class EpicFightClientIntegration {

    private static final int LEAN_TICKS = 6;
    private static final Int2ObjectMap<float[]> LEANS = new Int2ObjectOpenHashMap<>();

    private EpicFightClientIntegration() {}

    public static void initialize() {
        EpicFightClientEventHooks.Entity.MODIFY_PLAYER_LIVING_MOTION_BASE.registerEvent(
                EpicFightClientIntegration::onLivingMotion, Guzhenren.MOD_ID);
        EpicFightClientEventHooks.Entity.MODIFY_PLAYER_LIVING_MOTION_COMPOSITE.registerEvent(
                EpicFightClientIntegration::onCompositeMotion, Guzhenren.MOD_ID);
        EpicFightClientEventHooks.Render.PREPARE_MODEL_TO_RENDER.registerEvent(
                EpicFightClientIntegration::onPrepareModel, Guzhenren.MOD_ID);
        EpicFightEventHooks.Animation.INIT_ANIMATOR.registerEvent(
                EpicFightClientIntegration::onInitAnimator, Guzhenren.MOD_ID);
    }

    /** The living entity Epic Fight's camera is locked on, or null while it is not locking on. */
    public static @Nullable LivingEntity getLockOnTarget() {
        EpicFightCameraAPI camera = EpicFightCameraAPI.getInstance();
        if (!camera.isLockingOnTarget()) return null;
        LivingEntity target = camera.getFocusingEntity();
        return target != null && target.isAlive() ? target : null;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) return;
        for (Player player : level.players()) {
            if (isLeaning(player) && !LEANS.containsKey(player.getId())) LEANS.put(player.getId(), new float[2]);
        }
        for (ObjectIterator<Int2ObjectMap.Entry<float[]>> it = Int2ObjectMaps.fastIterator(LEANS); it.hasNext(); ) {
            Int2ObjectMap.Entry<float[]> entry = it.next();
            float[] lean = entry.getValue();
            lean[0] = lean[1];
            boolean leaning = level.getEntity(entry.getIntKey()) instanceof Player player && isLeaning(player);
            lean[1] = Mth.clamp(lean[1] + (leaning ? 1.0F : -1.0F) / LEAN_TICKS, 0.0F, 1.0F);
            if (!leaning && lean[0] == 0.0F && lean[1] == 0.0F) it.remove();
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) { LEANS.clear(); }

    private static void onLivingMotion(ModifyPlayerLivingMotionEvent.BaseLayer event) {
        if (event.inaction() || event.getMotion() == LivingMotions.DEATH) return;
        Player player = event.getPlayerPatch().getOriginal();
        if (PlungeService.isActive(player)) {
            event.setMotion(EpicFightLivingMotions.PLUNGE_FALL);
        } else if (isLeaning(player)) {
            event.setMotion(EpicFightLivingMotions.SECOND_STAGE_FLIGHT_LOOP);
        } else if (EpicFightIntegration.isMeditating(player)) {
            event.setMotion(EpicFightLivingMotions.MEDITATE_LOOP);
        }
    }

    private static void onCompositeMotion(ModifyPlayerLivingMotionEvent.CompositeLayer event) {
        if (TendedGuItem.isRefining(event.getPlayerPatch().getOriginal())) {
            event.setMotion(EpicFightLivingMotions.REFINE_LOOP);
        }
    }

    private static void onInitAnimator(InitAnimatorEvent event) {
        if (!(event.getEntityPatch() instanceof AbstractClientPlayerPatch<?>)) return;
        event.getAnimator().addLivingAnimation(EpicFightLivingMotions.SECOND_STAGE_FLIGHT_LOOP,
                EpicFightIntegration.getSecondStageFlightLoop());
        event.getAnimator().addLivingAnimation(EpicFightLivingMotions.PLUNGE_FALL,
                EpicFightIntegration.getPlungeFall());
        event.getAnimator().addLivingAnimation(EpicFightLivingMotions.MEDITATE_LOOP,
                EpicFightIntegration.getMeditateLoop());
        event.getAnimator().addLivingAnimation(EpicFightLivingMotions.REFINE_LOOP,
                EpicFightIntegration.getRefineLoop());
    }

    private static void onPrepareModel(PrepareModelEvent event) {
        if (!(event.getEntityPatch().getOriginal() instanceof AbstractClientPlayer player)) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (player == minecraft.player && minecraft.options.getCameraType().isFirstPerson()) return;
        float[] lean = LEANS.get(player.getId());
        if (lean == null) return;
        float partialTick = event.getPartialTicks();
        float amount = Mth.lerp(partialTick, lean[0], lean[1]);
        if (amount <= 0.0F) return;
        event.getPoseStack().mulPose(Axis.XP.rotationDegrees(-amount * player.getViewXRot(partialTick)));
    }

    private static boolean isLeaning(Player player) {
        return !player.isPassenger() && SecondStageService.isActive(player);
    }
}
