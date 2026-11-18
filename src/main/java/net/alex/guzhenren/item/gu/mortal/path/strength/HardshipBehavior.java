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
 * 苦力蛊[Hardship Strength Gu]行为：实现 {@link AutoUseBehavior}，由 {@link AutoUse} 在玩家受伤时自动触发。
 * <ul>
 *   <li>当玩家生命值低于上限 75% 时尝试开启：要求玩家已持有永久人力钧力流派或兽力虚影流派，且当前常态肉身承受力在最轻伤
 *       势档位下至少能获得 1 点加成（避免加成为 0 时白白扣费）。</li>
 *   <li>开启后持续 {@link #DURATION_SECONDS} 秒（另加永久兽力时长加成），窗口自身不随血量回升而提前关闭，但生命值回到
 *       75% 及以上时暂停提供加成（各伤势档位的具体加成见 {@link HardshipStrength}）。</li>
 * </ul>
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
