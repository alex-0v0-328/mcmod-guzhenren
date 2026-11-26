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
 * 玩家肉身数据（{@link BodyData}）的唯一运行时写入服务：统一管理寿元与年龄时钟、体质转换、筋骨皮肉淬炼、解剖伤势与种族
 * 天赋。
 * <ul>
 *   <li>{@link #tickAging} 锚定 {@code lastDayIndex}，仅用于驱动三处蛊虫每日饱食[Hunger]结算；{@link #tickLifespan} 锚
 *       定 {@code lastBilledTick}，每次心跳扣减在线寿元（{@link #skipOfflineLifespan} 在登录时解锚以跳过离线时长，而日
 *       钟照常计入离线跨日）。</li>
 *   <li>⚠ {@link #tickAging} 返回的跨日天数在长离线或 {@code /time add} 后可能远大于 {@code 1}，调用方必须按返回天数完
 *       整结算，吞掉返回值会让蛊虫漏扣饱食。</li>
 *   <li>⚠ 寿元消耗经 {@link Beat#scale}（底层 {@link TimeFlow#scale}）按自身时间倍率缩放：自身时间加速时寿元流逝同步加
 *       快，切勿反写除法或负号以免变成倒流延寿。</li>
 *   <li>⚠ 两个时间锚点均取主世界 {@code dayTime}（而非 {@code gameTime}，使 {@code /time add} 同样推算年龄与寿元）；当
 *       时间倒退（{@code today < lastDayIndex} 或 {@code now < lastBilledTick}）时仅重新锚定而不扣减。</li>
 *   <li>⚠ 外部调用方一律以「年」为单位传参，寿元「份」数仅在本类与 {@link BodyData} 内部换算；{@link #clampParts} 使用
 *       {@link BigInteger} 计算中间乘积后再夹取到 {@code long}，防止大数加减提前溢出截断。</li>
 *   <li>⚠ {@link #setExtremePhysique} 不校验玩家是否已开窍[Awakening]，赋非 {@code NONE}
 *       的十绝体[Ten Extreme Physiques]时由调用方先行把关（管理指令经 {@code AwakenedGate.AWAKENED} 拦截，开窍仪式先开
 *       空窍再赋体质）。</li>
 *   <li>⚠ 临时僵尸化到期摘除 {@link Physique#TEMPORARY_ZOMBIE} 时仍保留 {@code temporaryZombieEndTick}：到期后
 *       {@link BodyData#RELAPSE_WINDOW_TICKS}（5 分钟）内若再次使用僵尸类蛊（{@link #isWithinRelapseWindow}），会直接恶
 *       化为永久僵尸；该时间戳仅在 {@link #revive} 时清除。进入临时僵尸或永久僵尸期间寿元冻结不扣，转为永久僵尸时寿元设
 *       为 {@link BodyData#ZOMBIE_LIFESPAN}。</li>
 *   <li>限时皮肤（玉皮、白玉皮）的到期时刻按 {@code gameTime} 计，由 {@link SkinService} 在心跳中检查脱下，并在
 *       {@link #revive} 时随不死形态一并褪去；永久皮肤与筋、骨、肉无到期时间，重生保留。</li>
 *   <li>肢体与七窍伤势及附加部位当前仅由管理指令读写，暂不发布事件。</li>
 *   <li>⚠ {@link #setRace} 是现有两处跨领域授予之一（另一处为 {@code ApertureService.reconcileTalentPaths}）：切换异人
 *       种族时撤销旧种族天赋并授予新种族天赋，通过 {@link PathService#shiftAttainment} 平移对应流派造诣，并以
 *       {@link #RACE_TALENT_SOURCE} 挂载 10 条整道痕[Path Marks]的永久 {@code RACE_TALENT} 肉身流派效果（登录缺失或种族
 *       不符时由 {@link #syncRaceTalent} 补齐，但不重复平移造诣）。</li>
 *   <li>本类作为 {@link BodyData} 全部字段族的唯一写入入口，按功能分 {@code //region} 组织在同一文件中（超 300
 *       行）。</li>
 * </ul>
 */

public final class BodyService {

    private BodyService() {}

    public static final ResourceLocation RACE_TALENT_SOURCE = Guzhenren.id("innate/race");

    private static long getDayIndex(@NotNull MinecraftServer server) {
        return server.overworld().getDayTime() / Ticks.DAY;
    }

    public static @NotNull BodyData get(@NotNull Player player) { return PlayerData.get(player).getBody(); }

    public static boolean isZombie(@NotNull Player player) { return get(player).isZombie(); }

    public static boolean isTemporaryZombie(@NotNull Player player) { return get(player).isTemporaryZombie(); }

    public static boolean isUndead(@NotNull Player player) { return get(player).isZombieOrTemporaryZombie(); }

    public static boolean isExtreme(@NotNull Player player) { return get(player).isExtreme(); }

    public static @NotNull ExtremePhysique getExtremePhysique(@NotNull Player player) {
        return get(player).extremePhysique();
    }

    private static @NotNull Race getRace(@NotNull Player player) { return get(player).race(); }

    private static void store(ServerPlayer player, BodyData data) {
        PlayerData.get(player).setBody(data);
    }

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

        BodyData updated = (physique == Physique.TEMPORARY_ZOMBIE ? body.without(Physique.ZOMBIE) : body)
                .with(physique);
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

    public static void enterTemporaryZombie(@NotNull ServerPlayer player, int tier, int durationTicks) {
        storePhysique(player, get(player).without(Physique.ZOMBIE).with(Physique.TEMPORARY_ZOMBIE)
                .withTemporaryZombieEndTick(player.level().getGameTime() + durationTicks)
                .withZombieTier(tier));
    }

    public static void turnZombie(@NotNull ServerPlayer player, int tier) {
        storePhysique(player, get(player).with(Physique.ZOMBIE)
                .withLifespanParts(BodyData.toParts(BodyData.ZOMBIE_LIFESPAN))
                .withZombieTier(tier));
    }

    public static void restoreTemporaryZombie(@NotNull ServerPlayer player, long ticksLeft) {
        BodyData body = get(player);
        if (body.isZombie()) return;

        BodyData next = ticksLeft > 0L
                ? body.with(Physique.TEMPORARY_ZOMBIE)
                        .withTemporaryZombieEndTick(player.level().getGameTime() + ticksLeft)
                : body.without(Physique.TEMPORARY_ZOMBIE).withTemporaryZombieEndTick(BodyData.UNTRACKED);
        if (!next.equals(body)) storePhysique(player, next);
    }

    private static void storePhysique(ServerPlayer player, BodyData data) {
        store(player, data);
        ModEvents.post(new PhysiqueChangedEvent(player));
    }

    public static boolean isWithinRelapseWindow(@NotNull Player player) {
        return get(player).isWithinRelapseWindow(player.level().getGameTime());
    }

    public static long getTemporaryZombieTicksLeft(@NotNull Player player) {
        return get(player).getTemporaryZombieTicksLeft(player.level().getGameTime());
    }

    public static boolean hasTemporaryZombieRunOut(@NotNull Player player) {
        return get(player).hasTemporaryZombieRunOut(player.level().getGameTime());
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

    //region Limbs and orifices [肢体与七窍]
    public static @NotNull Anatomy getAnatomy(@NotNull Player player) { return get(player).anatomy(); }

    public static void setInjury(@NotNull ServerPlayer player, @NotNull BodyPart part, @NotNull Injury injury) {
        BodyData body = get(player);
        store(player, body.withAnatomy(body.anatomy().withInjury(part, injury)));
    }

    public static void setInjury(@NotNull ServerPlayer player, @NotNull Organ organ, @NotNull Injury injury) {
        BodyData body = get(player);
        store(player, body.withAnatomy(body.anatomy().withInjury(organ, injury)));
    }

    public static void healInjuries(@NotNull ServerPlayer player) {
        BodyData body = get(player);
        store(player, body.withAnatomy(body.anatomy().withoutInjuries()));
    }

    public static void setAppendage(@NotNull ServerPlayer player, @NotNull Appendage appendage, boolean present) {
        BodyData body = get(player);
        store(player, body.withAnatomy(body.anatomy().withAppendage(appendage, present)));
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
                || body.isZombieOrTemporaryZombie()) {
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
