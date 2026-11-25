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
 * 肉身分层护盾服务：管理各 {@link ShieldType} 护盾层的升起、到期清理与受击格挡结算。
 * <ul>
 *   <li>{@link #raise} 将新护盾层满额置顶并重置计时（同种旧层直接替换、不叠加）；初始点数先经
 *       {@link ShieldRaisingEvent} 供上层特性缩放（如石龟兽力[Beast Strength] ×1.5），落盘后发布
 *       {@link ShieldRaisedEvent}。</li>
 *   <li>{@link #tickShield} 在心跳中清理超时护盾层、同步状态栏标记效果，并为每个新到期的层发布
 *       {@link ShieldDownEvent}；真实状态唯一存于 {@link ShieldData}，防止 {@code /effect clear} 静默清掉护盾。</li>
 *   <li>{@link #guard} 在皮肤固定减伤之后、原版冰冻/头盔系数、无敌帧、护甲与抗性结算之前承接伤害，按由新到旧顺序扣减各
 *       层护盾；击穿所有层后将剩余伤害放行给原版后续结算。</li>
 *   <li>无敌帧窗口（{@link ShieldWindow}）内仅扣减超出该帧窗口已承受最大伤害的差额，使岩浆等每 tick 伤害在单次无敌帧内
 *       只扣一次护盾；窗口到期刻在无敌帧结束后多留 1 tick，以便先于玩家 tick 的实体能对齐同一无敌帧。</li>
 *   <li>全额挡下伤害时取消事件并模拟原版盾牌格挡表现：不闪红、不播受伤音，照常开启受击无敌帧；带空间来源的攻击播放格挡
 *       音效并按 {@link #ATTACKER_KNOCKBACK} 击退近战攻击者，非霸体护盾自身承受 {@link #DEFENDER_KNOCKBACK} 击退（环境
 *       持续伤害静默吸收）。</li>
 *   <li>⚠ 穿透无敌或药水效果的伤害（{@code /kill}、虚空、饥饿）以及已被原版手持盾牌挡下的伤害直接跳过护盾。</li>
 *   <li>⚠ 在受击无敌帧内击穿护盾放行余量时，因原版后续会再减去实体自身的 {@code lastHurt}，必须将放行伤害写为
 *       {@code lastHurt + rest}（见 {@link LastHurtView}），否则穿透伤害会被原版二次抵扣。</li>
 * </ul>
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
