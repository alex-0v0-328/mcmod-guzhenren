package net.alex.guzhenren.registry.item;

import com.mojang.serialization.Codec;
import java.util.UUID;
import java.util.function.Supplier;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.item.gu.stack.RefinedGuState;
import net.alex.guzhenren.item.gu.mortal.path.wood.LeafGrowth;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 物品组件：单个物品堆独有的状态。
 *
 * <ul>
 *   <li>只放部分持续照顾型蛊[Tended Gu]才有的状态；全体共有的归 {@link RefinedGuState}。</li>
 *   <li>{@code VITAL_*} 只经 {@code VitalGuState} 读写。</li>
 *   <li>{@code LITTLE_LIGHT_BRIGHT} 亮时才有，供动态光源；{@code LITTLE_LIGHT_FORCED} 仅强行催动[Forced Use]轮内。</li>
 *   <li>⚠ 时间戳默认 {@code 0} 也是真实游戏时间：判 {@code null} 不判 {@code 0}，否则新世界误判已催动。</li>
 *   <li>经 NeoForge {@code Supplier} 重载直接用持有者读写；设 {@code null} 即移除。</li>
 * </ul>
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
    //region Every tended Gu [养护蛊共用]
    public static final Supplier<DataComponentType<RefinedGuState>> REFINED_GU_STATE =
            register("refined_gu_state", RefinedGuState.CODEC,
                    StreamCodec.of(RefinedGuState::encode, RefinedGuState::decode));
    public static final Supplier<DataComponentType<Long>> REFINED_AT = registerLong("refined_at");
    public static final Supplier<DataComponentType<Long>> USED_AT = registerLong("used_at");
    //endregion

    //region Vital Gu [本命蛊]
    public static final Supplier<DataComponentType<UUID>> VITAL_OWNER =
            register("vital_owner", UUIDUtil.CODEC, UUIDUtil.STREAM_CODEC);
    public static final Supplier<DataComponentType<Integer>> VITAL_APERTURE = registerInt("vital_aperture");
    public static final Supplier<DataComponentType<Long>> VITAL_TOKEN = registerLong("vital_token");
    //endregion

    //region Hunger clock [饱食时钟]
    public static final Supplier<DataComponentType<Long>> RECOVERED_AT = registerLong("recovered_at");
    public static final Supplier<DataComponentType<Long>> HEALED_AT = registerLong("healed_at");
    //endregion

    //region One Gu's own [单只蛊专用]
    public static final Supplier<DataComponentType<Integer>> AWAKEN_BASE = registerInt("awaken_base");
    public static final Supplier<DataComponentType<Long>> STORED_STONES = registerLong("stored_stones");
    public static final Supplier<DataComponentType<Integer>> HEAL_BANK = registerInt("heal_bank");
    public static final Supplier<DataComponentType<LeafGrowth>> LEAF_GROWTH =
            register("leaf_growth", LeafGrowth.CODEC, StreamCodec.of(LeafGrowth::encode, LeafGrowth::decode));
    public static final Supplier<DataComponentType<Boolean>> LITTLE_LIGHT_BRIGHT =
            register("little_light_bright", Codec.BOOL, ByteBufCodecs.BOOL);
    public static final Supplier<DataComponentType<Long>> LITTLE_LIGHT_BRIGHT_UNTIL =
            registerLong("little_light_bright_until");
    public static final Supplier<DataComponentType<Boolean>> LITTLE_LIGHT_FORCED =
            register("little_light_forced", Codec.BOOL, ByteBufCodecs.BOOL);
    //endregion

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
