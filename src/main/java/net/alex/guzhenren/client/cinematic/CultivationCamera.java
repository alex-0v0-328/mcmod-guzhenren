package net.alex.guzhenren.client.cinematic;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.cinematic.CultivationShots.Pose;
import net.alex.guzhenren.client.cinematic.CultivationShots.Shot;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import org.jetbrains.annotations.Nullable;

/**
 * 温养[Nourish]空窍时本地玩家的电影化运镜控制器：仅运行于客户端，机位几何定义在 {@link CultivationShots}。
 * <ul>
 *   <li>温养进行中（{@link ApertureNourishService#isCultivating}）镜头在 1 秒内平滑脱离玩家视角并切入机位循环，结束时用
 *       1 秒平滑回到原第一或第三人称视角；鼠标转向与移动输入另由 {@code NourishInputEvents} 锁定。</li>
 *   <li>{@link #getFrame} 计算本帧镜头姿态（{@link Frame}）并由 {@code GameRendererMixin} 写回；当镜头同时远离原视点与
 *       坐姿头部超过 {@link #DETACH_DISTANCE} 格时才离体并渲染玩家模型，防止近距离穿入头部模型看到内部贴图。</li>
 *   <li>进入过渡中的偏航沿上一帧的转向连续变化，不按每帧最短角重新选向，以免机位绕至背后时发生半圈甩头；完全进入后不再
 *       记录，退出时再从最短方向重新起算。</li>
 *   <li>机位开始时对开头、中间、结尾三处进行遮挡检测：若从坐姿头部到镜头的可视距离不足 {@link #MIN_CLEAR} 倍则跳过该机
 *       位；若所有机位均被遮挡（如室内）则退回环绕机位；每帧仍会根据实时射线遮挡把镜头拉回墙前 {@link #WALL_GAP}
 *       格。</li>
 *   <li>远眺机位的方向在机位开始时探测一次：从头部向 8 个水平方向各发射 {@link #VISTA_RAY} 格并略向上抬升射线；侧推机位
 *       首轮先试左侧并每轮交替，若首试方向受阻则换向另一侧。</li>
 *   <li>运镜显示期间（{@link #isShowing}，含进出过渡）隐藏第一人称手部、方块选框及水下、着火、窒息贴脸遮罩；左键攻击与
 *       选取方块交互一律丢弃（准星不再对应画面中心），右键保留给扇形选择器、长按滑条与物品交互。</li>
 *   <li>HUD 由 {@code ClientEvents} 过滤至仅保留白名单层，上下黑边与切镜黑屏由 {@code CinematicHud} 读取
 *       {@link #getBars} 和 {@link #getFade} 绘制；退出时不再触发切镜，黑屏强度停留在退出瞬间并随退出过渡淡出。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class CultivationCamera {

    private CultivationCamera() {}

    static final double DETACH_DISTANCE = 0.6;
    static final double MIN_CLEAR = 0.9;
    static final double WALL_GAP = 0.2;
    static final double VISTA_RAY = 48.0;
    private static final double VISTA_RISE = 4.0;
    private static final float[] CLEAR_SAMPLES = {0.0F, 0.5F, 1.0F};

    private static final class State {

        boolean active;
        int blendTicks;
        int blendTicksO;
        float facing;
        Shot shot = Shot.ORBIT;
        int shotTicks;
        boolean firstShot = true;
        float vistaYaw = Float.NaN;
        int side = -1;
        int cycle;
        float exitFade;
        float yawTurn = Float.NaN;
    }

    private static final State STATE = new State();

    public record Frame(Vec3 position, float yaw, float pitch, float roll, boolean detached) {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            reset();
            return;
        }
        if (minecraft.isPaused()) return;

        boolean wasActive = STATE.active;
        STATE.active = isWanted(minecraft, player);
        if (wasActive && !STATE.active) {
            STATE.exitFade = CultivationShots.getFade(STATE.shotTicks + 1.0F, STATE.shot.ticks, STATE.firstShot);
        }
        STATE.blendTicksO = STATE.blendTicks;
        if (STATE.active && STATE.blendTicks == 0) start(player);
        STATE.blendTicks = Mth.clamp(STATE.blendTicks + (STATE.active ? 1 : -1), 0, CultivationShots.BLEND_TICKS);
        if (isShowing()) {
            tickShot(player);
        } else {
            STATE.yawTurn = Float.NaN;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        if (isShowing()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderHighlight(RenderHighlightEvent.Block event) {
        if (isShowing()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderScreenEffect(RenderBlockScreenEffectEvent event) {
        if (isShowing()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!isShowing() || event.isUseItem()) return;

        event.setCanceled(true);
        event.setSwingHand(false);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) { reset(); }

    public static boolean isShowing() { return STATE.blendTicks > 0 || STATE.blendTicksO > 0; }

    public static float getBars(float partialTick) {
        return CultivationShots.getBars(Mth.lerp(partialTick, STATE.blendTicksO, STATE.blendTicks));
    }

    public static float getFade(float partialTick) {
        float fade = STATE.active
                ? CultivationShots.getFade(getShotTime(partialTick), STATE.shot.ticks, STATE.firstShot)
                : STATE.exitFade;
        return fade * getBlend(partialTick);
    }

    public static @Nullable Frame getFrame(Camera camera) {
        if (!isShowing()) return null;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || camera.getEntity() != player) return null;
        float partialTick = camera.getPartialTickTime();
        float blend = getBlend(partialTick);
        if (blend <= 0.0F) return null;

        Vec3 anchor = player.getPosition(partialTick);
        Pose pose = CultivationShots.getPose(STATE.shot, anchor, STATE.facing, STATE.vistaYaw, STATE.side,
                getShotTime(partialTick) / STATE.shot.ticks);
        Vec3 pivot = getPivot(anchor);
        Vec3 eye = getWallClamped(player, pivot, pose.eye());
        Vec3 origin = camera.getPosition();
        Vec3 position = origin.lerp(eye, blend);
        float previous = blend < 1.0F ? STATE.yawTurn : Float.NaN;
        float turn = CultivationShots.getContinuousTurn(CultivationShots.getYaw(eye, pose.target()) - camera.getYRot(),
                previous);
        STATE.yawTurn = blend < 1.0F ? turn : Float.NaN;
        boolean detached = camera.isDetached()
                || (position.distanceTo(origin) > DETACH_DISTANCE && position.distanceTo(pivot) > DETACH_DISTANCE);
        return new Frame(position, camera.getYRot() + turn * blend,
                Mth.lerp(blend, camera.getXRot(), CultivationShots.getPitch(eye, pose.target())),
                camera.getRoll() * (1.0F - blend), detached);
    }

    private static boolean isWanted(Minecraft minecraft, LocalPlayer player) {
        return player.isAlive() && !player.isSpectator() && minecraft.getCameraEntity() == player
                && ApertureNourishService.isCultivating(player);
    }

    private static float getBlend(float partialTick) {
        return CultivationShots.getBlend(Mth.lerp(partialTick, STATE.blendTicksO, STATE.blendTicks));
    }

    private static float getShotTime(float partialTick) {
        return Math.min(STATE.shotTicks + partialTick, STATE.shot.ticks);
    }

    //region shot sequence [机位轮换]
    private static void start(LocalPlayer player) {
        STATE.facing = player.getYRot();
        STATE.shot = Shot.ORBIT;
        STATE.shotTicks = 0;
        STATE.firstShot = true;
        STATE.cycle = 0;
        enterShot(player);
    }

    private static void tickShot(LocalPlayer player) {
        if (!STATE.active) {
            STATE.shotTicks = Math.min(STATE.shotTicks + 1, STATE.shot.ticks);
            return;
        }
        if (++STATE.shotTicks < STATE.shot.ticks) return;
        STATE.shotTicks = 0;
        STATE.firstShot = false;
        shiftShot();
        enterShot(player);
    }

    private static void enterShot(LocalPlayer player) {
        for (int tries = 0; tries < Shot.values().length; tries++) {
            if (prepareShot(player)) return;
            shiftShot();
        }
        STATE.shot = Shot.ORBIT;
    }

    private static void shiftShot() {
        STATE.shot = STATE.shot.getNext();
        if (STATE.shot == Shot.ORBIT) STATE.cycle++;
    }

    private static boolean prepareShot(LocalPlayer player) {
        if (STATE.shot == Shot.VISTA) {
            STATE.vistaYaw = getVistaYaw(player);
            return !Float.isNaN(STATE.vistaYaw) && isClear(player);
        }
        if (STATE.shot == Shot.DOLLY) {
            STATE.side = STATE.cycle % 2 == 0 ? -1 : 1;
            if (isClear(player)) return true;
            STATE.side = -STATE.side;
        }
        return isClear(player);
    }

    private static float getVistaYaw(LocalPlayer player) {
        Vec3 pivot = getPivot(player.position());
        double[] reaches = new double[CultivationShots.VISTA_DIRECTIONS];
        float step = 360.0F / CultivationShots.VISTA_DIRECTIONS;
        for (int i = 0; i < reaches.length; i++) {
            Vec3 end = pivot.add(CultivationShots.getHorizontal(STATE.facing + i * step).scale(VISTA_RAY))
                    .add(0.0, VISTA_RISE, 0.0);
            reaches[i] = getClearance(player, pivot, end);
        }
        int best = CultivationShots.pickVista(reaches);
        return best < 0 ? Float.NaN : STATE.facing + best * step;
    }

    private static boolean isClear(LocalPlayer player) {
        Vec3 anchor = player.position();
        Vec3 pivot = getPivot(anchor);
        for (float progress : CLEAR_SAMPLES) {
            Vec3 eye = CultivationShots.getPose(STATE.shot, anchor, STATE.facing, STATE.vistaYaw, STATE.side,
                    progress).eye();
            if (getClearance(player, pivot, eye) < pivot.distanceTo(eye) * MIN_CLEAR) return false;
        }
        return true;
    }
    //endregion

    private static Vec3 getPivot(Vec3 anchor) { return anchor.add(0.0, CultivationShots.HEAD_HEIGHT, 0.0); }

    private static Vec3 getWallClamped(Entity entity, Vec3 pivot, Vec3 eye) {
        double wanted = pivot.distanceTo(eye);
        if (wanted <= 0.0) return eye;
        double clear = getClearance(entity, pivot, eye);
        if (clear >= wanted) return eye;
        return pivot.add(eye.subtract(pivot).scale(Math.max(clear - WALL_GAP, 0.0) / wanted));
    }

    private static double getClearance(Entity entity, Vec3 from, Vec3 to) {
        HitResult hit = entity.level().clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE,
                entity));
        return hit.getType() == HitResult.Type.MISS ? from.distanceTo(to) : hit.getLocation().distanceTo(from);
    }

    private static void reset() {
        STATE.active = false;
        STATE.blendTicks = 0;
        STATE.blendTicksO = 0;
        STATE.shot = Shot.ORBIT;
        STATE.shotTicks = 0;
        STATE.firstShot = true;
        STATE.vistaYaw = Float.NaN;
        STATE.side = -1;
        STATE.cycle = 0;
        STATE.exitFade = 0.0F;
        STATE.yawTurn = Float.NaN;
    }
}
