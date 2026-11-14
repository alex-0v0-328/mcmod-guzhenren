package net.alex.guzhenren.gameplay.aperture.awakening;

import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Talent;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 开窍[Awakening]仪式：希望蛊[Hope Gu]单击到开出第一空窍[Aperture]的唯一路径；独写 {@link AwakeningRitual}。
 * <ul>
 *   <li>{@link #start} 存仪式时已掷定资质[Aptitude]、十绝体，悬停点在眼前 {@link #ANCHOR_DISTANCE} 格；打断只截动画、
 *       开同一空窍，重登不能重掷。</li>
 *   <li>{@link #tick} 按 {@link AwakeningTimeline} 开窍、揭晓（全服播报、本人揭晓音）、结束；
 *       客户端凭同步仪式画牌匾。</li>
 *   <li>{@link #holdsStill} 为两端共读定身：客户端锁输入期间，服务端拒冲刺与温养[Nourish]。</li>
 *   <li>{@link #finish} 立即结束，补未发生的开窍和播报；到点、死亡、换维度、登出、登录遇崩溃遗留皆走此。</li>
 *   <li>开窍发 {@link ApertureAwakenedEvent}：希望蛊进度只认仪式。</li>
 *   <li>{@link #isInDanger} 是单击前门槛：5 秒内被他者攻击（原版留攻击者这么久，自己弹射物除外），或水平
 *       {@link #DANGER_REACH} 格、上下 {@link #DANGER_HEIGHT} 格内有阻止休息的怪物；范围、判定、创造模式跳过皆同
 *       {@code ServerPlayer.startSleepInBed}。</li>
 *   <li>⚠ {@code ApertureService.awaken} 不拒已开窍者，由仪式拒：仪式中被命令开窍则留命令的空窍，蛊照耗；播报当时资质，
 *       牌匾仍画掷出值；重置（{@code PlayerDataService.resetAll}）清仪式不开窍。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see AwakeningTimeline
 * @see AwakeningRitualEvents
 * @since 1.0.0
 */

public final class AwakeningRitualService {

    private AwakeningRitualService() {}

    public static final double ANCHOR_DISTANCE = 1.5;
    public static final double DANGER_REACH = 8.0;
    public static final double DANGER_HEIGHT = 5.0;
    public static final String ANNOUNCEMENT = "guzhenren.chat.awakened";
    public static final String APTITUDE = "guzhenren.chat.awakened_aptitude";
    public static final String GRADE_PREFIX = "guzhenren.display.talent_grade.";
    private static final SoundEvent START_SOUND = SoundEvents.BEACON_ACTIVATE;
    private static final SoundEvent AWAKEN_SOUND = SoundEvents.ENCHANTMENT_TABLE_USE;
    private static final SoundEvent REVEAL_SOUND = SoundEvents.PLAYER_LEVELUP;
    private static final float START_PITCH = 1.2F;

    public static @Nullable AwakeningRitual get(@NotNull Player player) {
        return PlayerData.get(player).getAwakeningRitual();
    }

    public static boolean isInRitual(@NotNull Player player) { return get(player) != null; }

    public static boolean holdsStill(@NotNull Player player) {
        AwakeningRitual ritual = get(player);
        return ritual != null && AwakeningTimeline.holdsStill(ritual.getElapsed(player.level().getGameTime(), 0.0F));
    }

    public static boolean isInDanger(@NotNull Player player) {
        if (player.isCreative()) return false;
        LivingEntity attacker = player.getLastHurtByMob();
        if (attacker != null && attacker != player) return true;
        Vec3 feet = player.position();
        AABB area = new AABB(feet.x - DANGER_REACH, feet.y - DANGER_HEIGHT, feet.z - DANGER_REACH,
                feet.x + DANGER_REACH, feet.y + DANGER_HEIGHT, feet.z + DANGER_REACH);
        return !player.level().getEntitiesOfClass(Monster.class, area,
                monster -> monster.isPreventingPlayerRest(player)).isEmpty();
    }

    public static void start(@NotNull ServerPlayer player, int base) {
        ExtremePhysique physique = Talent.fromPercent(base) == Talent.TEN_EXTREMES
                ? ExtremePhysique.randomTenExtreme() : ExtremePhysique.NONE;
        Vec3 anchor = player.getEyePosition().add(player.getLookAngle().scale(ANCHOR_DISTANCE));
        PlayerData.get(player).setAwakeningRitual(
                new AwakeningRitual(player.level().getGameTime(), base, physique, anchor, false));
        playSound(player, START_SOUND, START_PITCH);
    }

    public static void tick(@NotNull ServerPlayer player) {
        AwakeningRitual ritual = get(player);
        if (ritual == null) return;
        float elapsed = ritual.getElapsed(player.level().getGameTime(), 0.0F);
        if (elapsed >= AwakeningTimeline.END_TICKS) {
            finish(player);
            return;
        }
        if (elapsed >= AwakeningTimeline.AWAKEN_TICKS) awaken(player, ritual);
        if (elapsed >= AwakeningTimeline.RELEASE_TICKS && !ritual.revealed()) {
            PlayerData.get(player).setAwakeningRitual(ritual.reveal());
            announce(player);
            player.playNotifySound(REVEAL_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    public static void finish(@NotNull ServerPlayer player) {
        AwakeningRitual ritual = get(player);
        if (ritual == null) return;
        PlayerData.get(player).setAwakeningRitual(null);
        awaken(player, ritual);
        if (!ritual.revealed()) announce(player);
    }

    /** 播报用资质[Aptitude]：等级，十绝报十绝体。 */
    public static @NotNull Component getAptitudeName(@NotNull Player player) {
        Talent talent = ApertureService.getTalent(player);
        return talent == Talent.TEN_EXTREMES
                ? Component.translatable(BodyService.getExtremePhysique(player).getTranslationKey())
                : Component.translatable(APTITUDE, Component.translatable(GRADE_PREFIX + talent.getSerializedName()),
                Talent.formatPercent(ApertureService.getAperture(player).baseEssence()));
    }

    private static void awaken(ServerPlayer player, AwakeningRitual ritual) {
        if (ApertureService.isAwakened(player)) return;

        ApertureService.awaken(player, ritual.base(), ritual.physique());
        ModEvents.post(new ApertureAwakenedEvent(player));
        player.server.getCommands().sendCommands(player);
        playSound(player, AWAKEN_SOUND, 1.0F);
    }

    private static void announce(ServerPlayer player) {
        player.server.getPlayerList().broadcastSystemMessage(
                Component.translatable(ANNOUNCEMENT, player.getDisplayName(), getAptitudeName(player)), false);
    }

    private static void playSound(ServerPlayer player, SoundEvent sound, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS,
                1.0F, pitch);
    }
}
