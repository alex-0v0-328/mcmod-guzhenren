package net.alex.guzhenren.gameplay.blissful.storage;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.alex.guzhenren.Guzhenren;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.jetbrains.annotations.NotNull;

/**
 * 库藏页的服务端浏览：每位主人各自的搜索、排序与页码，按它们从库藏里挑出当前一页（{@value #PAGE_SIZE} 条，同客户端
 * 9×5 格）下发。
 * <ul>
 *   <li>搜索按物品名（服务端语言）或 ID 包含输入，不分大小写；另认客户端按本地物品名算好的提示表 {@code hints}（至多
 *       {@value #HINT_LIMIT} 种）：专用服务器只有英文原版名，中文搜原版物品靠它。输入至多 {@value #TEXT_LENGTH} 字，
 *       超长或提示表超量在解码时即拒；页码越界按首页或末页算。</li>
 *   <li>排序：入库顺序、数量（多的在前，同数按入库顺序）、名称（服务端语言按中文排序规则）。</li>
 *   <li>⚠ 包大小有界：一条物品编码超过 {@value #ENTRY_BYTES} 字节（写满的书、装满东西的潜影盒）就只发同种空白物品并
 *       标 {@code trimmed}，取物仍按流水号拿真物品。一页至多约 24 KB，开面板附加数据守 NeoForge 文档的 32,600 字节。
 *       </li>
 *   <li>筛选排序结果按库藏修订号缓存：库藏没变、查询没变时每秒刷新只切页，几千种条目也不每秒重排。</li>
 *   <li>状态只在内存：开面板时清搜索与页码、保留排序（{@link #reset}），登出与停服时丢掉。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class StorageBrowser {

    public static final int PAGE_SIZE = 45;
    public static final int TEXT_LENGTH = 32;
    public static final int HINT_LIMIT = 512;
    public static final int ENTRY_BYTES = 512;
    private static final Map<UUID, View> VIEWS = new HashMap<>();

    private StorageBrowser() {}

    public enum Sort {
        ORDER,
        COUNT,
        NAME;

        public static final StreamCodec<ByteBuf, Sort> STREAM_CODEC = ByteBufCodecs.idMapper(index -> values()[index],
                Sort::ordinal);

        public @NotNull Sort next() { return values()[(ordinal() + 1) % values().length]; }
    }

    private static final class View {

        private String text = "";
        private Sort sort = Sort.ORDER;
        private int page;
        private Set<Item> hints = Set.of();
        private BlissfulStorage source;
        private long revision = -1;
        private List<BlissfulStorage.Entry> matched = List.of();
    }

    //region Query [查询]
    public static void browse(@NotNull UUID viewer, @NotNull String text, @NotNull Sort sort, int page,
                              @NotNull Collection<Item> hints) {
        View view = VIEWS.computeIfAbsent(viewer, id -> new View());
        view.text = normalize(text);
        view.sort = sort;
        view.page = Math.max(0, page);
        view.hints = view.text.isEmpty() ? Set.of() : Set.copyOf(hints);
        view.revision = -1;
    }

    public static void reset(@NotNull UUID viewer) {
        View view = VIEWS.get(viewer);
        if (view != null) browse(viewer, "", view.sort, 0, List.of());
    }

    public static @NotNull String normalize(@NotNull String text) {
        String normalized = text.strip().toLowerCase(Locale.ROOT);
        return normalized.length() > TEXT_LENGTH ? normalized.substring(0, TEXT_LENGTH) : normalized;
    }
    //endregion

    //region Page [当前页]
    public static @NotNull StorageModule.Section page(@NotNull UUID viewer, @NotNull BlissfulStorage storage,
                                                      @NotNull RegistryAccess access) {
        View view = VIEWS.computeIfAbsent(viewer, id -> new View());
        List<BlissfulStorage.Entry> matched = matched(view, storage);
        int page = Math.min(view.page, pages(matched.size()) - 1);
        view.page = page;
        List<StorageModule.Stored> entries = new ArrayList<>();
        for (int i = page * PAGE_SIZE; i < Math.min(matched.size(), (page + 1) * PAGE_SIZE); i++) {
            BlissfulStorage.Entry entry = matched.get(i);
            boolean trimmed = !fits(entry.key(), access);
            ItemStack shown = trimmed ? new ItemStack(entry.key().getItem()) : entry.key().copy();
            entries.add(new StorageModule.Stored(entry.serial(), shown, entry.count(), trimmed));
        }
        return new StorageModule.Section(entries, storage.entries().size(), matched.size(), page, view.sort);
    }

    public static int pages(int matched) { return Math.max(1, (matched + PAGE_SIZE - 1) / PAGE_SIZE); }

    private static List<BlissfulStorage.Entry> matched(View view, BlissfulStorage storage) {
        if (view.source == storage && view.revision == storage.revision()) return view.matched;

        List<BlissfulStorage.Entry> matched = new ArrayList<>();
        for (BlissfulStorage.Entry entry : storage.entries()) {
            if (matches(entry.key(), view.text, view.hints)) matched.add(entry);
        }
        if (view.sort == Sort.COUNT) {
            matched.sort(Comparator.comparingLong(entry -> -entry.count()));
        } else if (view.sort == Sort.NAME) {
            Collator collator = Collator.getInstance(Locale.CHINA);
            Map<BlissfulStorage.Entry, CollationKey> keys = new HashMap<>();
            for (BlissfulStorage.Entry entry : matched) {
                keys.put(entry, collator.getCollationKey(entry.key().getHoverName().getString()));
            }
            matched.sort(Comparator.comparing(keys::get));
        }
        view.source = storage;
        view.revision = storage.revision();
        view.matched = matched;
        return matched;
    }

    private static boolean matches(ItemStack key, String text, Set<Item> hints) {
        return text.isEmpty() || hints.contains(key.getItem()) || StorageWork.idOf(key).contains(text)
                || key.getHoverName().getString().toLowerCase(Locale.ROOT).contains(text);
    }

    private static boolean fits(ItemStack key, RegistryAccess access) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), access);
        try {
            ItemStack.STREAM_CODEC.encode(buffer, key);
            return buffer.writerIndex() <= ENTRY_BYTES;
        } finally {
            buffer.release();
        }
    }
    //endregion

    //region Cleanup [清理]
    public static void forget(@NotNull UUID viewer) { VIEWS.remove(viewer); }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) { forget(event.getEntity().getUUID()); }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) { VIEWS.clear(); }
    //endregion
}
