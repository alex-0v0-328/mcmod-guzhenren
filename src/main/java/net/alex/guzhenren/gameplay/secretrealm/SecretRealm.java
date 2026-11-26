package net.alex.guzhenren.gameplay.secretrealm;

import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.place.PlaceTitle;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.jetbrains.annotations.NotNull;

/**
 * 天地秘境[Secret Realm]：世界里唯一一处、由 Gu World 结构集定位的特殊地域，在范围内改写生灵规则。
 * <ul>
 *   <li>实现只描述「在哪」与「范围内发生什么」：{@link #dimension}、{@link #structureSet}、{@link #radius} 交给
 *       {@link SecretRealms} 解出 {@link RealmSite}；钩子由 {@link SecretRealms} 调用并传入已解出的范围，实现不自己查山
 *       心，也不重复范围判定。</li>
 *   <li>钩子时机：{@link #tickPlayer} 本秒玩家在范围内；{@link #tickPlayerAway} 秘境已解出、本秒玩家不在范围内（含身处
 *       其它维度）；{@link #tickArea} 每秒一次且范围外扩 {@link SecretRealms#PLAYER_MARGIN} 格内有玩家；
 *       {@link #onDeath} 范围内生物死亡（不死图腾救下的不算）。</li>
 *   <li>{@link #rank}、{@link #path} 是秘境的转数与流派属性，只作记载，不参与任何结算。</li>
 *   <li>{@link #title} 是玩家进入范围时弹出的地名标题，类别统一用 {@link #KIND_KEY}「天地秘境」；进入判定与冷却由
 *       {@link SecretRealms} 交给 {@code PlaceTitles}。</li>
 *   <li>{@link #summitRise} 是山巅最高一层比山底高的格数：{@link SecretRealms} 用结构片边界框最高顶（山巅）减它得山底，
 *       记进 {@link RealmSite#center} 的 y。</li>
 *   <li>⚠ 结构集的放置器在维度原点 ±{@link SecretRealms#SCAN_CHUNKS} 区块内只能有一个命中区块（取首个）；结构集不存
 *       在、扫不到或该区块没有有效起点时整个秘境静默关闭。</li>
 *   <li>⚠ 钩子只在服务端主线程调用；实现保持无状态，世界状态存 {@link net.alex.guzhenren.core.ModSavedData}。</li>
 * </ul>
 */

public interface SecretRealm {

    String KIND_KEY = "guzhenren.place.kind.secret_realm";

    @NotNull ResourceKey<Level> dimension();

    @NotNull ResourceKey<StructureSet> structureSet();

    int radius();

    int summitRise();

    @NotNull Rank rank();

    @NotNull GuPath path();

    @NotNull PlaceTitle title();

    default void tickPlayer(@NotNull ServerPlayer player, @NotNull RealmSite site) {}

    default void tickPlayerAway(@NotNull ServerPlayer player, @NotNull RealmSite site) {}

    default void tickArea(@NotNull ServerLevel level, @NotNull RealmSite site) {}

    default void onDeath(@NotNull ServerLevel level, @NotNull RealmSite site, @NotNull LivingEntity entity) {}
}
