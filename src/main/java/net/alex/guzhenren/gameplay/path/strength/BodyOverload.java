package net.alex.guzhenren.gameplay.path.strength;

import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import org.jetbrains.annotations.NotNull;

/**
 * 肉体过载：限时兽力[Beast Strength]超承受力部分全额发挥，以血偿付。
 * <ul>
 *   <li>限时兽力（花豕蛊[Flower Boar Gu]、蛮力天牛蛊[Brute Force Longhorn Beetle Gu]）不带承受力，
 *       超出部分即 {@code StrengthSnapshot.overloadPoints}。</li>
 *   <li>每次过 {@link BeastPhantomEvents} 门槛的拳击都扣，不按轮，挥空不扣；类型 {@code guzhenren:body_overload}，
 *       无视护甲，无攻击者。</li>
 *   <li>⚠ 伤害类型须带 {@code minecraft:no_knockback}：1.21 里无来源位置的伤害会被往随机方向推 0.4。</li>
 *   <li>不致死，生命停在 {@link #HEALTH_FLOOR}；不耗也不给受击无敌帧，连拳每击都扣，怪物攻击也不被吞。</li>
 *   <li>蓄力蛊[Store Strength Gu]的冲击反噬同算式：蓄力一拳超当前承受力的点数，类型 {@code guzhenren:impact_backlash}，
 *       每拳一次（{@link StoreStrength}）。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see StrengthService#getOverloadPoints
 * @since 1.0.0
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
