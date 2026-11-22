package net.alex.guzhenren.gameplay.blissful.storage;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.gameplay.blissful.LandModule;
import net.alex.guzhenren.gameplay.blissful.panel.ActionContext;
import net.alex.guzhenren.gameplay.blissful.panel.EarthSpiritPanel;
import net.alex.guzhenren.gameplay.blissful.panel.Page;
import net.alex.guzhenren.gameplay.blissful.panel.PanelAction;
import net.alex.guzhenren.gameplay.blissful.panel.PanelContext;
import net.alex.guzhenren.gameplay.blissful.panel.PanelSection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 「库藏」页模块：下发全部库藏条目（顺序即入库顺序，下标是取物参数）；取物、存入的规则在 {@link StorageWork}。
 * <ul>
 *   <li>搜索与排序只在客户端做，取物仍带条目在本份快照里的原下标与物品 ID。</li>
 * </ul>
 */

public final class StorageModule implements LandModule<StorageModule.Section> {

    public static final StorageModule INSTANCE = new StorageModule();
    public static final int ITEM_ID_LENGTH = 256;

    private StorageModule() {}

    public record Stored(@NotNull ItemStack key, long count) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Stored> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Stored::key,
                ByteBufCodecs.VAR_LONG, Stored::count,
                Stored::new);
    }

    public record Section(@NotNull List<Stored> entries) implements PanelSection {

        public static final Section EMPTY = new Section(List.of());
        public static final StreamCodec<RegistryFriendlyByteBuf, Section> STREAM_CODEC =
                Stored.STREAM_CODEC.apply(ByteBufCodecs.list()).map(Section::new, Section::entries);

        @Override
        public @NotNull Page page() { return Page.STORAGE; }
    }

    //region Actions [操作]
    public record Take(int index, @NotNull String itemId, boolean all) implements PanelAction {

        public static final PanelAction.Type<Take> TYPE = new PanelAction.Type<>(Take.class,
                StreamCodec.composite(ByteBufCodecs.VAR_INT, Take::index, ByteBufCodecs.stringUtf8(ITEM_ID_LENGTH),
                        Take::itemId, ByteBufCodecs.BOOL, Take::all, Take::new));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            StorageWork.take(context.player(), context.menu(), context.data().storage(), index, itemId, all);
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
        return List.of(Take.TYPE, DepositCarried.TYPE, DepositMatching.TYPE);
    }

    @Override
    public @NotNull Section section(@NotNull PanelContext context) {
        List<Stored> entries = new ArrayList<>();
        for (BlissfulStorage.Entry entry : context.data().storage().entries()) {
            entries.add(new Stored(entry.key().copy(), entry.count()));
        }
        return new Section(entries);
    }
}
