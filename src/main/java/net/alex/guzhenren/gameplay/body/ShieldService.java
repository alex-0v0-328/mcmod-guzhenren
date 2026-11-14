package net.alex.guzhenren.gameplay.body;

import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 肉身分层护盾：每种 {@link ShieldType} 一层，过皮后先扣的独立血条。
 *
 * <ul>
 *   <li>{@link #raise} 满额置顶、重置计时，同种仍在的层被替换不叠加；点数先经 {@link ShieldRaisingEvent} 供上层特性改写
 *       （如石龟兽力[Beast Strength] ×1.5），所有护盾蛊通用。</li>
 *   <li>{@link #tickShield} 清超时层、同步标记效果；升起发 {@link ShieldRaisedEvent}，每层结束发
 *       {@link ShieldDownEvent}。</li>
 *   <li>{@link #guard} 在原版盾牌格挡处接皮放过的伤害，早于冰冻与头盔系数、无敌帧、护甲、抗性提升、保护、伤害吸收，
 *       故各层按新到旧扣原始伤害；击穿最后一层，余量作普通伤害放过。</li>
 *   <li>无敌帧内只扣超出该帧段已遇最大伤害的部分（{@link ShieldWindow}），岩浆每帧窗口只扣一次而非每 tick。</li>
 *   <li>全额吃下即取消并按格挡重演：无红闪与受伤音效，无敌帧照开；有来源位置（攻击者、投射物、爆炸）才放格挡音效，
 *       岩浆、火、中毒、溺水静默；近战攻击者击退 {@link #ATTACKER_KNOCKBACK}，肉身击退 {@link #DEFENDER_KNOCKBACK}，
 *       最新吃伤层为霸体则不退。</li>
 *   <li>真相在 {@code ShieldData}：{@code MobEffect} 无到期钩子，{@code /effect clear} 静默移除，故心跳重施，同
 *       {@link SkinService} 之于玉皮。</li>
 *   <li>⚠ 绕过无敌或效果的伤害（{@code /kill}、虚空、饥饿）与原版手持盾牌挡下的伤害原样通过。</li>
 *   <li>⚠ 帧内原版会再减自身 {@code lastHurt}，被取消的伤害遗留该值，故余量以 {@code lastHurt} 加余量交出。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see ShieldData
 * @since 1.0.0
 */

public final class ShieldService {

    private ShieldService() {}

    static final int FRAMES_OPEN_ABOVE = 10;
    static final double DEFENDER_KNOCKBACK = 0.4D;
    static final double ATTACKER_KNOCKBACK = 0.5D;

    public static @NotNull ShieldData get(@NotNull Player player) { return PlayerData.get(player).getShield(); }

    public static boolean isUp(@NotNull Player player) { return get(player).isUp(player.level().getGameTime()); }

    public static @Nullable ShieldLayer find(@NotNull Player player, @NotNull ShieldType type) {
        ShieldLayer layer = get(player).find(type);
        return layer != null && layer.isUp(player.level().getGameTime()) ? layer : null;
    }

    public static long getTicksLeft(@NotNull Player player, @NotNull ShieldType type) {
        ShieldLayer layer = find(player, type);
        return layer == null ? 0L : layer.getTicksLeft(player.level().getGameTime());
    }

    public static void raise(@NotNull ServerPlayer player, @NotNull ShieldType type, float points, int durationTicks) {
        ShieldRaisingEvent raising = new ShieldRaisingEvent(player, type, points);
        ModEvents.post(raising);
        float laid = Math.max(0.0F, raising.getPoints());
        long endTick = player.level().getGameTime() + durationTicks;
        PlayerData.get(player).setShield(get(player).raise(new ShieldLayer(type, laid, laid, endTick)));
        tickShield(player);
        ModEvents.post(new ShieldRaisedEvent(player, type));
    }

    /** 各层承接将落地的伤害并改写放过部分；true 表示调用方须取消（各层或无敌帧已全吃下）。 */
    public static boolean guard(@NotNull ServerPlayer player, @NotNull DamageContainer container) {
        DamageSource source = container.getSource();
        float hit = container.getNewDamage();
        if (hit <= 0.0F || !SkinService.canTurn(source) || player.isDamageSourceBlocked(source)) return false;

        long now = player.level().getGameTime();
        PlayerData data = PlayerData.get(player);
        ShieldWindow window = data.getShieldWindow();
        boolean framed = player.invulnerableTime > FRAMES_OPEN_ABOVE && !source.is(DamageTypeTags.BYPASSES_COOLDOWN);
        boolean tracked = framed && window.isOpen(now);
        boolean up = isUp(player);
        if (!up && !tracked) return false;

        float lastHurt = ((LastHurtView) player).guzhenren$getLastHurt();
        float prior = !framed ? 0.0F : tracked ? window.lastHit() : lastHurt;
        float due = Math.max(hit - prior, 0.0F);
        Spent spent = up ? spend(player, due) : Spent.NONE;
        float rest = due - spent.taken();
        int frames = framed ? player.invulnerableTime : container.getPostAttackInvulnerabilityTicks();
        // One tick past the frames: an entity ticking before the player meets them one tick later than one after.
        data.setShieldWindow(new ShieldWindow(Math.max(prior, hit), now + frames - FRAMES_OPEN_ABOVE + 1));
        if (rest > 0.0F) {
            container.setNewDamage(framed ? lastHurt + rest : rest);
            return false;
        }
        if (up) block(player, source, framed, spent.first(), container.getPostAttackInvulnerabilityTicks());
        return true;
    }

    public static void dismiss(@NotNull ServerPlayer player, @NotNull ShieldType type) {
        if (get(player).find(type) == null) return;

        PlayerData.get(player).setShield(get(player).without(type));
        player.removeEffect(type.getEffect());
        ModEvents.post(new ShieldDownEvent(player, type));
    }

    public static void tickShield(@NotNull ServerPlayer player) {
        long now = player.level().getGameTime();
        ShieldData shield = get(player);
        ShieldData up = shield.onlyUp(now);
        PlayerData.get(player).setShield(up);
        for (ShieldType type : ShieldType.values()) {
            ShieldLayer layer = up.find(type);
            BodyService.projectMarker(player, type.getEffect(), layer != null,
                    layer == null ? 0L : layer.getTicksLeft(now));
        }
        for (ShieldLayer layer : shield.layers()) {
            if (!layer.isUp(now)) ModEvents.post(new ShieldDownEvent(player, layer.type()));
        }
    }

    private static @NotNull Spent spend(@NotNull ServerPlayer player, float amount) {
        if (amount <= 0.0F) return Spent.NONE;

        long now = player.level().getGameTime();
        List<ShieldLayer> kept = new ArrayList<>();
        List<ShieldType> ended = new ArrayList<>();
        float left = amount;
        ShieldType first = null;
        for (ShieldLayer layer : get(player).layers()) {
            if (left <= 0.0F || !layer.isUp(now)) {
                kept.add(layer);
                continue;
            }
            float taken = Math.min(layer.points(), left);
            left -= taken;
            if (first == null) first = layer.type();
            if (layer.points() > taken) {
                kept.add(layer.withPoints(layer.points() - taken));
            } else {
                ended.add(layer.type());
            }
        }
        PlayerData.get(player).setShield(new ShieldData(kept));
        for (ShieldType type : ended) {
            player.removeEffect(type.getEffect());
            ModEvents.post(new ShieldDownEvent(player, type));
        }
        return new Spent(amount - left, first);
    }

    private static void block(ServerPlayer player, DamageSource source, boolean framed, @Nullable ShieldType first,
                              int frames) {
        if (source.getEntity() instanceof LivingEntity attacker && attacker != player
                && !source.is(DamageTypeTags.NO_ANGER)) {
            player.setLastHurtByMob(attacker);
            if (attacker instanceof Player by) player.setLastHurtByPlayer(by);
        }
        if (!source.is(DamageTypeTags.IS_PROJECTILE) && source.getDirectEntity() instanceof LivingEntity attacker
                && attacker != player) {
            attacker.knockback(ATTACKER_KNOCKBACK, player.getX() - attacker.getX(), player.getZ() - attacker.getZ());
            attacker.hurtMarked = true;
        }
        if (framed) return;

        player.invulnerableTime = frames;
        if (source.getSourcePosition() != null) player.level().broadcastEntityEvent(player, EntityEvent.ATTACK_BLOCKED);
        if (first == null || first.isSuperArmor() || source.is(DamageTypeTags.NO_KNOCKBACK)) return;

        double x = 0.0D;
        double z = 0.0D;
        if (source.getDirectEntity() instanceof Projectile projectile) {
            DoubleDoubleImmutablePair direction = projectile.calculateHorizontalHurtKnockbackDirection(player, source);
            x = -direction.leftDouble();
            z = -direction.rightDouble();
        } else if (source.getSourcePosition() != null) {
            x = source.getSourcePosition().x() - player.getX();
            z = source.getSourcePosition().z() - player.getZ();
        }
        player.knockback(DEFENDER_KNOCKBACK, x, z);
        if (!source.is(DamageTypeTags.NO_IMPACT)) player.hurtMarked = true;
    }

    private record Spent(float taken, @Nullable ShieldType first) {

        static final Spent NONE = new Spent(0.0F, null);
    }
}
