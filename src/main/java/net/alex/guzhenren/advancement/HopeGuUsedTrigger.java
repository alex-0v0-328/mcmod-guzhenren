package net.alex.guzhenren.advancement;

import com.google.gson.JsonObject;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Criterion for the ritual of the first Hope Gu [希望蛊]: an unawakened player holds the ritual to
 * the end and {@link ApertureService#awaken} opens the first aperture. The only fire site is
 * {@code HopeGuItem.apply}, so an awakening granted by command never completes the advancement.
 *
 * <p>⚠ Loader glue: the trigger API differs between the NeoForge 1.21.1 mainline (codec, {@code Criterion}) and the
 * Forge 1.20.1 port (JSON deserializer, {@code CriterionTriggerInstance}); {@link #trigger} is the same on both.
 * {@link #instance()} is what advancement data hangs on a criterion.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.registry.advancement.ModCriteriaTriggers
 * @since 1.0.0
 */

public class HopeGuUsedTrigger extends SimpleCriterionTrigger<HopeGuUsedTrigger.Instance> {

    public static final ResourceLocation ID = Guzhenren.id("used_hope_gu");

    @Override
    public @NotNull ResourceLocation getId() { return ID; }

    @Override
    protected @NotNull Instance createInstance(@NotNull JsonObject json, @NotNull ContextAwarePredicate player,
                                               @NotNull DeserializationContext context) {
        return new Instance(player);
    }

    public Instance instance() { return new Instance(ContextAwarePredicate.ANY); }

    public void trigger(ServerPlayer player) { this.trigger(player, instance -> true); }

    public static class Instance extends AbstractCriterionTriggerInstance {

        public Instance(ContextAwarePredicate player) { super(ID, player); }
    }
}
