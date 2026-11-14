package net.alex.guzhenren.item.material;

import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 元石[Primeval Stone]：右键将所含真元[Essence]灌入持有者空窍[Aperture]，真元值取自注册。
 * <ul>
 *   <li>⚠ 门槛拒绝池满与未开窍：服务写入对未开窍者静默无效，不拒绝则元石白吃。</li>
 *   <li>自动扣携带元石（补池线、付消耗、炼蛊台供给槽）归 {@code PrimevalStoneSupply}，本类只管元石与价值。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.material.GuMaterialItem
 * @since 1.0.0
 */

public class PrimevalStoneItem extends GuMaterialItem {

    private static final String FAILED_FULL = "guzhenren.item.failed.essence_full";
    private final long essence;

    public PrimevalStoneItem(Properties properties, long essence) {
        super(properties, Rank.ONE, GuPath.HEAVEN);
        this.essence = essence;
    }

    public long getEssence() { return essence; }

    @Override
    protected boolean hasUse() { return true; }

    @Override
    protected @Nullable Refusal checkUse(Player player, ItemStack stack) {
        if (!ApertureService.hasAperture(player)) return new Refusal(Refusal.UNAWAKENED);
        return ApertureEssenceService.getCurrentEssence(player) >= ApertureEssenceService.getMaxEssence(player)
                ? new Refusal(FAILED_FULL) : null;
    }

    @Override
    protected int performUse(ServerPlayer player, ItemStack stack) {
        int used = getUsableCount(player, stack);
        ApertureEssenceService.add(player, essence * used);
        return used;
    }

    private int getUsableCount(Player player, ItemStack stack) {
        long deficit = ApertureEssenceService.getMaxEssence(player) - ApertureEssenceService.getCurrentEssence(player);
        return (int) Math.min(stack.getCount(), (deficit + essence - 1) / essence);
    }
}
