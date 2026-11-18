package net.alex.guzhenren.gameplay.plunge;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.gameplay.attribute.TransientModifiers;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.dimension.DimensionTravelService;
import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.alex.guzhenren.gameplay.impact.ImpactCrater;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 坠击服务：在空中按潜行 + Alt，以 {@link #GRAVITY_MULTIPLIER}（8）倍重力垂直急坠，落地造成 3 倍摔落自伤并炸坑。
 * <ul>
 *   <li>{@link #canStart} 校验起手条件（双端共用，所有玩家含创造模式均无冷却可用）：存活、未睡眠、未骑乘、离地（飞行、
 *       下落、飘浮、鞘翅滑翔均可）、不在流体或梯子中、不在宝黄天[Treasure Yellow Heaven]内且未处于坠击中。</li>
 *   <li>{@link #start} 记录起跳最高点（{@code getY() + fallDistance}，使半途起砸也计入前段坠落）、
 *       关闭二段飞行[Second-Stage Flight]与普通飞行、收起鞘翅、清除飘浮与缓降、将水平与向上速度清零，并通过
 *       {@link TransientModifiers#setGravityFactor} 挂载同步的 8 倍重力修饰符，使双端同速下坠。</li>
 *   <li>{@link #tick} 每个服务端 tick 维持锁飞行/鞘翅、清飘浮/缓降并封顶下落速度（不可主动取消）；若中途死亡、骑乘、入
 *       水/岩浆、抓梯、进入宝黄天、切换维度或传送（{@code !canContinue}），调用 {@link #stop} 提前结束，不炸坑且不加倍
 *       摔伤。</li>
 *   <li>⚠ 服务端每 tick 先模拟一步移动再拉回客户端上报坐标：当服务端模拟步比客户端早一 tick 触地并把服务端垂直速度清零
 *       后，若客户端最后一 tick 位移超过 10 格会被原版「移动过快」检查拉回，因此双端统一将每 tick 下落速度封顶为
 *       {@link #MAX_FALL_SPEED}（8 格/tick，约 160 格/秒）。</li>
 *   <li>⚠ 落地判定必须同时满足 {@code onGround() && fallDistance == 0.0F}：因为仅客户端移动数据包会触发原版落地重置
 *       {@code fallDistance}，等客户端确认落地再调 {@code land} 可确保炸坑位置与客户端落点一致（坠击期间原版摔落事件由
 *       {@link PlungeEvents} 取消）。</li>
 *   <li>{@code land} 依次发布 {@link PlungeLandedEvent}（脚下地面尚未炸毁）、调用 {@link ImpactCrater#blast} 炸坑，最后
 *       按玩家安全摔落距离与摔落伤害倍率结算 {@link PlungeImpact#getDamage} 普通摔落伤害（吃摔落缓冲、保护与抗性提升，
 *       干草块、黏液块与床不减免；可飞行玩家同样受伤，脚下炸出坑后继续跌入坑底按普通摔落计）。</li>
 *   <li>{@link #tickLocal} 在本机每 tick 锁死飞行与鞘翅并在触地瞬间刹停速度（防止被黏液块弹起）；坠击顶点附件同步给所有
 *       追踪客户端以驱动下坠动作（不落盘，重登或死亡自动清除）。</li>
 * </ul>
 */

public final class PlungeService {

    private PlungeService() {}

    public static final double GRAVITY_MULTIPLIER = 8.0D;
    public static final double MAX_FALL_SPEED = 8.0D;
    private static final ResourceLocation GRAVITY_MODIFIER_ID = Guzhenren.id("plunge_gravity");

    public static boolean isActive(@NotNull Player player) { return PlayerData.get(player).getPlungeTopY() != null; }

    public static boolean canStart(@NotNull Player player) {
        return !isActive(player) && !player.onGround() && canContinue(player);
    }

    public static boolean start(@NotNull ServerPlayer player) {
        if (!canStart(player)) return false;
        PlayerData.get(player).setPlungeTopY(player.getY() + player.fallDistance);
        SecondStageService.stop(player);
        clearFloating(player);
        holdDown(player);
        player.setDeltaMovement(0.0D, Math.min(player.getDeltaMovement().y, 0.0D), 0.0D);
        player.hurtMarked = true;
        TransientModifiers.setGravityFactor(player, GRAVITY_MODIFIER_ID, GRAVITY_MULTIPLIER);
        return true;
    }

    public static void stop(@NotNull ServerPlayer player) {
        PlayerData.get(player).setPlungeTopY(null);
        TransientModifiers.setGravityFactor(player, GRAVITY_MODIFIER_ID, 1.0D);
    }

    public static void tick(@NotNull ServerPlayer player) {
        Double topY = PlayerData.get(player).getPlungeTopY();
        if (topY == null) {
            TransientModifiers.setGravityFactor(player, GRAVITY_MODIFIER_ID, 1.0D);
        } else if (!canContinue(player)) {
            stop(player);
        } else if (player.onGround() && player.fallDistance == 0.0F) {
            land(player, topY);
        } else {
            clearFloating(player);
            holdDown(player);
        }
    }

    public static void tickLocal(@NotNull Player player) {
        if (!isActive(player)) return;
        holdDown(player);
        if (player.onGround()) player.setDeltaMovement(Vec3.ZERO);
    }

    private static boolean canContinue(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.isPassenger() && !player.isSleeping()
                && !player.isInFluidType() && !player.onClimbable()
                && !DimensionTravelService.isInside(player, ModDimensions.TREASURE_YELLOW_HEAVEN);
    }

    private static void clearFloating(ServerPlayer player) {
        player.removeEffect(MobEffects.LEVITATION);
        player.removeEffect(MobEffects.SLOW_FALLING);
    }

    private static void holdDown(Player player) {
        if (player.isFallFlying()) player.stopFallFlying();
        Vec3 motion = player.getDeltaMovement();
        if (motion.y < -MAX_FALL_SPEED) player.setDeltaMovement(motion.x, -MAX_FALL_SPEED, motion.z);
        if (!player.getAbilities().flying) return;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
    }

    private static void land(ServerPlayer player, double topY) {
        double height = PlungeImpact.getHeight(topY, player.getY());
        stop(player);
        player.setDeltaMovement(Vec3.ZERO);
        float impact = PlungeImpact.getImpactPower(height);
        if (impact > 0.0F) ModEvents.post(new PlungeLandedEvent(player, player.position(), impact));
        float power = PlungeImpact.getCraterPower(height);
        if (power > 0.0F) ImpactCrater.blast(player, player.position(), power);
        int damage = PlungeImpact.getDamage(height, player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE),
                player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER));
        if (damage > 0) player.hurt(player.damageSources().fall(), damage);
    }
}
