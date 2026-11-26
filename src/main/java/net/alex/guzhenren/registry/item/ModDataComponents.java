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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 挂载在单个物品堆上的持久同步 {@link DataComponentType} 注册表。
 * <ul>
 *   <li>全体持续照顾型蛊[Tended Gu]共用的核心状态归 {@link RefinedGuState} 管理，本类其余组件仅覆盖本命蛊绑定
 *       （{@code VITAL_*}，仅经 {@code VitalGuState} 读写）、饱食时钟与单蛊专属字段（例如小光蛊的动态光源标记
 *       {@code LITTLE_LIGHT_BRIGHT} 与强行催动[Forced Use]轮次标记 {@code LITTLE_LIGHT_FORCED}）。</li>
 *   <li>⚠ 时间戳组件的 {@code 0L} 值代表合法的游戏零刻：判断组件是否已写入必须检查 {@code null} 而禁止判 {@code 0}，否
 *       则新创建的世界会被误判为刚刚催动过。</li>
 * </ul>
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

    //region Gu recipe [蛊方]
    public static final Supplier<DataComponentType<ResourceLocation>> RECIPE_ID =
            register("recipe_id", ResourceLocation.CODEC, ResourceLocation.STREAM_CODEC);
    //endregion

    //region Clothing [服饰]
    public static final Supplier<DataComponentType<DyeColor>> UNIFORM_COLOR =
            register("uniform_color", DyeColor.CODEC, DyeColor.STREAM_CODEC);
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
