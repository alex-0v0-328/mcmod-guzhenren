package net.alex.guzhenren.gameplay.path.strength;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 运行期间动态提供力道增幅的流派效果 {@code payload} 接口：{@link StrengthService#getAmplifiers}
 * 遍历玩家流派效果时传入永久力量点[Strength Points]与承受力供其计算 {@link StrengthAmplifier}
 * （限时兽力[Beast Strength]增益走原版药水效果接口 {@link BeastStrengthBuff}，由
 * {@link PathStrengthService#getBeastBuffs} 单独采集）。
 */

public interface StrengthAmplifierProvider {

    @Nullable StrengthAmplifier getAmplifier(@NotNull Player player, int permanentPoints, int capacity);
}
