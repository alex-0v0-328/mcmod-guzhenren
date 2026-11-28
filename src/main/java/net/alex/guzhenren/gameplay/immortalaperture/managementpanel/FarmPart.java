package net.alex.guzhenren.gameplay.immortalaperture.managementpanel;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmRules;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmState;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmWork;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FieldKind;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FieldPlans;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FieldPlot;
import net.alex.guzhenren.gameplay.immortalaperture.plot.Siting;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandCensus;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandHostingJob;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandNoticeEvent;
import net.alex.guzhenren.gameplay.immortalaperture.storage.BlissfulStorage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * 「种植」小节面板段（信息页统筹卡）：按作物计成熟 / 未熟、标准田与规划数、库藏种子合计 / 骨粉 / 腐泥冻土、托管开关与上一轮
 * 结果；地图上的规划、一键收获与选中田的操作也登记在这里。
 * <ul>
 *   <li>{@link Plan} 经 {@link FieldPlans}（选址复判与队列）；{@link PlanMany} 是开垦工具拖框，客户端只交它预判合格的区块，
 *       服务端逐块复判，不合格的跳过并报第一条原因。</li>
 *   <li>选中田的操作都带区块列表（至多 {@value #LIMIT} 个，服务端按账重找，不是田的跳过）：{@link Sow} 种植：给能种的田
 *       （{@link FarmWork#accepts}，田里没有别的作物）定作物并补满空格，{@link Uproot} 清除作物并重置为未选，{@link Retype} 改土质（改蛊植田先按所选普通田合计扣腐泥冻土，不够一块都
 *       不改），{@link Ripen} 骨粉催熟（{@link FarmWork#ripen}），{@link HarvestFields} 只收所选，{@link Clear} 拆田并取消
 *       所选规划；{@link Harvest} 是地图左上「一键收获」，收全部田。</li>
 *   <li>规则都在 {@link FarmWork}；托管每轮至多 {@value FarmRules#HOSTING_FIELDS} 块田，结果记进 {@link FarmState}，页面显示下一
 *       轮倒计时与上一轮结果。</li>
 * </ul>
 */

public final class FarmPart implements PanelSectionSource<FarmPart.Section> {

    public static final FarmPart INSTANCE = new FarmPart();

    private FarmPart() {}

    public record Crop(@NotNull ResourceLocation block, int mature, int growing) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Crop> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Crop::block,
                ByteBufCodecs.VAR_INT, Crop::mature,
                ByteBufCodecs.VAR_INT, Crop::growing,
                Crop::new);
    }

    public record Section(@NotNull List<Crop> rows, int fields, int planned, int capacity, boolean hosting,
                          int hostingIn, int lastReaped, int lastSown, long seeds, long boneMeal, long mud)
            implements PanelSection {

        public static final Section EMPTY = new Section(List.of(), 0, 0, 0, false, 0, 0, 0, 0L, 0L, 0L);
        public static final StreamCodec<RegistryFriendlyByteBuf, Section> STREAM_CODEC =
                StreamCodec.of(Section::encode, Section::decode);

        private static void encode(RegistryFriendlyByteBuf buffer, Section section) {
            Crop.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, section.rows);
            for (int value : new int[] {section.fields, section.planned, section.capacity, section.hostingIn,
                    section.lastReaped, section.lastSown}) {
                buffer.writeVarInt(value);
            }
            buffer.writeBoolean(section.hosting);
            for (long value : new long[] {section.seeds, section.boneMeal, section.mud}) buffer.writeVarLong(value);
        }

        private static Section decode(RegistryFriendlyByteBuf buffer) {
            List<Crop> rows = Crop.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buffer);
            int fields = buffer.readVarInt();
            int planned = buffer.readVarInt();
            int capacity = buffer.readVarInt();
            int hostingIn = buffer.readVarInt();
            int lastReaped = buffer.readVarInt();
            int lastSown = buffer.readVarInt();
            boolean hosting = buffer.readBoolean();
            long seeds = buffer.readVarLong();
            long boneMeal = buffer.readVarLong();
            long mud = buffer.readVarLong();
            return new Section(rows, fields, planned, capacity, hosting, hostingIn, lastReaped, lastSown, seeds,
                    boneMeal, mud);
        }

        @Override
        public @NotNull PanelSectionSource<?> source() { return INSTANCE; }
    }

    //region Actions [操作]
    /** 地图上选中的田（含规划田）：区块坐标（{@code ChunkPos#toLong}），至多 {@value #LIMIT} 个，服务端按账重找。 */
    public static final int LIMIT = Siting.FIELD_CAP;
    private static final StreamCodec<ByteBuf, List<Long>> CHUNKS = ByteBufCodecs.VAR_LONG.apply(
            ByteBufCodecs.list(LIMIT));

    public record Plan(int chunkX, int chunkZ) implements PanelAction {

        public static final PanelAction.Type<Plan> TYPE = new PanelAction.Type<>(Plan.class,
                StreamCodec.composite(ByteBufCodecs.VAR_INT, Plan::chunkX, ByteBufCodecs.VAR_INT, Plan::chunkZ,
                        Plan::new));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            Siting.Reason reason = FieldPlans.plan(context.level(), context.data(),
                    MapPart.plot(context.level(), context.data()), chunkX, chunkZ);
            if (reason != Siting.Reason.OK) {
                return LandNoticeEvent.text("siting_refused", Component.translatable(reason.key()));
            }
            return LandNoticeEvent.text("planned_many", 1, FarmState.of(context.data()).plans().size());
        }
    }

    public record PlanMany(@NotNull List<Long> chunks) implements PanelAction {

        public static final PanelAction.Type<PlanMany> TYPE = new PanelAction.Type<>(PlanMany.class,
                CHUNKS.map(PlanMany::new, PlanMany::chunks));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            int planned = 0;
            Siting.Reason first = Siting.Reason.OK;
            for (long chunk : chunks) {
                Siting.Reason reason = FieldPlans.plan(context.level(), context.data(),
                        MapPart.plot(context.level(), context.data()), ChunkPos.getX(chunk), ChunkPos.getZ(chunk));
                if (reason == Siting.Reason.OK) planned++;
                else if (first == Siting.Reason.OK) first = reason;
            }
            if (planned == chunks.size()) {
                return LandNoticeEvent.text("planned_many", planned, FarmState.of(context.data()).plans().size());
            }
            return LandNoticeEvent.text("planned_some", planned, chunks.size() - planned,
                    Component.translatable(first.key()));
        }
    }

    public record Harvest() implements PanelAction {

        public static final PanelAction.Type<Harvest> TYPE = new PanelAction.Type<>(Harvest.class,
                StreamCodec.unit(new Harvest()));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            FarmWork.Harvest done = FarmWork.harvest(context.level(), context.data(), Integer.MAX_VALUE);
            return LandNoticeEvent.text("harvested", done.reaped(), done.sown());
        }
    }

    public record SetHosting(boolean on) implements PanelAction {

        public static final PanelAction.Type<SetHosting> TYPE = new PanelAction.Type<>(SetHosting.class,
                ByteBufCodecs.BOOL.map(SetHosting::new, SetHosting::on));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            FarmState.of(context.data()).setHosting(on);
            return Component.empty();
        }
    }

    private static List<FieldPlot> fields(BlissfulLandData data, List<Long> chunks) {
        FarmState state = FarmState.of(data);
        List<FieldPlot> fields = new ArrayList<>();
        for (long chunk : chunks) {
            FieldPlot field = state.fieldAt(ChunkPos.getX(chunk), ChunkPos.getZ(chunk));
            if (field != null && !fields.contains(field)) fields.add(field);
        }
        return fields;
    }

    public record HarvestFields(@NotNull List<Long> chunks) implements PanelAction {

        public static final PanelAction.Type<HarvestFields> TYPE = new PanelAction.Type<>(HarvestFields.class,
                CHUNKS.map(HarvestFields::new, HarvestFields::chunks));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            List<FieldPlot> fields = fields(context.data(), chunks);
            if (fields.isEmpty()) return LandNoticeEvent.text("no_field");
            FarmWork.Harvest done = FarmWork.harvest(context.level(), context.data(), fields, Integer.MAX_VALUE);
            return LandNoticeEvent.text("harvested", done.reaped(), done.sown());
        }
    }

    public record Sow(@NotNull List<Long> chunks, int crop) implements PanelAction {

        public static final PanelAction.Type<Sow> TYPE = new PanelAction.Type<>(Sow.class,
                StreamCodec.composite(CHUNKS, Sow::chunks, ByteBufCodecs.VAR_INT, Sow::crop, Sow::new));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            List<FieldPlot> fields = fields(context.data(), chunks);
            if (fields.isEmpty()) return LandNoticeEvent.text("no_field");
            if (FarmState.clampCrop(crop) == FarmState.NO_CROP) return Component.empty();
            fields.removeIf(field -> !FarmWork.accepts(context.level(), field, crop));
            if (fields.isEmpty()) return LandNoticeEvent.text("uproot_first");
            FarmState state = FarmState.of(context.data());
            for (FieldPlot field : fields) state.setCrop(field, crop);
            int sown = FarmWork.sow(context.level(), context.data(), fields);
            return LandNoticeEvent.text("sown", fields.size(), FarmRules.crops().get(crop).getName(), sown);
        }
    }

    public record Uproot(@NotNull List<Long> chunks) implements PanelAction {

        public static final PanelAction.Type<Uproot> TYPE = new PanelAction.Type<>(Uproot.class,
                CHUNKS.map(Uproot::new, Uproot::chunks));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            List<FieldPlot> fields = fields(context.data(), chunks);
            if (fields.isEmpty()) return LandNoticeEvent.text("no_field");
            int cleared = FarmWork.uproot(context.level(), context.data(), fields);
            return LandNoticeEvent.text("uprooted", fields.size(), cleared);
        }
    }

    public record Ripen(@NotNull List<Long> chunks) implements PanelAction {

        public static final PanelAction.Type<Ripen> TYPE = new PanelAction.Type<>(Ripen.class,
                CHUNKS.map(Ripen::new, Ripen::chunks));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            List<FieldPlot> fields = fields(context.data(), chunks);
            if (fields.isEmpty()) return LandNoticeEvent.text("no_field");
            FarmWork.Ripen done = FarmWork.ripen(context.level(), context.data(), fields);
            if (done.unripe() == 0) return LandNoticeEvent.text("nothing_to_ripen");
            if (done.ripened() < done.unripe()) {
                return LandNoticeEvent.text("ripened_short", done.ripened(), done.unripe() - done.ripened());
            }
            return LandNoticeEvent.text("ripened", done.ripened());
        }
    }

    public record Retype(@NotNull List<Long> chunks, int kind) implements PanelAction {

        public static final PanelAction.Type<Retype> TYPE = new PanelAction.Type<>(Retype.class,
                StreamCodec.composite(CHUNKS, Retype::chunks, ByteBufCodecs.VAR_INT, Retype::kind, Retype::new));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            FieldKind target = FieldKind.byId(kind);
            FarmState state = FarmState.of(context.data());
            List<FieldPlot> fields = fields(context.data(), chunks);
            fields.removeIf(field -> state.kind(field) == target);
            if (fields.isEmpty()) return LandNoticeEvent.text("no_field");
            long cost = target == FieldKind.GU_PLANT ? fields.stream().mapToLong(FieldPlot::farmland).sum() : 0L;
            long mud = BlissfulStorage.of(context.data()).count(FarmWork::isMud);
            if (mud < cost) return LandNoticeEvent.text("mud_short", cost, mud);
            for (FieldPlot field : fields) {
                if (FarmWork.retype(context.level(), context.data(), field, target, context.player())
                        == FarmWork.Retype.DENIED) {
                    return LandNoticeEvent.text("grief_denied");
                }
            }
            return target == FieldKind.GU_PLANT ? LandNoticeEvent.text("retyped_gu_plant", fields.size(), cost)
                    : LandNoticeEvent.text("retyped_plain", fields.size());
        }
    }

    public record Clear(@NotNull List<Long> chunks) implements PanelAction {

        public static final PanelAction.Type<Clear> TYPE = new PanelAction.Type<>(Clear.class,
                CHUNKS.map(Clear::new, Clear::chunks));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            int plans = 0;
            for (long chunk : chunks) {
                if (FieldPlans.unplan(context.data(), ChunkPos.getX(chunk), ChunkPos.getZ(chunk))) plans++;
            }
            int removed = 0;
            int reaped = 0;
            for (FieldPlot field : fields(context.data(), chunks)) {
                Integer done = FarmWork.remove(context.level(), context.data(), field, context.player());
                if (done == null) return LandNoticeEvent.text("grief_denied");
                removed++;
                reaped += done;
            }
            if (removed == 0 && plans == 0) return LandNoticeEvent.text("no_field");
            if (plans == 0) return LandNoticeEvent.text("fields_removed", removed, reaped);
            if (removed == 0) return LandNoticeEvent.text("plans_cancelled", plans);
            return LandNoticeEvent.text("fields_removed", removed, reaped).copy().append("；")
                    .append(LandNoticeEvent.text("plans_cancelled", plans));
        }
    }

    //endregion

    @Override
    public @NotNull Page page() { return Page.INFO; }

    @Override
    public @NotNull StreamCodec<RegistryFriendlyByteBuf, Section> sectionCodec() { return Section.STREAM_CODEC; }

    @Override
    public @NotNull List<PanelAction.Type<?>> actions() {
        return List.of(Plan.TYPE, Harvest.TYPE, SetHosting.TYPE, PlanMany.TYPE, HarvestFields.TYPE,
                Sow.TYPE, Uproot.TYPE, Ripen.TYPE, Retype.TYPE, Clear.TYPE);
    }

    @Override
    public @NotNull Section section(@NotNull PanelContext context) {
        ServerLevel level = context.level();
        BlissfulLandData data = context.data();
        FarmState state = FarmState.of(data);
        List<Crop> rows = new ArrayList<>();
        for (Map.Entry<Block, FarmWork.Count> crop : LandCensus.take(level, data, FarmWork.CENSUS).entrySet()) {
            rows.add(new Crop(BuiltInRegistries.BLOCK.getKey(crop.getKey()), crop.getValue().mature(),
                    crop.getValue().growing()));
        }
        BlissfulStorage storage = BlissfulStorage.of(data);
        return new Section(rows, state.fields().size(), state.plans().size(), Siting.FIELD_CAP, state.hosting(),
                LandHostingJob.secondsUntilNext(level.getServer()), state.lastReaped(), state.lastSown(),
                storage.count(FarmWork::isSeed), storage.count(FarmWork::isBoneMeal), storage.count(FarmWork::isMud));
    }
}
