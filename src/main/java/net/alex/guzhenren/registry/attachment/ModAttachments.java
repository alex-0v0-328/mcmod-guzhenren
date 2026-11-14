package net.alex.guzhenren.registry.attachment;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishData;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitual;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorage;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.ShieldData;
import net.alex.guzhenren.gameplay.body.ShieldWindow;
import net.alex.guzhenren.gameplay.dimension.DimensionReturnData;
import net.alex.guzhenren.gameplay.dimension.TossStashData;
import net.alex.guzhenren.gameplay.mind.KillingMoveData;
import net.alex.guzhenren.gameplay.mind.MindData;
import net.alex.guzhenren.gameplay.path.PathData;
import net.alex.guzhenren.gameplay.path.PathEffectData;
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
 * 本模组挂在玩家身上的全部数据附件。
 *
 * <ul>
 *   <li>14 个记录附件（不可变，只经服务写）加 7 个临时字段；{@link #registerSynced} 存并只同步本
 *       人（{@code OWNER_ONLY}），{@link #registerSaved} 只存；{@code ESSENCE_CARRY}、{@code SHIELD_WINDOW}、
 *       {@code SECOND_STAGE_THROTTLE} 不存不同步，两端各一份，{@code SHIELD_WINDOW} 仅服务端写。</li>
 *   <li>{@code SECOND_STAGE}、{@code PLUNGE} 不存，同步给所有看得见该玩家的客户端（下称广播）以画前倾、下坠姿势；
 *       有即分别处于二段飞行[Second-Stage Flight]（重登即出）、坠击（值为下坠最高点）。</li>
 *   <li>{@code AWAKENING_RITUAL}：有即在开窍[Awakening]仪式中，存并广播，画悬浮希望蛊[Hope Gu]与头顶资质[Aptitude]牌
 *       匾（含十绝体质）；{@code APERTURE_NOURISH} 如 {@link #registerSynced} 般存、继承但广播，
 *       温养[Nourish]中画盘坐。</li>
 *   <li>他人需见的记录只放宽同步谓词，不另发下行 payload。</li>
 *   <li>服务经 {@code PlayerData} 存记录，setter 跳过相等值：NeoForge 每次 {@code setData} 都向本人发同步；
 *       记录按值比较，相等即客户端已有。{@code ESSENCE_CARRY} 唯一原地写：回复直接累进数组。</li>
 *   <li>两个辅助方法注册的都继承：NeoForge 随每次克隆复制，含死亡（{@code copyOnDeath}），{@link #getInherited}
 *       列入生命周期全量重置；新附件在此注册即被携带、重置；未保存者不随克隆。</li>
 *   <li>手写、死亡不带：{@code HEALTH_CARRY}；{@code PLAYER_REWIND}（人如故[Man As Before Gu]对刚死身体的记忆，
 *       存不同步）；{@code AWAKENING_RITUAL}（死亡在克隆前结束仪式）。{@code TOSS_STASH} 相反：死亡带走，无
 *       keepInventory 的死亡重置也不清，因存玩家自己的物品。</li>
 *   <li>⚠ {@code BORN} 须继承，否则下次登录重抽才情。</li>
 *   <li>⚠ 记录放功能包（aperture、body、soul、path、mind、dimension；{@code strength} 在 path 下）；常量名为类名去
 *       {@code Data}（{@code PathEffectData} 即 {@code PATH_EFFECT}）；勿用裸领域词，
 *       {@code qi}/{@code soul}/{@code strength} 也是 {@code GuPath} 名。</li>
 *   <li>⚠ ID 即存档键，改名丢全部存值；记录 {@code CODEC} 每字段用 {@code optionalFieldOf} 带默认值（列表、映射形的
 *       {@code ApertureData}、{@code PathData} 在条目记录里），旧档可读；例外 {@code DimensionReturnData.ReturnPoint}
 *       字段在可选 {@code point} 内必填，新增仍用 {@code optionalFieldOf}。</li>
 * </ul>
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
    public static final Supplier<AttachmentType<ApertureNourishData>> APERTURE_NOURISH = registerInherited(
            "nourish_data", () -> AttachmentType.builder(() -> ApertureNourishData.DEFAULT)
                    .serialize(ApertureNourishData.CODEC)
                    .copyOnDeath()
                    .sync(StreamCodec.of(ApertureNourishData::encode, ApertureNourishData::decode))
                    .build());
    public static final Supplier<AttachmentType<AwakeningRitual>> AWAKENING_RITUAL = ATTACHMENT_TYPES.register(
            "awakening_ritual", () -> AttachmentType.builder(() -> AwakeningRitual.DEFAULT)
                    .serialize(AwakeningRitual.CODEC)
                    .sync(StreamCodec.of(AwakeningRitual::encode, AwakeningRitual::decode))
                    .build());
    //endregion

    //region Body [肉身]
    public static final Supplier<AttachmentType<BodyData>> BODY =
            registerSynced("body_data", BodyData.DEFAULT, BodyData.CODEC,
                    StreamCodec.of(BodyData::encode, BodyData::decode));
    public static final Supplier<AttachmentType<ShieldData>> SHIELD =
            registerSynced("shield_data", ShieldData.DEFAULT, ShieldData.CODEC,
                    StreamCodec.of(ShieldData::encode, ShieldData::decode));
    public static final Supplier<AttachmentType<ShieldWindow>> SHIELD_WINDOW = ATTACHMENT_TYPES.register(
            "shield_window", () -> AttachmentType.builder(() -> ShieldWindow.CLOSED).build());
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
    public static final Supplier<AttachmentType<PathEffectData>> PATH_EFFECT = registerSynced(
            "path_effect_data", PathEffectData.DEFAULT, PathEffectData.CODEC,
            StreamCodec.of(PathEffectData::encode, PathEffectData::decode));
    //endregion

    //region Mind [脑海]
    public static final Supplier<AttachmentType<MindData>> MIND =
            registerSynced("mind_data", MindData.DEFAULT, MindData.CODEC,
                    StreamCodec.of(MindData::encode, MindData::decode));
    public static final Supplier<AttachmentType<KillingMoveData>> KILLING_MOVE =
            registerSynced("killing_move_data", KillingMoveData.DEFAULT, KillingMoveData.CODEC,
                    StreamCodec.of(KillingMoveData::encode, KillingMoveData::decode));
    //endregion

    //region Dimension return [维度往返]
    public static final Supplier<AttachmentType<DimensionReturnData>> DIMENSION_RETURN =
            registerSaved("dimension_return", DimensionReturnData.DEFAULT, DimensionReturnData.CODEC);
    //endregion

    //region Treasure Yellow Heaven [宝黄天]
    public static final Supplier<AttachmentType<TossStashData>> TOSS_STASH = ATTACHMENT_TYPES.register(
            "toss_stash", () -> AttachmentType.builder(() -> TossStashData.DEFAULT)
                    .serialize(TossStashData.CODEC, stash -> !stash.isEmpty())
                    .copyOnDeath()
                    .build());
    //endregion

    //region Rewind [如故]
    public static final Supplier<AttachmentType<PlayerRewindData>> PLAYER_REWIND = ATTACHMENT_TYPES.register(
            "player_rewind", () -> AttachmentType.builder(() -> PlayerRewindData.DEFAULT)
                    .serialize(PlayerRewindData.CODEC, data -> !data.isEmpty())
                    .build());
    //endregion

    //region Flight [飞行]
    public static final Supplier<AttachmentType<Boolean>> SECOND_STAGE = ATTACHMENT_TYPES.register(
            "second_stage_flag", () -> AttachmentType.builder(() -> Boolean.TRUE)
                    .sync(ByteBufCodecs.BOOL)
                    .build());
    public static final Supplier<AttachmentType<Double>> SECOND_STAGE_THROTTLE = ATTACHMENT_TYPES.register(
            "second_stage_throttle", () -> AttachmentType.builder(() -> 0.0D).build());
    //endregion

    //region Plunge [坠击]
    public static final Supplier<AttachmentType<Double>> PLUNGE = ATTACHMENT_TYPES.register(
            "plunge_top", () -> AttachmentType.builder(() -> 0.0D)
                    .sync(ByteBufCodecs.DOUBLE)
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
        return INHERITED.stream().<AttachmentType<?>>map(Supplier::get).toList();
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
