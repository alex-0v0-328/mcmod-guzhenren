package net.alex.guzhenren.item.gu.killingmove;

import java.util.List;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 杀招[Killing Move]封闭总表，一行一招。
 * <ul>
 *   <li>每行：id、主手主蛊、唯一辅蛊、刃伤倍率、开启期间辅蛊每刃真元[Essence]；启动时辅蛊付一次自身催动饱食[Hunger]
 *       （{@link #getStartHunger}）；新招即新行，配同 id 杀招书（{@link KillingMoveBookItem}）。</li>
 *   <li>两蛊均为持续照顾型蛊[Tended Gu]，须已炼化；{@link #isLedBy}、{@link #isBackedBy} 是唯一资格判定，客户端轮盘与
 *       {@link KillingMoveService} 共用；是否学会（{@link KillingMoveService#knows}）、辅蛊放哪归该服务。</li>
 *   <li>⚠ id 即存档键、payload 招名、已学列表项（{@code KillingMoveData}）、书物品 id、翻译键；改名即全员遗忘。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see KillingMoveService
 * @since 1.0.0
 */

public enum KillingMove {

    /**
     * 小光蛊增幅：月光蛊[Moonlight Gu]、月芒蛊[Moonglow Gu]刃伤 ×2；辅蛊小光蛊[Little Light Gu]
     * 启动付一次催动饱食[Hunger]，每刃 20 真元[Essence]（其催动的一半）。
     */
    LITTLE_LIGHT_AMPLIFY("km_mortal_little_light_amplify", List.of(ModItems.MOONLIGHT_GU_1, ModItems.MOONGLOW_GU_2),
            ModItems.LITTLE_LIGHT_GU_1, 2.0F, 20);

    private final String id;
    private final List<ItemLike> mains;
    private final ItemLike partner;
    private final float damageFactor;
    private final int partnerEssence;

    KillingMove(String id, List<ItemLike> mains, ItemLike partner, float damageFactor, int partnerEssence) {
        this.id = id;
        this.mains = mains;
        this.partner = partner;
        this.damageFactor = damageFactor;
        this.partnerEssence = partnerEssence;
    }

    public String getId() { return id; }

    public String getTranslationKey() { return "guzhenren.killing_move." + id; }

    public List<ItemLike> getMains() { return mains; }

    public ItemLike getPartner() { return partner; }

    public float getDamageFactor() { return damageFactor; }

    /** 启动时辅蛊付的饱食[Hunger]：一次自身催动量，取自其 guProperties。 */
    public int getStartHunger() { return ((TendedGuItem) partner.asItem()).getGuProperties().getHungerPerUse(); }

    /** 开启期间辅蛊每刃付的真元[Essence]。 */
    public int getPartnerEssence() { return partnerEssence; }

    // Plain loop: the radial menu and the tooltip ask while drawing.
    public boolean isLedBy(Item item) {
        for (ItemLike main : mains) {
            if (main.asItem() == item) return true;
        }
        return false;
    }

    public boolean isBackedBy(@NotNull ItemStack stack) {
        return stack.getItem() == partner.asItem() && stack.getItem() instanceof TendedGuItem backer
                && backer.isRefined(stack);
    }

    public static @Nullable KillingMove byId(@NotNull String id) {
        for (KillingMove move : values()) {
            if (move.id.equals(id)) return move;
        }
        return null;
    }
}
