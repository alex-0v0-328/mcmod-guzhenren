package net.alex.guzhenren.item.gu.mortal.path.time;

import java.util.List;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.display.ModDisplayText;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.gameplay.rewind.PlayerRewindService;
import net.alex.guzhenren.gameplay.rewind.RewindClock;
import net.alex.guzhenren.item.gu.tending.GuHunger;
import net.alex.guzhenren.item.gu.GuProperties;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * 如故系五转持续照顾型蛊[Tended Gu]的抽象基类：涵盖将玩家身体回溯一天的 {@link ManAsBeforeGuItem} 与将周围陆地/水域恢复
 * 为初始裸地形的 {@link TerrainAsBeforeGuItem}，封装共用的自然恢复进度条、身体快照采样与拒绝提示。
 * <ul>
 *   <li>进度条固定采用随世界时钟自然恢复的 {@link GuHunger.RecoverBar}（{@link RewindClock}），无需喂食也不会饿死；当剩
 *       余点数不足以支付单次催动时，遵循持续照顾型蛊的强行催动[Forced Use]规则，首次按下发出警告、确认后强行催动并力竭
 *       死亡。</li>
 *   <li>⚠ 进度条必须在服务端实际执行催动、判定点数与扣点前才调用 {@link GuHunger.RecoverBar#settle} 结算
 *       （{@link #performOwnUse}），绝不可在双端共跑的门槛检查中写回物品栈；若扣点前进度条原本已满，扣点后必须调用
 *       {@link GuHunger.RecoverBar#restart} 将恢复起点重置为当前时刻（满条期间锚点时间戳已停滞过时）。</li>
 *   <li>当玩家随身背包、空窍存储或本命槽中携带有已炼化的人如故[Man As Before Gu]时，{@link #remember} 随服务端心跳每隔
 *       {@link RewindClock#INTERVAL_TICKS} 记录一次玩家身体快照；山如故与江如故无需记录快照（世界种子本身即初始地形记
 *       忆）。</li>
 *   <li>⚠ 客户端不同步玩家身体回溯缓冲，因此子类与历史记录相关的催动门槛仅对 {@link ServerPlayer} 校验，客户端直接放
 *       行。</li>
 * </ul>
 */

public abstract class AsBeforeGuItem extends TendedGuItem {

    protected static final String FAILED_NO_RECORD = "guzhenren.item.failed.as_before_no_record";
    private static final String TOOLTIP_NEXT_POINT = "guzhenren.item.gu.next_point";
    protected final GuHunger.RecoverBar bar;

    protected AsBeforeGuItem(GuProperties guProperties) {
        super(guProperties);
        if (!(hunger instanceof GuHunger.RecoverBar recover)) {
            throw new IllegalArgumentException("as before gu needs a recover bar");
        }
        this.bar = recover;
    }

    //region the bar [恢复条]
    public GuHunger.RecoverBar getBar() { return bar; }

    @Override
    protected int performOwnUse(ServerPlayer player, ItemStack stack) {
        long now = player.level().getGameTime();
        bar.settle(stack, now);
        boolean wasFull = bar.getPoints(stack) >= bar.max();
        int result = super.performOwnUse(player, stack);
        if (wasFull) bar.restart(stack, now);
        return result;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        Level level = context.level();
        if (level == null || !isRefined(stack)) return;

        int ticks = bar.getTicksToNextPoint(stack, level.getGameTime());
        if (ticks <= 0) return;

        tooltip.add(Component.translatable(TOOLTIP_NEXT_POINT, ModDisplayText.getCountdownSeconds(ticks))
                .withStyle(ChatFormatting.GRAY));
    }
    //endregion

    //region the memory [记忆]
    public static void remember(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (now % RewindClock.INTERVAL_TICKS >= Ticks.SECOND) return;

        if (isCarriedRefined(player, ManAsBeforeGuItem.class)) PlayerRewindService.record(player, now);
    }

    private static boolean isCarriedRefined(ServerPlayer player, Class<? extends AsBeforeGuItem> kind) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (isRefinedOf(inventory.getItem(slot), kind)) return true;
        }
        for (int aperture = 0; aperture < ApertureData.MAX_APERTURES; aperture++) {
            if (isRefinedOf(ApertureStorageService.getVital(player, aperture), kind)) return true;
            for (ItemStack stack : ApertureStorageService.getView(player, aperture)) {
                if (isRefinedOf(stack, kind)) return true;
            }
        }
        return false;
    }

    private static boolean isRefinedOf(ItemStack stack, Class<? extends AsBeforeGuItem> kind) {
        return kind.isInstance(stack.getItem()) && ((AsBeforeGuItem) stack.getItem()).isRefined(stack);
    }
    //endregion
}
