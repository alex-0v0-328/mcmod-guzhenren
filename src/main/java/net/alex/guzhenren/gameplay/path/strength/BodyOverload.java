package net.alex.guzhenren.gameplay.path.strength;

import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import org.jetbrains.annotations.NotNull;

/**
 * 肉体过载与冲击反噬服务：限时兽力[Beast Strength]或蓄力一拳[Charged Punch]超出当前承受力的点数全额发挥后，按比例扣减自
 * 身生命值。
 * <ul>
 *   <li>限时兽力（花豕蛊[Flower Boar Gu]、蛮力天牛蛊[Brute Force Longhorn Beetle Gu]）不提供承受力加成，其超出剩余承受
 *       力的部分记入 {@link StrengthSnapshot#overloadPoints}；每次通过 {@link BeastPhantomEvents} 门槛的空手命中（以及
 *       命中至少一个目标的 {@link CrashStrike}）均调用 {@link #recoil(ServerPlayer)} 扣减
 *       {@code guzhenren:body_overload} 无源穿透护甲伤害（挥空不扣）。</li>
 *   <li>蓄力蛊[Store Strength Gu]的蓄力一拳共用 {@link #recoil(ServerPlayer, int, int, ResourceKey)} 算式，对超出承受力
 *       的点数施加一次 {@code guzhenren:impact_backlash} 冲击反噬。</li>
 *   <li>反噬伤害不致死（生命值保底停在 {@link #HEALTH_FLOOR}，即 2 点），且在扣血前后保存并恢复玩家的
 *       {@code invulnerableTime}，既不消耗也不产生受击无敌帧。</li>
 *   <li>⚠ 过载与反噬伤害类型的 JSON 定义必须包含 {@code minecraft:no_knockback} 标签，否则原版 1.21 会对无坐标来源的伤
 *       害施加随机水平击退。</li>
 * </ul>
 */

public final class BodyOverload {

    private BodyOverload() {}

    public static final float RECOIL_RATIO = 0.5F;
    public static final float HEALTH_FLOOR = 2.0F;

    public static float recoil(@NotNull ServerPlayer player) {
        StrengthSnapshot snapshot = StrengthService.snapshot(player);
        return recoil(player, snapshot.overloadPoints(), snapshot.capacity(), ModDamageTypes.BODY_OVERLOAD);
    }

    public static float recoil(@NotNull ServerPlayer player, int points, int capacity,
                               @NotNull ResourceKey<DamageType> type) {
        float damage = getDamage(getRecoil(points, capacity, player.getMaxHealth()), player.getHealth());
        if (damage <= 0.0F) return 0.0F;

        int hurtFrames = player.invulnerableTime;
        player.invulnerableTime = 0;
        boolean hurt = player.hurt(ModDamageTypes.source(player, type), damage);
        player.invulnerableTime = hurtFrames;
        return hurt ? damage : 0.0F;
    }

    public static float getRecoil(int overloadPoints, int capacity, float maxHealth) {
        if (overloadPoints <= 0 || capacity <= 0 || maxHealth <= 0.0F) return 0.0F;
        return (float) overloadPoints / capacity * maxHealth * RECOIL_RATIO;
    }

    public static float getDamage(float recoil, float health) {
        if (recoil <= 0.0F || health <= HEALTH_FLOOR) return 0.0F;
        return Math.min(recoil, health - HEALTH_FLOOR);
    }
}
