package net.alex.guzhenren.compat;

import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.minecraft.client.Minecraft;
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

/**
 * The client half of the Epic Fight bridge: a second-stage flier [二段飞行] leans into the flight; client only.
 *
 * <p>Epic Fight draws an elytra flier with its glide animation ({@code LivingMotions.FLY}), whose body already lies
 * along the flight, and tilts the model by the negated pitch about its X axis. {@link #onLivingMotion} gives a
 * second-stage flier the same glide in place of the creative flight, unless an action animation holds the body or
 * the flier is dying, and {@link #onPrepareModel} the same tilt, scaled
 * by the flier's lean: it fills over {@link #LEAN_TICKS} while the mark is on and empties as fast once it goes
 * ({@link #onClientTick}), interpolated between ticks. The mark reaches every client that sees the flier, so others
 * see the lean too; the flier's own first-person view stays untilted. Epic Fight's own motion blend eases the
 * animation change.
 *
 * <p>{@link #getLockOnTarget} reads Epic Fight's lock-on [锁定目标] for the moon blade Gu [月刃]: the entity its camera
 * is locked on, while the lock is held, read off {@code EpicFightCameraAPI} (the local player patch's own target is
 * that same focused entity).
 *
 * <p>{@link #initialize} registers both hooks; client setup calls it once.
 *
 * @author Alex
 * @version 1.0.0
 * @see EpicFightIntegration
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class EpicFightClientIntegration {

    private static final int LEAN_TICKS = 6;
    private static final Map<Integer, float[]> LEANS = new HashMap<>();

    private EpicFightClientIntegration() {}

    public static void initialize() {
        EpicFightClientEventHooks.Entity.MODIFY_PLAYER_LIVING_MOTION_BASE.registerEvent(
                EpicFightClientIntegration::onLivingMotion, Guzhenren.MOD_ID);
        EpicFightClientEventHooks.Render.PREPARE_MODEL_TO_RENDER.registerEvent(
                EpicFightClientIntegration::onPrepareModel, Guzhenren.MOD_ID);
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
        if (minecraft.level == null || minecraft.isPaused()) return;
        for (Player player : minecraft.level.players()) {
            if (isLeaning(player)) LEANS.computeIfAbsent(player.getId(), id -> new float[2]);
        }
        LEANS.entrySet().removeIf(entry -> {
            float[] lean = entry.getValue();
            lean[0] = lean[1];
            boolean leaning = minecraft.level.getEntity(entry.getKey()) instanceof Player player && isLeaning(player);
            lean[1] = Mth.clamp(lean[1] + (leaning ? 1.0F : -1.0F) / LEAN_TICKS, 0.0F, 1.0F);
            return !leaning && lean[0] == 0.0F && lean[1] == 0.0F;
        });
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) { LEANS.clear(); }

    private static void onLivingMotion(ModifyPlayerLivingMotionEvent.BaseLayer event) {
        if (event.inaction() || event.getMotion() == LivingMotions.DEATH) return;
        if (isLeaning(event.getPlayerPatch().getOriginal())) event.setMotion(LivingMotions.FLY);
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
