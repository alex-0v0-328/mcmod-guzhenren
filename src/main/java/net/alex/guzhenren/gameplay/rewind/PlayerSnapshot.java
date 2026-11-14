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
 * 人如故[Man As Before Gu]记住的某一游戏时刻的玩家身体。
 * <ul>
 *   <li>⚠ 有意不含真元[Essence]、寿元、物品栏、位置、经验、空窍[Aperture]与各道[Path]：放回真元或寿元会退还蛊的代价并撤
 *       销一天衰老，放回背包会凭空造物。</li>
 *   <li>僵尸也不记（转化是终局）；临时僵尸化存为剩余 tick（0 为无），还原为同长的新窗口。</li>
 *   <li>⚠ 效果列表宽松读取：模组已移除的效果只丢列表不丢快照；整体读不出的快照由加载方跳过并记错误，玩家其余不动。</li>
 * </ul>
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
