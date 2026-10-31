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
import net.alex.guzhenren.gameplay.path.DaoMarks;
import net.alex.guzhenren.gameplay.path.EffectCarrier;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.PathService;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The only runtime writer of Body [肉身] state, and the home of every physique [体质] transition. Static
 * service; owns two clocks with two anchors: {@link #tickAging} keeps {@code lastDayIndex} and counts
 * DAYS for the three Gu-hunger walks and nothing else; {@link #tickLifespan} keeps
 * {@code lastBilledTick} and bills lifespan every heartbeat instead of once a day. Lifespan bills online time
 * only: {@link #skipOfflineLifespan} un-anchors the bill on login, so the first heartbeat re-anchors and bills
 * nothing for the time away, while the day clock still counts the days away for the Gu-hunger walks. The
 * per-heartbeat bill writes the synced BODY once a second per player; that small packet is the accepted price of
 * exact Time Path [宙道] scaling and an on-time exhaustion check, so the bill is not batched.
 *
 * <p>⚠ {@code tickAging}'s returned day count can far exceed one and drives three day-clock walks --
 * swallowing it starves every Gu at once. ☠ 寿元 is SPENT through {@link TimeFlow#scale}
 * like every other thing he spends -- hastened means FASTER; a hand-rolled rate runs it
 * BACKWARDS, into a pure longevity buff. ⚠ Both anchors are {@code dayTime}, not {@code gameTime} (so
 * {@code /time add} still ages him): time running backwards re-anchors and bills nothing, and each clock's
 * whole guard is one {@code <} -- {@code today < lastDayIndex} in {@code tickAging},
 * {@code now < lastBilledTick} in {@code tickLifespan}.
 *
 * <p>In {@link #clampParts}, the intermediate {@code BigInteger} products are kept exact rather than
 * clamped early, because a later signed addition or fraction can bring them back into range.
 *
 * <p>Physique and undead writes go through {@link #storePhysique}, which posts a {@link PhysiqueChangedEvent}
 * (the attack modifier refreshes);
 * {@link #setExtremePhysique} posts an {@link ExtremePhysiqueChangedEvent} (the aperture reconciles its
 * talent paths, base essence and pressure). ⚠ {@code setExtremePhysique} does not check awakening: a
 * caller setting a physique other than {@code NONE} gates on it -- the command through
 * {@code AwakenedGate.AWAKENED}, awakening by opening the aperture first. ⚠ A temp zombie form that runs
 * out keeps its end tick: a Zombie Gu used within {@code RELAPSE_WINDOW_TICKS} after it turns the body
 * permanently 僵 ({@link #isWithinRelapseWindow}); only {@link #revive} clears it. ⚠ Lifespan across the
 * three stages, human, temp zombie and zombie: entering temp zombie freezes it (an undead body is never billed)
 * and leaving it only thaws it; turning zombie [僵尸], from a human or from temp zombie, sets it to
 * {@code ZOMBIE_LIFESPAN}, and that year stays when the form ends.
 *
 * <p>The skin [皮肤] is worn through {@link #wearSkin} and shed through {@link #shedSkin}; a jade skin's end
 * tick is {@code gameTime}, read by {@code SkinService} on the heartbeat, and {@link #revive} sheds it with the
 * undead forms; a permanent skin keeps no end tick and stays through a respawn. Wearing posts a
 * {@link SkinWornEvent} (the flash goes out); shedding posts {@link SkinShedEvent}. The tendons [筋] and bones [骨] are
 * tempered through {@link #temperTendon} and {@link #temperBone}, which post a {@link BodyTemperedEvent} (the
 * attribute layer refreshes capacity, max health and knockback resistance); the gate that decides whether a
 * grade replaces the worn one is each part's {@code canReplace}, asked by the Gu, never here.
 *
 * <p>{@link #setRace} is the second of the two cross-domain grants, beside
 * {@code ApertureService.reconcileTalentPaths}: it revokes the old race's talent and grants the new race's -- the
 * attainment shift through {@code PathService}, the Dao marks as the permanent {@code RACE_TALENT} body effect
 * through {@code PathEffectService}, ten whole marks on the race's path from {@link #RACE_TALENT_SOURCE}.
 * {@link #syncRaceTalent} puts that effect back on login when it is missing or names another race; it never
 * touches the attainment, which only a race change moves.
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

    public static long getDayIndex(@NotNull MinecraftServer server) {
        return server.overworld().getDayTime() / Ticks.DAY;
    }

    public static @NotNull BodyData get(@NotNull Player player) { return PlayerData.get(player).getBody(); }

    public static boolean isZombie(@NotNull Player player) { return get(player).isZombie(); }

    public static boolean isTempZombie(@NotNull Player player) { return get(player).isTempZombie(); }

    public static boolean isZombieOrTempZombie(@NotNull Player player) { return get(player).isZombieOrTempZombie(); }

    public static boolean isUndead(@NotNull Player player) { return isZombieOrTempZombie(player); }

    public static boolean isExtreme(@NotNull Player player) { return get(player).isExtreme(); }

    public static @NotNull ExtremePhysique getExtremePhysique(@NotNull Player player) {
        return get(player).extremePhysique();
    }

    public static @NotNull Race getRace(@NotNull Player player) { return get(player).race(); }

    public static long getGameTime(@NotNull Player player) { return player.level().getGameTime(); }

    private static void store(ServerPlayer player, BodyData data) {
        PlayerData.get(player).setBody(data);
    }

    //region lifespan and age [寿元与年龄] -- ⚠ every caller speaks YEARS; only this file knows parts
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
                .withTempZombieEndTick(getGameTime(player) + durationTicks)
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
                ? body.with(Physique.TEMP_ZOMBIE).withTempZombieEndTick(getGameTime(player) + ticksLeft)
                : body.without(Physique.TEMP_ZOMBIE).withTempZombieEndTick(BodyData.UNTRACKED);
        if (!next.equals(body)) storePhysique(player, next);
    }

    private static void storePhysique(ServerPlayer player, BodyData data) {
        store(player, data);
        ModEvents.post(new PhysiqueChangedEvent(player));
    }

    public static boolean isWithinRelapseWindow(@NotNull Player player) {
        return get(player).isWithinRelapseWindow(getGameTime(player));
    }

    public static long getTempZombieTicksLeft(@NotNull Player player) {
        return get(player).getTempZombieTicksLeft(getGameTime(player));
    }

    public static boolean hasTempZombieRunOut(@NotNull Player player) {
        return get(player).hasTempZombieRunOut(getGameTime(player));
    }
    //endregion

    //region Skin [皮肤]
    public static @NotNull Skin getSkin(@NotNull Player player) { return get(player).skin(); }

    public static void wearSkin(@NotNull ServerPlayer player, @NotNull Skin skin, int durationTicks) {
        long endTick = skin.isTimed() ? getGameTime(player) + durationTicks : BodyData.UNTRACKED;
        store(player, get(player).withSkin(skin, endTick));
        ModEvents.post(new SkinWornEvent(player, skin));
    }

    public static void shedSkin(@NotNull ServerPlayer player) {
        store(player, get(player).withSkin(Skin.NATIVE, BodyData.UNTRACKED));
        ModEvents.post(new SkinShedEvent(player));
    }

    public static boolean hasSkinRunOut(@NotNull Player player) {
        return get(player).hasSkinRunOut(getGameTime(player));
    }

    public static long getSkinTicksLeft(@NotNull Player player) {
        return get(player).getSkinTicksLeft(getGameTime(player));
    }
    //endregion

    //region Tendons and bones [筋骨] -- permanent, posted as one tempering so the attributes refresh
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
        PathEffect<Race> worn = PathEffectService.find(player, ModPathEffectTypes.RACE_TALENT);
        if (!race.isVariant()) {
            if (worn != null) PathEffectService.removeType(player, ModPathEffectTypes.RACE_TALENT);
            return;
        }
        if (worn == null || worn.payload() != race) PathEffectService.apply(player, getRaceTalent(race));
    }

    public static @NotNull PathEffect<Race> getRaceTalent(@NotNull Race race) {
        Map<GuPath, DaoMarks> paths = race.getTalentPath() == null ? Map.of()
                : Map.of(race.getTalentPath(), DaoMarks.ofMarks(Race.TALENT_MARKS));
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

    //region Death Qi [死气] debt
    public static void drainByDeathQi(@NotNull ServerPlayer player, long years) {
        BodyData body = get(player);
        BigInteger parts = BigInteger.valueOf(body.lifespanParts()).subtract(toYearParts(years));
        store(player, body.withLifespanParts(clampParts(parts))
                .withDeathQiLifespanLost(LongMath.saturatedAdd(body.deathQiLifespanLost(), years)));
    }

    public static double refundDeathQiDebt(@NotNull ServerPlayer player, int numerator, int denominator) {
        BodyData body = get(player);
        BigInteger refundParts = toYearParts(body.deathQiLifespanLost()).multiply(BigInteger.valueOf(numerator))
                .divide(BigInteger.valueOf(denominator));
        store(player, body.withLifespanParts(clampParts(BigInteger.valueOf(body.lifespanParts()).add(refundParts)))
                .withDeathQiLifespanLost(0L));
        return refundParts.doubleValue() / BodyData.PARTS_PER_YEAR;
    }

    public static void clearDeathQiDebt(@NotNull ServerPlayer player) {
        store(player, get(player).withDeathQiLifespanLost(0L));
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

    //region 寿元的钟 -- billed on the heartbeat, because 宙道 changes how fast he spends it
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
