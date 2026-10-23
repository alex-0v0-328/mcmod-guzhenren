package net.alex.guzhenren.registry.menu;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageMenu;
import net.alex.guzhenren.gameplay.trade.SoulTradeMenu;
import net.alex.guzhenren.refinement.RefinementMenu;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

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
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Guzhenren.MOD_ID);
    public static final RegistryObject<MenuType<ApertureStorageMenu>> APERTURE_STORAGE_MENU =
            MENUS.register("aperture_storage_menu", () -> IForgeMenuType.create(
                    (id, inventory, buffer) -> new ApertureStorageMenu(id, inventory, buffer.readVarInt(), 0)));
    public static final RegistryObject<MenuType<RefinementMenu>> REFINEMENT_MENU =
            MENUS.register("refinement_menu", () -> new MenuType<>(
                    RefinementMenu::new,
                    FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<SoulTradeMenu>> SOUL_TRADE_MENU =
            MENUS.register("soul_trade_menu", () -> IForgeMenuType.create(SoulTradeMenu::new));

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
