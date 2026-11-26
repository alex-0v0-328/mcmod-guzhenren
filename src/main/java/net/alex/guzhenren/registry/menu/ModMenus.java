package net.alex.guzhenren.registry.menu;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.EarthSpiritMenu;
import net.alex.guzhenren.gameplay.trade.SoulTradeMenu;
import net.alex.guzhenren.refinement.RefinementMenu;
import net.alex.guzhenren.vault.ApertureStorageMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 本模组全部容器菜单类型（{@link MenuType}）注册表：包含仅由客户端 B 面板请求开启的分页空窍存储
 * （{@link ApertureStorageMenu}）与炼蛊界面（{@link RefinementMenu}），以及右键魂商实体时读取额外打开数据的交易界面
 * （{@link SoulTradeMenu}）。
 */

public final class ModMenus {

    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Guzhenren.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<ApertureStorageMenu>> APERTURE_STORAGE_MENU =
            MENUS.register("aperture_storage_menu", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new ApertureStorageMenu(id, inventory, buffer.readVarInt(), 0)));
    public static final DeferredHolder<MenuType<?>, MenuType<RefinementMenu>> REFINEMENT_MENU =
            MENUS.register("refinement_menu", () -> new MenuType<>(
                    RefinementMenu::new,
                    FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<SoulTradeMenu>> SOUL_TRADE_MENU =
            MENUS.register("soul_trade_menu", () -> IMenuTypeExtension.create(SoulTradeMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<EarthSpiritMenu>> EARTH_SPIRIT_MENU =
            MENUS.register("earth_spirit_menu", () -> IMenuTypeExtension.create(EarthSpiritMenu::new));

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
