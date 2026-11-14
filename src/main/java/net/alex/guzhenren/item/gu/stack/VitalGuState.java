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
 * 物品的本命蛊绑定：{@code VITAL_OWNER}、{@code VITAL_APERTURE}、{@code VITAL_TOKEN} 三组件的唯一读写方。
 * <ul>
 *   <li>任何物品都能答；只有持续照顾型蛊[Tended Gu]会被绑定，{@link MortalGuItem} 显示标记。</li>
 *   <li>按种类（{@code ApertureData.PRIMARY} / {@code SECOND}）而非位置认空窍[Aperture]：希望蛊[Hope Gu]把第一空窍插到
 *       单独的第二空窍前，旧绑定仍有效；{@link #bind} 存索引的种类，{@link #getBoundIndex} 映射回索引。</li>
 *   <li>每空窍一只：{@link #bind} 盖新令牌，与存储所记令牌相符者才是当前的（{@link #isCurrentVital}）；旧蛊无害。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see MortalGuItem
 * @since 1.0.0
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
