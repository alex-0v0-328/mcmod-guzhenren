package net.alex.guzhenren.item.material.qi;

import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.qi.PathQiService;
import net.alex.guzhenren.gameplay.path.qi.QiKind;
import net.alex.guzhenren.item.material.GuMaterialItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Gu material [蛊材] that turns into Qi [气] over a charged press, paid for tick by tick.
 *
 * <p>Extends {@link net.alex.guzhenren.item.material.GuMaterialItem}. The {@link QiKind} comes from
 * registration, and so does the essence [真元] cost (set per rank [转数] in {@code ModItems}), spread evenly
 * across the charge ladder (5/10/20 ticks via {@code useChargeByGap}). The apply delegates to
 * {@link PathQiService#add}.
 *
 * <p>⚠ A material, not a Gu: it keeps the base item's hooks, and it stacks; no progress is stored -- the
 * component is shared by the whole stack, and storing it would be the Hope Gu bug.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.material.GuMaterialItem
 * @since 1.0.0
 */

public class QiMaterialItem extends GuMaterialItem {

    private static final String CHARGE_CAPTION = "guzhenren.hud.refining_plain";
    private static final String FAILED_ESSENCE = "guzhenren.item.failed.essence";
    private final QiKind kind;
    private final long essenceCost;

    public QiMaterialItem(Properties properties, Rank rank, QiKind kind, long essenceCost) {
        super(properties, rank, GuPath.QI);
        this.kind = kind;
        this.essenceCost = essenceCost;
    }

    public QiKind kind() { return kind; }

    protected long qiAmount() { return QiKind.tierAmount(tier()); }

    protected long essenceCost() { return essenceCost; }

    @Override
    protected boolean hasUse() { return true; }

    @Override
    protected int useDurationTicks(Player player, ItemStack stack) { return useChargeByGap(player); }

    @Override
    protected @Nullable Refusal gate(Player player, ItemStack stack) {
        return essenceCost() > 0 && ApertureEssenceService.spendable(player) < essenceCost()
                ? new Refusal(FAILED_ESSENCE)
                : null;
    }

    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity entity, @NotNull ItemStack stack,
                          int remaining) {
        if (!(entity instanceof ServerPlayer player)) return;

        int duration = useDurationTicks(player, stack);
        if (duration <= 0 || essenceCost() <= 0) return;

        int tick = duration - remaining + 1;
        long step = paidBy(tick, duration) - paidBy(tick - 1, duration);
        if (step > 0 && !ApertureEssenceService.consume(player, step)) player.stopUsingItem();
    }

    private long paidBy(int ticks, int duration) { return essenceCost() * ticks / duration; }

    @Override
    public @Nullable Component chargeCaption(ItemStack stack, int remainingTicks) {
        return Component.translatable(CHARGE_CAPTION);
    }

    @Override
    protected int apply(ServerPlayer player, ItemStack stack) {
        PathQiService.add(player, kind, qiAmount());
        return 1;
    }
}
