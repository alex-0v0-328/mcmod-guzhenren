package net.alex.guzhenren.vault;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.core.MenuSlots;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.stack.VitalGuState;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.menu.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 单个空窍[Aperture]存储的容器菜单。
 *
 * <p>分页与负载：
 * <ul>
 *   <li>存储超一屏故分页；负载上限 {@code ApertureStorageService.MAX_LOAD}，每蛊至少计 1；本命蛊槽不随翻页，在
 *       {@code imageWidth} 外。</li>
 *   <li>Shift 点击走原版合并，逐格按 {@code GuSlot#getMaxStackSize} 限量，负载上限照样成立。</li>
 *   <li>{@code countPages} 按存储蛊数加一计页，满页后仍留一空页。</li>
 *   <li>⚠ 负载取同步的 {@code DATA_LOAD}：存储不发往客户端，槽位不按客户端附件重算。</li>
 * </ul>
 * <p>保存：
 * <ul>
 *   <li>由容器监听器触发，不覆写 {@code slotsChanged}：{@code AbstractContainerMenu} 非 {@code ContainerListener}，
 *       覆写永不被调；{@code load} 置 {@code loading}，自身 {@code setItem} 不存半载的页。</li>
 *   <li>⚠ 每改一格即存，非每次点击：槽位按 {@code getLoad()} 收蛊，只有保存刷新它；延迟则一次 Shift 点击或拖动会
 *       塞超本页、丢掉被拒的蛊。</li>
 *   <li>⚠ 心跳在养护遍历后调 {@code refresh}：每秒都可能治疗、补食、充满存储蛊，开着的菜单存回过期视图会撤销之，
 *       甚至复活刚饿死的蛊。</li>
 *   <li>⚠ 开着菜单时改写存储的命令（{@code /gzr reset}、{@code /gzr awaken}）先调 {@link #closeFor}：否则心跳刷新前，
 *       一次点击或关闭会存回过期窗口，盖掉重置或写进移位的索引。</li>
 * </ul>
 * <p>本命蛊：
 * <ul>
 *   <li>一空窍一只；在此绑定只标记物品堆，再由 {@code ApertureStorageService.setVital} 设该空窍主修流派。</li>
 *   <li>换新则旧的变回普通蛊：令牌不再匹配存储，{@link #releaseFormer} 清掉原版刚移到光标或背包那份拷贝的标记。</li>
 *   <li>⚠ {@link VitalSlot#mayPlace} 按交换后负载收蛊，鼠标、数字键皆然：{@code save} 里被拒的 {@code setVital}
 *       会重载槽位，却收不回原版已移到光标或快捷栏的拷贝，致旧本命蛊被复制、新的丢失。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see ApertureStorageService
 * @since 1.0.0
 */

public class ApertureStorageMenu extends AbstractContainerMenu {

    public static final int COLS = 9;
    public static final int ROWS = 6;
    public static final int INVENTORY_ROWS = 3;
    public static final int PAGE_SIZE = COLS * ROWS;
    public static final int VITAL_SLOT = PAGE_SIZE + Inventory.INVENTORY_SIZE;
    public static final int BUTTON_PREV = 0;
    public static final int BUTTON_NEXT = 1;
    public static final int SLOT = 18;
    public static final int STORAGE_X = 8;
    public static final int STORAGE_Y = 18;
    public static final int INVENTORY_Y = 140;
    public static final int HOTBAR_Y = 198;
    public static final int VITAL_X = 186;
    public static final int VITAL_Y = 22;
    private static final int DATA_PAGE = 0;
    private static final int DATA_PAGES = 1;
    private static final int DATA_LOAD = 2;
    private final Player player;
    private final int aperture;
    private final SimpleContainer page = new SimpleContainer(PAGE_SIZE);
    private final SimpleContainer vital = new SimpleContainer(1);
    private final ContainerData pageData = new SimpleContainerData(3);
    private boolean loading;

    public ApertureStorageMenu(int id, Inventory inventory, int aperture, int pageIndex) {
        super(ModMenus.APERTURE_STORAGE_MENU.get(), id);
        this.player = inventory.player;
        this.aperture = aperture;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                addSlot(new GuSlot(page, row * COLS + col,
                        STORAGE_X + col * SLOT, STORAGE_Y + row * SLOT));
            }
        }
        MenuSlots.addPlayerInventory(this::addSlot, inventory, STORAGE_X, INVENTORY_Y, HOTBAR_Y, SLOT);
        addSlot(new VitalSlot(VITAL_X, VITAL_Y));

        page.addListener(container -> save());
        vital.addListener(container -> save());

        addDataSlots(pageData);
        load(pageIndex);
    }

    public static void closeFor(@NotNull ServerPlayer player) {
        if (player.containerMenu instanceof ApertureStorageMenu) player.closeContainer();
    }

    public int getPageIndex() { return pageData.get(DATA_PAGE); }

    public int getPageCount() { return Math.max(1, pageData.get(DATA_PAGES)); }

    public int getLoad() { return pageData.get(DATA_LOAD); }

    private int countPages() {
        int count = ApertureStorageService.count(player, aperture);
        return count / PAGE_SIZE + 1;
    }

    //region paging [翻页]
    @Override
    public boolean clickMenuButton(@NotNull Player who, int id) {
        int current = getPageIndex();
        int next = switch (id) {
            case BUTTON_PREV -> current - 1;
            case BUTTON_NEXT -> current + 1;
            default -> current;
        };
        if (next < 0 || next >= countPages() || next == current) return false;

        save();
        load(next);
        broadcastChanges();
        return true;
    }

    public void refresh() {
        if (!(player instanceof ServerPlayer server) || matchesStore(server)) return;

        load(getPageIndex());
        broadcastChanges();
    }

    private boolean matchesStore(ServerPlayer server) {
        return ApertureStorageService.isSamePage(server, aperture, getPageIndex() * PAGE_SIZE, collectWindow())
                && ItemStack.matches(vital.getItem(0), ApertureStorageService.getVital(server, aperture))
                && getLoad() == ApertureStorageService.load(server, aperture)
                && pageData.get(DATA_PAGES) == countPages();
    }

    private List<ItemStack> collectWindow() {
        List<ItemStack> window = new ArrayList<>(PAGE_SIZE);
        for (int i = 0; i < PAGE_SIZE; i++) window.add(page.getItem(i).copy());
        return window;
    }

    private void load(int index) {
        loading = true;
        int at = Mth.clamp(index, 0, countPages() - 1);
        pageData.set(DATA_PAGE, at);
        pageData.set(DATA_PAGES, countPages());
        pageData.set(DATA_LOAD, ApertureStorageService.load(player, aperture));

        int from = at * PAGE_SIZE;
        List<ItemStack> items = ApertureStorageService.getPage(player, aperture, from, PAGE_SIZE);
        for (int i = 0; i < PAGE_SIZE; i++) {
            page.setItem(i, i < items.size() ? items.get(i) : ItemStack.EMPTY);
        }
        vital.setItem(0, ApertureStorageService.getVital(player, aperture).copy());
        loading = false;
    }

    private void save() {
        if (loading || !(player instanceof ServerPlayer server)) return;

        List<ItemStack> window = collectWindow();
        int from = getPageIndex() * PAGE_SIZE;
        if (!ApertureStorageService.isSamePage(server, aperture, from, window)) {
            if (!ApertureStorageService.setPage(server, aperture, from, window)) {
                load(getPageIndex());
                return;
            }
        }

        ItemStack bound = vital.getItem(0);
        if (!bound.isEmpty()) bindVital(bound, server, aperture);
        if (!ItemStack.matches(bound, ApertureStorageService.getVital(server, aperture))) {
            int kind = ApertureService.get(server).getKindAt(aperture);
            long former = ApertureStorageService.getVitalToken(server, kind);
            if (!ApertureStorageService.setVital(server, aperture, bound.copy())) {
                load(getPageIndex());
                return;
            }
            if (former != 0L && former != ApertureStorageService.getVitalToken(server, kind)) {
                releaseFormer(server, former);
            }
        }

        pageData.set(DATA_PAGES, countPages());
        pageData.set(DATA_LOAD, ApertureStorageService.load(server, aperture));
    }

    private static void bindVital(ItemStack stack, ServerPlayer player, int aperture) {
        if (stack.getItem() instanceof MortalGuItem && (!VitalGuState.isCurrentVital(stack, player)
                || VitalGuState.getBoundIndex(stack, player) != aperture)) {
            VitalGuState.bind(stack, player, aperture);
        }
    }

    private void releaseFormer(ServerPlayer server, long token) {
        if (VitalGuState.getToken(getCarried()) == token) VitalGuState.unbind(getCarried());
        Inventory inventory = server.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (VitalGuState.getToken(inventory.getItem(i)) == token) VitalGuState.unbind(inventory.getItem(i));
        }
    }
    //endregion

    @Override
    public void removed(@NotNull Player who) {
        save();
        super.removed(who);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player who, int index) {
        boolean toInventory = index < PAGE_SIZE || index == VITAL_SLOT;
        return MenuSlots.quickMove(slots.get(index), stack -> toInventory
                ? moveItemStackTo(stack, PAGE_SIZE, VITAL_SLOT, true)
                : moveItemStackTo(stack, 0, PAGE_SIZE, false));
    }

    @Override
    public boolean stillValid(@NotNull Player who) { return who == player && who.isAlive(); }

    private class GuSlot extends Slot {

        GuSlot(Container container, int index, int x, int y) { super(container, index, x, y); }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return ApertureStorageService.canStore(stack)
                    && (hasItem()
                    || ApertureStorageService.getMaxStackSize(player, aperture, getLoad(), getItem(), stack) > 0);
        }

        @Override
        public int getMaxStackSize(@NotNull ItemStack stack) {
            return ApertureStorageService.getMaxStackSize(player, aperture, getLoad(), getItem(), stack);
        }
    }

    private class VitalSlot extends Slot {

        VitalSlot(int x, int y) { super(vital, 0, x, y); }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            if (!canBeVital(stack, player)) return false;
            return ApertureStorageService.getMaxStackSize(player, aperture, getLoad(), getItem(), stack) > 0;
        }

        @Override
        public int getMaxStackSize() { return 1; }

        private static boolean canBeVital(ItemStack stack, Player player) {
            return stack.getItem() instanceof TendedGuItem gu && gu.isRefined(stack) && gu.canBeVital()
                    && !VitalGuState.isForeignVital(stack, player);
        }
    }
}
