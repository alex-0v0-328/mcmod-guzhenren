package net.alex.guzhenren.client.camera;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.cinematic.CultivationCamera;
import net.alex.guzhenren.client.hud.StoreStrengthCharge;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.event.types.camera.CoupleTPSCamera;

/**
 * Epic Fight 第三人称镜头[TPS Camera]之上的战斗构图层，仅运行于客户端：推拉镜头距离、收拢肩部偏移，补发镜头角度事件。
 * <ul>
 *   <li>{@link #getFrame} 由 {@code GameRendererMixin} 在 {@code Camera#setup} 之后、温养运镜之前调用并写回；温养运镜
 *       显示期间整层让位，距离与偏移只在 EF 镜头处于第三人称模式（{@code isTPSMode}）时调整。</li>
 *   <li>⚠ EF 第三人称模式与原版第三人称下的锁定都会取消原版 {@code Camera#setup}，NeoForge 打在其中的
 *       {@code ViewportEvent.ComputeCameraAngles} 随之不触发：{@link #onComputeCameraAngles} 记下本帧是否已触发，未触
 *       发时由 {@link #getFrame} 补发一次，让 {@code ImpactShakes}、{@code SecondStageCamera} 等角度监听照常生效；已触
 *       发时不得再发，否则同一帧叠加两次。</li>
 *   <li>镜头距离按 Leawind's Third Person 的半衰期指数平滑（{@link #approach}，单帧至多计
 *       {@link #MAX_FRAME_SECONDS} 秒）趋向 {@link #getDistanceTarget}：惯力蛊[Habitual Strength Gu]蓄力中乘
 *       {@link #CHARGE_DISTANCE}，二段飞行[Second-Stage Flight]按油门最多再拉远 {@link #FLIGHT_DISTANCE}；蓄力、
 *       飞行、恢复各用一档半衰期。</li>
 *   <li>俯仰绝对值超过 {@link #SQUEEZE_START} 度后按平滑阶跃收拢镜头相对眼睛的左右与上下偏移，到
 *       {@link #SQUEEZE_END} 度完全居中（{@link #getSqueeze}），低头或抬头时角色不挡准星。</li>
 *   <li>推拉后的位置从眼睛出发重做原版式 8 射线避障（偏移 {@link #PROBE} 格），只会被墙拉近，不穿墙。</li>
 *   <li>蓄力期间经 EF 的 {@code COUPLE_CAMERA} 让身体朝向跟随镜头（{@link #onCoupleCamera}）。</li>
 *   <li>横冲系冲撞由 {@code DashInput} 调 {@link #kick} 记下时刻；{@link #onComputeFov} 按距该时刻的秒数衰减视野
 *       放宽（{@link #getKick}），与镜头模式无关，第一人称同样生效，并随原版「视场角效果」滑条缩放；手部渲染的 FOV
 *       不受影响。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class CombatCameraRig {

    private CombatCameraRig() {}

    static final float CHARGE_DISTANCE = 0.8F;
    static final float FLIGHT_DISTANCE = 0.3F;
    static final float SQUEEZE_START = 50.0F;
    static final float SQUEEZE_END = 80.0F;
    static final float KICK_FOV = 0.08F;
    static final float KICK_HALF_LIFE = 0.12F;
    private static final float CHARGE_HALF_LIFE = 0.15F;
    private static final float FLIGHT_HALF_LIFE = 0.3F;
    private static final float RELEASE_HALF_LIFE = 0.25F;
    private static final float MAX_FRAME_SECONDS = 0.1F;
    private static final float KICK_SECONDS = 1.0F;
    private static final float MIN_CHANGE = 1.0E-3F;
    private static final double PROBE = 0.1D;

    private static final class State {

        long lastFrameNanos;
        float distance = 1.0F;
        long kickNanos;
        boolean anglesPosted;
    }

    private static final State STATE = new State();

    public record Frame(Vec3 position, float yaw, float pitch, float roll) {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            EpicFightClientEventHooks.Camera.COUPLE_CAMERA.registerEvent(CombatCameraRig::onCoupleCamera,
                    Guzhenren.MOD_ID);
        });
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) { STATE.anglesPosted = true; }

    public static void kick() { STATE.kickNanos = System.nanoTime(); }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!event.usedConfiguredFov() || STATE.kickNanos == 0L || CultivationCamera.isShowing()) return;
        float kick = getKick((System.nanoTime() - STATE.kickNanos) / 1.0E9F);
        if (kick <= 0.0F) return;
        float scale = Minecraft.getInstance().options.fovEffectScale().get().floatValue();
        event.setFOV(event.getFOV() * (1.0F + KICK_FOV * kick * scale));
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        STATE.lastFrameNanos = 0L;
        STATE.distance = 1.0F;
        STATE.kickNanos = 0L;
        STATE.anglesPosted = false;
    }

    static float getDistanceTarget(boolean charging, float throttle) {
        return (charging ? CHARGE_DISTANCE : 1.0F) * (1.0F + FLIGHT_DISTANCE * Mth.clamp(throttle, 0.0F, 1.0F));
    }

    static float getSqueeze(float pitch) {
        float progress = Mth.clamp((Math.abs(pitch) - SQUEEZE_START) / (SQUEEZE_END - SQUEEZE_START), 0.0F, 1.0F);
        return progress * progress * (3.0F - 2.0F * progress);
    }

    static float approach(float current, float target, float seconds, float halfLife) {
        if (halfLife <= 0.0F) return target;
        return current + (target - current) * (1.0F - (float) Math.pow(2.0D, -seconds / halfLife));
    }

    static float getKick(float seconds) {
        if (seconds < 0.0F || seconds >= KICK_SECONDS) return 0.0F;
        return (float) Math.pow(2.0D, -seconds / KICK_HALF_LIFE);
    }

    public static @Nullable Frame getFrame(Camera camera) {
        boolean anglesPosted = STATE.anglesPosted;
        STATE.anglesPosted = false;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || camera.getEntity() != player || CultivationCamera.isShowing()) return null;

        float partialTick = camera.getPartialTickTime();
        Vec3 position = camera.getPosition();
        if (EpicFightCameraAPI.getInstance().isTPSMode()) {
            boolean charging = StoreStrengthCharge.INSTANCE.isCharging();
            boolean flying = player.getAbilities().flying && SecondStageService.isActive(player);
            float throttle = flying ? (float) PlayerData.get(player).getSecondStageThrottle() : 0.0F;
            float halfLife = charging ? CHARGE_HALF_LIFE : flying ? FLIGHT_HALF_LIFE : RELEASE_HALF_LIFE;
            float target = getDistanceTarget(charging, throttle);
            STATE.distance = approach(STATE.distance, target, getFrameSeconds(), halfLife);
            float squeeze = getSqueeze(camera.getXRot());
            if (Math.abs(STATE.distance - 1.0F) > MIN_CHANGE || squeeze > 0.0F) {
                position = getPosition(camera, player, partialTick, STATE.distance, squeeze);
            }
        }
        if (anglesPosted) return new Frame(position, camera.getYRot(), camera.getXRot(), camera.getRoll());

        ViewportEvent.ComputeCameraAngles angles = NeoForge.EVENT_BUS.post(new ViewportEvent.ComputeCameraAngles(
                camera, partialTick, camera.getYRot(), camera.getXRot(), camera.getRoll()));
        STATE.anglesPosted = false;
        return new Frame(position, angles.getYaw(), angles.getPitch(), angles.getRoll());
    }

    private static void onCoupleCamera(CoupleTPSCamera event) {
        if (StoreStrengthCharge.INSTANCE.isCharging()) event.setShouldCouple(true);
    }

    private static Vec3 getPosition(Camera camera, LocalPlayer player, float partialTick, float distance,
                                    float squeeze) {
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 offset = camera.getPosition().subtract(eye);
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        double side = offset.x * left.x() + offset.y * left.y() + offset.z * left.z();
        double rise = offset.x * up.x() + offset.y * up.y() + offset.z * up.z();
        Vec3 wanted = offset
                .subtract(left.x() * side * squeeze, left.y() * side * squeeze, left.z() * side * squeeze)
                .subtract(up.x() * rise * squeeze, up.y() * rise * squeeze, up.z() * rise * squeeze)
                .scale(distance);
        double length = wanted.length();
        if (length < MIN_CHANGE) return eye;
        Vec3 direction = wanted.scale(1.0D / length);
        return eye.add(direction.scale(getClearDistance(player, eye, direction, length)));
    }

    private static double getClearDistance(LocalPlayer player, Vec3 eye, Vec3 direction, double length) {
        double allowed = length;
        for (int i = 0; i < 8; i++) {
            Vec3 from = eye.add(((i & 1) * 2 - 1) * PROBE, ((i >> 1 & 1) * 2 - 1) * PROBE,
                    ((i >> 2 & 1) * 2 - 1) * PROBE);
            HitResult hit = player.level().clip(new ClipContext(from, from.add(direction.scale(length)),
                    ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.MISS) allowed = Math.min(allowed, hit.getLocation().distanceTo(eye));
        }
        return allowed;
    }

    private static float getFrameSeconds() {
        long now = System.nanoTime();
        float seconds = STATE.lastFrameNanos == 0L ? 0.0F : (now - STATE.lastFrameNanos) / 1.0E9F;
        STATE.lastFrameNanos = now;
        return Math.min(seconds, MAX_FRAME_SECONDS);
    }
}
