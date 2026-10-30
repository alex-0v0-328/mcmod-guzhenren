package net.alex.guzhenren.item.gu.mortal;

import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.SkinService;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem.Amplification;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The skin module [皮蛊]: the jade skin Gu [玉皮蛊 / 白玉蛊] lend the holder a skin [皮肤] for a while, the bronze
 * [铜皮蛊] and iron [铁皮蛊] skin Gu leave theirs for good; which skin, and for a jade skin how long, come from
 * registration.
 *
 * <p>Extends {@link TemperingModule}. ⚠ A timed skin takes a positive duration and a permanent skin none, checked
 * at construction. The effect writes the skin through {@link BodyService#wearSkin}, then projects it at once so
 * the HUD shows a jade skin's effect before the next heartbeat; the reduction, the timer and the flash all hang
 * off that write. An Amplify Effect Gu [功倍蛊] multiplies a timed skin's length; a permanent skin cannot be
 * amplified, so it is never billed there. {@code TimedEffects} finds the Gu that lent a jade skin by this module.
 *
 * @author Alex
 * @version 1.0.0
 * @see SkinService
 * @since 1.0.0
 */

public final class SkinModule extends TemperingModule<Skin> {

    private static final String FAILED_SKIN_TAKEN = "guzhenren.item.failed.skin_taken";
    private final int durationTicks;

    public SkinModule(Skin skin, int durationTicks) {
        super(skin, FAILED_SKIN_TAKEN);
        if (skin.isTimed() != durationTicks > 0) {
            throw new IllegalArgumentException(skin + " takes " + (skin.isTimed() ? "a duration" : "no duration"));
        }
        this.durationTicks = durationTicks;
    }

    public int getDurationTicks() { return durationTicks; }

    @Override
    public boolean canBeAmplified() { return durationTicks > 0; }

    @Override
    protected @NotNull Skin getWorn(Player player) { return BodyService.getSkin(player); }

    @Override
    protected void temper(MortalGuItem gu, ServerPlayer player) {
        Amplification amplification = canBeAmplified() ? AmplifyEffectGuItem.bill(player, gu) : Amplification.NONE;
        BodyService.wearSkin(player, getGrade(), durationTicks * amplification.factor());
        TimedEffects.markSkinAmplified(player, amplification);
        SkinService.tickSkin(player);
    }
}
