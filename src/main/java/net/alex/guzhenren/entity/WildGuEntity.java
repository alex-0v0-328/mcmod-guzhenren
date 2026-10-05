package net.alex.guzhenren.entity;

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
 * A wild Gu [野生蛊虫] living in the world; catching one hands over the Gu item, unrefined.
 *
 * <p>Extends {@link net.minecraft.world.entity.PathfinderMob}. The catch is a bare right click and is
 * NEVER gated on awakening [开窍] -- 希望蛊 is the only key to it, so a gate here would make awakening
 * unreachable. Drops nothing on death ({@code shouldDropLoot} is false).
 *
 * <p>⚠ The caught item arrives wild for free, because being refined is the presence of a component and a
 * caught Gu simply has none. That is why the world needs no special case downstream.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.entity.FlyingGuEntity
 * @since 1.0.0
 */

public abstract class WildGuEntity extends PathfinderMob {

    private final Supplier<Item> caughtGu;

    protected WildGuEntity(EntityType<? extends WildGuEntity> type, Level level, Supplier<Item> caughtGu) {
        super(type, level);
        this.caughtGu = caughtGu;
    }

    public Item caughtGu() { return caughtGu.get(); }

    //region catching -- a bare right click, ungated
    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (!(player instanceof ServerPlayer server)) return InteractionResult.SUCCESS;

        server.getInventory().placeItemBackInInventory(new ItemStack(caughtGu()));
        discard();
        return InteractionResult.CONSUME;
    }
    //endregion

    @Override
    protected boolean shouldDropLoot() { return false; }
}
