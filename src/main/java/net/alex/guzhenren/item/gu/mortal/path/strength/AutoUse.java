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
 * 所有 {@link AutoUseBehavior} 蛊的自动催动。
 * <ul>
 *   <li>背包（{@code items}、副手）或任一空窍[Aperture]存储中的已炼化蛊，在持有者低于最大生命 75%
 *       （{@link HardshipStrength#isHurt}）时自行开启状态；窗口见 {@link #getEndTick}（青牛 +10 秒），回血不结束窗口；
 *       到期仍受伤，等效果冷却允许后重开、再计费。</li>
 *   <li>{@link #tryStart} 走心跳：每种状态只算持有的最高转，逐份试到一份开启（不扫本命蛊格）；开启即蛊自己的
 *       {@code TendedGuItem.tryAutoUse}：按该转付一次真元[Essence]与催动饱食[Hunger]，同受真元门槛、效果冷却和饱食不足
 *       静默跳过，并打效果冷却隔开两次开启；运行中不计费。</li>
 *   <li>{@link #tick} 每 tick 在持有者死亡濒死或不再持有开启它的已炼化蛊时停状态，不计费（到期靠流派效果自身过期）；
 *       蛊放回后重开、再付。</li>
 *   <li>⚠ 两种状态都是限时流派效果：重登后保留，首个 tick 重查不计费；低转运行中拾起高转，下次开启才接替。</li>
 *   <li>⚠ 手持门槛（{@code AutoUseBehavior.checkEffect}）只放行开启：{@link #isStarting} 仅在服务端线程、
 *       {@code tryAutoUse} 期间对正在开启的玩家成立。</li>
 *   <li>唯一可变静态字段 {@code starting}：调用级服务端线程上下文，try/finally 包住一次自动催动，不越调用。</li>
 * </ul>
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
