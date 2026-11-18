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
 * 元石[Primeval Stone]物品：右键将蕴含的真元注入持有者空窍，单枚真元数值由注册参数决定。
 * <ul>
 *   <li>⚠ 门槛必须显式拒绝真元池已满与尚未开窍的玩家：底层服务对未开窍者的写入静默无操作，若不提前拒绝会导致元石被白
 *       白消耗。</li>
 *   <li>自动扣除随身元石（补池线、支付催动消耗与炼蛊台供给槽）均归 {@code PrimevalStoneSupply} 管理，本类只负责元石本
 *       身的使用与数值承载。</li>
 * </ul>
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
