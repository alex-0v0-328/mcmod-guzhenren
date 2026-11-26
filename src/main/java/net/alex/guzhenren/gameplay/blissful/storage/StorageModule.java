package net.alex.guzhenren.gameplay.blissful.storage;

import java.util.List;
import java.util.UUID;
import net.alex.guzhenren.gameplay.blissful.LandModule;
import net.alex.guzhenren.gameplay.blissful.panel.ActionContext;
import net.alex.guzhenren.gameplay.blissful.panel.EarthSpiritPanel;
import net.alex.guzhenren.gameplay.blissful.panel.Page;
import net.alex.guzhenren.gameplay.blissful.panel.PanelAction;
import net.alex.guzhenren.gameplay.blissful.panel.PanelContext;
import net.alex.guzhenren.gameplay.blissful.panel.PanelSection;
import net.alex.guzhenren.gameplay.blissful.storage.StorageBrowser.Sort;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 「库藏」页模块：只下发主人当前查询下的一页条目（搜索、排序、分页在 {@link StorageBrowser}）；取物、存入的规则在
 * {@link StorageWork}。
 * <ul>
 *   <li>{@link Browse} 一次带齐查询参数（输入、排序、页码、本地名提示表），服务端整份替换这位主人的查询。</li>
 *   <li>取物带条目流水号（{@link Stored#serial}），不带下标：快照过时也拿不错物品。</li>
 *   <li>段里的总种数、匹配数、页码与排序供客户端画右上角计数、翻页与排序按钮。</li>
 * </ul>
 */

public final class StorageModule implements LandModule<StorageModule.Section> {

    public static final StorageModule INSTANCE = new StorageModule();

    private StorageModule() {}

    public record Stored(long serial, @NotNull ItemStack key, long count, boolean trimmed) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Stored> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, Stored::serial,
                ItemStack.STREAM_CODEC, Stored::key,
                ByteBufCodecs.VAR_LONG, Stored::count,
                ByteBufCodecs.BOOL, Stored::trimmed,
                Stored::new);
    }

    public record Section(@NotNull List<Stored> entries, int total, int matched, int pageIndex, @NotNull Sort sort)
            implements PanelSection {

        public static final Section EMPTY = new Section(List.of(), 0, 0, 0, Sort.ORDER);
        public static final StreamCodec<RegistryFriendlyByteBuf, Section> STREAM_CODEC = StreamCodec.composite(
                Stored.STREAM_CODEC.apply(ByteBufCodecs.list(StorageBrowser.PAGE_SIZE)), Section::entries,
                ByteBufCodecs.VAR_INT, Section::total,
                ByteBufCodecs.VAR_INT, Section::matched,
                ByteBufCodecs.VAR_INT, Section::pageIndex,
                Sort.STREAM_CODEC, Section::sort,
                Section::new);

        public int pages() { return StorageBrowser.pages(matched); }

        @Override
        public @NotNull LandModule<?> module() { return INSTANCE; }
    }

    //region Actions [操作]
    public record Take(long serial, boolean all) implements PanelAction {

        public static final PanelAction.Type<Take> TYPE = new PanelAction.Type<>(Take.class,
                StreamCodec.composite(ByteBufCodecs.VAR_LONG, Take::serial, ByteBufCodecs.BOOL, Take::all, Take::new));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            StorageWork.take(context.player(), context.menu(), context.data().storage(), serial, all);
            return Component.empty();
        }
    }

    public record Browse(@NotNull String text, @NotNull Sort sort, int pageIndex, @NotNull List<Item> hints)
            implements PanelAction {

        public static final PanelAction.Type<Browse> TYPE = new PanelAction.Type<>(Browse.class,
                StreamCodec.composite(ByteBufCodecs.stringUtf8(StorageBrowser.TEXT_LENGTH), Browse::text,
                        Sort.STREAM_CODEC, Browse::sort, ByteBufCodecs.VAR_INT, Browse::pageIndex,
                        ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs.list(StorageBrowser.HINT_LIMIT)),
                        Browse::hints, Browse::new));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            StorageBrowser.browse(context.player().getUUID(), text, sort, pageIndex, hints);
            return Component.empty();
        }
    }

    public record DepositCarried() implements PanelAction {

        public static final PanelAction.Type<DepositCarried> TYPE = new PanelAction.Type<>(DepositCarried.class,
                StreamCodec.unit(new DepositCarried()));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            StorageWork.depositCarried(context.menu(), context.data().storage());
            return Component.empty();
        }
    }

    public record DepositMatching() implements PanelAction {

        public static final PanelAction.Type<DepositMatching> TYPE = new PanelAction.Type<>(DepositMatching.class,
                StreamCodec.unit(new DepositMatching()));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            int moved = StorageWork.depositMatching(context.player(), context.data().storage());
            return moved > 0 ? EarthSpiritPanel.notice("deposited", moved) : EarthSpiritPanel.notice("nothing_matches");
        }
    }
    //endregion

    @Override
    public @NotNull Page page() { return Page.STORAGE; }

    @Override
    public @NotNull StreamCodec<RegistryFriendlyByteBuf, Section> sectionCodec() { return Section.STREAM_CODEC; }

    @Override
    public @NotNull List<PanelAction.Type<?>> actions() {
        return List.of(Take.TYPE, DepositCarried.TYPE, DepositMatching.TYPE, Browse.TYPE);
    }

    @Override
    public void opened(@NotNull UUID viewer) { StorageBrowser.reset(viewer); }

    @Override
    public @NotNull Section section(@NotNull PanelContext context) {
        return StorageBrowser.page(context.viewer(), context.data().storage(), context.level().registryAccess());
    }
}
