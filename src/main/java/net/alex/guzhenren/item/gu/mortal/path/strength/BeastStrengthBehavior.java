package net.alex.guzhenren.item.gu.mortal.path.strength;

import net.alex.guzhenren.display.ModDisplayText;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 永久兽力授予行为：灌注[Channel]完成后为玩家永久赋予注册指定的 {@link BeastStrength}。
 * <ul>
 *   <li>{@link #checkEffect} 依次拦截三类情形：已持有人力钧力流派（{@link MarkInterference}）；同兽种已持有相同或更高档
 *       位的永久兽力（{@link PathStrengthData#getCovering}，拒绝提示中附上该兽种当前读数）；玩家常态肉身承受力不足以容
 *       纳本兽力与其余保留兽力之和（{@link StrengthService#canAdmit}，不含限时增益与全力以赴蛊[All-Out Effort Gu]的临时
 *       抬升）。</li>
 *   <li>⚠ 各兽力的兽种归属与力量点数全部声明在 {@link BeastStrength} 枚举中，新增兽力蛊时无需修改本
 *       类；{@link #applyEffect} 直接委托 {@link PathStrengthService#grant} 写入并自动替换同兽种低档兽力。</li>
 * </ul>
 */

public record BeastStrengthBehavior(BeastStrength beast) implements GuBehavior {

    private static final String FAILED_STRENGTH_HELD = "guzhenren.item.failed.beast_strength_held";
    private static final String FAILED_CAPACITY = "guzhenren.item.failed.beast_strength_capacity";

    @Override
    public @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        Refusal interference = MarkInterference.check(player, StrengthPathBranch.BEAST_STRENGTH_PHANTOM);
        if (interference != null) return interference;
        PathStrengthData held = PathStrengthService.get(player);
        BeastStrength covering = held.getCovering(beast);
        if (covering != null) {
            BeastStrengthFamily family = covering.getFamily();
            return new Refusal(FAILED_STRENGTH_HELD, ModDisplayText.formatBeastStrength(family,
                    held.getBeastReadings().getOrDefault(family, 1), held.getInnate(family)));
        }

        return StrengthService.canAdmit(player, beast) ? null : new Refusal(FAILED_CAPACITY);
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        PathStrengthService.grant(player, beast);
    }
}
