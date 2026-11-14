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
 * 石窍蛊[Stone Aperture Gu]行为：目标空窍[Aperture]升本转巅峰并石化。
 * <ul>
 *   <li>从此不再温养[Nourish]、不再冲刷窍壁；注册三档；{@link #getStoneTarget} 是单元测试钉住的纯接缝：主空窍 NORMAL
 *       即目标，主空窍死亡或已石化才转给 NORMAL 第二空窍，皆不可用为 {@code NO_TARGET}。</li>
 *   <li>{@link #getTargetOf} 映射至列表实位：独存第二空窍居 0，缺槽按 DEAD；主空窍转数不符不顺延，只因失去才顺延。</li>
 *   <li>门槛拒绝无可用目标、目标转数不符、目标已在巅峰（只换来锁定）；生效委托 {@link ApertureNourishService#petrify}；
 *       {@code StateEffects} 凭此行为按石化转数找蛊。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
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
