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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

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
            register("refined_gu_state", RefinedGuState.CODEC, RefinedGuState.STREAM_CODEC);
    public static final Supplier<DataComponentType<UUID>> VITAL_OWNER =
            register("vital_owner", UUIDUtil.CODEC, UUIDUtil.STREAM_CODEC);
    public static final Supplier<DataComponentType<Integer>> VITAL_APERTURE = registerInt("vital_aperture");
    public static final Supplier<DataComponentType<Long>> VITAL_TOKEN = registerLong("vital_token");
    public static final Supplier<DataComponentType<Long>> STORED_STONES = registerLong("stored_stones");
    public static final Supplier<DataComponentType<Long>> REFINED_AT = registerLong("refined_at");
    public static final Supplier<DataComponentType<Integer>> AWAKEN_BASE = registerInt("awaken_base");
    public static final Supplier<DataComponentType<Long>> USED_AT = registerLong("used_at");
    public static final Supplier<DataComponentType<Integer>> HEAL_BANK = registerInt("heal_bank");
    public static final Supplier<DataComponentType<LeafGrowth>> LEAF_GROWTH =
            register("leaf_growth", LeafGrowth.CODEC, LeafGrowth.STREAM_CODEC);

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
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
