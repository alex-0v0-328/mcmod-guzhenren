package net.alex.guzhenren.lifecycle;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.item.gu.VitalGuLostEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The player lifecycle moments -- login, logout, death, clone, respawn, dimension change, sleep -- each forwarded
 * to {@link PlayerDataService}, a Vital Gu's death ({@code VitalGuLostEvent}) forwarded to {@link
 * VitalLossService}, and the full sync of {@link PlayerData} to the owner after login, respawn and dimension
 * change. Apart from the copy onto a clone and the sync, this file holds no decisions of its own; a
 * handler that starts deciding for itself is how two of them disagree about what a respawn keeps.
 * {@code keepInventory} is read off the {@link net.minecraft.server.MinecraftServer}'s game rules,
 * never {@code level()}, and passed into {@code onClone} -- the single place a death's reset is settled.
 *
 * <p>The clone runs in three steps. {@code onCloneCopy} runs at {@code HIGHEST} priority so every later clone
 * handler reads the copied values on the new player; {@code onClone} runs at {@code LOW}, after the copy, so a
 * reset is never copied over; {@code onCloneDone} runs at {@code LOWEST} and only then invalidates the old
 * player's capabilities, so a later handler can still read the old player.
 *
 * <p>⚠ Forge invalidates a dead player's capabilities before {@code PlayerEvent.Clone}; reading the old
 * player without {@code reviveCaps()} returns nothing and the clone keeps defaults.
 *
 * <p>⚠ The client builds a new player entity on respawn and on dimension change, and that entity starts
 * from defaults; the whole synced set must go out after both, not only on login.
 *
 * <p>⚠ {@code onWakeUp} guards on {@code wakeImmediately} / {@code updateLevel} / {@code
 * isSleepingLongEnough} -- it is an edge, not a level, so watching the tick handler would double-fire.
 *
 * @author Alex
 * @version 1.0.0
 * @see PlayerData
 * @see PlayerDataService
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

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onClone(PlayerEvent.Clone event) {
        MinecraftServer server = event.getOriginal().getServer();
        boolean keepInventory = server != null
                && server.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        PlayerDataService.onClone(event.getOriginal(), event.getEntity(), event.isWasDeath(), keepInventory);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCloneDone(PlayerEvent.Clone event) {
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerDataService.onJoin(player);
            PlayerData.get(player).syncAll();
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PlayerDataService.onLogout(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerDataService.onRespawn(player);
            PlayerData.get(player).syncAll();
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PlayerData.get(player).syncAll();
    }

    @SubscribeEvent
    public static void onWakeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.wakeImmediately() || event.updateLevel()) return;
        if (!player.isSleepingLongEnough()) return;

        PlayerDataService.onSleepComplete(player);
    }

    @SubscribeEvent
    public static void onVitalGuLost(VitalGuLostEvent event) { VitalLossService.onVitalGuLost(event); }
}
