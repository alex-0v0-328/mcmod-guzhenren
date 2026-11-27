package net.alex.guzhenren.gameplay.immortalaperture.managementpanel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.core.MenuSlots;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.EarthSpiritEntity;
import net.alex.guzhenren.gameplay.immortalaperture.storage.BlissfulStorage;
import net.alex.guzhenren.registry.menu.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 地灵经营面板的原版容器：只含玩家背包与快捷栏（库藏页用来存入），库藏本身不设格子，经 {@link PanelAction} 存取。
 * <ul>
 *   <li>服务端实例绑定打开它的地灵；{@link #stillValid} 要求地灵还活着、玩家与它同一维度且在
 *       {@value EarthSpiritEntity#USE_RANGE} 格内、仍是主人，失主、走远或换维度即关；经地图传送过（{@link #setRoaming}）
 *       就不再量距离，⚠ 行宫区块随之卸载、地灵实体被移出世界也不算失效（只认被杀等真正移除），关掉后再开仍要回地灵旁。服务端还记着主人正在看的页（{@link #page}）与开面板时站的位置
 *       （{@link #origin}），快照只算这一页的各段。</li>
 *   <li>客户端实例从开面板的缓冲读首份 {@link PanelSnapshot}，之后由下行通知 {@link #setSnapshot} 替换；各段
 *       {@link PanelSection} 按面板段缓存，切到没收到过的页时 {@link #section} 为空。</li>
 *   <li>背包格只在库藏页显示：客户端界面调 {@link #setInventoryShown}，隐藏的格子 {@code isActive} 为假，既不画也点不
 *       中；服务端实例恒为真。⚠ 隐藏时还要把格子换成坐标在屏幕外（{@value #HIDDEN_XY}）的副本（同原版创造模式物品栏换
 *       格的做法）：InventoryProfilesNext 按格子坐标画快捷栏箭头，不看 {@code isActive}，也不认界面类上的
 *       {@code IPNIgnore}。</li>
 *   <li>Shift 点背包格 = 整组存入库藏（胆识蛊[Guts Gu]不收，原样留下）；只在服务端写库藏。</li>
 *   <li>背包坐标（{@value #INVENTORY_X}, {@value #INVENTORY_Y}）、快捷栏 y {@value #HOTBAR_Y} 相对库藏页两张卡片的左上角，
 *       落在右边「背包」卡里（{@code StoragePage}）。</li>
 * </ul>
 */

public class EarthSpiritMenu extends AbstractContainerMenu {

    public static final int SLOT = 18;
    public static final int INVENTORY_X = 188;
    public static final int INVENTORY_Y = 19;
    public static final int HOTBAR_Y = 77;
    public static final int HIDDEN_XY = -10000;
    private final @Nullable EarthSpiritEntity spirit;
    private final List<Slot> homes = new ArrayList<>();
    private final Map<PanelSectionSource<?>, PanelSection> sections = new HashMap<>();
    private PanelSnapshot snapshot;
    private Page page;
    private boolean inventoryShown = true;
    private @Nullable Vec3 origin;
    private boolean roaming;

    public EarthSpiritMenu(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(id, inventory, null, PanelSnapshot.STREAM_CODEC.decode(data));
    }

    public EarthSpiritMenu(int id, Inventory inventory, @Nullable EarthSpiritEntity spirit,
                           @NotNull PanelSnapshot snapshot) {
        super(ModMenus.EARTH_SPIRIT_MENU.get(), id);
        this.spirit = spirit;
        setSnapshot(snapshot);
        this.page = snapshot.page();
        MenuSlots.addPlayerInventory(slot -> this.homes.add(this.addSlot(new BagSlot(slot, slot.x, slot.y))),
                inventory, INVENTORY_X, INVENTORY_Y, HOTBAR_Y, SLOT);
    }

    public void setInventoryShown(boolean shown) {
        if (this.inventoryShown == shown) return;
        this.inventoryShown = shown;
        for (int i = 0; i < this.homes.size(); i++) {
            Slot home = this.homes.get(i);
            Slot slot = shown ? home : new BagSlot(home, HIDDEN_XY, HIDDEN_XY);
            slot.index = home.index;
            this.slots.set(i, slot);
        }
    }

    private final class BagSlot extends Slot {

        private BagSlot(Slot slot, int x, int y) { super(slot.container, slot.getContainerSlot(), x, y); }

        @Override
        public boolean isActive() { return EarthSpiritMenu.this.inventoryShown; }
    }

    public @Nullable EarthSpiritEntity getSpirit() { return this.spirit; }

    public @NotNull PanelSnapshot getSnapshot() { return this.snapshot; }

    public void setSnapshot(@NotNull PanelSnapshot snapshot) {
        this.snapshot = snapshot;
        for (PanelSection section : snapshot.sections()) this.sections.put(section.source(), section);
    }

    @SuppressWarnings("unchecked")
    public <S extends PanelSection> @Nullable S section(@NotNull PanelSectionSource<S> module) {
        return (S) this.sections.get(module);
    }

    public @NotNull Page page() { return this.page; }

    /** 服务端：主人打开面板时站的位置（「回到地灵身边」传回这里）。 */
    public @Nullable Vec3 origin() { return this.origin; }

    public void setOrigin(@NotNull Vec3 origin) { this.origin = origin; }

    /** 服务端：这次面板传送过，之后不再按离地灵的距离关面板。 */
    public void setRoaming() { this.roaming = true; }

    public void setPage(@NotNull Page page) { this.page = page; }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player who, int index) {
        if (!(who instanceof ServerPlayer server) || this.spirit == null) return ItemStack.EMPTY;

        Slot slot = this.slots.get(index);
        ItemStack stack = slot.getItem();
        if (!BlissfulStorage.accepts(stack)) return ItemStack.EMPTY;

        BlissfulLandData data = BlissfulLandData.get(server.server, this.spirit.getLand());
        if (data.isOwner(server.getUUID()) && BlissfulStorage.of(data).insert(stack)) slot.set(ItemStack.EMPTY);
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player who) {
        if (this.spirit == null) return true;
        if (!this.spirit.isAlive() && !(this.roaming && unloaded(this.spirit)) || who.level() != this.spirit.level()
                || !this.roaming && who.distanceToSqr(this.spirit) > EarthSpiritEntity.USE_RANGE
                * EarthSpiritEntity.USE_RANGE || !(who instanceof ServerPlayer server)) {
            return false;
        }
        return BlissfulLandData.get(server.server, this.spirit.getLand()).isOwner(server.getUUID());
    }

    private static boolean unloaded(EarthSpiritEntity spirit) {
        Entity.RemovalReason reason = spirit.getRemovalReason();
        return reason != null && !reason.shouldDestroy();
    }
}
