package net.alex.guzhenren.client.hud;

import net.alex.guzhenren.gameplay.aperture.Talent;
import net.alex.guzhenren.item.GuItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 蓄力催动蛊时快捷栏上方的蓄力条，注册在原版氧气层之上。
 *
 * <ul>
 *   <li>读手持物品在 {@link net.alex.guzhenren.item.GuItem} 的 {@code getUseDuration} 与 {@code chargeFraction}：物品给
 *       出比例就用，否则退回原版剩余 tick 比例。</li>
 *   <li>开窍[Awakening]仪式期间改显资质[Aptitude]条：{@link AwakeningHud#getBarBase} 爬升到掷出基础值，填充中灰色，满后
 *       默认蓄力色，标题同资质行（八成七）。</li>
 *   <li>蓄力蛊[Store Strength Gu]蓄力时（{@link StoreStrengthCharge#isCharging}）显示该蓄力：填充中灰色，服务端判满
 *       （{@link StoreStrengthCharge#isFull}）后默认蓄力色，标题“蓄力中”/“蓄满”。</li>
 *   <li>{@link #isShowing} 是“蓄力催动进行中”唯一判定；{@link NourishHud} 也读它，把自己的条叠在本条上方。</li>
 *   <li>⚠ 催动带持续时间的蛊自动得本条，新增蓄力类蛊无需改这里。</li>
 *   <li>叶子类标题写成 {@code a / b} 时须给出同一对数值的 {@code chargeFraction}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ChargeHud extends HotbarHud {

    public static final ChargeHud INSTANCE = new ChargeHud();

    private ChargeHud() {}

    private static final int AWAKENING_FILLING_COLOR = 0xFFCCCCCC;
    private static final String STORE_STRENGTH = "guzhenren.hud.store_strength";
    private static final String STORE_STRENGTH_FULL = "guzhenren.hud.store_strength.full";
    static final HudBar AWAKENING = HudBar.of("awakening", BAR_WIDTH, BAR_HEIGHT);

    static boolean isShowing(Minecraft minecraft, LocalPlayer player) {
        if (minecraft.options.hideGui || player.isSpectator()) return false;
        if (AwakeningHud.INSTANCE.getBarBase(player, 0.0F) >= 0) return true;
        if (StoreStrengthCharge.INSTANCE.isCharging()) return true;
        if (!player.isUsingItem()) return false;

        ItemStack stack = player.getUseItem();
        return stack.getItem() instanceof GuItem gu && gu.getUseDuration(stack, player) > 0;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !isShowing(minecraft, player)) return;

        int x = getBarLeft(minecraft);
        int y = getBarTop(minecraft);
        float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        int awakening = AwakeningHud.INSTANCE.getBarBase(player, partialTick);
        if (awakening >= 0) {
            int color = AwakeningHud.INSTANCE.isBarFull(player, partialTick) ? GuItem.CHARGE_COLOR_DEFAULT
                    : AWAKENING_FILLING_COLOR;
            draw(graphics, minecraft, AWAKENING, x, y, awakening / 100.0F, color,
                    Talent.formatPercent(awakening));
            return;
        }

        if (StoreStrengthCharge.INSTANCE.isCharging()) {
            boolean full = StoreStrengthCharge.INSTANCE.isFull();
            draw(graphics, minecraft, HOTBAR, x, y, StoreStrengthCharge.INSTANCE.getDisplayProgress(),
                    full ? GuItem.CHARGE_COLOR_DEFAULT : AWAKENING_FILLING_COLOR,
                    Component.translatable(full ? STORE_STRENGTH_FULL : STORE_STRENGTH));
            return;
        }

        ItemStack stack = player.getUseItem();
        GuItem gu = (GuItem) stack.getItem();
        int total = gu.getUseDuration(stack, player);
        int remaining = player.getUseItemRemainingTicks();
        Float own = gu.chargeFraction(stack, remaining);
        float progress = own != null ? own : 1.0F - remaining / (float) total;
        draw(graphics, minecraft, HOTBAR, x, y, progress, gu.chargeColor(stack, remaining),
                gu.chargeCaption(stack, remaining));
    }
}
