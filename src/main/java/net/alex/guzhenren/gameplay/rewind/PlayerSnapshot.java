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
 * 人如故[Man As Before Gu]在单个游戏时刻捕获的玩家身体状态快照（嵌于 {@link PlayerRewindData}）。
 * <ul>
 *   <li>⚠ 刻意不记录真元、寿元、物品栏、坐标、经验、空窍与流派数据：回退真元或寿元会白嫖蛊虫消耗并抵消自然衰老，回退背
 *       包则会凭空复制物品。</li>
 *   <li>永久僵尸形态不可逆转，不纳入回溯；临时僵尸化仅记录剩余持续 tick（无则为 {@code 0}），还原时按剩余时长重新赋予临
 *       时僵尸化窗口。</li>
 *   <li>⚠ 药水效果列表使用 {@code lenientOptionalFieldOf} 容错反序列化：若旧快照中含有已被移除的模组效果，仅丢弃该效果
 *       列表而不会使整份玩家存档加载失败。</li>
 * </ul>
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
