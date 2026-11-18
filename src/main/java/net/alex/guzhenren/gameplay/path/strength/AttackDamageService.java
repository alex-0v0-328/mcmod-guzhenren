package net.alex.guzhenren.gameplay.path.strength;

import java.util.HashSet;
import java.util.Set;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.attribute.TransientModifiers;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 本模组改写玩家原版 {@code ATTACK_DAMAGE} 属性的唯一服务入口。
 * <ul>
 *   <li>总攻击力加成 = 空手可发挥力量点[Strength Points]（{@link StrengthService#getUsablePoints}，兽力[Beast Strength]
 *       与人力共用承受力）× 每点攻击加成 + 僵尸档位固定加成；力量点加成仅在主手完全为空（{@link #isFistHand}，手持 Epic
 *       Fight 拳套也视为非空手）时生效，{@link #getFistBonus} 则忽略手持物返回空手加成供 B 面板展示。</li>
 *   <li>由 {@link AttackDamageEvents} 在状态或主手变更时即时刷新，并由每秒心跳兜底。</li>
 *   <li>{@link #getFistDamage} 按原版属性修饰符运算顺序重算指定力量点数下的空手攻击力（保留其他模组的乘算加成，
 *       供蓄力蛊[Store Strength Gu]突破承受力的蓄力一拳使用）；{@link #getBareFistDamage} 额外剔除主手物品自带的攻击力
 *       修饰符（供不看手持物的终点破坏打击[Crash Strike]使用）。</li>
 *   <li>⚠ 必须通过 {@link TransientModifiers} 挂载临时属性修饰符；本模组任何药水效果均不得自带
 *       {@code addAttributeModifier} 直接改攻击力，所有限时力量增益一律转为力量点走承受力结算。</li>
 *   <li>⚠ 僵尸攻击力加成直接读取 {@link BodyData#zombieTier}（指令直接赋予的永久僵尸等级为 {@code -1} 时无加成），不读
 *       药水效果。</li>
 * </ul>
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

    public static double getFistBonus(@NotNull Player player) {
        return getFistStrengthBonus(player) + getZombieBonus(player);
    }

    private static double getFistStrengthBonus(@NotNull Player player) {
        return StrengthService.getUsablePoints(player) * PathStrengthData.ATTACK_PER_POINT;
    }

    public static double getFistDamage(@NotNull Player player, int points) {
        return getFistDamage(player, points, Set.of());
    }

    public static double getBareFistDamage(@NotNull Player player, int points) {
        Set<ResourceLocation> held = new HashSet<>();
        player.getMainHandItem().forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.value() == Attributes.ATTACK_DAMAGE.value()) held.add(modifier.id());
        });
        return getFistDamage(player, points, held);
    }

    private static double getFistDamage(Player player, int points, Set<ResourceLocation> skipped) {
        AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack == null) return 0.0D;

        double added = attack.getBaseValue() + getZombieBonus(player)
                + Math.max(0, points) * PathStrengthData.ATTACK_PER_POINT;
        for (AttributeModifier modifier : attack.getModifiers()) {
            if (isOther(modifier, AttributeModifier.Operation.ADD_VALUE, skipped)) added += modifier.amount();
        }
        double total = added;
        for (AttributeModifier modifier : attack.getModifiers()) {
            if (isOther(modifier, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, skipped)) {
                total += added * modifier.amount();
            }
        }
        for (AttributeModifier modifier : attack.getModifiers()) {
            if (isOther(modifier, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, skipped)) {
                total *= 1.0D + modifier.amount();
            }
        }
        return Attributes.ATTACK_DAMAGE.value().sanitizeValue(total);
    }

    private static boolean isOther(AttributeModifier modifier, AttributeModifier.Operation operation,
                                   Set<ResourceLocation> skipped) {
        return modifier.operation() == operation && !modifier.id().equals(MODIFIER_ID)
                && !skipped.contains(modifier.id());
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
