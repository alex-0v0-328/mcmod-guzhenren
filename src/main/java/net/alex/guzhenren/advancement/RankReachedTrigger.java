package net.alex.guzhenren.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.alex.guzhenren.gameplay.aperture.Aperture;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.Stage;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Criterion for a cultivator's realm [境界]: the first aperture [空窍] stands at or past {@code rank} and
 * {@code stage}, rank compared first. A second aperture never counts. The fire sites are the cultivation
 * writes alone -- a stage nourished up, the wall struck through and a petrified gauge converted in {@code
 * ApertureNourishService}, a Stone Aperture Gu or Relics Gu used -- so a realm set by command never
 * completes the advancement.
 *
 * <p>"At or past" lets the next real step award every grade below it: an aperture that arrived at Rank III
 * before these advancements existed earns Rank II on its next stage up.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.registry.advancement.ModCriteriaTriggers
 * @since 1.0.0
 */

public class RankReachedTrigger extends SimpleCriterionTrigger<RankReachedTrigger.Instance> {

    @Override
    public @NotNull Codec<Instance> codec() { return Instance.CODEC; }

    public Criterion<Instance> createCriterion(Rank rank, Stage stage) {
        return new Criterion<>(this, new Instance(Optional.empty(), rank, stage));
    }

    public void trigger(ServerPlayer player, Aperture aperture) {
        if (aperture.second()) return;
        this.trigger(player, instance -> instance.matches(aperture.rank(), aperture.stage()));
    }

    public record Instance(Optional<ContextAwarePredicate> player, Rank rank, Stage stage)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Rank.CODEC.fieldOf("rank").forGetter(Instance::rank),
                Stage.CODEC.optionalFieldOf("stage", Stage.LOWEST).forGetter(Instance::stage)
        ).apply(instance, Instance::new));

        public boolean matches(Rank reached, Stage at) {
            int byRank = Integer.compare(reached.ordinal(), rank.ordinal());
            return byRank > 0 || byRank == 0 && at.ordinal() >= stage.ordinal();
        }
    }
}
