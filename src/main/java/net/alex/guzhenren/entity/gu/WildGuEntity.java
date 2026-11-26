package net.alex.guzhenren.entity.gu;

import java.util.function.Supplier;
import net.alex.guzhenren.core.Catchable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * 野生蛊实体抽象基类：玩家右键交互即可将其捕获为未炼化的蛊虫物品（{@link #caughtStack}，福地「蛊虫」页捕获入库也用它）。
 * <ul>
 *   <li>⚠ 右键捕捉严禁设置开窍[Awakening]门槛：野生希望蛊[Hope Gu]本身就是玩家开窍的唯一钥匙，若限制开窍后才能捕捉会导
 *       致开窍流程死锁。</li>
 *   <li>⚠ 捕获生成的物品栈天然不带炼化组件（野生状态），下游逻辑直接通过组件有无判定是否已炼化，无需额外标记。</li>
 * </ul>
 */

public abstract class WildGuEntity extends PathfinderMob implements Catchable {

    private final Supplier<Item> caughtGu;

    protected WildGuEntity(EntityType<? extends WildGuEntity> type, Level level, Supplier<Item> caughtGu) {
        super(type, level);
        this.caughtGu = caughtGu;
    }

    @Override
    public @NotNull ItemStack caughtStack() { return new ItemStack(caughtGu.get()); }

    //region catching [捕捉]
    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (!(player instanceof ServerPlayer server)) return InteractionResult.SUCCESS;

        server.getInventory().placeItemBackInInventory(caughtStack());
        discard();
        return InteractionResult.CONSUME;
    }
    //endregion

    @Override
    protected boolean shouldDropLoot() { return false; }
}
