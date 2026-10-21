package net.alex.guzhenren.lifecycle;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The player lifecycle moments that move {@link PlayerData}: the copy onto a clone, and the full sync to
 * the owner after login, respawn and dimension change.
 *
 * <p>{@code onCloneCopy} runs at {@code HIGHEST} priority so every later clone handler reads the copied
 * values on the new player; {@code onCloneDone} runs at {@code LOWEST} and only then invalidates the old
 * player's capabilities, so a later handler can still read the old player.
 *
 * <p>⚠ Forge invalidates a dead player's capabilities before {@code PlayerEvent.Clone}; reading the old
 * player without {@code reviveCaps()} returns nothing and the clone keeps defaults.
 *
 * <p>⚠ The client builds a new player entity on respawn and on dimension change, and that entity starts
 * from defaults; the whole synced set must go out after both, not only on login.
 *
 * @author Alex
 * @version 1.0.0
 * @see PlayerData
 * @since 1.0.0
 */

@Mod.EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class PlayerDataEvents {

    private PlayerDataEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCloneCopy(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        PlayerData.get(event.getEntity()).copyFrom(PlayerData.get(original), event.isWasDeath());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCloneDone(PlayerEvent.Clone event) {
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PlayerData.get(player).syncAll();
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PlayerData.get(player).syncAll();
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PlayerData.get(player).syncAll();
    }
}
