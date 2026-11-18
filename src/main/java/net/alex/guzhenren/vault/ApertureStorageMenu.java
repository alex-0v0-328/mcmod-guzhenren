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
 * 单个空窍内部蛊虫存储的容器菜单，负责分页视图、负载门禁与本命蛊槽位绑定（业务存储由 {@link ApertureStorageService} 托
 * 管）。
 * <ul>
 *   <li>分页与负载：存储支持多页切换并在末页后保留一个空页（{@link #countPages}），本命蛊槽固定在 {@code imageWidth} 外
 *       侧不随翻页变化；Shift 快捷移动沿用原版合并逻辑并逐格通过 {@link GuSlot#getMaxStackSize} 校验总量上限
 *       {@code ApertureStorageService.MAX_LOAD}（每只蛊至少计 1 负载）。</li>
 *   <li>⚠ 负载校验依赖服务端同步的 {@code DATA_LOAD} 数据槽：客户端不持有完整空窍存储副本，槽位上限严禁在客户端依据附件
 *       自行推算。</li>
 *   <li>实时保存机制：通过容器监听器即时写回而不覆写 {@code slotsChanged}（因 {@link AbstractContainerMenu} 未实现
 *       {@code ContainerListener}，覆写不会被调用），加载页期间通过 {@code loading} 标志屏蔽自身 {@code setItem} 引发的
 *       误存。</li>
 *   <li>⚠ 必须按每次槽位改动即时保存而非按点击保存：Shift 点击或拖动会连续写入多个槽位，若不即时刷新 {@link #getLoad()}
 *       会导致单次操作塞超总负载并丢弃被拒物品。</li>
 *   <li>⚠ 心跳在完成养护遍历后必须调用 {@link #refresh}：养护每秒可能治疗、补食或移入新蛊，若不及时刷新视图，开启的菜单
 *       后续写回过期快照会撤销养护成果甚至复活已饿死的蛊。</li>
 *   <li>⚠ 指令在直接修改存储前必须先调用 {@link #closeFor}：否则在下次心跳刷新前，玩家点击或关闭菜单会把过期窗口写回并
 *       覆盖 {@code /gzr reset} 或 {@code /gzr awaken} 的结果。</li>
 *   <li>本命蛊绑定与替换：每个空窍仅允许绑定一只本命蛊，放入槽位即打上堆叠令牌并由
 *       {@link ApertureStorageService#setVital} 同步空窍主修流派；更换新本命蛊时，{@link #releaseFormer} 会同步清除已被
 *       原版移至光标或背包的旧本命蛊拷贝标记。</li>
 *   <li>⚠ {@link VitalSlot#mayPlace} 必须按交换后的净负载放行（无论鼠标点击还是数字键切换）：若在 {@code save()} 中才被
 *       {@code setVital} 拒绝并重载槽位，将无法收回原版已搬到光标或快捷栏的拷贝，进而造成旧本命蛊被复制而新蛊丢
 *       失。</li>
 * </ul>
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
