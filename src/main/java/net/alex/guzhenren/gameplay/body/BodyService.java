package net.alex.guzhenren.gameplay.body;

import com.google.common.math.LongMath;
import java.math.BigInteger;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.clock.Beat;
import net.alex.guzhenren.gameplay.clock.TimeFlow;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.MarkTag;
import net.alex.guzhenren.gameplay.path.PathService;
import net.alex.guzhenren.registry.attachment.ModAttachments;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;

/**
 * The only runtime writer of Body [肉身] state, and the home of every physique [体质] transition. Static
 * service; owns two clocks with two anchors: {@link #tickAging} keeps {@code lastDayIndex} and counts
 * DAYS for the three Gu-hunger walks and nothing else; {@link #tickLifespan} keeps
 * {@code lastBilledTick} and bills lifespan every heartbeat instead of once a day. Lifespan bills online time
 * only: {@link #skipOfflineLifespan} un-anchors the bill on login, so the first heartbeat re-anchors and bills
 * nothing for the time away, while the day clock still counts the days away for the Gu-hunger walks.
 *
 * <p>⚠ {@code tickAging}'s returned day count can far exceed one and drives three day-clock walks --
 * swallowing it starves every Gu at once. ☠ 寿元 is SPENT through {@link TimeFlow#scale}
 * like every other thing he spends -- hastened means FASTER; a hand-rolled rate runs it
 * BACKWARDS, into a pure longevity buff. ⚠ The anchor is {@code dayTime}, not {@code gameTime} (so
 * {@code /time add} still ages him): time running backwards re-anchors and bills nothing -- one
 * {@code <} is the whole guard, BOTH clocks share it.
 *
 * <p>In {@link #clampParts}, the intermediate {@code BigInteger} products are kept exact rather than
 * clamped early, because a later signed addition or fraction can bring them back into range.
 *
 * <p>Physique and undead writes go through {@link #storePhysique}, which posts a {@link PhysiqueChangedEvent}
 * (the attack modifier refreshes);
 * {@link #setExtremePhysique} posts an {@link ExtremePhysiqueChangedEvent} (the aperture reconciles its
 * talent paths, base essence and pressure). ⚠ {@code setExtremePhysique} does not check awakening: a
 * caller setting a physique other than {@code NONE} gates on it -- the command through
 * {@code AwakenedGate.AWAKENED}, awakening by opening the aperture first. ⚠ A half-zombie form that runs
 * out keeps its end tick: a Zombie Gu used within {@code RELAPSE_WINDOW_TICKS} after it turns the body
 * permanently 僵 ({@link #isWithinRelapseWindow}); only {@link #revive} clears it. ⚠ Lifespan across the
 * three stages, human, half-zombie and zombie: entering half-zombie freezes it (an undead body is never billed)
 * and leaving it only thaws it; turning fully 僵, from a human or from half-zombie, sets it to
 * {@code ZOMBIE_LIFESPAN}, and that year stays when the form ends.
 *
 * @author Alex
 * @version 1.0.0
 * @see TimeFlow
 * @see PhysiqueChangedEvent
 * @since 1.0.0
 */

public final class BodyService {

    private BodyService() {}

    public static long getDayIndex(@NotNull MinecraftServer server) {
        return server.overworld().getDayTime() / Ticks.DAY;
    }

    public static @NotNull BodyData get(@NotNull Player player) { return player.getData(ModAttachments.BODY); }

    public static boolean isZombie(@NotNull Player player) { return get(player).isZombie(); }

    public static boolean isHalfZombie(@NotNull Player player) { return get(player).isHalfZombie(); }

    public static boolean isZombieOrHalfZombie(@NotNull Player player) { return get(player).isZombieOrHalfZombie(); }

    public static boolean isUndead(@NotNull Player player) { return isZombieOrHalfZombie(player); }

    public static boolean isExtreme(@NotNull Player player) { return get(player).isExtreme(); }

    public static @NotNull ExtremePhysique getExtremePhysique(@NotNull Player player) {
        return get(player).extremePhysique();
    }

    public static @NotNull Race getRace(@NotNull Player player) { return get(player).race(); }

    public static long getGameTime(@NotNull Player player) { return player.level().getGameTime(); }

    private static void store(ServerPlayer player, BodyData data) { player.setData(ModAttachments.BODY, data); }

    //region lifespan and age [寿元与年龄] -- ⚠ every caller speaks YEARS; only this file knows parts
    public static void setAge(@NotNull ServerPlayer player, long years) {
        store(player, get(player).withAgeParts(BodyData.parts(years)));
    }

    public static void setLifespan(@NotNull ServerPlayer player, long years) {
        store(player, get(player).withLifespanParts(BodyData.parts(years)));
    }

    public static void addAge(@NotNull ServerPlayer player, long years) {
        BigInteger parts = BigInteger.valueOf(get(player).ageParts()).add(yearParts(years));
        store(player, get(player).withAgeParts(clampParts(parts)));
    }

    public static void addLifespan(@NotNull ServerPlayer player, long years) {
        BigInteger parts = BigInteger.valueOf(get(player).lifespanParts()).add(yearParts(years));
        store(player, get(player).withLifespanParts(clampParts(parts)));
    }

    private static BigInteger yearParts(long years) {
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

        BodyData updated = (physique == Physique.HALF_ZOMBIE ? body.without(Physique.ZOMBIE) : body).with(physique);
        if (physique == Physique.ZOMBIE) updated = updated.withLifespanParts(BodyData.parts(BodyData.ZOMBIE_LIFESPAN));
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
        NeoForge.EVENT_BUS.post(new ExtremePhysiqueChangedEvent(player, before, physique));
    }

    public static void revive(@NotNull ServerPlayer player) { storePhysique(player, get(player).revived()); }

    public static void enterHalfZombie(@NotNull ServerPlayer player, int tier, int durationTicks) {
        storePhysique(player, get(player).without(Physique.ZOMBIE).with(Physique.HALF_ZOMBIE)
                .withHalfZombieEndTick(getGameTime(player) + durationTicks)
                .withZombieTier(tier));
    }

    public static void turnZombie(@NotNull ServerPlayer player, int tier) {
        storePhysique(player, get(player).with(Physique.ZOMBIE)
                .withLifespanParts(BodyData.parts(BodyData.ZOMBIE_LIFESPAN))
                .withZombieTier(tier));
    }

    private static void storePhysique(ServerPlayer player, BodyData data) {
        store(player, data);
        NeoForge.EVENT_BUS.post(new PhysiqueChangedEvent(player));
    }

    public static boolean isWithinRelapseWindow(@NotNull Player player) {
        return get(player).withinRelapseWindow(getGameTime(player));
    }

    public static long getHalfZombieTicksLeft(@NotNull Player player) {
        return get(player).halfZombieTicksLeft(getGameTime(player));
    }

    public static boolean hasHalfZombieRunOut(@NotNull Player player) {
        return get(player).halfZombieRanOut(getGameTime(player));
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

    private static void grantTalent(ServerPlayer player, Race race) {
        GuPath path = race.talentPath();
        if (path == null) return;

        PathService.setMark(player, path, MarkTag.RACE, Race.TALENT_MARKS);
        PathService.shiftAttainment(player, path, Race.TALENT_SHIFT);
    }

    private static void revokeTalent(ServerPlayer player, Race race) {
        GuPath path = race.talentPath();
        if (path == null) return;

        PathService.setMark(player, path, MarkTag.RACE, 0L);
        PathService.shiftAttainment(player, path, -Race.TALENT_SHIFT);
    }
    //endregion

    //region Death Qi [死气] debt
    public static void drainByDeathQi(@NotNull ServerPlayer player, long years) {
        BodyData body = get(player);
        BigInteger parts = BigInteger.valueOf(body.lifespanParts()).subtract(yearParts(years));
        store(player, body.withLifespanParts(clampParts(parts))
                .withDeathQiLifespanLost(LongMath.saturatedAdd(body.deathQiLifespanLost(), years)));
    }

    public static double refundDeathQiDebt(@NotNull ServerPlayer player, int numerator, int denominator) {
        BodyData body = get(player);
        BigInteger refundParts = yearParts(body.deathQiLifespanLost()).multiply(BigInteger.valueOf(numerator))
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
                || body.isZombieOrHalfZombie()) {
            store(player, body.withLastBilledTick(now));
            return;
        }
        long lived = beat.scale(getElapsedParts(now - body.lastBilledTick()));
        if (lived <= 0L) return;

        store(player, body.lived(lived, now));
    }

    public static void skipOfflineLifespan(@NotNull ServerPlayer player) {
        store(player, get(player).withLastBilledTick(BodyData.UNTRACKED));
    }

    public static long getElapsedParts(long elapsedTicks) {
        return LongMath.saturatedMultiply(Math.max(0L, elapsedTicks), BodyData.PARTS_PER_TICK);
    }
    //endregion
}
