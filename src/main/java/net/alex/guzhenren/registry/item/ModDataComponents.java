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
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The item components this mod adds, for state belonging to one particular stack.
 *
 * <p>DeferredRegister holder: {@link RefinedGuState}, {@code VITAL_OWNER}, {@code VITAL_APERTURE},
 * {@code STORED_STONES}, {@code REFINED_AT}, {@code AWAKEN_BASE}, {@code USED_AT}, {@code HEAL_BANK} and
 * {@link LeafGrowth}. A
 * component here is for state that not every tended Gu shares; anything all of them carry belongs on the shared state
 * record instead.
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
            DATA_COMPONENTS.registerComponentType("refined_gu_state", builder -> builder
                    .persistent(RefinedGuState.CODEC)
                    .networkSynchronized(RefinedGuState.STREAM_CODEC));
    public static final Supplier<DataComponentType<UUID>> VITAL_OWNER =
            DATA_COMPONENTS.registerComponentType("vital_owner", builder -> builder
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC));
    public static final Supplier<DataComponentType<Integer>> VITAL_APERTURE =
            DATA_COMPONENTS.registerComponentType("vital_aperture", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final Supplier<DataComponentType<Long>> STORED_STONES =
            DATA_COMPONENTS.registerComponentType("stored_stones", builder -> builder
                    .persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG));
    public static final Supplier<DataComponentType<Long>> REFINED_AT =
            DATA_COMPONENTS.registerComponentType("refined_at", builder -> builder
                    .persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG));
    public static final Supplier<DataComponentType<Integer>> AWAKEN_BASE =
            DATA_COMPONENTS.registerComponentType("awaken_base", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final Supplier<DataComponentType<Long>> USED_AT =
            DATA_COMPONENTS.registerComponentType("used_at", builder -> builder
                    .persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG));
    public static final Supplier<DataComponentType<Integer>> HEAL_BANK =
            DATA_COMPONENTS.registerComponentType("heal_bank", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final Supplier<DataComponentType<LeafGrowth>> LEAF_GROWTH =
            DATA_COMPONENTS.registerComponentType("leaf_growth", builder -> builder
                    .persistent(LeafGrowth.CODEC)
                    .networkSynchronized(LeafGrowth.STREAM_CODEC));

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }
}
