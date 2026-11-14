package net.alex.guzhenren.item.gu.tending;

import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.GuProperties;
import net.alex.guzhenren.item.gu.OneShotGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 一次性消耗的持续照顾型蛊[Tended Gu]：照顾同其他蛊，催动后消失。
 * <ul>
 *   <li>不同于炼化即使用的一次性蛊（{@link OneShotGuItem}）；{@code drive} 返回 1，令基类 {@code spend} 减堆。</li>
 *   <li>⚠ 若作本命，首点即失并罚主人，故本命槽问 {@code canBeVital} 而非查类：「用后还在否」的第三种答案（另为
 *       {@link OneShotGuItem}、普通持续照顾型蛊）。</li>
 *   <li>⚠ 不喂将消失之物：{@code drive} 只扣费生效（{@code trySpendAndApply}），跳过基类前后进食。</li>
 *   <li>催动不扣饱食[Hunger]，无缺口可补；仍同他蛊在日切扣量前后进食。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see TendedGuItem
 * @see OneShotGuItem
 * @since 1.0.0
 */

public class ConsumedGuItem extends TendedGuItem {

    public ConsumedGuItem(GuProperties guProperties, GuBehavior... behaviors) {
        super(guProperties, behaviors);
    }

    @Override
    public boolean canBeVital() { return false; }

    @Override
    protected int drive(ServerPlayer player, ItemStack stack) {
        trySpendAndApply(player, stack);
        return 1;
    }
}
