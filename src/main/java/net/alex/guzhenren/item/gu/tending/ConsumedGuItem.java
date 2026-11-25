package net.alex.guzhenren.item.gu.tending;

import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.GuProperties;
import net.alex.guzhenren.item.gu.OneShotGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 一次性消耗的持续照顾型蛊[Tended Gu]：炼化与喂养规则与普通照顾型蛊一致，但在成功催动后直接消耗自身。
 * <ul>
 *   <li>与炼化即催动的一次性消耗蛊（{@link OneShotGuItem}）不同，本类先经历炼化与日常喂养；{@link #drive} 返回 1，使基
 *       类 {@code spend} 在结算后扣减物品栈。</li>
 *   <li>⚠ 若允许其成为本命蛊，首次催动便会立刻消耗并触发本命反噬惩罚，因此本命槽通过 {@link #canBeVital} 判定而非按类
 *       区分，将其与 {@link OneShotGuItem} 及常规持续照顾型蛊区分开。</li>
 *   <li>⚠ 既已注定在催动后消失，便无需为其执行催动前后的自动进食：{@link #drive} 仅调用 {@code trySpendAndApply} 扣费
 *       并生效，跳过基类的进食流程。</li>
 *   <li>催动本身不消耗饱食[Hunger]，不存在因催动产生的饱食缺口，但仍会在跨日结算时与其他持续照顾型蛊一样按日扣量并自动
 *       进食。</li>
 * </ul>
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
