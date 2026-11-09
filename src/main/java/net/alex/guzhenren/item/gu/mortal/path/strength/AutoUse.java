package net.alex.guzhenren.item.gu.mortal.path.strength;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.PathEffectType;
import net.alex.guzhenren.gameplay.path.strength.HardshipStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The auto use [自动催动] of every {@link AutoUseBehavior} Gu: a refined copy held in the bag ({@code items} and
 * offhand) or stored in any aperture [空窍] starts its state on its own once the holder falls under 75% of max health
 * ({@link HardshipStrength#isHurt}), for a window the behavior names ({@link #getEndTick}: its seconds plus the held
 * beasts' duration bonus, the green bull's ten); healing meanwhile does not end it. A window that runs out on a body
 * still hurt is followed by a new start, billed again, once the Gu's effect cooldown allows.
 *
 * <p>{@link #tryStart} runs on the heartbeat. Per state, only the highest rank held counts, however many copies:
 * its copies are tried in scan order (bag, offhand, then each aperture's store; the Vital Gu [本命蛊] slot is not
 * scanned) until one starts. A start is the Gu's own {@code TendedGuItem.tryAutoUse}, so it pays the essence and the
 * use's hunger once, at that rank, behind the same essence gate, effect cooldown and silent skip of a short bar as
 * any automatic use, and stamps the effect cooldown that spaces two starts. A stored copy is used on a copy written
 * back through {@code ApertureStorageService.setSlot}. Nothing is billed while the state runs.
 *
 * <p>{@link #tick} runs every tick and stops a running state, unbilled, when the holder is dead or dying or holds no
 * refined copy of the Gu that started it (the window's end is the path effect's own expiry); then it pulses the Self
 * Reliance heal. A state put back later starts again, and pays again.
 *
 * <p>⚠ The hand gate ({@code AutoUseBehavior.checkEffect}) lets only a start through: {@link #isStarting} holds for
 * the one player whose start is under way, on the server thread, for the length of {@code tryAutoUse}.
 *
 * <p>⚠ Both states are timed path effects: they survive a relog, and the first tick after it checks them again
 * without billing. A higher rank picked up while a lower one runs takes over only at the next start.
 *
 * @author Alex
 * @version 1.0.0
 * @see AutoUseBehavior
 * @since 1.0.0
 */

public final class AutoUse {

    private AutoUse() {}

    private static final List<PathEffectType<?>> STATES =
            List.of(ModPathEffectTypes.SELF_RELIANCE, ModPathEffectTypes.HARDSHIP_STRENGTH);
    private static final int CARRIED = -1;
    private static @Nullable Player starting;

    private record Held(TendedGuItem gu, AutoUseBehavior behavior, ItemStack stack, int aperture, int index) {

        private boolean isStored() { return aperture != CARRIED; }
    }

    public static boolean isStarting(@NotNull Player player) { return starting == player; }

    public static boolean isHurt(@NotNull Player player) {
        return !player.isDeadOrDying() && HardshipStrength.isHurt(player.getHealth(), player.getMaxHealth());
    }

    public static long getEndTick(@NotNull ServerPlayer player, @NotNull AutoUseBehavior behavior) {
        return player.level().getGameTime() + (long) behavior.getDurationSeconds() * Ticks.SECOND
                + PathStrengthService.getDurationBonus(player);
    }

    //region Start [启动]
    public static void tryStart(@NotNull ServerPlayer player) {
        if (!isHurt(player)) return;

        List<Held> held = findHeld(player);
        for (PathEffectType<?> state : STATES) {
            if (PathEffectService.find(player, state) != null) continue;

            for (Held copy : getHighestRank(held, state)) {
                if (!copy.behavior().canStart(player)) break;
                if (start(player, copy)) break;
            }
        }
    }

    private static List<Held> getHighestRank(List<Held> held, PathEffectType<?> state) {
        List<Held> highest = new ArrayList<>();
        for (Held copy : held) {
            if (copy.behavior().getStateType() != state) continue;

            int order = highest.isEmpty() ? -1 : highest.getFirst().gu().getRank().compareTo(copy.gu().getRank());
            if (order < 0) highest.clear();
            if (order <= 0) highest.add(copy);
        }
        return highest;
    }

    private static boolean start(ServerPlayer player, Held held) {
        ItemStack stack = held.isStored() ? held.stack().copy() : held.stack();
        boolean started;
        starting = player;
        try {
            started = held.gu().tryAutoUse(player, stack);
        } finally {
            starting = null;
        }
        if (held.isStored() && !ItemStack.matches(held.stack(), stack)) {
            ApertureStorageService.setSlot(player, held.aperture(), held.index(), stack);
        }
        return started;
    }
    //endregion

    //region Stop [关闭]
    public static void tick(@NotNull ServerPlayer player) {
        List<PathEffect<?>> running = findRunning(player);
        if (running.isEmpty()) return;

        if (player.isDeadOrDying()) {
            for (PathEffect<?> state : running) PathEffectService.removeType(player, state.type());
            return;
        }
        List<Held> held = findHeld(player);
        for (PathEffect<?> state : running) {
            if (!holds(held, state.source())) PathEffectService.removeType(player, state.type());
        }
        SelfRelianceBehavior.pulse(player);
    }

    private static List<PathEffect<?>> findRunning(Player player) {
        List<PathEffect<?>> running = new ArrayList<>();
        for (PathEffectType<?> state : STATES) {
            PathEffect<?> effect = PathEffectService.find(player, state);
            if (effect != null) running.add(effect);
        }
        return running;
    }

    private static boolean holds(List<Held> held, ResourceLocation source) {
        for (Held copy : held) {
            if (copy.gu().getEffectSource().equals(source)) return true;
        }
        return false;
    }
    //endregion

    //region Scan [扫描]
    private static List<Held> findHeld(Player player) {
        List<Held> held = new ArrayList<>();
        Inventory inventory = player.getInventory();
        for (ItemStack stack : inventory.items) addIfAuto(held, stack, CARRIED, 0);
        for (ItemStack stack : inventory.offhand) addIfAuto(held, stack, CARRIED, 0);
        for (int aperture = 0; aperture < ApertureData.MAX_APERTURES; aperture++) {
            List<ItemStack> view = ApertureStorageService.getView(player, aperture);
            for (int index = 0; index < view.size(); index++) addIfAuto(held, view.get(index), aperture, index);
        }
        return held;
    }

    private static void addIfAuto(List<Held> held, ItemStack stack, int aperture, int index) {
        if (!(stack.getItem() instanceof TendedGuItem gu) || !gu.isRefined(stack)) return;

        AutoUseBehavior behavior = gu.getBehavior(AutoUseBehavior.class);
        if (behavior != null) held.add(new Held(gu, behavior, stack, aperture, index));
    }
    //endregion
}
