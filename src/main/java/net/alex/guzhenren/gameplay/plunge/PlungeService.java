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
 * 坠击：空中按潜行 + Alt，以 {@link #GRAVITY_MULTIPLIER} 倍重力垂直下砸。
 * <ul>
 *   <li>落地炸坑，受 3 倍摔落伤害；本类为服务端半边与两端共用规则，客户端发组合键
 *       （{@code net.alex.guzhenren.client.input.PlungeInput}）。</li>
 *   <li>{@link #canStart}：存活、未睡、未骑乘、离地、不在流体与梯子上、不在宝黄天[Treasure Yellow Heaven]、未在坠击，飞
 *       行、下落、漂浮、滑翔都算空中；客户端发前查，服务端 {@link #start} 再查；人人可用，含创造，无冷却；落差不足以伤
 *       人则不留痕。</li>
 *   <li>{@link #start} 关飞行与二段飞行[Second-Stage Flight]、收鞘翅、清漂浮与缓降、水平与向上速度归零，经同步重力属性
 *       乘重力（{@code TransientModifiers#setGravityFactor}），两端同速下落；顶点 = 高度 + 已坠距离，长坠途中才开始也不
 *       少算。</li>
 *   <li>⚠ 服务端每 tick 先自行步进再放回客户端上报位置，速度才跟上下落；该速度即原版「移动过快」在每步 10 格外的额外放
 *       行，服务端步进早一 tick 触地将其归零，故客户端末步须 < 10 格：两端封顶每 tick {@link #MAX_FALL_SPEED} 格（约
 *       160 格/秒，坠约 60 格后），10 TPS 一 tick 两步也成立。</li>
 *   <li>{@link #tick} 每服务端 tick 运行（{@link PlungeEvents}），落地前锁飞行与鞘翅、再清漂浮与缓降、封顶速度；不可取
 *       消；死亡、骑乘、流体、梯子、宝黄天、换维度、传送提前结束，不成坑无额外伤害（{@link #stop}）。</li>
 *   <li>⚠ 在地面且原版已重置摔落距离才算落地：只有客户端移动走原版摔落处理（{@code ServerPlayer#checkFallDamage} 为
 *       空），服务端步进虽先触地，客户端落地才重置距离，故坑在客户端落点，原版看到的摔落即坠击自身的，{@link #isActive}
 *       期间取消。</li>
 *   <li>有表现的落地（{@link PlungeImpact#getImpactPower}）先在完整地面发 {@link PlungeLandedEvent} 供上层地裂与震屏，
 *       再按 {@link PlungeImpact#getCraterPower} 炸坑，再按原版 {@code LivingEntity#calculateFallDamage} 所读的安全摔落
 *       距离与摔落伤害倍率造成 {@link PlungeImpact#getDamage} 普通摔落伤害：摔落缓冲、保护、抗性提升照常减免；可飞行玩
 *       家也受伤；干草块、黏液块、床不缓冲；坑挖掉脚下，掉进坑是普通摔落。</li>
 *   <li>坑为 {@link ImpactCrater#blast}：原版爆炸按爆炸抗性破坏地面、不伤人，先查 {@code ModEvents#canGrief}，击退坠击
 *       者以外的一切。</li>
 *   <li>{@link #tickLocal}：服务端纠正之间锁飞行与鞘翅、封顶速度，落地即刹停，黏液块弹不回；顶点同步给所有看得到该玩家
 *       的客户端以保持坠落姿势（{@code net.alex.guzhenren.compat.EpicFightClientIntegration}），不存档，死亡清除，重登
 *       重生不在坠击中；重力修饰为临时，{@link #tick} 从未坠击的玩家身上摘除。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see PlungeImpact
 * @since 1.0.0
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
