package net.alex.guzhenren.registry.attachment;

import com.mojang.serialization.Codec;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishData;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorage;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.dimension.DimensionReturnData;
import net.alex.guzhenren.gameplay.mind.MindData;
import net.alex.guzhenren.gameplay.path.PathData;
import net.alex.guzhenren.gameplay.path.qi.PathQiData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.soul.SoulData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Every data attachment this mod puts on a player.
 *
 * <p>DeferredRegister holder: the ten record attachments (immutable, written only through their
 * service) plus the three scratch fields ({@code ESSENCE_CARRY}, {@code BORN}, {@code HEALTH_CARRY}).
 * {@link #registerSynced} saves an attachment and syncs it to its owner alone ({@code OWNER_ONLY});
 * {@link #registerSaved} only saves it; {@code ESSENCE_CARRY} is neither saved nor synced.
 *
 * <p>⚠ Each record lives in its feature package (aperture, body, soul, path, mind, dimension), with
 * {@code qi} and {@code strength} as subpackages of path. A constant is its record's class name without
 * {@code Data} ({@code PathQiData} is {@code PATH_QI}). Never give an attachment the bare domain word --
 * {@code qi}/{@code soul}/{@code strength} are also {@code GuPath} names. The ids are save keys: renaming
 * one loses every saved value.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ModAttachments {

    private ModAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Guzhenren.MOD_ID);
    private static final BiPredicate<IAttachmentHolder, ServerPlayer> OWNER_ONLY =
            (holder, viewer) -> holder == viewer;

    //region Aperture [空窍]
    public static final Supplier<AttachmentType<ApertureData>> APERTURE =
            registerSynced("aperture_data", ApertureData.DEFAULT, ApertureData.CODEC, ApertureData.STREAM_CODEC);
    public static final Supplier<AttachmentType<float[]>> ESSENCE_CARRY = ATTACHMENT_TYPES.register(
            "essence_carry", () -> AttachmentType.builder(
                    () -> new float[ApertureData.MAX_APERTURES]).build());
    public static final Supplier<AttachmentType<ApertureStorage>> APERTURE_STORAGE =
            registerSaved("aperture_storage", ApertureStorage.DEFAULT, ApertureStorage.CODEC);
    public static final Supplier<AttachmentType<ApertureNourishData>> APERTURE_NOURISH = registerSynced(
            "nourish_data", ApertureNourishData.DEFAULT, ApertureNourishData.CODEC, ApertureNourishData.STREAM_CODEC);
    //endregion

    //region Body [肉身]
    public static final Supplier<AttachmentType<BodyData>> BODY =
            registerSynced("body_data", BodyData.DEFAULT, BodyData.CODEC, BodyData.STREAM_CODEC);
    //endregion

    //region Soul [魂魄]
    public static final Supplier<AttachmentType<SoulData>> SOUL =
            registerSynced("soul_data", SoulData.DEFAULT, SoulData.CODEC, SoulData.STREAM_CODEC);
    //endregion

    //region Path [流派]
    public static final Supplier<AttachmentType<PathData>> PATH =
            registerSynced("path_data", PathData.DEFAULT, PathData.CODEC, PathData.STREAM_CODEC);
    public static final Supplier<AttachmentType<PathStrengthData>> PATH_STRENGTH = registerSynced(
            "strength_data", PathStrengthData.DEFAULT, PathStrengthData.CODEC, PathStrengthData.STREAM_CODEC);
    public static final Supplier<AttachmentType<PathQiData>> PATH_QI =
            registerSynced("qi_data", PathQiData.DEFAULT, PathQiData.CODEC, PathQiData.STREAM_CODEC);
    //endregion

    //region Mind [脑海]
    public static final Supplier<AttachmentType<MindData>> MIND =
            registerSynced("mind_data", MindData.DEFAULT, MindData.CODEC, MindData.STREAM_CODEC);
    //endregion

    //region Dimension return [维度往返]
    public static final Supplier<AttachmentType<DimensionReturnData>> DIMENSION_RETURN =
            registerSaved("dimension_return", DimensionReturnData.DEFAULT, DimensionReturnData.CODEC);
    //endregion

    //region Lifecycle [生命周期]
    public static final Supplier<AttachmentType<Boolean>> BORN = registerSaved("born_flag", Boolean.FALSE, Codec.BOOL);
    public static final Supplier<AttachmentType<Float>> HEALTH_CARRY =
            registerSaved("health_carry", 0.0F, Codec.FLOAT);
    //endregion

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    private static <T> Supplier<AttachmentType<T>> registerSynced(
            String id, T defaultValue, Codec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        return ATTACHMENT_TYPES.register(id, () -> AttachmentType.builder(() -> defaultValue)
                .serialize(codec)
                .sync(OWNER_ONLY, streamCodec)
                .build());
    }

    private static <T> Supplier<AttachmentType<T>> registerSaved(String id, T defaultValue, Codec<T> codec) {
        return ATTACHMENT_TYPES.register(id, () -> AttachmentType.builder(() -> defaultValue).serialize(codec).build());
    }
}
