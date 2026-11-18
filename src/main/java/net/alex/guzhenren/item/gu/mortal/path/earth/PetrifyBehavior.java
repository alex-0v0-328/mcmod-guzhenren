package net.alex.guzhenren.item.gu.mortal.path.earth;

import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.ApertureStatus;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 石窍蛊[Stone Aperture Gu]行为：将目标空窍直接推升至本转巅峰并永久石化。
 * <ul>
 *   <li>石化后的空窍不再需要温养[Nourish]、窍壁也不再受真元冲刷；共注册一至三转三档；{@link #getStoneTarget} 为供单元测
 *       试直接验证的纯函数：主空窍处于 {@link ApertureStatus#NORMAL} 时固定选主空窍，仅当主空窍已死亡或已石化时才顺延选
 *       择处于 {@link ApertureStatus#NORMAL} 的第二空窍，两者均不可用时返回 {@link #NO_TARGET}。</li>
 *   <li>{@link #getTargetOf} 将逻辑目标映射为空窍列表的实际下标（仅存第二空窍时位于下标 0，缺失槽位按死亡处理）；若主空
 *       窍仍正常但转数与本蛊不符，直接拒绝而不会顺延到第二空窍。</li>
 *   <li>{@link #checkEffect} 拒绝无可用空窍、目标空窍转数不符或已处于巅峰（避免白白换来石化锁定）；{@link #applyEffect}
 *       委托 {@link ApertureNourishService#petrify} 执行；{@code StateEffects} 通过检索本行为按石化转数反查对应的石窍
 *       蛊。</li>
 * </ul>
 */

public record PetrifyBehavior() implements GuBehavior {

    public static final int NO_TARGET = -1;

    public static int getStoneTarget(@NotNull ApertureStatus primary, @NotNull ApertureStatus second) {
        if (primary == ApertureStatus.NORMAL) return ApertureData.PRIMARY;
        if (second == ApertureStatus.NORMAL) return ApertureData.SECOND;
        return NO_TARGET;
    }

    private static int getTargetOf(Player player) {
        ApertureData data = ApertureService.get(player);
        int primary = data.getFirstIndex();
        int second = data.getSecondIndex();
        int target = getStoneTarget(
                primary < 0 ? ApertureStatus.DEAD : ApertureService.getStatus(player, primary),
                second < 0 ? ApertureStatus.DEAD : ApertureService.getStatus(player, second));
        if (target == NO_TARGET) return NO_TARGET;
        return target == ApertureData.SECOND ? Math.max(second, 0) : Math.max(primary, 0);
    }

    @Override
    public @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        int target = getTargetOf(player);
        if (target == NO_TARGET) return new Refusal(Refusal.CANNOT_USE);
        return gu.checkStageUpGate(ApertureService.getAperture(player, target));
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        int target = getTargetOf(player);
        if (target != NO_TARGET) ApertureNourishService.petrify(player, target);
    }
}
