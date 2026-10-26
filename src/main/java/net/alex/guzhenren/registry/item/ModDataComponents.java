package net.alex.guzhenren.registry.item;

import com.mojang.serialization.Codec;
import java.util.UUID;
import java.util.function.Supplier;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.item.gu.RefinedGuState;
import net.alex.guzhenren.item.gu.mortal.wood.LeafGrowth;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The item components this mod adds, for state belonging to one particular stack.
 *
 * <p>DeferredRegister holder: {@link RefinedGuState}, {@code VITAL_OWNER}, {@code VITAL_APERTURE}, {@code VITAL_TOKEN},
 * {@code STORED_STONES}, {@code REFINED_AT}, {@code AWAKEN_BASE}, {@code USED_AT}, {@code HEAL_BANK} and {@link
 * LeafGrowth}. A component here is for state that not every tended Gu shares; anything all of them carry belongs on the
 * shared state record instead.
 *
 * <p>⚠ Game-clock timestamps default to {@code 0}, which is a real game time -- judge {@code null},
 * never {@code 0}, or a fresh world reads "already used".
 *
 * <p>⚠ A version seam: callers read and write through {@link #get}, {@link #getOrDefault}, {@link #set}, {@link #has}
 * and {@link #remove} only, never the stack's own data API, so they stay identical between this NeoForge 1.21.1
 * mainline (data components) and the Forge 1.20.1 port (item NBT).
 *
 * @author Alex
 * @version 1.0.0
 * @see RefinedGuState
 * @since 1.0.0
 */

public final class ModDataComponents {

    private ModDataComponents() {}

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Guzhenren.MOD_ID);
    public static final Supplier<DataComponentType<RefinedGuState>> REFINED_GU_STATE =
            register("refined_gu_state", RefinedGuState.CODEC,
                    StreamCodec.of(RefinedGuState::encode, RefinedGuState::decode));
    public static final Supplier<DataComponentType<UUID>> VITAL_OWNER =
            register("vital_owner", UUIDUtil.CODEC, UUIDUtil.STREAM_CODEC);
    public static final Supplier<DataComponentType<Integer>> VITAL_APERTURE = registerInt("vital_aperture");
    public static final Supplier<DataComponentType<Long>> VITAL_TOKEN = registerLong("vital_token");
    public static final Supplier<DataComponentType<Long>> STORED_STONES = registerLong("stored_stones");
    public static final Supplier<DataComponentType<Long>> REFINED_AT = registerLong("refined_at");
    public static final Supplier<DataComponentType<Integer>> AWAKEN_BASE = registerInt("awaken_base");
    public static final Supplier<DataComponentType<Long>> USED_AT = registerLong("used_at");
    public static final Supplier<DataComponentType<Integer>> HEAL_BANK = registerInt("heal_bank");
    public static final Supplier<DataComponentType<Long>> RECOVERED_AT = registerLong("recovered_at");
    public static final Supplier<DataComponentType<Long>> HEALED_AT = registerLong("healed_at");
    public static final Supplier<DataComponentType<LeafGrowth>> LEAF_GROWTH =
            register("leaf_growth", LeafGrowth.CODEC, StreamCodec.of(LeafGrowth::encode, LeafGrowth::decode));

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }

    public static <T> @Nullable T get(@NotNull ItemStack stack, @NotNull Supplier<DataComponentType<T>> key) {
        return stack.get(key.get());
    }

    public static <T> T getOrDefault(@NotNull ItemStack stack, @NotNull Supplier<DataComponentType<T>> key,
                                     T fallback) {
        return stack.getOrDefault(key.get(), fallback);
    }

    public static boolean has(@NotNull ItemStack stack, @NotNull Supplier<? extends DataComponentType<?>> key) {
        return stack.has(key.get());
    }

    /** Stores {@code value}; {@code null} removes the component. */
    public static <T> void set(@NotNull ItemStack stack, @NotNull Supplier<DataComponentType<T>> key,
                               @Nullable T value) {
        if (value == null) {
            stack.remove(key.get());
        } else {
            stack.set(key.get(), value);
        }
    }

    public static void remove(@NotNull ItemStack stack, @NotNull Supplier<? extends DataComponentType<?>> key) {
        stack.remove(key.get());
    }

    private static <T> Supplier<DataComponentType<T>> register(
            String id, Codec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        return DATA_COMPONENTS.registerComponentType(id, builder -> builder
                .persistent(codec)
                .networkSynchronized(streamCodec));
    }

    private static Supplier<DataComponentType<Long>> registerLong(String id) {
        return register(id, Codec.LONG, ByteBufCodecs.VAR_LONG);
    }

    private static Supplier<DataComponentType<Integer>> registerInt(String id) {
        return register(id, Codec.INT, ByteBufCodecs.VAR_INT);
    }
}
