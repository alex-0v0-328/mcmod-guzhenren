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
 * 小光蛊[Little Light Gu]物品：一转持续照顾型蛊[Tended Gu]，催动作为手持强光开关而非单次法术效果。
 * <ul>
 *   <li>握在手中时常驻提供基础光照，已炼化后催动可消耗一轮饱食[Hunger]点亮 2 分钟强光；野生状态下不支持点亮开关，仅发出
 *       10 级基础光。</li>
 *   <li>动态光源由 {@code assets/guzhenren/dynamiclights/item/} 下的两份 LambDynamicLights 配置声明（任意小光蛊物品栈亮
 *       度 10，携带 {@link ModDataComponents#LITTLE_LIGHT_BRIGHT} 组件时亮度 15，取匹配项的最大值；未安装该模组时不产生
 *       动态光源）；点亮组件同步至客户端且仅在开启期间存在，熄灭时直接移除组件而非写入 {@code false}，以便匹配器区分开
 *       关状态并保证熄灭后的物品栈与新物品栈完全一致。</li>
 *   <li>⚠ 开关点亮不消耗真元（覆写 {@link #performOwnUse} 接管催动且 {@link #useThreshold} 恒返回 {@code 0L}）：每次点
 *       亮扣除一次 {@code GuProperties} 声明的单次催动饱食，扣点前后照常从背包自动进食，随后无论是否继续手持均持续点亮
 *       {@link #BRIGHT_TICKS} 服务端游戏时间（按世界时钟结算，不受玩家自身时间流速缩放影响）；亮起期间再次催动则提前熄
 *       灭，不退还已付饱食；作为杀招[Killing Move]辅蛊启动时扣除的也是同一份单次饱食。</li>
 *   <li>⚠ 截止时刻 {@link ModDataComponents#LITTLE_LIGHT_BRIGHT_UNTIL} 仅在开关切换时写入一次，绝不在每 tick 递减写回
 *       （每次修改组件都会触发该背包槽位全量网络同步）；由于仅随身背包物品栈会执行 {@link #inventoryTick}，放入箱子或丢
 *       在地上的已点亮小光蛊在超时后一旦被玩家重新拾回身上即刻熄灭。</li>
 *   <li>⚠ 饱食不足时点亮遵循强行催动[Forced Use]（{@code GuForcedUse}）规则：首次按下发出警告，5 秒内再次按下同一只蛊
 *       则强行点亮整轮，待熄灭时（无论自然到期或主动关灯）蛊虫力竭死亡；{@link ModDataComponents#LITTLE_LIGHT_FORCED}
 *       标记该轮强行点亮，与点亮组件同写同删；主动熄灭不设任何门槛；本命小光蛊在饱食不足时直接拒绝点亮；创造模式下既不
 *       警告也不致死。</li>
 * </ul>
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
