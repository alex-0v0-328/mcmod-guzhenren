package net.alex.guzhenren.item.gu.mortal.strength;

import net.alex.guzhenren.gameplay.body.BodyGrade;
import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The shared class behind the body tempering Gu [炼体蛊]: a tended Gu that makes one grade of one body part -- a
 * skin [皮], tendons [筋] or bones [骨] -- of the holder's. Which part and which grade come from the leaf and its
 * registration; the Gu itself is never consumed, so a holder who has outgrown it can hand it on. The two jade
 * skins are instant defensive Gu; every permanent grade is a channeled [灌注] dao mark carving Gu whose one round
 * is the payout, after which the gate refuses the next.
 *
 * <p>Extends {@link TendedGuItem}. The gate asks the grade itself ({@link BodyGrade#canReplace}) whether the
 * holder's current grade gives way, and refuses with the part's own key and the worn grade's name; the payout
 * is the leaf's {@link #temper}, one write through {@code BodyService}. Every number and rule hangs off that
 * write and the grade enum, not off this item.
 *
 * @author Alex
 * @version 1.0.0
 * @see SkinGuItem
 * @see TendonGuItem
 * @see BoneGuItem
 * @since 1.0.0
 */

public abstract class TemperingGuItem<G extends Enum<G> & BodyGrade<G>> extends TendedGuItem {

    private final G grade;
    private final String failedTakenKey;

    protected TemperingGuItem(Properties properties, G grade, String failedTakenKey, GuSpec spec) {
        super(properties, spec);
        this.grade = grade;
        this.failedTakenKey = failedTakenKey;
    }

    public @NotNull G getGrade() { return grade; }

    protected abstract @NotNull G getWorn(Player player);

    protected abstract void temper(ServerPlayer player);

    @Override
    protected final @Nullable Refusal checkPayoutGate(Player player, ItemStack stack) {
        G worn = getWorn(player);
        return grade.canReplace(worn) ? null
                : new Refusal(failedTakenKey, Component.translatable(worn.getTranslationKey()));
    }

    @Override
    protected final void payOut(ServerPlayer player, ItemStack stack) { temper(player); }
}
