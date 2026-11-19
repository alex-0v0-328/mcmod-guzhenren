package net.alex.guzhenren.item.gu.mortal.path.strength;

import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.PathEffectType;
import net.alex.guzhenren.gameplay.path.strength.HardshipStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 苦力蛊[Hardship Strength Gu]行为：实现 {@link AutoUseBehavior}，由 {@link AutoUse} 在玩家受伤时自动触发。
 * <ul>
 *   <li>当玩家生命值低于上限 75% 时尝试开启：要求玩家已入人力钧力流派或兽力虚影流派（含生效中的限时兽力）。</li>
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
        return PathStrengthService.getHeldBranch(player) != StrengthPathBranch.NORMAL;
    }

    @Override
    public int getDurationSeconds() { return DURATION_SECONDS; }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        PathEffectService.apply(player, gu.createEffect(ModPathEffectTypes.HARDSHIP_STRENGTH,
                AutoUse.getEndTick(player, this), HardshipStrength.State.INSTANCE));
    }
}
