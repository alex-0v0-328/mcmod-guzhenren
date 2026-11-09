package net.alex.guzhenren.item;

import java.util.function.Consumer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * A booklet [书册]: a right click in either hand opens its {@link Booklet} on a reading screen and turns a page
 * ({@link SoundEvents#BOOK_PAGE_TURN}). Reading is the whole of it: the book stacks to 1, is never used up, and
 * writes no state on either side.
 *
 * <p>Extends {@link Item} directly, not {@code BookItem}: vanilla's {@code BookItem} is the enchantable blank book.
 *
 * <p>The client's {@link #use} hands the item to whatever the client registered through {@link #readOnClient},
 * which opens the screen; until then, and always on a dedicated server, it does nothing. This class names no client
 * class, the same seam as {@code ModPayloads}'s client handlers. The page turn plays on both sides with the reader
 * as the source, so the reader hears it at once from their own client and the server plays it to everyone else.
 *
 * <p>The one mutable static, {@code reader}, is a client seam: the client installs its reader screen once at load, and
 * the server never reads it.
 *
 * @author Alex
 * @version 1.0.0
 * @see Booklet
 * @since 1.0.0
 */

public final class BookletItem extends Item {

    private static Consumer<BookletItem> reader = item -> {};
    private final Booklet booklet;

    public BookletItem(Properties properties, Booklet booklet) {
        super(properties.stacksTo(1));
        this.booklet = booklet;
    }

    public static void readOnClient(Consumer<BookletItem> reader) { BookletItem.reader = reader; }

    public Booklet getBooklet() { return booklet; }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        if (level.isClientSide()) reader.accept(this);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
