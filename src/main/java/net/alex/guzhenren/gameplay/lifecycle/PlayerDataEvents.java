package net.alex.guzhenren.gameplay.lifecycle;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.item.gu.VitalGuLostEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

/**
 * The player lifecycle moments — login, death, clone, respawn, sleep — each forwarded to {@link
 * PlayerDataService}, and a Vital Gu's death ({@code VitalGuLostEvent}) forwarded to {@link VitalLossService}.
 * This file holds no decisions of its own; a
 * handler that starts deciding for itself is how two of them disagree about what a respawn keeps.
 * {@code keepInventory} is read off the {@link net.minecraft.server.MinecraftServer}'s game rules,
 * never {@code level()}, and passed into {@code onClone} -- the single place a death-copy and a reset
 * are settled.
 *
 * <p>⚠ {@code onWakeUp} guards on {@code wakeImmediately} / {@code updateLevel} / {@code
 * isSleepingLongEnough} — it is an edge, not a level, so watching the tick handler would double-fire.
 *
 * @author Alex
 * @version 1.0.0
 * @see PlayerDataService
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class PlayerDataEvents {

    private PlayerDataEvents() {}

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerDataService.onJoin(player);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        MinecraftServer server = event.getOriginal().getServer();
        boolean keepInventory = server != null
                && server.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        PlayerDataService.onClone(event.getOriginal(), event.getEntity(), event.isWasDeath(), keepInventory);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerDataService.onRespawn(player);
        }
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
