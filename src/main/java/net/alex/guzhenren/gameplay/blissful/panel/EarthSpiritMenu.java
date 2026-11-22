package net.alex.guzhenren.gameplay.blissful.panel;

import java.util.EnumMap;
import java.util.Map;
import net.alex.guzhenren.core.MenuSlots;
import net.alex.guzhenren.gameplay.blissful.BlissfulLandData;
import net.alex.guzhenren.gameplay.blissful.EarthSpiritEntity;
import net.alex.guzhenren.gameplay.blissful.storage.BlissfulStorage;
import net.alex.guzhenren.registry.menu.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 地灵经营面板的原版容器：只含玩家背包与快捷栏（库藏页用来存入），库藏本身不设格子，经 {@link PanelAction} 存取。
 * <ul>
 *   <li>服务端实例绑定打开它的地灵；{@link #stillValid} 要求地灵还活着、玩家与它同一维度且在
 *       {@value EarthSpiritEntity#USE_RANGE} 格内、仍是主人，失主、走远或换维度即关。服务端还记着主人正在看的页
 *       （{@link #page}），快照只算这一页。</li>
 *   <li>客户端实例从开面板的缓冲读首份 {@link PanelSnapshot}，之后由下行通知 {@link #setSnapshot} 替换；各页的
 *       {@link PanelSection} 按页缓存，切到没收到过的页时 {@link #section} 为空。</li>
 *   <li>背包格只在库藏页显示：客户端界面调 {@link #setInventoryShown}，隐藏的格子 {@code isActive} 为假，既不画也点不中；
 *       服务端实例恒为真。</li>
 *   <li>Shift 点背包格 = 整组存入库藏（胆识蛊[Guts Gu]不收，原样留下）；只在服务端写库藏。</li>
 *   <li>背包坐标同原版大箱子（{@value #INVENTORY_X}, {@value #INVENTORY_Y}）、快捷栏 y {@value #HOTBAR_Y}，与面板美术规
 *       格一致。</li>
 * </ul>
 */

public class EarthSpiritMenu extends AbstractContainerMenu {

    public static final int SLOT = 18;
    public static final int INVENTORY_X = 8;
    public static final int INVENTORY_Y = 140;
    public static final int HOTBAR_Y = 198;
    private final @Nullable EarthSpiritEntity spirit;
    private final Map<Page, PanelSection> sections = new EnumMap<>(Page.class);
    private PanelSnapshot snapshot;
    private Page page;
    private boolean inventoryShown = true;

    public EarthSpiritMenu(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(id, inventory, null, PanelSnapshot.STREAM_CODEC.decode(data));
    }

    public EarthSpiritMenu(int id, Inventory inventory, @Nullable EarthSpiritEntity spirit,
                           @NotNull PanelSnapshot snapshot) {
        super(ModMenus.EARTH_SPIRIT_MENU.get(), id);
        this.spirit = spirit;
        setSnapshot(snapshot);
        this.page = snapshot.section().page();
        MenuSlots.addPlayerInventory(slot -> this.addSlot(new Slot(slot.container, slot.getContainerSlot(), slot.x,
                slot.y) {
            @Override
            public boolean isActive() { return EarthSpiritMenu.this.inventoryShown; }
        }), inventory, INVENTORY_X, INVENTORY_Y, HOTBAR_Y, SLOT);
    }

    public void setInventoryShown(boolean shown) { this.inventoryShown = shown; }

    public @Nullable EarthSpiritEntity getSpirit() { return this.spirit; }

    public @NotNull PanelSnapshot getSnapshot() { return this.snapshot; }

    public void setSnapshot(@NotNull PanelSnapshot snapshot) {
        this.snapshot = snapshot;
        this.sections.put(snapshot.section().page(), snapshot.section());
    }

    public @Nullable PanelSection section(@NotNull Page page) { return this.sections.get(page); }

    public @NotNull Page page() { return this.page; }

    public void setPage(@NotNull Page page) { this.page = page; }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player who, int index) {
        if (!(who instanceof ServerPlayer server) || this.spirit == null) return ItemStack.EMPTY;

        Slot slot = this.slots.get(index);
        ItemStack stack = slot.getItem();
        if (!BlissfulStorage.accepts(stack)) return ItemStack.EMPTY;

        BlissfulLandData data = BlissfulLandData.get(server.server, this.spirit.getLand());
        if (data.isOwner(server.getUUID()) && data.storage().insert(stack)) slot.set(ItemStack.EMPTY);
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player who) {
        if (this.spirit == null) return true;
        if (!this.spirit.isAlive() || who.level() != this.spirit.level()
                || who.distanceToSqr(this.spirit) > EarthSpiritEntity.USE_RANGE
                * EarthSpiritEntity.USE_RANGE || !(who instanceof ServerPlayer server)) {
            return false;
        }
        return BlissfulLandData.get(server.server, this.spirit.getLand()).isOwner(server.getUUID());
    }
}
