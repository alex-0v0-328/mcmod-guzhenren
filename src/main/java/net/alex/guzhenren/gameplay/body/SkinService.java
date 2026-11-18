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
 * 肉身皮肤的心跳维护与固定减伤服务。
 * <ul>
 *   <li>{@link #tickSkin} 在心跳中脱下已到期的限时玉皮，并将穿戴中的限时玉皮投影为对应药水效果（{@code JADE_SKIN_GU} /
 *       {@code WHITE_JADE_GU}）供 HUD 显示图标与倒计时；永久皮肤不投影药水效果，仅在 B 面板肉身页展示。</li>
 *   <li>{@link #reduceDamage} 从可抵挡伤害中减去当前皮肤的固定减免值；已到期的限时玉皮在心跳正式脱下前的间隔内不再减
 *       伤。</li>
 *   <li>真实状态唯一存于 {@link BodyData#skin} 与 {@code skinEndTick}：因原版 {@code MobEffect} 无到期回调且会被
 *       {@code /effect clear} 静默清除，心跳中会按真实状态重新补挂投影效果（与 {@link UndeadService} 投影临时僵尸一
 *       致）。</li>
 *   <li>⚠ 穿透无敌或药水效果的伤害（{@code /kill}、虚空、饥饿）不受皮肤减免，判定口径与原版抗性提升一致。</li>
 * </ul>
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
            if (effect != null) {
                BodyService.projectMarker(player, effect, skin == worn, BodyService.getSkinTicksLeft(player));
            }
        }
    }

    private static @Nullable Holder<MobEffect> getEffectOf(@NotNull Skin skin) {
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
