package net.alex.guzhenren.registry.menu;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageMenu;
import net.alex.guzhenren.refinement.RefinementMenu;
import net.alex.guzhenren.gameplay.trade.SoulTradeMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The container menus this mod registers.
 *
 * <p>DeferredRegister holder: owns {@link ApertureStorageMenu} (the Gu vault, paged) and
 * {@link RefinementMenu} (the 炼蛊 furnace), both opened client-intent-only from the B panel, and
 * {@link SoulTradeMenu}, opened by right-clicking a soul trader; its type reads the offers from the menu-open
 * payload.
 *
 * @author Alex
 * @version 1.0.0
 * @see ApertureStorageMenu
 * @see RefinementMenu
 * @see SoulTradeMenu
 * @since 1.0.0
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

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
