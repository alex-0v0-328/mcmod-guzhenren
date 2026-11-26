package net.alex.guzhenren.compat;

import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.alex.guzhenren.gameplay.path.strength.StoreStrength;
import net.alex.guzhenren.gameplay.plunge.PlungeService;
import net.alex.guzhenren.item.armor.SchoolUniformItem;
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
import yesman.epicfight.api.client.event.types.render.AnimatedArmorTextureEvent;
import yesman.epicfight.api.client.event.types.render.PrepareModelEvent;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.animation.InitAnimatorEvent;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;

/**
 * Epic Fight 联动的客户端动画、模型姿态与锁定目标桥接。
 * <ul>
 *   <li>{@link #initialize} 在客户端初始化时向 Epic Fight 注册动画初始化、基础层动作、合成层动作与模型渲染前准备四个事
 *       件钩子；{@link #onInitAnimator} 在玩家动画器创建时将 {@link EpicFightLivingMotions} 的 5 个自定义常态动作绑定到
 *       {@code biped/living/} 下的同名循环动画。</li>
 *   <li>基础层常态动作（{@link #onLivingMotion}）在无高优动作占用且玩家未濒死时按「坠击 &gt;
 *       二段飞行[Second-Stage Flight] &gt; 打坐」优先级切换，由 Epic Fight 负责动作混合过渡；这三类状态均已同步至所有可
 *       见客户端（坠击启动时会终止二段飞行，两端标记在网络传输中可能短暂交错）。</li>
 *   <li>二段飞行俯仰倾斜（{@link #onPrepareModel}）：仿照鞘翅飞行姿态将玩家模型绕 X 轴旋转负俯仰角，倾斜权重在
 *       {@link #onClientTick} 中按 {@link #LEAN_TICKS} 线性渐入渐出并在帧间插值；本地玩家处于第一人称视角时不倾
 *       斜。</li>
 *   <li>打坐状态（{@link EpicFightIntegration#isMeditating}）涵盖温养[Nourish]空窍、开窍[Awakening]仪式定身与
 *       灌注类蛊[Channeled Gu]注入真元：入座与起身过渡动画由服务端下发并短暂接管基础层，入座完成后落入循环，起身完成后
 *       恢复待机。</li>
 *   <li>合成层常态动作（{@link #onCompositeMotion}）在 {@code chest_arms} 上半身遮罩下叠加，保留腿部走跑跳动作：
 *       惯力蛊[Habitual Strength Gu]可移动蓄力（{@link StoreStrength#isMobile}）在 {@code HIGH} 叠加层播放
 *       {@link EpicFightLivingMotions#CHARGE_HOLD_MOVE}，优先级高于在 {@code MIDDLE} 叠加层播放的野生蛊炼化持握动作
 *       {@link EpicFightLivingMotions#REFINE_LOOP}（{@link TendedGuItem#isRefining}）；当基础层播放攻击或落地等主帧动画
 *       时合成层自动让位。</li>
 *   <li>{@link #getLockOnTarget} 读取 {@link EpicFightCameraAPI} 当前镜头锁定且存活的实体（未锁定时返回
 *       {@code null}），供月刃等投射物制导使用。</li>
 * </ul>
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
        EpicFightClientEventHooks.Render.ANIMATED_ARMOR_TEXTURE.registerEvent(
                EpicFightClientIntegration::onAnimatedArmorTexture, Guzhenren.MOD_ID, -1000);
        EpicFightEventHooks.Animation.INIT_ANIMATOR.registerEvent(
                EpicFightClientIntegration::onInitAnimator, Guzhenren.MOD_ID);
    }

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
        Player player = event.getPlayerPatch().getOriginal();
        if (StoreStrength.isMobile(player)) {
            event.setMotion(EpicFightLivingMotions.CHARGE_HOLD_MOVE);
        } else if (TendedGuItem.isRefining(player)) {
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
        event.getAnimator().addLivingAnimation(EpicFightLivingMotions.CHARGE_HOLD_MOVE,
                EpicFightIntegration.getChargeHoldMove());
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

    private static void onAnimatedArmorTexture(AnimatedArmorTextureEvent event) {
        if (event.getItemStack().getItem() instanceof SchoolUniformItem) {
            event.setResultLocation(null);
        }
    }

    private static boolean isLeaning(Player player) {
        return !player.isPassenger() && SecondStageService.isActive(player);
    }
}
