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
 * 皮的心跳步骤与伤害规则。
 *
 * <ul>
 *   <li>{@link #tickSkin} 蜕下超时玉皮，把穿着的玉皮投影为其蛊效果（{@link #getEffectOf}）供 HUD 显示图标与计时；
 *       永久皮不投影，显示在肉身面板。</li>
 *   <li>{@link #reduceDamage} 从可抵挡的伤害中扣固定减免；超时玉皮在心跳蜕下前那一秒不抵挡。</li>
 *   <li>真相是 {@code BodyData.skin} 及结束 tick：{@code MobEffect} 无到期钩子，{@code /effect clear} 静默移除，
 *       故心跳重施，同 {@link UndeadService} 投影临时僵尸。</li>
 *   <li>⚠ 绕过无敌或效果的伤害（{@code /kill}、虚空、饥饿）原样穿过，同原版抗性提升。</li>
 * </ul>
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
