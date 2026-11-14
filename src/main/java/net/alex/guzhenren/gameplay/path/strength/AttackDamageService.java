package net.alex.guzhenren.gameplay.path.strength;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.attribute.TransientModifiers;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 本模组唯一改写 {@code ATTACK_DAMAGE} 的入口。
 * <ul>
 *   <li>加成 = 可发挥力量点[Strength Points]（{@link StrengthService#getUsablePoints}，兽力[Beast Strength]、
 *       人力共用承受力）× 每点攻击 + 僵尸等级加成；力量只算空手，Epic Fight 拳套也不算，故武器固有技不与力道共键。</li>
 *   <li>{@link AttackDamageEvents} 在变化、换手时即刷，心跳每秒兜底；{@code TransientModifiers.swap}
 *       无变化即空转。</li>
 *   <li>{@link #getFistDamage} 按运算方式重算任意点数下的拳头攻击，他模组乘算照常放大；供蓄力蛊[Store Strength Gu]
 *       超承受力的蓄力一拳。</li>
 *   <li>⚠ 修饰须为临时（{@link TransientModifiers}）。</li>
 *   <li>⚠ 效果不得自带 {@code addAttributeModifier}：限时攻击增益一律走承受力内的力量点，不另开入口。</li>
 *   <li>⚠ 僵尸加成读 {@code BodyData.zombieTier}，不读 MobEffect（永久僵尸无效果；指令等级 -1 无加成）。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.gameplay.attribute.MaxHealthService
 * @see StrengthService
 * @since 1.0.0
 */

public final class AttackDamageService {

    private AttackDamageService() {}

    private static final ResourceLocation MODIFIER_ID =
            Guzhenren.id("strength_attack_damage");
    public static final double ZOMBIE_ATTACK_BASE = 5.0D;

    public static boolean isFistHand(@NotNull Player player) { return player.getMainHandItem().isEmpty(); }

    public static double getBonus(@NotNull Player player) {
        return getStrengthBonus(player) + getZombieBonus(player);
    }

    public static double getStrengthBonus(@NotNull Player player) {
        return isFistHand(player) ? getFistStrengthBonus(player) : 0.0D;
    }

    /** 不论手持何物，拳头此刻的加成：B 面板攻击行。 */
    public static double getFistBonus(@NotNull Player player) {
        return getFistStrengthBonus(player) + getZombieBonus(player);
    }

    private static double getFistStrengthBonus(@NotNull Player player) {
        return StrengthService.getUsablePoints(player) * PathStrengthData.ATTACK_PER_POINT;
    }

    public static double getFistDamage(@NotNull Player player, int points) {
        AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack == null) return 0.0D;

        double added = attack.getBaseValue() + getZombieBonus(player)
                + Math.max(0, points) * PathStrengthData.ATTACK_PER_POINT;
        for (AttributeModifier modifier : attack.getModifiers()) {
            if (isOther(modifier, AttributeModifier.Operation.ADD_VALUE)) added += modifier.amount();
        }
        double total = added;
        for (AttributeModifier modifier : attack.getModifiers()) {
            if (isOther(modifier, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)) total += added * modifier.amount();
        }
        for (AttributeModifier modifier : attack.getModifiers()) {
            if (isOther(modifier, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)) total *= 1.0D + modifier.amount();
        }
        return Attributes.ATTACK_DAMAGE.value().sanitizeValue(total);
    }

    private static boolean isOther(@NotNull AttributeModifier modifier, AttributeModifier.Operation operation) {
        return modifier.operation() == operation && !modifier.id().equals(MODIFIER_ID);
    }

    private static double getZombieBonus(@NotNull Player player) {
        BodyData body = BodyService.get(player);
        if (!body.isZombieOrTempZombie() || body.zombieTier() < 0) return 0.0D;

        return ZOMBIE_ATTACK_BASE * (1 << body.zombieTier());
    }

    public static void refresh(@NotNull ServerPlayer player) {
        TransientModifiers.swap(player, Attributes.ATTACK_DAMAGE, MODIFIER_ID, getBonus(player));
    }
}
