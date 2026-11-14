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
 * 书册：主手或副手右键打开其 {@link Booklet} 阅读界面。
 * <ul>
 *   <li>只供阅读：最多堆 1，不消耗，两端不写状态；直接继承 {@link Item}，因原版 {@code BookItem} 是可附魔空白书。</li>
 *   <li>客户端 {@link #use} 交给经 {@link #readOnClient} 注册的处理者开界面，注册前及专用服务端上不做事；本类不引用客户
 *       端类，同 {@code ModPayloads} 客户端处理器的接缝；唯一可变静态字段 {@code reader} 即此接缝，客户端加载时装一次，
 *       服务端从不读。</li>
 *   <li>翻页声 {@link SoundEvents#BOOK_PAGE_TURN} 两端都以阅读者为声源：本端立即听到，服务端播给他人。</li>
 * </ul>
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
