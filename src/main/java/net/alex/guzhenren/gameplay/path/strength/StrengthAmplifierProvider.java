package net.alex.guzhenren.gameplay.path.strength;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 运行期间增幅肉体力量的流派效果 payload。{@code StrengthService.getAmplifiers} 询问每个运行中的实现者，
 * 传入永久力量点[Strength Points]与常态承受力供其定量；限时兽力[Beast Strength]增益不是提供者，
 * 其值声明于原版效果（{@link BeastStrengthBuff}），由 {@link PathStrengthService#getBeastBuffs} 读取。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public interface StrengthAmplifierProvider {

    @Nullable StrengthAmplifier getAmplifier(@NotNull Player player, int permanentPoints, int normalCapacity);
}
