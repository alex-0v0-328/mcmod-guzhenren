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
 * 挂载 {@link AutoUseBehavior} 的力道凡蛊（自力更生蛊[Self Reliance Gu]、苦力蛊[Hardship Strength Gu]）的服务端自动催动
 * 调度器。
 * <ul>
 *   <li>当玩家随身背包（主背包、快捷栏、副手）或任意空窍存储中持有已炼化的自动催动蛊，且当前生命值低于上限的
 *       75%（{@link HardshipStrength#isHurt}）时，自动开启对应状态；持续窗口由 {@link #getEndTick} 计算（含青牛 +10 秒
 *       等永久兽力时长加成），期间生命值回升至 75% 以上不会提前终止窗口；窗口到期后若仍处于受伤状态，待效果冷却结束后自
 *       动重开并再次扣费。</li>
 *   <li>{@link #tryStart} 在玩家心跳中调用：每种状态仅取当前持有的最高转数蛊虫，逐个尝试调用
 *       {@link TendedGuItem#tryAutoUse}（跳过本命蛊槽位）直至有一只成功开启；开启时按该蛊转数扣除一次真元与
 *       催动饱食[Hunger]，同样受真元门槛、效果冷却与饱食不足约束（不满足时静默跳过），并写入效果冷却以隔开下次自动开
 *       启；状态运行期间不再重复扣费。</li>
 *   <li>{@link #tick} 每 tick 检查运行中的自动状态：当玩家死亡/濒死，或身上已不再持有赋予该状态的同源已炼化蛊虫时，立即
 *       移除状态且不扣费（正常到期则由流派效果自身过期处理）；将蛊虫重新放回身上后会重新触发开启并扣费。</li>
 *   <li>⚠ 自力更生与苦力状态均为持久化流派效果，重登后原样保留，登录首 tick 复查持有条件而不重新扣费；若低转状态运行途
 *       中拾入高转同种蛊，需等当前轮次结束后才由高转蛊接替开启。</li>
 *   <li>⚠ 手持右键门槛（{@link AutoUseBehavior#checkEffect}）仅放行来自本类的自动开启调用：可变静态字段
 *       {@code starting} 作为服务端单次 {@link #start} 调用栈内的线程上下文，由 {@code try/finally} 严格包裹，使
 *       {@link #isStarting} 仅在该瞬间对当前玩家返回 {@code true}。</li>
 * </ul>
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
