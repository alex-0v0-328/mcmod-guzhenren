package net.alex.guzhenren.item.gu.mortal.time;

import java.util.function.Supplier;
import net.alex.guzhenren.effect.timed.TimeRateUpEffect;
import net.alex.guzhenren.item.gu.ConsumedGuItem;
import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A Watch Gu [更蛊]: for a while its holder's own clock [自身时间] outruns the world's, and then it is gone.
 *
 * <p>Extends {@link net.alex.guzhenren.item.gu.ConsumedGuItem}, making it tended AND taken by its
 * own use. Two rungs register against this one class: 两更蛊 reaches two ×2 layers, and 三更蛊 reaches
 * three ×3 layers. The effect owns both its rate and layer cap.
 *
 * <p>⚠ It refuses nothing. Reusing one kind adds a capped layer and refreshes its duration; wearing
 * both kinds is the design, and their rates add.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.ConsumedGuItem
 * @since 1.0.0
 */

public class WatchGuItem extends ConsumedGuItem {

    private final Supplier<? extends MobEffect> form;
    private final int effectTicks;

    public WatchGuItem(Properties properties, Supplier<? extends MobEffect> form, int effectTicks, GuSpec spec) {
        super(properties, spec);
        if (!(form.get() instanceof TimeRateUpEffect)) {
            throw new IllegalArgumentException("watch gu needs a TimeRateUpEffect");
        }
        this.form = form;
        this.effectTicks = effectTicks;
    }

    @Override
    protected @Nullable Refusal checkPayoutGate(Player player, ItemStack stack) { return null; }

    @Override
    protected void payOut(ServerPlayer player, ItemStack stack) {
        MobEffectInstance current = ModEffects.get(player, form);
        TimeRateUpEffect effect = (TimeRateUpEffect) form.get();
        int amplifier = effect.getNextAmplifier(current == null ? -1 : current.getAmplifier());
        player.addEffect(ModEffects.instance(form, effectTicks, amplifier));
    }
}
