package net.alex.guzhenren.item.gu.mortal.time;

import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.gameplay.rewind.PlayerRewindData;
import net.alex.guzhenren.gameplay.rewind.PlayerRewindService;
import net.alex.guzhenren.gameplay.rewind.PlayerSnapshot;
import net.alex.guzhenren.item.gu.GuSpec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Man As Before Gu [人如故]: its carrier's body goes back to how it was a day ago -- health, hunger, effects, soul,
 * thoughts and the temp zombie form, as {@code PlayerSnapshot} lists them, and nothing else.
 *
 * <p>Extends {@link AsBeforeGuItem}. The memory is {@code PlayerRewindService}'s ring on the carrier, recorded only
 * while a refined one is carried: a Gu that has recorded nothing refuses with {@link #FAILED_NO_MEMORY}, one carried
 * for less than a day with {@code FAILED_TOO_YOUNG}, and neither refusal spends anything.
 *
 * @author Alex
 * @version 1.0.0
 * @see AsBeforeGuItem
 * @since 1.0.0
 */

public class ManAsBeforeGuItem extends AsBeforeGuItem {

    private static final String FAILED_NO_MEMORY = "guzhenren.item.failed.as_before_no_memory";
    private static final String MSG_RESTORED = "guzhenren.item.gu.as_before_body";

    public ManAsBeforeGuItem(Properties properties, GuSpec spec) {
        super(properties, spec);
    }

    @Override
    protected @Nullable Refusal checkEffect(Player player, ItemStack stack) {
        Refusal recovering = checkRecoverGate(player, stack);
        if (recovering != null) return recovering;
        if (!(player instanceof ServerPlayer server)) return null;

        PlayerRewindData memory = PlayerRewindService.get(server);
        if (memory.isEmpty()) return new Refusal(FAILED_NO_MEMORY);
        return memory.pick(server.level().getGameTime()) == null ? new Refusal(FAILED_TOO_YOUNG) : null;
    }

    @Override
    protected void applyEffect(ServerPlayer player, ItemStack stack) {
        PlayerSnapshot snapshot = PlayerRewindService.pick(player, player.level().getGameTime());
        if (snapshot == null) return;

        PlayerRewindService.restore(player, snapshot);
        ActionBar.inform(player, MSG_RESTORED);
    }
}
