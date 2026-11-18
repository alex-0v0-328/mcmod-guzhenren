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
 * 完成首只希望蛊[Hope Gu]开窍[Awakening]仪式的进度触发器。仅在未开窍玩家撑完仪式并通过 {@link ApertureService#awaken}
 * 开启第一空窍时由 {@code HopeGuItem.apply} 发布的事件触发，管理员通过指令直接开窍不会触发该进度。
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
