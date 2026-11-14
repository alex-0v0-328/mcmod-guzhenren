package net.alex.guzhenren.item.gu.mortal.path.strength;

import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.PathEffectType;
import net.alex.guzhenren.gameplay.path.strength.HardshipStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 苦力蛊[Hardship Strength Gu]的行为：自动状态（{@link AutoUseBehavior}）。
 * <ul>
 *   <li>低于最大生命 75% 开启，需持永久人力钧力流或兽力虚影流，且常态承受力在最轻档至少多 1 点（再少就白付费）。</li>
 *   <li>窗口 {@link #DURATION_SECONDS} 秒，不随血量变化，75% 及以上不借力；各档借多少见 {@link HardshipStrength}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see AutoUse
 * @see HardshipStrength
 * @since 1.0.0
 */

public record HardshipBehavior() implements AutoUseBehavior {

    public static final int DURATION_SECONDS = 30;

    @Override
    public @NotNull PathEffectType<?> getStateType() { return ModPathEffectTypes.HARDSHIP_STRENGTH; }

    @Override
    public boolean canStart(@NotNull ServerPlayer player) {
        PathStrengthData data = PathStrengthService.get(player);
        boolean branch = data.hasPathBranch(StrengthPathBranch.HUMAN_JUN_STRENGTH)
                || data.hasPathBranch(StrengthPathBranch.BEAST_STRENGTH_PHANTOM);
        return branch && HardshipStrength.getCapacityBonus(StrengthService.getNormalCapacity(player), 1) > 0;
    }

    @Override
    public int getDurationSeconds() { return DURATION_SECONDS; }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        PathEffectService.apply(player, gu.createEffect(ModPathEffectTypes.HARDSHIP_STRENGTH,
                AutoUse.getEndTick(player, this), HardshipStrength.State.INSTANCE));
    }
}
