package net.alex.guzhenren.gameplay.rewind;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.mind.MindData;
import net.alex.guzhenren.gameplay.mind.MindPoolType;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.alex.guzhenren.gameplay.mind.ThoughtTag;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.NotNull;

/**
 * One player's body at one game moment, as the Man As Before Gu [人如故] remembers it: health, hunger and
 * saturation, air and fire, every active effect with its time left, the soul [魂魄] he had, each mind pool's
 * thoughts [念] with their tags, and how long his temp zombie [临时僵尸化] form had left.
 *
 * <p>⚠ Deliberately not the whole player. Essence [真元], lifespan [寿元], inventory, position, experience, the
 * aperture and the paths stay out: putting essence or lifespan back would refund the Gu's own price and undo a day
 * of aging, putting a bag back would mint items. A zombie [僵尸] is not remembered either -- that turn is
 * final -- so the form is stored as the temp zombie ticks left, zero for none, and restored as a fresh window of
 * that length.
 *
 * <p>⚠ The effect list is read leniently: an effect whose mod has since been removed costs the list, not the
 * snapshot. A snapshot the codec cannot read at all is skipped by the loader with a logged error and nothing
 * else on the player is touched.
 *
 * @author Alex
 * @version 1.0.0
 * @see PlayerRewindService
 * @since 1.0.0
 */

public record PlayerSnapshot(long takenAt, float health, int foodLevel, float saturation, int airSupply,
                             int fireTicks, List<MobEffectInstance> effects, long soul,
                             Map<MindPoolType, Long> thoughts, Map<ThoughtTag, Long> taggedThoughts,
                             long tempZombieTicksLeft) {

    public static final Codec<PlayerSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("taken_at").forGetter(PlayerSnapshot::takenAt),
            Codec.FLOAT.optionalFieldOf("health", 1.0F).forGetter(PlayerSnapshot::health),
            Codec.INT.optionalFieldOf("food_level", 0).forGetter(PlayerSnapshot::foodLevel),
            Codec.FLOAT.optionalFieldOf("saturation", 0.0F).forGetter(PlayerSnapshot::saturation),
            Codec.INT.optionalFieldOf("air_supply", 0).forGetter(PlayerSnapshot::airSupply),
            Codec.INT.optionalFieldOf("fire_ticks", 0).forGetter(PlayerSnapshot::fireTicks),
            MobEffectInstance.CODEC.listOf().lenientOptionalFieldOf("effects", List.of())
                    .forGetter(PlayerSnapshot::effects),
            Codec.LONG.optionalFieldOf("soul", 0L).forGetter(PlayerSnapshot::soul),
            Codec.unboundedMap(MindPoolType.CODEC, Codec.LONG).optionalFieldOf("thoughts", Map.of())
                    .forGetter(PlayerSnapshot::thoughts),
            Codec.unboundedMap(ThoughtTag.CODEC, Codec.LONG).optionalFieldOf("tagged_thoughts", Map.of())
                    .forGetter(PlayerSnapshot::taggedThoughts),
            Codec.LONG.optionalFieldOf("temp_zombie_ticks_left", 0L).forGetter(PlayerSnapshot::tempZombieTicksLeft)
    ).apply(instance, PlayerSnapshot::new));

    public PlayerSnapshot {
        effects = List.copyOf(effects);
        thoughts = Map.copyOf(thoughts);
        taggedThoughts = Map.copyOf(taggedThoughts);
    }

    public static @NotNull PlayerSnapshot capture(@NotNull ServerPlayer player, long now) {
        MindData mind = MindService.get(player);
        Map<MindPoolType, Long> thoughts = new EnumMap<>(MindPoolType.class);
        mind.pools().forEach((type, pool) -> thoughts.put(type, pool.current()));
        BodyData body = BodyService.get(player);
        long tempZombieLeft = body.isTempZombie() ? Math.max(0L, body.getTempZombieTicksLeft(now)) : 0L;
        List<MobEffectInstance> effects = player.getActiveEffects().stream().map(MobEffectInstance::new).toList();
        return new PlayerSnapshot(now, player.getHealth(), player.getFoodData().getFoodLevel(),
                player.getFoodData().getSaturationLevel(), player.getAirSupply(), player.getRemainingFireTicks(),
                effects, SoulService.get(player).currentSoul(), thoughts, mind.taggedThoughts(), tempZombieLeft);
    }
}
