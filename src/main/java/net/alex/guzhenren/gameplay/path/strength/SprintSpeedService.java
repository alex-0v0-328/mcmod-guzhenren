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
 * 永久兽力[Beast Strength]移动速度加成服务：将疾跑加成与全面解放兽种的移速加成合成为单个原版 {@code MOVEMENT_SPEED} 临
 * 时属性修饰符。
 * <ul>
 *   <li>加成数值定义为原版步行速度的倍数：疾跑时叠加到原版疾跑倍率 {@link #VANILLA_SPRINT_FACTOR}（{@code 1.3}）之上
 *       （如驰马 {@code +0.5} 使疾跑从 {@code 1.3} 升至 {@code 1.8}）；当攻击轮次内某兽种处于全面解放状态
 *       （{@link StrengthRound#getFullyReleased}）时，其移速加成在步行与疾跑下均生效。</li>
 *   <li>{@link #getModifier} 计算 {@code ADD_MULTIPLIED_TOTAL} 修饰系数，使乘算原版疾跑后恰好等于目标总倍率（药水移速效
 *       果仍按比例整体缩放）；服务端每个玩家 tick 刷新（不受手持物影响），客户端通过原版属性同步接收。</li>
 *   <li>⚠ 必须使用 {@link TransientModifiers} 挂载临时修饰符；全面解放移速加成必须同时作用于步行状态，因为原版在疾跑中
 *       挥拳命中目标的瞬间会打断疾跑，若仅挂在疾跑上会导致出拳瞬间突然掉速。</li>
 * </ul>
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
