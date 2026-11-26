package net.alex.guzhenren.item.gu.tending;

import net.alex.guzhenren.item.gu.stack.GuCondition;
import net.alex.guzhenren.item.gu.stack.GuCondition.Stage;
import net.alex.guzhenren.item.gu.stack.VitalGuState;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 持续照顾型蛊[Tended Gu]的伤势规则：正常、受伤、濒死三态存于 {@link GuCondition} 组件（缺省即正常），不设生命点数。
 * <ul>
 *   <li>受伤 9 次、濒死 3 次，无时间期限；{@link #wearUse} 每次催动扣 1，受伤用尽转濒死，濒死用尽伤重而亡（创造模式与
 *       本命蛊[Vital Gu]不因伤死，炼废同理）；无饱食条的蛊不计（{@code TendedGuItem.wearUse} 把守）。</li>
 *   <li>来源只有炼蛊失败（{@link #rollRefineFailure}）与指令（{@link #set}）；进新档次数取满，炼化保留伤势。</li>
 *   <li>恢复接口 {@link #recover} 升一档；目前只有本命蛊借玩家回血（{@link #mend}）调用。</li>
 * </ul>
 */

public final class GuHealth {

    public static final float REFINE_INJURE_CHANCE = 0.2F;
    public static final float REFINE_WORSEN_CHANCE = 0.5F;
    public static final float REFINE_KILL_CHANCE = 0.8F;
    private static final String MSG_INJURED = "guzhenren.item.gu.injured";
    private static final String MSG_DYING = "guzhenren.item.gu.dying";
    private static final String MSG_MENDED = "guzhenren.item.gu.mended";

    private GuHealth() {}

    //region reading [读取]
    public static @Nullable GuCondition get(ItemStack stack) { return stack.get(ModDataComponents.GU_CONDITION); }

    public static Stage getStage(ItemStack stack) {
        GuCondition condition = get(stack);
        return condition == null ? Stage.NORMAL : condition.stage();
    }
    //endregion

    //region writing [写入]
    /** 设为 {@code stage} 并取满次数，保留回血进度；正常即移除组件。 */
    public static void set(ItemStack stack, Stage stage) {
        GuCondition condition = get(stack);
        setFresh(stack, stage, condition == null ? 0.0F : condition.mended());
    }

    /** 升一档（濒死→受伤取满次数，受伤→正常）；本已正常为 false。 */
    public static boolean recover(ItemStack stack) {
        GuCondition condition = get(stack);
        if (condition == null) return false;

        setFresh(stack, condition.stage().better(), condition.mended());
        return true;
    }

    private static void setFresh(ItemStack stack, Stage stage, float mended) {
        if (stage == Stage.NORMAL) {
            stack.remove(ModDataComponents.GU_CONDITION);
            return;
        }
        stack.set(ModDataComponents.GU_CONDITION, GuCondition.fresh(stage, mended));
    }

    private static void worsen(ServerPlayer holder, ItemStack stack) {
        Stage next = getStage(stack).worse();
        set(stack, next);
        TendedGuItem.announce(holder, stack, next == Stage.DYING ? MSG_DYING : MSG_INJURED);
    }
    //endregion

    //region uses, failures and mending [计次、炼败与回血]
    /** 一次催动后扣 1 次；伤重而亡时为 true（已公告，调用方移除物品栈）。 */
    static boolean wearUse(ServerPlayer holder, ItemStack stack) {
        GuCondition condition = get(stack);
        if (condition == null) return false;

        if (condition.usesLeft() > 1) {
            stack.set(ModDataComponents.GU_CONDITION, condition.withUsesLeft(condition.usesLeft() - 1));
            return false;
        }
        if (condition.stage() != Stage.DYING) {
            worsen(holder, stack);
            return false;
        }
        if (holder.getAbilities().instabuild || VitalGuState.isVital(stack)) {
            stack.set(ModDataComponents.GU_CONDITION, condition.withUsesLeft(0));
            return false;
        }
        TendedGuItem.dieOfWounds(holder, stack);
        return true;
    }

    /** 炼蛊失败按当前档掷一次：正常 20% 受伤、受伤 50% 濒死、濒死 80% 炼废（本命蛊不炼废）；炼废时为 true（调用方清格）。 */
    public static boolean rollRefineFailure(ServerPlayer holder, ItemStack stack, RandomSource random) {
        Stage stage = getStage(stack);
        float chance = switch (stage) {
            case NORMAL -> REFINE_INJURE_CHANCE;
            case INJURED -> REFINE_WORSEN_CHANCE;
            case DYING -> REFINE_KILL_CHANCE;
        };
        if (random.nextFloat() >= chance) return false;

        if (stage != Stage.DYING) {
            worsen(holder, stack);
            return false;
        }
        if (VitalGuState.isVital(stack)) return false;

        TendedGuItem.ruin(holder, stack);
        return true;
    }

    /** 本命蛊借主人实际回上的 {@code healed} 点生命累计，满 {@code full} 点升一档并留下余数；升档时为 true。 */
    public static boolean mend(ServerPlayer owner, ItemStack stack, float healed, float full) {
        GuCondition condition = get(stack);
        if (condition == null || healed <= 0.0F || full <= 0.0F) return false;

        float mended = condition.mended() + healed;
        if (mended < full) {
            stack.set(ModDataComponents.GU_CONDITION, condition.withMended(mended));
            return false;
        }
        stack.set(ModDataComponents.GU_CONDITION, condition.withMended(mended - full));
        recover(stack);
        TendedGuItem.announce(owner, stack, MSG_MENDED);
        return true;
    }
    //endregion
}
