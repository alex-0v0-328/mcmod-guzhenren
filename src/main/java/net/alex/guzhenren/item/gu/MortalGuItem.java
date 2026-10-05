package net.alex.guzhenren.item.gu;

import java.util.List;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.Aperture;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.Stage;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.item.GuItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A mortal Gu [凡蛊]: holds the {@link GuSpec} and the essence [真元] gate, but no per-stack state.
 *
 * <p>Sits between {@link GuItem} and three branches: {@link OneShotGuItem} (refining IS the use, stacks,
 * no state), {@link TendedGuItem} (refined then fed and used, carries {@link RefinedGuState}), and the Hope
 * Gu [希望蛊], which opens an aperture and is neither. The constructor runs {@link GuSpec#validate} so a
 * typo in the numbers fails at startup, naming the Gu. {@link #stageUpGate} is the one check a stage-raising Gu runs: the
 * aperture's rank must be the Gu's own, and its stage below the peak.
 *
 * <p>⚠ Every Gu answers a right click even with nothing else to do, because refining [炼化] is always
 * an answer. A leaf that declines to answer makes the Gu look broken in the hand.
 *
 * @author Alex
 * @version 1.0.0
 * @see GuSpec
 * @since 1.0.0
 */

public abstract class MortalGuItem extends GuItem {

    public static final int REFINE_DONE_COOLDOWN_TICKS = 2 * Ticks.SECOND;
    private static final String KIND_KEY = "guzhenren.display.gu";
    protected static final String FAILED_REFINE_ESSENCE = "guzhenren.item.failed.refine_essence";
    protected static final String FAILED_ESSENCE = "guzhenren.item.failed.essence";
    private static final String FAILED_RANK_MISMATCH = "guzhenren.item.failed.rank_mismatch";
    private static final String FAILED_STAGE_PEAK = "guzhenren.item.failed.stage_peak";
    protected static final String CAPTION_USING_PLAIN = "guzhenren.hud.using_plain";
    private static final String CAPTION_REFINING_PLAIN = "guzhenren.hud.refining_plain";
    private static final String CAPTION_REFINING = "guzhenren.hud.refining";
    protected final GuSpec spec;

    protected MortalGuItem(Properties properties, GuSpec spec) {
        super(properties, spec.rank(), spec.path());
        this.spec = spec;
        spec.validate(rank().name() + " " + getClass().getSimpleName());
    }

    @Override
    protected String kindKey() { return KIND_KEY; }

    public int refineCost() { return spec.refineCost(); }

    //region refining [炼化] -- every Gu answers it, and only the price differs
    protected final @Nullable Refusal essenceGate(Player player, long required, String key) {
        if (required <= 0) return null;
        return ApertureEssenceService.spendable(player) < required ? new Refusal(key) : null;
    }

    protected final @Nullable Refusal stageUpGate(Aperture aperture) {
        if (aperture.rank() != rank()) {
            return new Refusal(FAILED_RANK_MISMATCH, Component.translatable(rank().getTranslationKey()));
        }
        return aperture.stage() == Stage.HIGHEST ? new Refusal(FAILED_STAGE_PEAK) : null;
    }

    protected final void payRefineCost(ServerPlayer player) {
        if (refineCost() > 0) ApertureEssenceService.consume(player, refineCost());
    }
    //endregion

    //region the click -- a Gu always answers the right click, if only to be refined
    @Override
    protected final boolean hasUse() { return true; }

    protected @Nullable Refusal useGate(Player player, ItemStack stack) { return null; }

    protected int useApply(ServerPlayer player, ItemStack stack) { return 0; }
    //endregion

    //region display
    protected final Component refineCaptionPlain() { return Component.translatable(CAPTION_REFINING_PLAIN); }

    protected final Component refineCaption(int invested) {
        return refineCost() > 0
                ? Component.translatable(CAPTION_REFINING, invested, refineCost())
                : refineCaptionPlain();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        StrengthPathBranch branch = spec.strengthPathBranch();
        if (path() == GuPath.STRENGTH && branch != StrengthPathBranch.NORMAL) {
            tooltip.add(Component.translatable(branch.getTranslationKey()).withStyle(ChatFormatting.GRAY));
        }
        MutableComponent line = progressLine(stack);
        if (line != null) tooltip.add(line.withStyle(ChatFormatting.GRAY));
    }

    protected @Nullable MutableComponent progressLine(ItemStack stack) { return null; }
    //endregion

    @Override
    public boolean onDroppedByPlayer(@NotNull ItemStack stack, @NotNull Player player) { return !isVital(stack); }
}
