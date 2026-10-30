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
import net.minecraft.world.inventory.InventoryMenu;
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
 * typo in the numbers fails at startup, naming the Gu. {@link #checkStageUpGate} is the one check a stage-raising Gu
 * runs: the aperture's rank must be the Gu's own, and its stage below the peak. {@link #getUseEssence} is what one
 * use pays (a tended Gu's cost per use, a one-shot Gu's refining, nothing for a channel), and
 * {@link #canBeAmplified} says whether an Amplify Effect Gu [功倍蛊] multiplies it; each that does asks it from its
 * own payout. The tooltip's usage type line [类型] ({@link #formatType}) shows the type alone, or a branch after
 * its parent ({@code 防御 · 反击}).
 *
 * <p>⚠ Every Gu answers a right click even with nothing else to do, because refining [炼化] is always
 * an answer. A leaf that declines to answer makes the Gu look broken in the hand.
 *
 * <p>⚠ No Gu gates on {@code isAwakened}; the essence gate ({@link #checkEssenceGate}) stands in for it and
 * passes a zero cost. No one-shot Gu has a refine cost, so a mortal without an aperture uses the Vitality Leaf,
 * Lifespan, Guts and Second Aperture Gu by design; the Relics Gu stop at {@link #checkStageUpGate}. The Hope Gu
 * is the one {@code isAwakened} check, refusing a player who already has a first aperture.
 *
 * <p>⚠ A Vital Gu refuses the Q-key drop in {@link #onDroppedByPlayer}, but the client has already emptied
 * the hotbar slot by then: the refusal marks the server's remote copy of that slot stale, so the next
 * broadcast puts the Gu back on screen.
 *
 * @author Alex
 * @version 1.0.0
 * @see GuSpec
 * @since 1.0.0
 */

public abstract class MortalGuItem extends GuItem {

    public static final int REFINE_DONE_COOLDOWN_TICKS = 2 * Ticks.SECOND;
    private static final String KIND_KEY = "guzhenren.display.gu";
    private static final String TYPE_KEY = "guzhenren.display.gu_type";
    private static final String TYPE_BRANCH_KEY = "guzhenren.display.gu_type_branch";
    protected static final String FAILED_REFINE_ESSENCE = "guzhenren.item.failed.refine_essence";
    protected static final String FAILED_ESSENCE = "guzhenren.item.failed.essence";
    private static final String FAILED_RANK_MISMATCH = "guzhenren.item.failed.rank_mismatch";
    private static final String FAILED_STAGE_PEAK = "guzhenren.item.failed.stage_peak";
    protected static final String CAPTION_USING_PLAIN = "guzhenren.hud.using_plain";
    private static final String CAPTION_REFINING_PLAIN = "guzhenren.hud.refining_plain";
    private static final String CAPTION_REFINING = "guzhenren.hud.refining";
    protected final GuSpec spec;

    protected MortalGuItem(Properties properties, GuSpec spec) {
        super(properties, spec.getRank(), spec.getPath());
        this.spec = spec;
        spec.validate(getRank().name() + " " + getClass().getSimpleName());
    }

    @Override
    protected String getKindKey() { return KIND_KEY; }

    public GuSpec getSpec() { return spec; }

    public int getRefineCost() { return spec.getRefineCost(); }

    public long getUseEssence() { return spec.isChanneled() ? 0L : spec.getEssencePerRound(); }

    public boolean canBeAmplified() { return false; }

    //region refining [炼化] -- every Gu answers it, and only the price differs
    protected final @Nullable Refusal checkEssenceGate(Player player, long required, String key) {
        if (required <= 0) return null;
        return ApertureEssenceService.getSpendable(player) < required ? new Refusal(key) : null;
    }

    protected final @Nullable Refusal checkStageUpGate(Aperture aperture) {
        if (aperture.rank() != getRank()) {
            return new Refusal(FAILED_RANK_MISMATCH, Component.translatable(getRank().getTranslationKey()));
        }
        return aperture.stage() == Stage.HIGHEST ? new Refusal(FAILED_STAGE_PEAK) : null;
    }

    protected final void payRefineCost(ServerPlayer player) {
        if (getRefineCost() > 0) ApertureEssenceService.consume(player, getRefineCost());
    }
    //endregion

    //region the click -- a Gu always answers the right click, if only to be refined
    @Override
    protected final boolean hasUse() { return true; }

    protected @Nullable Refusal checkOwnUse(Player player, ItemStack stack) { return null; }

    protected int performOwnUse(ServerPlayer player, ItemStack stack) { return 0; }
    //endregion

    //region display
    protected final Component refineCaptionPlain() { return Component.translatable(CAPTION_REFINING_PLAIN); }

    protected final Component refineCaption(int invested) {
        return getRefineCost() > 0
                ? Component.translatable(CAPTION_REFINING, invested, getRefineCost())
                : refineCaptionPlain();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        StrengthPathBranch branch = spec.getStrengthPathBranch();
        if (getPath() == GuPath.STRENGTH && branch != StrengthPathBranch.NORMAL) {
            tooltip.add(Component.translatable(branch.getTranslationKey()).withStyle(ChatFormatting.GRAY));
        }
        GuType type = spec.getType();
        if (type != null) tooltip.add(formatType(type).withStyle(ChatFormatting.GRAY));
        MutableComponent line = formatProgressLine(stack);
        if (line != null) tooltip.add(line.withStyle(ChatFormatting.GRAY));
    }

    protected @Nullable MutableComponent formatProgressLine(ItemStack stack) { return null; }

    static MutableComponent formatType(GuType type) {
        Component name = Component.translatable(type.getTranslationKey());
        GuType parent = type.getParent();
        if (parent == null) return Component.translatable(TYPE_KEY, name);
        return Component.translatable(TYPE_BRANCH_KEY, Component.translatable(parent.getTranslationKey()), name);
    }
    //endregion

    @Override
    public boolean onDroppedByPlayer(@NotNull ItemStack stack, @NotNull Player player) {
        if (!isVital(stack)) return true;

        if (player instanceof ServerPlayer server) {
            server.inventoryMenu.setRemoteSlot(InventoryMenu.USE_ROW_SLOT_START + server.getInventory().selected,
                    ItemStack.EMPTY);
        }
        return false;
    }
}
