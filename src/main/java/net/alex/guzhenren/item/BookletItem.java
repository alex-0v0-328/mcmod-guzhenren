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
 * 书册物品：主手或副手右键打开对应的 {@link Booklet} 阅读界面。
 * <ul>
 *   <li>纯阅读物品：最大堆叠为 1，使用时既不消耗也不修改双端状态；直接继承 {@link Item}（原版 {@code BookItem} 代表可附
 *       魔的空白书，语义不符）。</li>
 *   <li>客户端 {@link #use} 委托给通过 {@link #readOnClient} 注册的回调打开屏幕，在未注册前及专用服务端上均为空操作；本
 *       类借此隔离客户端 UI 类依赖（与 {@code ModPayloads} 客户端处理器接缝同理），唯一可变静态字段 {@code reader} 仅在
 *       客户端加载阶段注入一次，服务端从不读取。</li>
 *   <li>翻页音效 {@link SoundEvents#BOOK_PAGE_TURN} 在双端均以阅读者为声源播放，保证本地立即响应且周围玩家同步听
 *       见。</li>
 * </ul>
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
