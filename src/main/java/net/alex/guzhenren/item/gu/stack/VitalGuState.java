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
 * The Vital Gu [本命蛊] binding a stack carries: the one reader and writer of the {@code VITAL_OWNER},
 * {@code VITAL_APERTURE} and {@code VITAL_TOKEN} data components. It asks nothing of the item, so any stack
 * answers; only a tended Gu ever gets bound, and {@link MortalGuItem} shows the mark.
 *
 * <p>A Vital Gu names its aperture by kind ({@code ApertureData.PRIMARY} or {@code SECOND}), never by
 * position, so the binding survives Hope Gu inserting a first aperture ahead of a lone second one wherever
 * the Gu sits: {@link #bind} stores the kind of the holder's index, {@link #getBoundIndex} maps it back. An
 * aperture has one Vital Gu: {@link #bind} also stamps a fresh token, and only the stack whose token the
 * store holds for that kind is the current one ({@link #isCurrentVital}); a replaced one is harmless.
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
