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
import net.alex.guzhenren.gameplay.mind.GuRecipeKnowledgeData;
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
 * 本模组挂载在玩家实体上的全部 NeoForge 数据附件类型定义。
 * <ul>
 *   <li>共包含 14 个不可变业务记录附件（仅通过领域服务写入）与 8 个辅助或状态标志字段：其中 {@link #registerSynced} 持
 *       久化并仅同步至本人（{@code OWNER_ONLY}），{@link #registerSaved} 仅持久化不同步；{@code ESSENCE_CARRY}、
 *       {@code SHIELD_WINDOW} 与 {@code SECOND_STAGE_THROTTLE} 为不持久化也不同步的瞬时缓存（{@code SHIELD_WINDOW} 仅服
 *       务端写入）。</li>
 *   <li>{@code SECOND_STAGE}（二段飞行）、{@code PLUNGE}（坠击最高点高度）与 {@code MOBILE_CHARGE}
 *       （惯力蛊[Habitual Strength Gu]可移动蓄力）为不持久化但向全部可见客户端广播的姿态标记，用于驱动他人视角下的飞行
 *       前倾、下落或上半身蓄力动画；{@code AWAKENING_RITUAL}（悬浮希望蛊与头顶资质[Aptitude]牌匾）与
 *       {@code APERTURE_NOURISH}（盘坐温养[Nourish]）亦向可见客户端广播（前者不随死亡继承，后者按常规继承并持久
 *       化）。</li>
 *   <li>需要让其他玩家客户端看见的状态仅通过放宽同步谓词实现广播，不为此另建下行网络载荷；业务层经 {@code PlayerData}
 *       更新记录时会自动跳过相等值以避免重复发包（唯独 {@code ESSENCE_CARRY} 直接原地累加浮点数组）。</li>
 *   <li>经 {@link #registerSynced} 与 {@link #registerSaved} 注册的附件均启用 {@code copyOnDeath} 并加入
 *       {@link #getInherited} 列表供生命周期全量重置；手动注册的字段则按语义分化：{@code HEALTH_CARRY}、
 *       {@code PLAYER_REWIND}（人如故[Man As Before Gu]对死亡前身体的记忆，存不同步）与 {@code AWAKENING_RITUAL} 不随死
 *       亡继承，而 {@code TOSS_STASH}（保存玩家自己的丢弃物）不仅随死亡携带且在非 {@code keepInventory} 死亡重置中也不
 *       清空。</li>
 *   <li>⚠ {@code BORN} 标记必须启用死亡继承，否则玩家在死亡重开或重登后会重新随机抽选才情。</li>
 *   <li>⚠ 数据记录类须归属各自功能包（如 {@code aperture}、{@code body}、{@code soul}、{@code path}、{@code mind}、
 *       {@code dimension}），且附件常量命名应取记录类名去掉 {@code Data} 后缀（如 {@code PathEffectData} 对应
 *       {@code PATH_EFFECT}），严禁使用裸领域词（因 {@code qi}、{@code soul}、{@code strength} 同时是 {@code GuPath} 枚
 *       举名）。</li>
 *   <li>⚠ 附件注册 ID 即存档键名，改名会导致对应旧存档数据全部丢失；各记录 {@code CODEC} 字段必须使用带默认值的
 *       {@code optionalFieldOf} 以保持旧档兼容（列表或映射成员如 {@code ApertureData}、{@code PathData} 在条目内部各自
 *       兜底，唯 {@code DimensionReturnData.ReturnPoint} 在可选 {@code point} 内部仍为必填字段）。</li>
 * </ul>
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
    public static final Supplier<AttachmentType<GuRecipeKnowledgeData>> GU_RECIPE_KNOWLEDGE = registerSynced(
            "gu_recipe_knowledge_data", GuRecipeKnowledgeData.DEFAULT, GuRecipeKnowledgeData.CODEC,
            StreamCodec.of(GuRecipeKnowledgeData::encode, GuRecipeKnowledgeData::decode));
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

    //region Strength [力道]
    public static final Supplier<AttachmentType<Boolean>> MOBILE_CHARGE = ATTACHMENT_TYPES.register(
            "mobile_charge_flag", () -> AttachmentType.builder(() -> Boolean.TRUE)
                    .sync(ByteBufCodecs.BOOL)
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
