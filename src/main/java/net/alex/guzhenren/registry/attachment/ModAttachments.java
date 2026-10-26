package net.alex.guzhenren.registry.attachment;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
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
import net.alex.guzhenren.gameplay.rewind.ChunkMemory;
import net.alex.guzhenren.gameplay.rewind.PlayerRewindData;
import net.alex.guzhenren.gameplay.soul.SoulData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Every data attachment this mod puts on a player, and the one it puts on a chunk.
 *
 * <p>DeferredRegister holder: the eleven record attachments (immutable, written only through their
 * service) plus the four scratch fields ({@code ESSENCE_CARRY}, {@code BORN}, {@code HEALTH_CARRY},
 * {@code SECOND_STAGE}), and {@code CHUNK_MEMORY}, the only attachment on a {@code LevelChunk} rather than a player:
 * the land's past day for the Mountain and River As Before Gu [山如故 / 江如故], saved in the chunk's own file
 * only while it holds something, and read and written through {@code ChunkRewindService} alone.
 * {@link #registerSynced} saves an attachment and syncs it to its owner alone ({@code OWNER_ONLY});
 * {@link #registerSaved} only saves it; {@code ESSENCE_CARRY} is neither saved nor synced, and
 * {@code SECOND_STAGE} is synced but never saved: it counts by presence, a player who has it flies the second stage
 * [二段飞行], and a relog starts outside it. A record other players must see widens its sync predicate; it
 * never gets a downstream payload.
 *
 * <p>Services store their records through {@code PlayerData}, whose setters skip an equal value: NeoForge sends a
 * synced attachment to its owner on every {@code setData}, equal or not, so a store that changes nothing writes and
 * sends nothing. The records compare by value, so equal means the client already holds it. {@code ESSENCE_CARRY}
 * is the one attachment written in place: regen banks into the array itself, never through {@code setData}.
 *
 * <p>Every attachment the two helpers register is inherited: NeoForge copies it to every clone, a death
 * included ({@code copyOnDeath}), and {@link #getInherited} lists it for the lifecycle's full reset, so a new
 * attachment is carried and reset by being registered here, nowhere else. {@code HEALTH_CARRY} is built by hand
 * because a death must not carry it, and so is {@code PLAYER_REWIND}, the Man As Before Gu's [人如故] memory of a
 * body that the death just ended; {@code ESSENCE_CARRY} and {@code SECOND_STAGE} are unsaved, so no clone carries
 * them. ⚠ {@code BORN} stays inherited, or
 * the next login re-rolls brilliance.
 *
 * <p>⚠ Each record lives in its feature package (aperture, body, soul, path, mind, dimension), with
 * {@code qi} and {@code strength} as subpackages of path. A constant is its record's class name without
 * {@code Data} ({@code PathQiData} is {@code PATH_QI}). Never give an attachment the bare domain word --
 * {@code qi}/{@code soul}/{@code strength} are also {@code GuPath} names. The ids are save keys: renaming
 * one loses every saved value.
 *
 * <p>⚠ A record's {@code CODEC} reads every field with {@code optionalFieldOf} and that field's default (the
 * list- and map-shaped {@code ApertureData}, {@code PathData} and {@code PathQiData} in their entry records), so
 * a save written before a field existed still loads. {@code DimensionReturnData.ReturnPoint} is the one
 * exception, its fields required inside an optional {@code point}; a field added there still takes
 * {@code optionalFieldOf}.
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
    private static final List<Supplier<AttachmentType<?>>> INHERITED = new ArrayList<>();

    //region Aperture [空窍]
    public static final Supplier<AttachmentType<ApertureData>> APERTURE =
            registerSynced("aperture_data", ApertureData.DEFAULT, ApertureData.CODEC,
                    StreamCodec.of(ApertureData::encode, ApertureData::decode));
    public static final Supplier<AttachmentType<float[]>> ESSENCE_CARRY = ATTACHMENT_TYPES.register(
            "essence_carry", () -> AttachmentType.builder(
                    () -> new float[ApertureData.MAX_APERTURES]).build());
    public static final Supplier<AttachmentType<ApertureStorage>> APERTURE_STORAGE =
            registerSaved("aperture_storage", ApertureStorage.DEFAULT, ApertureStorage.CODEC);
    public static final Supplier<AttachmentType<ApertureNourishData>> APERTURE_NOURISH = registerSynced(
            "nourish_data", ApertureNourishData.DEFAULT, ApertureNourishData.CODEC,
            StreamCodec.of(ApertureNourishData::encode, ApertureNourishData::decode));
    //endregion

    //region Body [肉身]
    public static final Supplier<AttachmentType<BodyData>> BODY =
            registerSynced("body_data", BodyData.DEFAULT, BodyData.CODEC,
                    StreamCodec.of(BodyData::encode, BodyData::decode));
    //endregion

    //region Soul [魂魄]
    public static final Supplier<AttachmentType<SoulData>> SOUL =
            registerSynced("soul_data", SoulData.DEFAULT, SoulData.CODEC,
                    StreamCodec.of(SoulData::encode, SoulData::decode));
    //endregion

    //region Path [流派]
    public static final Supplier<AttachmentType<PathData>> PATH =
            registerSynced("path_data", PathData.DEFAULT, PathData.CODEC,
                    StreamCodec.of(PathData::encode, PathData::decode));
    public static final Supplier<AttachmentType<PathStrengthData>> PATH_STRENGTH = registerSynced(
            "strength_data", PathStrengthData.DEFAULT, PathStrengthData.CODEC,
            StreamCodec.of(PathStrengthData::encode, PathStrengthData::decode));
    public static final Supplier<AttachmentType<PathQiData>> PATH_QI =
            registerSynced("qi_data", PathQiData.DEFAULT, PathQiData.CODEC,
                    StreamCodec.of(PathQiData::encode, PathQiData::decode));
    //endregion

    //region Mind [脑海]
    public static final Supplier<AttachmentType<MindData>> MIND =
            registerSynced("mind_data", MindData.DEFAULT, MindData.CODEC,
                    StreamCodec.of(MindData::encode, MindData::decode));
    //endregion

    //region Dimension return [维度往返]
    public static final Supplier<AttachmentType<DimensionReturnData>> DIMENSION_RETURN =
            registerSaved("dimension_return", DimensionReturnData.DEFAULT, DimensionReturnData.CODEC);
    //endregion

    //region Rewind [如故] -- saved, never synced; the chunk one lives on the chunk
    public static final Supplier<AttachmentType<PlayerRewindData>> PLAYER_REWIND = ATTACHMENT_TYPES.register(
            "player_rewind", () -> AttachmentType.builder(() -> PlayerRewindData.DEFAULT)
                    .serialize(PlayerRewindData.CODEC, data -> !data.isEmpty())
                    .build());
    public static final Supplier<AttachmentType<ChunkMemory>> CHUNK_MEMORY = ATTACHMENT_TYPES.register(
            "chunk_memory", () -> AttachmentType.builder(() -> ChunkMemory.EMPTY)
                    .serialize(ChunkMemory.CODEC, memory -> !memory.isEmpty())
                    .build());
    //endregion

    //region Flight [飞行]
    public static final Supplier<AttachmentType<Boolean>> SECOND_STAGE = ATTACHMENT_TYPES.register(
            "second_stage_flag", () -> AttachmentType.builder(() -> Boolean.TRUE)
                    .sync(OWNER_ONLY, ByteBufCodecs.BOOL)
                    .build());
    //endregion

    //region Lifecycle [生命周期]
    public static final Supplier<AttachmentType<Boolean>> BORN = registerSaved("born_flag", Boolean.FALSE, Codec.BOOL);
    public static final Supplier<AttachmentType<Float>> HEALTH_CARRY = ATTACHMENT_TYPES.register(
            "health_carry", () -> AttachmentType.builder(() -> 0.0F).serialize(Codec.FLOAT).build());
    //endregion

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    public static List<AttachmentType<?>> getInherited() {
        return INHERITED.stream().map(Supplier::get).toList();
    }

    private static <T> Supplier<AttachmentType<T>> registerSynced(
            String id, T defaultValue, Codec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        return registerInherited(id, () -> AttachmentType.builder(() -> defaultValue)
                .serialize(codec)
                .copyOnDeath()
                .sync(OWNER_ONLY, streamCodec)
                .build());
    }

    private static <T> Supplier<AttachmentType<T>> registerSaved(String id, T defaultValue, Codec<T> codec) {
        return registerInherited(id, () -> AttachmentType.builder(() -> defaultValue)
                .serialize(codec)
                .copyOnDeath()
                .build());
    }

    private static <T> Supplier<AttachmentType<T>> registerInherited(String id, Supplier<AttachmentType<T>> type) {
        Supplier<AttachmentType<T>> holder = ATTACHMENT_TYPES.register(id, type);
        INHERITED.add(holder::get);
        return holder;
    }
}
