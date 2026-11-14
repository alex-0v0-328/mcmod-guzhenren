package net.alex.guzhenren.gameplay.body;

import com.google.common.math.LongMath;
import java.math.BigInteger;
import java.util.Map;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.clock.Beat;
import net.alex.guzhenren.gameplay.clock.TimeFlow;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.path.PathMarks;
import net.alex.guzhenren.gameplay.path.EffectCarrier;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.PathService;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 肉身状态唯一运行时写入者，体质转换归口。
 *
 * <p>时钟：
 * <ul>
 *   <li>{@link #tickAging} 锚 {@code lastDayIndex}，只为三项蛊饱食[Hunger]遍历计天；{@link #tickLifespan} 锚
 *       {@code lastBilledTick}，每次心跳结算寿元。</li>
 *   <li>寿元只计在线：{@link #skipOfflineLifespan} 登录时解锚，首次心跳重锚、不补离线；日钟仍计离线天数。每次心跳写同步
 *       的 BODY（每人每秒 1 次）以换宙道精确缩放与按时耗尽，不批量。</li>
 *   <li>⚠ {@code tickAging} 返回天数可远大于 1，吞掉它会让所有蛊同时挨饿。</li>
 *   <li>☠ 寿元经 {@link TimeFlow#scale} 消耗：加速即更快；手写速率会倒流成纯延寿。</li>
 *   <li>⚠ 两锚点用 {@code dayTime} 非 {@code gameTime}（{@code /time add} 照样增龄）；时间倒退即重锚不结算，守卫仅一个
 *       {@code <}：{@code today < lastDayIndex}、{@code now < lastBilledTick}。</li>
 *   <li>⚠ 调用方只用年，寿元份数仅本文件知道；{@link #clampParts} 的 {@code BigInteger} 中间积不提前截断，后续有符号加
 *       法或分数可拉回范围。</li>
 * </ul>
 * <p>体质：
 * <ul>
 *   <li>⚠ {@code setExtremePhysique} 不查开窍[Awakening]：设非 {@code NONE} 时调用方自设门槛（命令经
 *       {@code AwakenedGate.AWAKENED}，开窍流程先开空窍[Aperture]）。</li>
 *   <li>⚠ 临时僵尸到期仍留结束 tick：其后 {@code RELAPSE_WINDOW_TICKS} 内再用僵尸类蛊即永久成僵
 *       （{@link #isWithinRelapseWindow}），仅 {@link #revive} 清除。</li>
 *   <li>⚠ 寿元：进入临时僵尸即冻结（不死之躯不结算），离开只解冻；转僵尸置 {@code ZOMBIE_LIFESPAN}，形态结束保留。</li>
 * </ul>
 * <p>皮与筋骨：
 * <ul>
 *   <li>玉皮结束 tick 用 {@code gameTime}，{@code SkinService} 心跳读取，{@link #revive} 随不死形态一并蜕下；
 *       永久皮无结束 tick，重生保留。</li>
 *   <li>新品阶能否替换由各部位 {@code canReplace} 判定，蛊来问，不在此。</li>
 * </ul>
 * <p>种族：
 * <ul>
 *   <li>{@link #setRace} 是第二个跨域授予（另一个 {@code ApertureService.reconcileTalentPaths}）：撤旧授新，造诣经
 *       {@code PathService} 平移；道痕[Path Marks]作永久 {@code RACE_TALENT} 肉身效果经 {@code PathEffectService}
 *       施加，来源 {@link #RACE_TALENT_SOURCE}，种族之道[Path]上 10 整道痕。</li>
 *   <li>{@link #syncRaceTalent} 登录时补回缺失或错种族的效果，不动造诣。</li>
 *   <li>超 300 行：肉身记录读写唯一入口，每字段族一个 region，拆分会让入口分属两个服务。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see TimeFlow
 * @see PhysiqueChangedEvent
 * @since 1.0.0
 */

public final class BodyService {

    private BodyService() {}

    public static final ResourceLocation RACE_TALENT_SOURCE = Guzhenren.id("innate/race");

    private static long getDayIndex(@NotNull MinecraftServer server) {
        return server.overworld().getDayTime() / Ticks.DAY;
    }

    public static @NotNull BodyData get(@NotNull Player player) { return PlayerData.get(player).getBody(); }

    public static boolean isZombie(@NotNull Player player) { return get(player).isZombie(); }

    public static boolean isTempZombie(@NotNull Player player) { return get(player).isTempZombie(); }

    public static boolean isUndead(@NotNull Player player) { return get(player).isZombieOrTempZombie(); }

    public static boolean isExtreme(@NotNull Player player) { return get(player).isExtreme(); }

    public static @NotNull ExtremePhysique getExtremePhysique(@NotNull Player player) {
        return get(player).extremePhysique();
    }

    private static @NotNull Race getRace(@NotNull Player player) { return get(player).race(); }

    private static void store(ServerPlayer player, BodyData data) {
        PlayerData.get(player).setBody(data);
    }

    /** 同步肉身状态标记效果：按剩余 tick（至少 1）显示，否则移除。 */
    static void projectMarker(ServerPlayer player, Holder<MobEffect> effect, boolean shown, long ticksLeft) {
        if (shown) {
            player.addEffect(ModEffects.instance(effect, Math.max(1, (int) ticksLeft)));
        } else if (player.hasEffect(effect)) {
            player.removeEffect(effect);
        }
    }

    //region lifespan and age [寿元与年龄]
    public static void setAge(@NotNull ServerPlayer player, long years) {
        store(player, get(player).withAgeParts(BodyData.toParts(years)));
    }

    public static void setLifespan(@NotNull ServerPlayer player, long years) {
        store(player, get(player).withLifespanParts(BodyData.toParts(years)));
    }

    public static void addAge(@NotNull ServerPlayer player, long years) {
        BigInteger parts = BigInteger.valueOf(get(player).ageParts()).add(toYearParts(years));
        store(player, get(player).withAgeParts(clampParts(parts)));
    }

    public static void addLifespan(@NotNull ServerPlayer player, long years) {
        BigInteger parts = BigInteger.valueOf(get(player).lifespanParts()).add(toYearParts(years));
        store(player, get(player).withLifespanParts(clampParts(parts)));
    }

    private static BigInteger toYearParts(long years) {
        return BigInteger.valueOf(years).multiply(BigInteger.valueOf(BodyData.PARTS_PER_YEAR));
    }

    private static long clampParts(BigInteger parts) {
        return parts.max(BigInteger.valueOf(Long.MIN_VALUE)).min(BigInteger.valueOf(Long.MAX_VALUE)).longValue();
    }
    //endregion

    //region Physique [体质]
    public static void addPhysique(@NotNull ServerPlayer player, @NotNull Physique physique) {
        BodyData body = get(player);
        if (physique == Physique.EXTREME || body.hasPhysique(physique)) return;

        BodyData updated = (physique == Physique.TEMP_ZOMBIE ? body.without(Physique.ZOMBIE) : body).with(physique);
        if (physique == Physique.ZOMBIE) {
            updated = updated.withLifespanParts(BodyData.toParts(BodyData.ZOMBIE_LIFESPAN));
        }
        storePhysique(player, updated);
    }

    public static void removePhysique(@NotNull ServerPlayer player, @NotNull Physique physique) {
        if (physique == Physique.EXTREME) {
            setExtremePhysique(player, ExtremePhysique.NONE);
            return;
        }
        BodyData body = get(player);
        if (body.hasPhysique(physique)) storePhysique(player, body.without(physique));
    }

    public static void setExtremePhysique(@NotNull ServerPlayer player, @NotNull ExtremePhysique physique) {
        BodyData body = get(player);
        ExtremePhysique before = body.extremePhysique();
        if (before == physique) return;

        store(player, body.withExtremePhysique(physique));
        ModEvents.post(new ExtremePhysiqueChangedEvent(player, before, physique));
    }

    public static void revive(@NotNull ServerPlayer player) { storePhysique(player, get(player).withRevived()); }

    public static void enterTempZombie(@NotNull ServerPlayer player, int tier, int durationTicks) {
        storePhysique(player, get(player).without(Physique.ZOMBIE).with(Physique.TEMP_ZOMBIE)
                .withTempZombieEndTick(player.level().getGameTime() + durationTicks)
                .withZombieTier(tier));
    }

    public static void turnZombie(@NotNull ServerPlayer player, int tier) {
        storePhysique(player, get(player).with(Physique.ZOMBIE)
                .withLifespanParts(BodyData.toParts(BodyData.ZOMBIE_LIFESPAN))
                .withZombieTier(tier));
    }

    public static void restoreTempZombie(@NotNull ServerPlayer player, long ticksLeft) {
        BodyData body = get(player);
        if (body.isZombie()) return;

        BodyData next = ticksLeft > 0L
                ? body.with(Physique.TEMP_ZOMBIE).withTempZombieEndTick(player.level().getGameTime() + ticksLeft)
                : body.without(Physique.TEMP_ZOMBIE).withTempZombieEndTick(BodyData.UNTRACKED);
        if (!next.equals(body)) storePhysique(player, next);
    }

    private static void storePhysique(ServerPlayer player, BodyData data) {
        store(player, data);
        ModEvents.post(new PhysiqueChangedEvent(player));
    }

    public static boolean isWithinRelapseWindow(@NotNull Player player) {
        return get(player).isWithinRelapseWindow(player.level().getGameTime());
    }

    public static long getTempZombieTicksLeft(@NotNull Player player) {
        return get(player).getTempZombieTicksLeft(player.level().getGameTime());
    }

    public static boolean hasTempZombieRunOut(@NotNull Player player) {
        return get(player).hasTempZombieRunOut(player.level().getGameTime());
    }
    //endregion

    //region Skin [皮肤]
    public static @NotNull Skin getSkin(@NotNull Player player) { return get(player).skin(); }

    public static void wearSkin(@NotNull ServerPlayer player, @NotNull Skin skin, int durationTicks) {
        long endTick = skin.isTimed() ? player.level().getGameTime() + durationTicks : BodyData.UNTRACKED;
        store(player, get(player).withSkin(skin, endTick));
        ModEvents.post(new SkinWornEvent(player, skin));
    }

    public static void shedSkin(@NotNull ServerPlayer player) {
        store(player, get(player).withSkin(Skin.NATIVE, BodyData.UNTRACKED));
        ModEvents.post(new SkinShedEvent(player));
    }

    public static boolean hasSkinRunOut(@NotNull Player player) {
        return get(player).hasSkinRunOut(player.level().getGameTime());
    }

    public static long getSkinTicksLeft(@NotNull Player player) {
        return get(player).getSkinTicksLeft(player.level().getGameTime());
    }
    //endregion

    //region Tendons and bones [筋骨]
    public static @NotNull Tendon getTendon(@NotNull Player player) { return get(player).tendon(); }

    public static @NotNull Bone getBone(@NotNull Player player) { return get(player).bone(); }

    public static void temperTendon(@NotNull ServerPlayer player, @NotNull Tendon tendon) {
        store(player, get(player).withTendon(tendon));
        ModEvents.post(new BodyTemperedEvent(player));
    }

    public static void temperBone(@NotNull ServerPlayer player, @NotNull Bone bone) {
        store(player, get(player).withBone(bone));
        ModEvents.post(new BodyTemperedEvent(player));
    }
    //endregion

    //region Race [种族]
    public static void setRace(@NotNull ServerPlayer player, @NotNull Race race) {
        Race current = getRace(player);
        if (current == race) return;

        revokeTalent(player, current);
        store(player, get(player).withRace(race));
        grantTalent(player, race);
    }

    public static void syncRaceTalent(@NotNull ServerPlayer player) {
        Race race = getRace(player);
        PathEffectService.sync(player, ModPathEffectTypes.RACE_TALENT, race.isVariant() ? getRaceTalent(race) : null);
    }

    public static @NotNull PathEffect<Race> getRaceTalent(@NotNull Race race) {
        Map<GuPath, PathMarks> paths = race.getTalentPath() == null ? Map.of()
                : Map.of(race.getTalentPath(), PathMarks.ofMarks(Race.TALENT_MARKS));
        return PathEffect.of(ModPathEffectTypes.RACE_TALENT, EffectCarrier.BODY, RACE_TALENT_SOURCE, paths,
                PathEffect.PERMANENT, race);
    }

    private static void grantTalent(ServerPlayer player, Race race) {
        GuPath path = race.getTalentPath();
        if (path == null) return;

        PathEffectService.apply(player, getRaceTalent(race));
        PathService.shiftAttainment(player, path, Race.TALENT_SHIFT);
    }

    private static void revokeTalent(ServerPlayer player, Race race) {
        GuPath path = race.getTalentPath();
        if (path == null) return;

        PathEffectService.removeType(player, ModPathEffectTypes.RACE_TALENT);
        PathService.shiftAttainment(player, path, -Race.TALENT_SHIFT);
    }
    //endregion

    public static long tickAging(@NotNull ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return 0L;

        long today = getDayIndex(server);
        BodyData body = get(player);

        if (body.lastDayIndex() == BodyData.UNTRACKED || today < body.lastDayIndex()) {
            store(player, body.withLastDayIndex(today));
            return 0L;
        }

        long elapsed = today - body.lastDayIndex();
        if (elapsed == 0L) return 0L;

        store(player, body.withLastDayIndex(today));
        return elapsed;
    }

    //region the lifespan clock [寿元时钟]
    public static void tickLifespan(@NotNull ServerPlayer player, @NotNull Beat beat) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        long now = server.overworld().getDayTime();
        BodyData body = get(player);

        if (body.lastBilledTick() == BodyData.UNTRACKED || now < body.lastBilledTick()
                || body.isZombieOrTempZombie()) {
            store(player, body.withLastBilledTick(now));
            return;
        }
        long lived = beat.scale(getElapsedParts(now - body.lastBilledTick()));
        if (lived <= 0L) return;

        store(player, body.withLived(lived, now));
    }

    public static void skipOfflineLifespan(@NotNull ServerPlayer player) {
        store(player, get(player).withLastBilledTick(BodyData.UNTRACKED));
    }

    public static long getElapsedParts(long elapsedTicks) {
        return LongMath.saturatedMultiply(Math.max(0L, elapsedTicks), BodyData.PARTS_PER_TICK);
    }
    //endregion
}
