package net.alex.guzhenren.gameplay.attribute;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;

/**
 * Max health as a transient {@link AttributeModifier} derived from {@code ApertureService.healthRank}:
 * the FIRST aperture's rank only -- unawakened reads mortal, a lone second aperture never boosts it.
 * Static service; {@link AttributeEvents} refreshes it on every {@code ApertureChangedEvent} (pressure
 * writes post none), plus login, clone and reset (a modifier does not ride a clone); keyed to the rank's
 * {@code getMaxHealth()} minus vanilla's 20 -- mortal reads 0.
 *
 * <p>⚠ The modifier MUST stay transient ({@link TransientModifiers}). ⚠ Lowering the cap must also
 * clamp current health down; {@link AttackDamageService} needs no clamp (attack has no "current").
 *
 * @author Alex
 * @version 1.0.0
 * @see AttackDamageService
 * @see ApertureService
 * @since 1.0.0
 */

public final class MaxHealthService {

    private MaxHealthService() {}

    public static final double VANILLA_MAX_HEALTH = 20.0D;
    private static final ResourceLocation MODIFIER_ID =
            Guzhenren.id("rank_max_health");

    public static void refresh(@NotNull ServerPlayer player) {
        AttributeInstance instance = player.getAttribute(Attributes.MAX_HEALTH);
        if (instance == null) return;

        int target = ApertureService.healthRank(player).getMaxHealth();
        double bonus = target > 0 ? target - VANILLA_MAX_HEALTH : 0.0D;
        TransientModifiers.swap(instance, MODIFIER_ID, bonus);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }
}
