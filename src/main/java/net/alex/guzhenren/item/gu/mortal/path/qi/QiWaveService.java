package net.alex.guzhenren.item.gu.mortal.path.qi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.effect.timed.StrengthQiGuEffect;
import net.alex.guzhenren.entity.projectile.BeastChargeEntity;
import net.alex.guzhenren.entity.projectile.QiBurst;
import net.alex.guzhenren.entity.projectile.QiWaveEntity;
import net.alex.guzhenren.gameplay.path.strength.BeastChargeDamage;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.ComboRound;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedEffects;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 力气蛊[Strength Qi Gu]（{@link StrengthQiGuEffect}）远程拳的服务端发射服务。
 * <ul>
 *   <li>处于力气蛊增益期间，Epic Fight 战斗模式下空手出拳且锁定目标在原版近战触及距离外时，这一拳化作远程拳（客户端仅
 *       上报目标实体 ID，服务端全量复验，射程仅受原版 128 格视线限制）；{@link #isPastReach} 是双端共用的唯一射程分界，
 *       触及距离内留给近战，同一拳不会近战与远程双重命中。</li>
 *   <li>一拳一发、没有间隔：客户端按下攻击键的上报（{@link #request}）与服务端 Epic Fight 真正起招（{@link #onStrike}）
 *       在 {@link #JOIN_TICKS} 内配对才发射，先到的一方等待另一方；Epic Fight 拒绝出招（动作中、采矿模式等）时上报作废，
 *       连按不会多发。</li>
 *   <li>只有连招轮[Combo Round]的收尾招结算本轮：兽力虚影流派按本招抽中的兽种各发一道 {@link BeastChargeEntity}（全面解
 *       放乘 3，合计截到当前肉身承受力），本轮范围冲击掷中则各自爆开时放球形冲击，并通知 {@link ComboRound#markRanged}
 *       不再放近战锥形；前几段或本招没抽中时从已持有的可显形永久兽力中随机一种按 1 倍发射。不可显形的兽种
 *       （{@code BeastStrengthFamily#canManifest}）不发射冲击。</li>
 *   <li>其他流派或未持有可显形永久兽力的玩家改为发射一枚 {@link QiWaveEntity}：伤害等同当次空手拳伤（收尾招含解放，并
 *       补入抽中那一击尚未计入属性的解放增量），不随原版攻击蓄力条缩放。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class QiWaveService {

    private QiWaveService() {}

    public static final int JOIN_TICKS = 2;
    private static final Map<UUID, Shot> SHOTS = new HashMap<>();
    private static final Map<UUID, Strike> STRIKES = new HashMap<>();

    private record Shot(int targetId, long tick) {}

    private record Strike(boolean finishing, long tick) {}

    public static boolean isPastReach(double distance, double reach) {
        return distance > reach;
    }

    public static boolean canPunch(@NotNull Player player) {
        return player.getMainHandItem().isEmpty() && player.isAlive() && !player.isSpectator();
    }

    public static double getBoxDistance(@NotNull Player player, @NotNull LivingEntity target) {
        return Math.sqrt(target.getBoundingBox().distanceToSqr(player.getEyePosition()));
    }

    public static boolean isJoined(long from, long now) { return now >= from && now - from <= JOIN_TICKS; }

    public static @NotNull List<BeastStrengthFamily> getChargeFamilies(boolean beastPath,
                                                                       @NotNull List<BeastStrengthFamily> drawn,
                                                                       @NotNull List<BeastStrengthFamily> held,
                                                                       @NotNull RandomSource random) {
        List<BeastStrengthFamily> shown = BeastStrengthFamily.getManifesting(drawn);
        List<BeastStrengthFamily> pool = BeastStrengthFamily.getManifesting(held);
        if (!beastPath || pool.isEmpty()) return List.of();
        if (!shown.isEmpty()) return List.copyOf(shown);
        return List.of(pool.get(random.nextInt(pool.size())));
    }

    //region Pairing [配对]
    public static @NotNull List<Projectile> request(@NotNull ServerPlayer player, int targetId) {
        if (getTarget(player, targetId) == null) return List.of();

        long now = player.level().getGameTime();
        Strike strike = STRIKES.remove(player.getUUID());
        if (strike != null && isJoined(strike.tick(), now)) return fire(player, targetId, strike.finishing());
        SHOTS.put(player.getUUID(), new Shot(targetId, now));
        return List.of();
    }

    public static @NotNull List<Projectile> onStrike(@NotNull ServerPlayer player, boolean finishing) {
        long now = player.level().getGameTime();
        Shot shot = SHOTS.remove(player.getUUID());
        if (shot != null && isJoined(shot.tick(), now)) return fire(player, shot.targetId(), finishing);
        STRIKES.put(player.getUUID(), new Strike(finishing, now));
        return List.of();
    }

    private static void clear(@NotNull Player player) {
        SHOTS.remove(player.getUUID());
        STRIKES.remove(player.getUUID());
    }
    //endregion

    //region Fire [发射]
    public static @Nullable LivingEntity getTarget(@NotNull ServerPlayer player, int targetId) {
        if (!canPunch(player) || !TimedEffects.hasBuff(player, ModEffects.STRENGTH_QI_GU)) return null;
        if (!(player.level().getEntity(targetId) instanceof LivingEntity target) || target == player) return null;
        if (!target.isAlive() || !target.canBeHitByProjectile()) return null;

        if (!isPastReach(getBoxDistance(player, target), player.entityInteractionRange())) return null;
        Vec3 toTarget = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        if (toTarget.dot(player.getLookAngle()) <= 0.0D) return null;
        return player.hasLineOfSight(target) ? target : null;
    }

    public static @NotNull List<Projectile> fire(@NotNull ServerPlayer player, int targetId, boolean finishing) {
        LivingEntity target = getTarget(player, targetId);
        if (target == null) return List.of();

        float gain = finishing ? ComboRound.strike(player, player.getRandom().nextFloat()) : 0.0F;
        boolean impact = finishing && ComboRound.rollsImpact(player);
        if (finishing) ComboRound.markRanged(player);
        QiBurst.Spec burst = QiBurst.Spec.qiWave(impact);
        PathStrengthData data = PathStrengthService.get(player);
        List<BeastStrengthFamily> families = getChargeFamilies(
                PathStrengthService.getHeldBranch(player) == StrengthPathBranch.BEAST_STRENGTH_PHANTOM,
                ComboRound.getFamilies(player), ComboRound.getAvailableFamilies(data), player.getRandom());
        if (families.isEmpty()) {
            return List.of(QiWaveEntity.shoot(player, target,
                    (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) + gain, burst));
        }

        List<BeastStrengthFamily> released = finishing ? ComboRound.getFullyReleased(player) : List.of();
        float[] damages = BeastChargeDamage.getDamages(data, families, released,
                StrengthService.FULL_RELEASE_FACTOR, StrengthService.getCapacity(player));
        List<Projectile> charges = new ArrayList<>();
        for (int i = 0; i < families.size(); i++) {
            charges.add(BeastChargeEntity.shoot(player, target, damages[i], families.get(i), burst, i,
                    families.size()));
        }
        return charges;
    }
    //endregion

    //region Event [事件]
    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) { clear(event.getEntity()); }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SHOTS.clear();
        STRIKES.clear();
    }
    //endregion
}
