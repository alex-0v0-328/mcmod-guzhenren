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
 * [功倍蛊] factor billed at the start, never past the ceiling. Damage taken meanwhile is healed too.
 *
 * <p>⚠ One heal is one round: the use that starts it pays the spec's hunger once, like any use, and the pulses pay
 * nothing. The heal ends at the ceiling or when no refined copy is carried in the bag ({@code items} or offhand).
 * The gate refuses at the ceiling and while a heal of the same or a higher grade runs; a higher grade takes a
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

public record SelfRelianceModule(int grade, float pulseHeal) implements GuModule {

    public static final int PULSE_TICKS = 5;
    public static final float HEAL_CEILING = 0.9F;
    // Float shares of max health: a heal that stops at the ceiling must read as healed, not as 89.99998%.
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
                PathEffect.PERMANENT, new SelfRelianceHeal(grade, amplification.factor())));
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
        if (module == null || isHealed(player)) {
            end(player);
            return;
        }
        module.mend(player, heal.payload().factor());
        if (isHealed(player)) end(player);
    }

    /** Heals one pulse, never past the ceiling. */
    private void mend(ServerPlayer player, int factor) {
        float max = player.getMaxHealth();
        player.heal(Math.min(pulseHeal * factor * max, max * HEAL_CEILING - player.getHealth()));
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
