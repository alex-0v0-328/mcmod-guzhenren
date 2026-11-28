package net.alex.guzhenren.gameplay.immortalaperture.managementpanel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.alex.guzhenren.core.StoneVault;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandNoticeEvent;
import net.alex.guzhenren.gameplay.immortalaperture.storage.BlissfulStorage;
import net.alex.guzhenren.gameplay.immortalaperture.storage.StorageBrowser;
import net.alex.guzhenren.gameplay.immortalaperture.storage.StorageBrowser.Sort;
import net.alex.guzhenren.gameplay.immortalaperture.storage.StorageWork;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 「库藏」页面板段：只下发主人当前查询下、列表滚到那一行起的一窗条目（搜索、排序、起始行在 {@link StorageBrowser}），
 * 以及元石栏：库藏元石数与主人背包里那只元老蛊的存量与容量（{@link StorageWork#vault}）；取物、存入、装入与倒出元老蛊的
 * 规则在 {@link StorageWork}。
 * <ul>
 *   <li>{@link Browse} 一次带齐查询参数（输入、排序、起始行、本地名提示表），服务端整份替换这位主人的查询。</li>
 *   <li>取物带条目流水号（{@link StorageBrowser.Stored#serial}），不带下标：快照过时也拿不错物品。</li>
 *   <li>段里的总种数、匹配数、起始行与排序供客户端画右上角计数、滚动条与排序按钮。</li>
 * </ul>
 */

public final class StoragePart implements PanelSectionSource<StoragePart.Section> {

    public static final StoragePart INSTANCE = new StoragePart();

    private StoragePart() {}

    /** 背包里那只元老蛊：物品、已存块数、容量。 */
    public record Vault(@NotNull ResourceLocation item, long stored, long capacity) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Vault> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Vault::item, ByteBufCodecs.VAR_LONG, Vault::stored,
                ByteBufCodecs.VAR_LONG, Vault::capacity, Vault::new);
    }

    public record Section(@NotNull List<StorageBrowser.Stored> entries, int total, int matched, int first,
                          @NotNull Sort sort, long stones, @NotNull Optional<Vault> vault) implements PanelSection {

        public static final Section EMPTY = new Section(List.of(), 0, 0, 0, Sort.ORDER, 0L, Optional.empty());
        private static final StreamCodec<RegistryFriendlyByteBuf, List<StorageBrowser.Stored>> ENTRIES =
                StorageBrowser.Stored.STREAM_CODEC.apply(ByteBufCodecs.list(StorageBrowser.PAGE_SIZE));
        private static final StreamCodec<RegistryFriendlyByteBuf, Optional<Vault>> VAULT =
                ByteBufCodecs.optional(Vault.STREAM_CODEC);
        public static final StreamCodec<RegistryFriendlyByteBuf, Section> STREAM_CODEC = StreamCodec.of(
                (buffer, section) -> {
                    ENTRIES.encode(buffer, section.entries);
                    for (int value : new int[] {section.total, section.matched, section.first}) buffer.writeVarInt(value);
                    Sort.STREAM_CODEC.encode(buffer, section.sort);
                    buffer.writeVarLong(section.stones);
                    VAULT.encode(buffer, section.vault);
                },
                buffer -> new Section(ENTRIES.decode(buffer), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readVarInt(), Sort.STREAM_CODEC.decode(buffer), buffer.readVarLong(),
                        VAULT.decode(buffer)));

        @Override
        public @NotNull PanelSectionSource<?> source() { return INSTANCE; }
    }

    //region Actions [操作]
    public record Take(long serial, boolean all) implements PanelAction {

        public static final PanelAction.Type<Take> TYPE = new PanelAction.Type<>(Take.class,
                StreamCodec.composite(ByteBufCodecs.VAR_LONG, Take::serial, ByteBufCodecs.BOOL, Take::all, Take::new));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            StorageWork.take(context.player(), context.menu(), BlissfulStorage.of(context.data()), serial, all);
            return Component.empty();
        }
    }

    public record Browse(@NotNull String text, @NotNull Sort sort, int first, @NotNull List<Item> hints)
            implements PanelAction {

        public static final PanelAction.Type<Browse> TYPE = new PanelAction.Type<>(Browse.class,
                StreamCodec.composite(ByteBufCodecs.stringUtf8(StorageBrowser.TEXT_LENGTH), Browse::text,
                        Sort.STREAM_CODEC, Browse::sort, ByteBufCodecs.VAR_INT, Browse::first,
                        ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs.list(StorageBrowser.HINT_LIMIT)),
                        Browse::hints, Browse::new));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            StorageBrowser.browse(context.player().getUUID(), text, sort, first, hints);
            return Component.empty();
        }
    }

    public record DepositCarried() implements PanelAction {

        public static final PanelAction.Type<DepositCarried> TYPE = new PanelAction.Type<>(DepositCarried.class,
                StreamCodec.unit(new DepositCarried()));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            StorageWork.depositCarried(context.menu(), BlissfulStorage.of(context.data()));
            return Component.empty();
        }
    }

    public record FillVault() implements PanelAction {

        public static final PanelAction.Type<FillVault> TYPE = new PanelAction.Type<>(FillVault.class,
                StreamCodec.unit(new FillVault()));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            if (StorageWork.vault(context.player()).isEmpty()) return LandNoticeEvent.text("no_vault");
            return LandNoticeEvent.text("vault_filled", StorageWork.fillVault(context.player(),
                    BlissfulStorage.of(context.data())));
        }
    }

    public record EmptyVault() implements PanelAction {

        public static final PanelAction.Type<EmptyVault> TYPE = new PanelAction.Type<>(EmptyVault.class,
                StreamCodec.unit(new EmptyVault()));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            if (StorageWork.vault(context.player()).isEmpty()) return LandNoticeEvent.text("no_vault");
            return LandNoticeEvent.text("vault_emptied", StorageWork.emptyVault(context.player(),
                    BlissfulStorage.of(context.data())));
        }
    }

    public record DepositMatching() implements PanelAction {

        public static final PanelAction.Type<DepositMatching> TYPE = new PanelAction.Type<>(DepositMatching.class,
                StreamCodec.unit(new DepositMatching()));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            int moved = StorageWork.depositMatching(context.player(), BlissfulStorage.of(context.data()));
            return moved > 0 ? LandNoticeEvent.text("deposited", moved) : LandNoticeEvent.text("nothing_matches");
        }
    }
    //endregion

    @Override
    public @NotNull Page page() { return Page.STORAGE; }

    @Override
    public @NotNull StreamCodec<RegistryFriendlyByteBuf, Section> sectionCodec() { return Section.STREAM_CODEC; }

    @Override
    public @NotNull List<PanelAction.Type<?>> actions() {
        return List.of(Take.TYPE, DepositCarried.TYPE, DepositMatching.TYPE, Browse.TYPE, FillVault.TYPE, EmptyVault.TYPE);
    }

    @Override
    public void opened(@NotNull UUID viewer) { StorageBrowser.reset(viewer); }

    @Override
    public @NotNull Section section(@NotNull PanelContext context) {
        StorageBrowser.Listing listing = StorageBrowser.page(context.viewer(), BlissfulStorage.of(context.data()),
                context.level().registryAccess());
        BlissfulStorage storage = BlissfulStorage.of(context.data());
        ServerPlayer viewer = context.level().getServer().getPlayerList().getPlayer(context.viewer());
        ItemStack stack = viewer == null ? ItemStack.EMPTY : StorageWork.vault(viewer);
        Optional<Vault> vault = stack.getItem() instanceof StoneVault kind
                ? Optional.of(new Vault(BuiltInRegistries.ITEM.getKey(stack.getItem()), kind.stored(stack), kind.capacity()))
                : Optional.empty();
        return new Section(listing.entries(), listing.total(), listing.matched(), listing.first(), listing.sort(),
                storage.count(StorageWork::isStone), vault);
    }
}
