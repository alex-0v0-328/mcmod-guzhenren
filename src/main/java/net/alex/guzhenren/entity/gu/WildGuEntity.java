package net.alex.guzhenren.entity.gu;

import java.util.function.Supplier;
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
 * 野生蛊：捕捉即得未炼化的蛊物品。
 * <ul>
 *   <li>⚠ 右键即可捕捉，绝不以开窍[Awakening]为门槛：希望蛊[Hope Gu]是开窍唯一钥匙，设门槛则开窍不可达。</li>
 *   <li>⚠ 捕获物天然野生：已炼化看有无组件，捕获的蛊没有，下游无需特判。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.entity.gu.FlyingGuEntity
 * @since 1.0.0
 */

public abstract class WildGuEntity extends PathfinderMob {

    private final Supplier<Item> caughtGu;

    protected WildGuEntity(EntityType<? extends WildGuEntity> type, Level level, Supplier<Item> caughtGu) {
        super(type, level);
        this.caughtGu = caughtGu;
    }

    private Item getCaughtGu() { return caughtGu.get(); }

    //region catching [捕捉]
    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (!(player instanceof ServerPlayer server)) return InteractionResult.SUCCESS;

        server.getInventory().placeItemBackInInventory(new ItemStack(getCaughtGu()));
        discard();
        return InteractionResult.CONSUME;
    }
    //endregion

    @Override
    protected boolean shouldDropLoot() { return false; }
}
