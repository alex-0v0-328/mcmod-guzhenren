package net.alex.guzhenren.gameplay.body;

import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.mixin.LivingEntityAccessor;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
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
 * The shields [护盾] over the body: layered, one per {@link ShieldType}, spent like a separate health bar before the
 * hit reaches anything past the skin.
 *
 * <p>{@link #raise} lays a kind's layer on top, full, its timer restarted; a layer of that kind still up is
 * replaced, never stacked. {@link #guard} meets every hit the skin passed at the slot where vanilla checks a shield
 * block, before the freeze and helmet factors, the invulnerability frames, armor, Resistance, Protection and
 * Absorption, so the layers spend raw damage, newest first. Inside the frames it spends only on what exceeds the
 * largest hit those frames already met ({@link ShieldWindow}), so lava drains it once per frame window, not every
 * tick. A hit the layers take whole is cancelled and replayed as a block: no red flash or hurt sound, the frames
 * started, the shield-block sound when the hit has a source position (an attacker, a projectile, an explosion; lava,
 * fire, poison and drowning are taken silently), a melee attacker knocked {@link #ATTACKER_KNOCKBACK} back, and the
 * body knocked {@link #DEFENDER_KNOCKBACK} back unless the newest layer that took the hit is super armor [霸体]. A
 * hit that breaks the last layer passes the rest on as an ordinary hit. {@link #tickShield} drops the layers whose
 * time ran out and keeps each kind's marker effect in step. Raising posts {@link ShieldRaisedEvent} and the end of
 * each layer {@link ShieldDownEvent}.
 *
 * <p>The truth is {@code ShieldData}, never the effects: a {@code MobEffect} has no expiry hook and
 * {@code /effect clear} removes it silently, so the heartbeat re-applies them as {@link SkinService} does a jade
 * skin. ⚠ Damage that bypasses invulnerability or effects ({@code /kill}, the void, starvation) passes untouched,
 * and so does a hit a held vanilla shield blocks. ⚠ Inside the frames vanilla subtracts its own {@code lastHurt}
 * from what passes, and a cancelled hit leaves that value behind, so the rest is handed on as {@code lastHurt}
 * plus the rest.
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

    public static boolean isUp(@NotNull Player player) { return get(player).isUp(BodyService.getGameTime(player)); }

    public static @Nullable ShieldLayer find(@NotNull Player player, @NotNull ShieldType type) {
        ShieldLayer layer = get(player).find(type);
        return layer != null && layer.isUp(BodyService.getGameTime(player)) ? layer : null;
    }

    public static long getTicksLeft(@NotNull Player player, @NotNull ShieldType type) {
        ShieldLayer layer = find(player, type);
        return layer == null ? 0L : layer.getTicksLeft(BodyService.getGameTime(player));
    }

    public static void raise(@NotNull ServerPlayer player, @NotNull ShieldType type, float points, int durationTicks) {
        long endTick = BodyService.getGameTime(player) + durationTicks;
        PlayerData.get(player).setShield(get(player).raise(new ShieldLayer(type, points, points, endTick)));
        tickShield(player);
        ModEvents.post(new ShieldRaisedEvent(player, type));
    }

    /**
     * Spends the layers on a hit about to land and rewrites what passes; true when the caller must cancel the hit,
     * the layers or the frames having taken all of it.
     */
    public static boolean guard(@NotNull ServerPlayer player, @NotNull DamageContainer container) {
        DamageSource source = container.getSource();
        float hit = container.getNewDamage();
        if (hit <= 0.0F || !SkinService.canTurn(source) || player.isDamageSourceBlocked(source)) return false;

        long now = BodyService.getGameTime(player);
        PlayerData data = PlayerData.get(player);
        ShieldWindow window = data.getShieldWindow();
        boolean framed = player.invulnerableTime > FRAMES_OPEN_ABOVE && !source.is(DamageTypeTags.BYPASSES_COOLDOWN);
        boolean tracked = framed && window.isOpen(now);
        boolean up = isUp(player);
        if (!up && !tracked) return false;

        float lastHurt = ((LivingEntityAccessor) player).guzhenren$getLastHurt();
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
        long now = BodyService.getGameTime(player);
        ShieldData shield = get(player);
        ShieldData up = shield.onlyUp(now);
        PlayerData.get(player).setShield(up);
        for (ShieldType type : ShieldType.values()) {
            ShieldLayer layer = up.find(type);
            Holder<MobEffect> effect = type.getEffect();
            if (layer != null) {
                player.addEffect(ModEffects.instance(effect, Math.max(1, (int) layer.getTicksLeft(now))));
            } else if (player.hasEffect(effect)) {
                player.removeEffect(effect);
            }
        }
        for (ShieldLayer layer : shield.layers()) {
            if (!layer.isUp(now)) ModEvents.post(new ShieldDownEvent(player, layer.type()));
        }
    }

    private static @NotNull Spent spend(@NotNull ServerPlayer player, float amount) {
        if (amount <= 0.0F) return Spent.NONE;

        long now = BodyService.getGameTime(player);
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
