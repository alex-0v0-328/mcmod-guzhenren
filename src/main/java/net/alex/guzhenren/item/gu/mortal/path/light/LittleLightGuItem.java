package net.alex.guzhenren.item.gu.mortal.path.light;

import java.util.List;
import net.alex.guzhenren.item.gu.GuProperties;
import net.alex.guzhenren.item.gu.stack.RefinedGuState;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * 小光蛊[Little Light Gu]：光道[Light Path]一转持续照顾型蛊[Tended Gu]。
 * <ul>
 *   <li>自身催动是开关非生效：手中发光，已炼化者付一轮可点亮 2 分钟；野生者无开关，只以 10 发光。</li>
 *   <li>发光不在代码：{@code assets/guzhenren/dynamiclights/item/} 两份 LambDynamicLights 定义，任意堆叠 10、带
 *       {@code LITTLE_LIGHT_BRIGHT} 为 15，取匹配中较大者，无该模组不发光；点亮组件同步到客户端，仅开启时存在、从不写
 *       {@code false}，定义才能匹配，关掉的堆叠与新堆叠相同。</li>
 *   <li>⚠ 开关不扣真元[Essence]：{@code performOwnUse} 取代 {@code drive}，{@code useThreshold} 为 0；点亮即一轮：
 *       付一次属性催动饱食[Hunger]，扣前后照常从背包进食，无论握否亮 {@link #BRIGHT_TICKS} 服务端游戏时间（日钟 tick，
 *       不受自身时间缩放）；再按提前熄灭，不退费；作杀招[Killing Move]辅蛊启动时扣的也是这份饱食。</li>
 *   <li>⚠ {@code LITTLE_LIGHT_BRIGHT_UNTIL} 仅开关时写一次，不逐 tick 写，因每次写入都重同步该槽；只有随身堆叠 tick，
 *       箱中或地上仍亮的一被带上即熄；创造模式进食不耗食物（{@code GuHunger}）。</li>
 *   <li>⚠ 饱食不足时点亮即强行催动[Forced Use]（{@code GuForcedUse}）：首按警告，5 秒内再按同蛊点亮整轮，熄灭时（到期或
 *       按下）蛊力竭而死；{@code LITTLE_LIGHT_FORCED} 标记该轮，与 {@code LITTLE_LIGHT_BRIGHT} 同写同删，不逐 tick 写；
 *       熄灭不设门槛；本命小光蛊饱食不足只拒绝；创造模式不警告、不死亡。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see TendedGuItem
 * @since 1.0.0
 */

public final class LittleLightGuItem extends TendedGuItem {

    public static final int BRIGHT_TICKS = 2_400;
    private static final String TOOLTIP_BRIGHT = "guzhenren.item.gu.little_light_bright";

    public LittleLightGuItem(GuProperties guProperties) {
        super(guProperties);
    }

    public static boolean isBright(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.LITTLE_LIGHT_BRIGHT, false);
    }

    //region the switch [开关]
    @Override
    protected int performOwnUse(ServerPlayer player, ItemStack stack) {
        feed(player, stack);
        if (isBright(stack)) {
            turnOff(player, stack);
            return 0;
        }
        if (stopsShortUse(player, stack)) return WARNED;

        boolean forced = isShortOfUse(player, stack);
        payRound(player, stack);
        stack.set(ModDataComponents.LITTLE_LIGHT_BRIGHT, true);
        stack.set(ModDataComponents.LITTLE_LIGHT_BRIGHT_UNTIL, player.level().getGameTime() + BRIGHT_TICKS);
        if (forced) stack.set(ModDataComponents.LITTLE_LIGHT_FORCED, true);
        return 0;
    }

    private void payRound(ServerPlayer player, ItemStack stack) {
        RefinedGuState state = getState(stack);
        stack.set(ModDataComponents.REFINED_GU_STATE,
                state.withHunger(Math.max(0, state.hunger() - guProperties.getHungerPerUse())));
        feedAndWarn(player, stack);
    }

    @Override
    protected long useThreshold(ItemStack stack) { return 0L; }

    private static void turnOff(ServerPlayer player, ItemStack stack) {
        boolean forced = stack.has(ModDataComponents.LITTLE_LIGHT_FORCED);
        stack.remove(ModDataComponents.LITTLE_LIGHT_BRIGHT);
        stack.remove(ModDataComponents.LITTLE_LIGHT_BRIGHT_UNTIL);
        stack.remove(ModDataComponents.LITTLE_LIGHT_FORCED);
        if (!forced || player.getAbilities().instabuild) return;

        exhaust(player, stack);
        stack.shrink(1);
    }
    //endregion

    //region the round [一轮]
    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slot,
                              boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || !isBright(stack)) return;

        Long until = stack.get(ModDataComponents.LITTLE_LIGHT_BRIGHT_UNTIL);
        if (until != null && level.getGameTime() < until) return;

        turnOff(player, stack);
    }
    //endregion

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (isBright(stack)) tooltip.add(Component.translatable(TOOLTIP_BRIGHT).withStyle(ChatFormatting.YELLOW));
    }
}
