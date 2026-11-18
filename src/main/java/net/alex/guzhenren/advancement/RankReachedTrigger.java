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
 * 第一空窍修为达到或超过指定转数与阶段的进度触发器（忽略第二空窍）。
 * <ul>
 *   <li>仅由正常修炼晋升路径触发（含 {@code ApertureNourishService} 的温养[Nourish]升阶、冲刷窍壁破转、石化转化，以及催
 *       动石窍蛊[Stone Aperture Gu]或舍利蛊[Relics Gu]），管理员指令直接修改修为不会触发。</li>
 *   <li>{@link Instance#matches} 采用「达到或超过」比较（先比转数、同转再比阶段），使高转玩家在后续任意一次正常晋升时能
 *       一并补发此前跳过的低转进度。</li>
 * </ul>
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
