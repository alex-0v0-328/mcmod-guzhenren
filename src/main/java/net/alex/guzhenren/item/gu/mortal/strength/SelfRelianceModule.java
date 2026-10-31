package net.alex.guzhenren.item.gu.mortal.strength;

import java.util.List;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.timed.SelfRelianceHeal;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuModule;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.alex.guzhenren.item.gu.mortal.TimedEffects;
import net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem.Amplification;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The Self Reliance module [自力更生]: a use starts a heal that runs until the holder is back to
 * {@link #HEAL_CEILING} of max health. The Self Reliance Gu carries it with an {@link AutoUseModule}.
 *
 * <p>Every {@link #PULSE_TICKS} the heal mends {@code pulseHeal} of max health, times the Amplify Effect Gu
 * [功倍蛊] factor billed at the start, never past the ceiling. Each whole {@code healedPerHunger} of max health
 * actually healed drains one hunger point from the first refined copy in the bag ({@code items} or offhand); a
 * share short of a point is dropped when the heal ends. Damage taken meanwhile is healed too.
 *
 * <p>The heal ends at the ceiling, when no refined copy is carried, or when that copy's bar is down to
 * {@link #HUNGER_FLOOR} after a meal from the bag ({@code TendedGuItem.drainAboveFloor}), so it never starves the
 * Gu. The gate refuses at the ceiling and while a heal of the same or a higher grade runs; a higher grade takes a
 * lower one's heal and strands.
 *
 * <p>⚠ The heal is a permanent path effect that {@link #pulse} removes, not a vanilla timer: its icon has no
 * countdown, and {@code /effect clear} costs only the icon until the next heartbeat.
 *
 * @author Alex
 * @version 1.0.0
 * @see AutoUseModule
 * @see SelfRelianceHeal
 * @since 1.0.0
 */

public record SelfRelianceModule(int grade, float pulseHeal, float healedPerHunger) implements GuModule {

    public static final int PULSE_TICKS = 5;
    public static final float HEAL_CEILING = 0.9F;
    public static final int HUNGER_FLOOR = 2;
    // Float shares of max health: three pulses of 10% must make the 30% point, not 29.99998%.
    private static final float EPSILON = 1.0E-4F;
    private static final String FAILED_HEALTHY = "guzhenren.item.failed.self_reliance_healthy";
    private static final String FAILED_RUNNING = "guzhenren.item.failed.self_reliance_running";

    @Override
    public boolean canBeAmplified() { return true; }

    @Override
    public @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        if (isHealed(player)) return new Refusal(FAILED_HEALTHY);

        PathEffect<SelfRelianceHeal> running = PathEffectService.find(player, ModPathEffectTypes.SELF_RELIANCE);
        return running != null && running.payload().grade() >= grade ? new Refusal(FAILED_RUNNING) : null;
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        Amplification amplification = AmplifyEffectGuItem.bill(player, gu);
        ResourceLocation source = BuiltInRegistries.ITEM.getKey(gu);
        PathEffectService.apply(player, gu.getSpec().createEffect(ModPathEffectTypes.SELF_RELIANCE, source,
                PathEffect.PERMANENT, new SelfRelianceHeal(grade, amplification.factor(), 0.0F)));
        TimedEffects.markAmplified(player, ModPathEffectTypes.SELF_RELIANCE, source, amplification);
    }

    public static void pulse(ServerPlayer player) {
        if (player.tickCount % PULSE_TICKS != 0 || player.isDeadOrDying()) return;

        PathEffect<SelfRelianceHeal> heal = PathEffectService.find(player, ModPathEffectTypes.SELF_RELIANCE);
        if (heal == null) return;

        ItemStack stack = findCarried(player, heal.source());
        if (!(stack.getItem() instanceof TendedGuItem gu)) {
            end(player);
            return;
        }
        SelfRelianceModule module = gu.getModule(SelfRelianceModule.class);
        if (module == null || isHealed(player) || !gu.drainAboveFloor(player, stack, 0, HUNGER_FLOOR)) {
            end(player);
            return;
        }
        float healed = heal.payload().healed() + module.mend(player, heal.payload().factor());
        boolean fed = true;
        while (fed && healed >= module.healedPerHunger() - EPSILON) {
            healed -= module.healedPerHunger();
            fed = gu.drainAboveFloor(player, stack, 1, HUNGER_FLOOR);
        }
        if (!fed || isHealed(player)) {
            end(player);
            return;
        }
        SelfRelianceHeal next = heal.payload().withHealed(Math.max(0.0F, healed));
        PathEffectService.update(player, ModPathEffectTypes.SELF_RELIANCE, heal.source(),
                effect -> effect.withPayload(next));
    }

    /** Heals one pulse up to the ceiling and answers the share of max health it actually mended. */
    private float mend(ServerPlayer player, int factor) {
        float max = player.getMaxHealth();
        float before = player.getHealth();
        player.heal(Math.min(pulseHeal * factor * max, max * HEAL_CEILING - before));
        return Math.max(0.0F, player.getHealth() - before) / max;
    }

    private static boolean isHealed(Player player) {
        return player.getHealth() >= player.getMaxHealth() * (HEAL_CEILING - EPSILON);
    }

    private static ItemStack findCarried(Player player, ResourceLocation source) {
        Inventory inventory = player.getInventory();
        for (List<ItemStack> part : List.of(inventory.items, inventory.offhand)) {
            for (ItemStack stack : part) {
                if (stack.getItem() instanceof TendedGuItem gu && gu.isRefined(stack)
                        && BuiltInRegistries.ITEM.getKey(gu).equals(source)) return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void end(ServerPlayer player) {
        PathEffectService.removeType(player, ModPathEffectTypes.SELF_RELIANCE);
    }
}
