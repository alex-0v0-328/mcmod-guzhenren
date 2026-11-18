package net.alex.guzhenren.item.gu.stack;

import java.util.UUID;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 物品栈的本命蛊[Vital Gu]绑定状态工具：作为 {@code VITAL_OWNER}、{@code VITAL_APERTURE}、{@code VITAL_TOKEN} 三项数据
 * 组件的唯一读写入口。
 * <ul>
 *   <li>任意物品栈均可安全查询绑定状态，但只有持续照顾型蛊[Tended Gu]允许被绑定；凡蛊基类 {@link MortalGuItem} 负责在名
 *       称与光效上呈现本命标记。</li>
 *   <li>空窍归属按种类（{@code ApertureData.PRIMARY} / {@code SECOND}）而非下标索引记录：当希望蛊[Hope Gu]把第一空窍插
 *       入独立的第二空窍之前时，原有绑定依然有效；{@link #bind} 存入下标对应的空窍种类，{@link #getBoundIndex} 再按持有
 *       者当前空窍列表映射回索引。</li>
 *   <li>每个空窍至多认领一只本命蛊：{@link #bind} 会写入新的随机令牌，只有与空窍存储所记令牌一致的物品才被视为当前有效
 *       本命蛊（{@link #isCurrentVital}），旧蛊失效且无副作用。</li>
 * </ul>
 */

public final class VitalGuState {

    private VitalGuState() {}

    public static @Nullable UUID getOwner(ItemStack stack) {
        return stack.get(ModDataComponents.VITAL_OWNER);
    }

    public static boolean isVital(ItemStack stack) {
        return stack.has(ModDataComponents.VITAL_OWNER);
    }

    public static boolean isVitalOf(ItemStack stack, Player player) { return player.getUUID().equals(getOwner(stack)); }

    public static boolean isForeignVital(ItemStack stack, Player player) {
        return isVital(stack) && !isVitalOf(stack, player);
    }

    public static int getBoundAperture(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.VITAL_APERTURE, ApertureData.PRIMARY);
    }

    public static int getBoundIndex(ItemStack stack, Player owner) {
        return ApertureService.get(owner).indexOf(getBoundAperture(stack));
    }

    public static long getToken(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.VITAL_TOKEN, 0L);
    }

    public static boolean isCurrentVital(ItemStack stack, ServerPlayer owner) {
        long token = getToken(stack);
        return token != 0L && isVitalOf(stack, owner)
                && token == ApertureStorageService.getVitalToken(owner, getBoundAperture(stack));
    }

    public static void bind(ItemStack stack, ServerPlayer player, int index) {
        long token = player.getRandom().nextLong();
        stack.set(ModDataComponents.VITAL_OWNER, player.getUUID());
        stack.set(ModDataComponents.VITAL_APERTURE, ApertureService.get(player).getKindAt(index));
        stack.set(ModDataComponents.VITAL_TOKEN, token == 0L ? 1L : token);
    }

    public static void setBoundAperture(ItemStack stack, int kind) {
        stack.set(ModDataComponents.VITAL_APERTURE, kind);
    }

    public static void unbind(ItemStack stack) {
        stack.remove(ModDataComponents.VITAL_OWNER);
        stack.remove(ModDataComponents.VITAL_APERTURE);
        stack.remove(ModDataComponents.VITAL_TOKEN);
    }
}
