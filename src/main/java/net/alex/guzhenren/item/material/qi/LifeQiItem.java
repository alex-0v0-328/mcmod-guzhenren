package net.alex.guzhenren.item.material.qi;

import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.path.qi.PathQiService;
import net.alex.guzhenren.gameplay.path.qi.QiKind;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Life Qi [生气] material: it pays down a Death Qi [死气] debt before it does anything else.
 *
 * <p>Extends {@link net.alex.guzhenren.item.material.qi.QiMaterialItem}. The apply overrides the base to
 * route the amount into {@link PathQiService} against Death
 * Qi first; only the excess reaches the Life Qi pool. When Death Qi clears to zero the lifespan [寿元]
 * refund is handled by {@link BodyService#refundDeathQiDebt}.
 *
 * <p>⚠ Only clearing that debt outright refunds any of the burnt lifespan [寿元] -- {@code
 * DEATH_QI_REFUND_NUMERATOR / DEATH_QI_REFUND_DENOMINATOR} of it; paying it partway down refunds nothing
 * at all. The asymmetry is deliberate.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.material.qi.QiMaterialItem
 * @since 1.0.0
 */

public class LifeQiItem extends QiMaterialItem {

    private static final String CURED = "guzhenren.item.death_qi_cured";
    public static final int DEATH_QI_REFUND_NUMERATOR = 3;
    public static final int DEATH_QI_REFUND_DENOMINATOR = 4;

    public LifeQiItem(Properties properties, Rank rank, long essenceCost) {
        super(properties, rank, QiKind.LIFE, essenceCost);
    }

    @Override
    protected int apply(ServerPlayer player, ItemStack stack) {
        long death = PathQiService.getCurrent(player, QiKind.DEATH);
        if (death <= 0L) return super.apply(player, stack);

        long remainder = qiAmount() - Math.min(qiAmount(), death);
        PathQiService.add(player, QiKind.DEATH, -qiAmount());
        if (remainder > 0L) PathQiService.add(player, QiKind.LIFE, remainder);
        if (PathQiService.getCurrent(player, QiKind.DEATH) <= 0L) {
            double refund = BodyService.refundDeathQiDebt(player,
                    DEATH_QI_REFUND_NUMERATOR, DEATH_QI_REFUND_DENOMINATOR);
            if (refund > 0.0) inform(player, CURED, refund);
        }
        return 1;
    }
}
