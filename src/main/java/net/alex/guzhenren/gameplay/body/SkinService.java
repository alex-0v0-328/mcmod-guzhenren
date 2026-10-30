package net.alex.guzhenren.gameplay.body;

import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The skin [皮肤] heartbeat and its damage rule. {@link #tickSkin} sheds a jade skin whose time has run out and
 * projects the one still worn as its Gu's effect ({@link #getEffectOf}), so the icon and timer read on the HUD;
 * a permanent bronze or iron skin has no timer and projects nothing, it reads on the body panel.
 * {@link #reduceDamage} takes any skin's flat reduction off a hit the skin can turn; a jade skin whose time ran
 * out turns nothing in the second before the heartbeat sheds it.
 *
 * <p>The truth is {@code BodyData.skin} with its end tick, never the effect: a {@code MobEffect} has no expiry
 * hook, and {@code /effect clear} removes it silently, so the heartbeat re-applies it the way
 * {@link UndeadService} projects the temp zombie form. ⚠ A skin gives way to nothing: damage that bypasses
 * invulnerability or effects ({@code /kill}, the void, starvation) passes untouched, like vanilla Resistance.
 *
 * @author Alex
 * @version 1.0.0
 * @see BodyService
 * @see Skin
 * @since 1.0.0
 */

public final class SkinService {

    private SkinService() {}

    public static void tickSkin(@NotNull ServerPlayer player) {
        if (BodyService.hasSkinRunOut(player)) BodyService.shedSkin(player);
        projectSkin(player);
    }

    private static void projectSkin(ServerPlayer player) {
        Skin worn = BodyService.getSkin(player);
        for (Skin skin : Skin.values()) {
            Holder<MobEffect> effect = getEffectOf(skin);
            if (effect == null) continue;

            if (skin == worn) {
                player.addEffect(ModEffects.instance(effect, Math.max(1, (int) BodyService.getSkinTicksLeft(player))));
            } else if (player.hasEffect(effect)) {
                player.removeEffect(effect);
            }
        }
    }

    public static @Nullable Holder<MobEffect> getEffectOf(@NotNull Skin skin) {
        return switch (skin) {
            case JADE_SKIN -> ModEffects.JADE_SKIN_GU;
            case WHITE_JADE_SKIN -> ModEffects.WHITE_JADE_GU;
            default -> null;
        };
    }

    public static float reduceDamage(@NotNull Player player, @NotNull DamageSource source, float amount) {
        Skin skin = BodyService.getSkin(player);
        if (skin.isNative() || BodyService.hasSkinRunOut(player) || !canTurn(source)) return amount;
        return skin.reduce(amount);
    }

    static boolean canTurn(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.is(DamageTypeTags.BYPASSES_EFFECTS);
    }
}
