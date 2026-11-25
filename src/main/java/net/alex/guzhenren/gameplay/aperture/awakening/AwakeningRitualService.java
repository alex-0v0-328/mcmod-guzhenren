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
 * 开窍[Awakening]仪式服务：希望蛊[Hope Gu]右键开启第一空窍的唯一业务路径，独家写入 {@link AwakeningRitual}。
 * <ul>
 *   <li>{@link #start} 启动仪式时即预先掷定资质与十绝体[Ten Extreme Physiques]，并将悬停锚点设在眼前
 *       {@link #ANCHOR_DISTANCE} 格；中途被打断或重登只会跳过剩余动画并落地同一结果，无法通过强退重掷。</li>
 *   <li>{@link #tick} 按 {@link AwakeningTimeline} 依次执行开窍、揭晓（全服播报与本人提示音，{@link #getAptitudeName}
 *       对十绝天资直接返回十绝体名称）与结束清理；客户端依据同步的仪式附件渲染特效与牌匾。</li>
 *   <li>{@link #holdsStill} 供双端共读定身状态：客户端锁定移动与视角输入，服务端拒绝冲刺与温养[Nourish]。</li>
 *   <li>{@link #finish} 立即移除仪式附件并补齐尚未执行的开窍与全服播报；正常到期、死亡、跨维度、登出以及崩溃后重登均走
 *       此收尾。</li>
 *   <li>开窍落盘时发布 {@link ApertureAwakenedEvent}，希望蛊进度仅由仪式触发。</li>
 *   <li>{@link #isInDanger} 为右键起手前的安全门槛：近 5 秒内受过非自身生物攻击，或水平 {@link #DANGER_REACH} 格、垂直
 *       {@link #DANGER_HEIGHT} 格内存在妨碍入睡的怪物时拒绝开窍（范围、判定与创造模式豁免均对齐
 *       {@code ServerPlayer.startSleepInBed}）。</li>
 *   <li>⚠ {@link ApertureService#awaken} 本身不拦截已开窍玩家，由本服务在起手与落盘时把关：若仪式中途被管理员命令提前
 *       开窍，仪式不再覆盖该空窍（希望蛊已消耗），揭晓播报按当前实际资质读取，而客户端牌匾仍按仪式预掷值绘制；角色重置
 *       会直接清除仪式附件而不触发开窍。</li>
 * </ul>
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
