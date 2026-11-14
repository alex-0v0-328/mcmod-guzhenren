package net.alex.guzhenren.advancement;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * 首只希望蛊[Hope Gu]仪式的进度条件：未觉醒玩家撑完仪式，{@link ApertureService#awaken} 开第一空窍[Aperture]；唯一触发
 * 点 {@code HopeGuItem.apply}，命令给的开窍[Awakening]不算。
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.registry.advancement.ModCriteriaTriggers
 * @since 1.0.0
 */

public class HopeGuUsedTrigger extends SimpleCriterionTrigger<HopeGuUsedTrigger.Instance> {

    @Override
    public @NotNull Codec<Instance> codec() { return Instance.CODEC; }

    public Criterion<Instance> createCriterion() { return new Criterion<>(this, new Instance(Optional.empty())); }

    public void trigger(ServerPlayer player) { this.trigger(player, instance -> true); }

    public record Instance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<Instance> CODEC = EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player")
                .xmap(Instance::new, Instance::player).codec();
    }
}
