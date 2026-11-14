package net.alex.guzhenren.gameplay.path.strength;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.attribute.TransientModifiers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;

/**
 * 持有兽力[Beast Strength]的移速加成，合为一个临时移速修饰。
 * <ul>
 *   <li>加成是步行速度的倍数：疾跑时加到原版疾跑倍率（驰马 0.5，1.3 → 1.8）；本轮全面解放其兽种时步行也加（解放的马步行
 *       1.5、疾跑 2.3）。</li>
 *   <li>{@link #getModifier} 乘总值，原版疾跑与本加成恰合成该倍率，药水仍整体缩放；服务端每 tick
 *       随攻击刷新（{@code PlayerTickEvents}），客户端经属性同步；不读手持。</li>
 *   <li>⚠ 修饰须为临时（{@link TransientModifiers}）。</li>
 *   <li>⚠ 全面解放加成也加到步行：原版疾跑中命中会打断疾跑，只挂疾跑会在出拳那一刻失效。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.gameplay.path.strength.BeastStrength#getSprintBonus
 * @since 1.0.0
 */

public final class SprintSpeedService {

    private SprintSpeedService() {}

    public static final double VANILLA_SPRINT_FACTOR = 1.3D;
    private static final ResourceLocation MODIFIER_ID = Guzhenren.id("beast_sprint_speed");

    public static void refresh(@NotNull ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;

        PathStrengthData data = PathStrengthService.get(player);
        double release = data.getBeastSprintBonus(StrengthRound.getFullyReleased(player));
        TransientModifiers.swap(speed, MODIFIER_ID,
                getModifier(player.isSprinting(), data.getBeastSprintBonus(), release),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    public static double getModifier(boolean sprinting, double sprintBonus, double releaseBonus) {
        if (!sprinting) return Math.max(0.0D, releaseBonus);
        double bonus = Math.max(0.0D, sprintBonus) + Math.max(0.0D, releaseBonus);
        if (bonus <= 0.0D) return 0.0D;
        return (VANILLA_SPRINT_FACTOR + bonus) / VANILLA_SPRINT_FACTOR - 1.0D;
    }
}
