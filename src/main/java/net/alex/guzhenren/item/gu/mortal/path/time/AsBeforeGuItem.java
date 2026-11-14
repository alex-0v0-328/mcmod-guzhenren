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
 * 如故蛊[As Before Gu]：把东西恢复原样的宙道蛊。
 * <ul>
 *   <li>三者中间类：{@link ManAsBeforeGuItem} 把携带者身体回退一天，{@link TerrainAsBeforeGuItem} 把周围陆地或水域回退
 *       到初次生成的裸地形；共用饱食[Hunger]条、身体记忆和拒绝提示。</li>
 *   <li>持续照顾型蛊[Tended Gu]，饱食条为随世界时钟回复的 {@link GuHunger.RecoverBar}，不补食、不饿死；点数不足时同
 *       其他饱食不足的催动，先警告再强行催动[Forced Use]（{@code TendedGuItem.drive}）。</li>
 *   <li>⚠ 条只在服务端、判不足并扣点前结算（{@link #performOwnUse}），不在门槛里：门槛客户端也跑，客户端写物品堆是
 *       服务端还得撤销的假象；满条扣点把时间戳重置到扣点时，因满条时间戳已过时（{@code GuHunger.RecoverBar}）。</li>
 *   <li>随身（背包、空窍[Aperture]存储或本命格）带已炼化人如故[Man As Before Gu]时，{@link #remember} 随心跳每
 *       {@code RewindClock.INTERVAL_TICKS} 记一次身体；土地无需记忆，种子即记忆。</li>
 *   <li>⚠ 客户端不持有身体记忆（不同步），相关门槛只对 {@code ServerPlayer} 运行，客户端放行。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see RewindClock
 * @since 1.0.0
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
