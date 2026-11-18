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
 * 杀招[Killing Move]封闭注册表枚举：每一个枚举项定义一式杀招及其配套参数。
 * <ul>
 *   <li>每式杀招包含唯一 ID、主手可主导该招的主蛊列表、唯一辅蛊、伤害倍率以及开启期间每次施放由辅蛊额外支付的真元
 *       （{@link #getPartnerEssence}）；启动杀招时辅蛊须先支付一次自身单次催动的饱食[Hunger]（{@link #getStartHunger}，
 *       直接读取辅蛊的 {@code GuProperties}）；新增杀招只需在此添加枚举项并注册同 ID 的杀招书
 *       （{@link KillingMoveBookItem}）。例如 {@link #LITTLE_LIGHT_AMPLIFY} 由月光蛊[Moonlight Gu]或月芒蛊[Moonglow Gu]
 *       主导、小光蛊[Little Light Gu]辅助，使月刃伤害翻倍，启动消耗一次小光蛊饱食，每发月刃额外消耗 20 点真元。</li>
 *   <li>主蛊与辅蛊均须为已炼化的持续照顾型蛊[Tended Gu]；{@link #isLedBy}（采用简单循环以便轮盘与提示框在渲染帧内零分配
 *       查询）与 {@link #isBackedBy} 是判定主辅蛊资格的唯一入口，由客户端轮盘与服务端 {@link KillingMoveService} 共用；
 *       玩家是否已习得杀招（{@link KillingMoveService#knows}）以及辅蛊在背包或空窍存储中的检索均归
 *       {@link KillingMoveService} 负责。</li>
 *   <li>⚠ {@link #getId()} 同时作为玩家已学列表存档键（{@code KillingMoveData}）、网络包标识、杀招书物品 ID 与翻译键后
 *       缀，改名会导致已有存档丢失该杀招记录。</li>
 * </ul>
 */

public enum KillingMove {

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

    public int getStartHunger() { return ((TendedGuItem) partner.asItem()).getGuProperties().getHungerPerUse(); }

    public int getPartnerEssence() { return partnerEssence; }

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
