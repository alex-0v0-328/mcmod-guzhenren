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
 * Epic Fight 桥接的客户端侧。
 *
 * <ul>
 *   <li>二段飞行[Second-Stage Flight]：仿 Epic Fight 鞘翅飞行者顺飞向躺平（模型绕 X 轴转负俯仰角），用本模组飞行循环
 *       （{@code biped/living/second_stage_flight_loop}，{@link EpicFightIntegration}）。</li>
 *   <li>{@link #onInitAnimator} 为每个客户端玩家动画器把 {@link EpicFightLivingMotions} 的四个动作映射到
 *       {@code biped/living/} 下同名小写循环，早于 Epic Fight 把动作取作默认值。</li>
 *   <li>{@link #onLivingMotion} 换基础动作，优先级坠击 > 二段飞行（替换创造飞行）> 打坐；有动画占着基础动作
 *       （动作、撞毁片段）或玩家濒死时不换；切换由 Epic Fight 动作混合过渡。</li>
 *   <li>{@link #onPrepareModel} 按前倾量倾斜：二段飞行标记在时 {@link #LEAN_TICKS} 内涨满，去掉后同速清空
 *       （{@link #onClientTick}），tick 间插值；飞行者自己的第一人称不倾斜。</li>
 *   <li>二段飞行、坠击标记与打坐三种状态都送达每个看得见该玩家的客户端。</li>
 *   <li>坠击：标记从开始保持到落地或其他结束坠击的情形；开始坠击会结束二段飞行，两个标记到客户端时可能交错。</li>
 *   <li>打坐（{@link EpicFightIntegration#isMeditating}）：温养[Nourish]空窍[Aperture]、被开窍[Awakening]仪式定住
 *       或向灌注类蛊[Channeled Gu]注入真元[Essence]；坐、起片段来自服务端并占基础动作，坐下后接循环，起身回待机。</li>
 *   <li>炼化野生蛊（{@code TendedGuItem#isRefining}：按住使用未炼化的持续照顾型蛊[Tended Gu]，各客户端从使用物品读出）
 *       的玩家得合成层动作 {@link EpicFightLivingMotions#REFINE_LOOP}（{@link #onCompositeMotion}）；数据文件将其置于
 *       {@code chest_arms} 遮罩下：右手抬到胸前、头朝它低下，腿保持基础层动作。</li>
 *   <li>举蛊随使用结束而结束，炼成或松手皆然；占基础动作的动画（动作、落地）清掉合成层动作，举蛊让位。</li>
 *   <li>{@link #getLockOnTarget} 为月刃读 Epic Fight 锁定目标：锁定期间 {@code EpicFightCameraAPI} 的聚焦实体
 *       （本地玩家 patch 自己的目标即同一实体）；{@link #initialize} 注册四个钩子，客户端初始化时调用一次。</li>
 * </ul>
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

    /** Epic Fight 镜头锁定的生物；未锁定时为 null。 */
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
