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
 * 修为进度条件：第一空窍[Aperture]达到或超过 {@code rank}、{@code stage}，先比转；第二空窍不计。
 *
 * <ul>
 *   <li>触发点仅修炼写入：{@code ApertureNourishService} 的温养[Nourish]升阶、冲刷窍壁成功、石化转化，及
 *       催动石窍蛊[Stone Aperture Gu]或舍利蛊[Relics Gu]；命令设的修为不算。</li>
 *   <li>“达到或超过”让下次真实进阶补发其下各档：进度加入前已三转的空窍，下次升阶即得二转。</li>
 * </ul>
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
