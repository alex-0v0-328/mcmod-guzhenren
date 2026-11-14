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
 * 兽力[Beast Strength]行为：永久授予一种兽力，种类来自注册。
 * <ul>
 *   <li>门槛依次拒：持人力钧力流（{@link MarkInterference}）；本兽种已有此兽力或要替换的同等或更强者
 *       （{@link PathStrengthData#getCovering}，提示写该兽种读数）；常态承受力容不下它与留下的兽力
 *       （{@link StrengthService#canAdmit}，增幅与全力以赴抬升不算）。</li>
 *   <li>⚠ 若觉得要改这里，就是数值放错了地方：兽种与价值在枚举上声明，新增兽力不改本行为；效果交
 *       {@link PathStrengthService#grant}，由它换出被替换者。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.tending.TendedGuItem
 * @since 1.0.0
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
