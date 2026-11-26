package net.alex.guzhenren.gameplay.immortalaperture.farm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.immortalaperture.BlissfulLands;
import net.alex.guzhenren.gameplay.immortalaperture.LandModule;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.ActionContext;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.EarthSpiritPanel;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.Page;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.PanelAction;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.PanelContext;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.PanelSection;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.Siting;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandCensus;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 「种植」小节模块：按作物计成熟 / 未熟、标准田与规划数、开垦作物与库藏种子数、托管开关、「收割并补种」；地图上的
 * 规划、取消规划与拆田也登记在这里。
 * <ul>
 *   <li>开垦作物按 {@link FarmWork#CROPS} 下标循环切换，{@link FarmState#AUTO_CROP} = 自动；库藏没有所选种子也照开，开
 *       出空田之后自动补种。</li>
 *   <li>{@link Plan}、{@link Unplan} 经 {@link FieldPlans}（选址复判与队列）；{@link Remove} 经
 *       {@link FarmWork#remove}。</li>
 *   <li>{@link Section#seeds()} 与 {@link FarmWork#CROPS} 同序，是库藏里各作物种子的数量。</li>
 *   <li>规则都在 {@link FarmWork}；托管每轮至多 {@value #HOSTING_FIELDS} 块田，结果记进 {@link FarmState}，页面显示下一
 *       轮倒计时与上一轮结果。</li>
 * </ul>
 */

public final class FarmModule implements LandModule<FarmModule.Section> {

    public static final FarmModule INSTANCE = new FarmModule();
    public static final int HOSTING_FIELDS = 8;

    private FarmModule() {}

    public record Crop(@NotNull ResourceLocation block, int mature, int growing) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Crop> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Crop::block,
                ByteBufCodecs.VAR_INT, Crop::mature,
                ByteBufCodecs.VAR_INT, Crop::growing,
                Crop::new);
    }

    public record Section(@NotNull List<Crop> rows, int fields, int planned, int capacity, int reclaimCooldown,
                          boolean hosting,
                          int hostingIn, int lastReaped, int lastSown, int crop, @NotNull List<Long> seeds)
            implements PanelSection {

        public static final Section EMPTY = new Section(List.of(), 0, 0, 0, 0, false, 0, 0, 0, FarmState.AUTO_CROP,
                List.of());
        public static final StreamCodec<RegistryFriendlyByteBuf, Section> STREAM_CODEC =
                StreamCodec.of(Section::encode, Section::decode);

        private static void encode(RegistryFriendlyByteBuf buffer, Section section) {
            Crop.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, section.rows);
            for (int value : new int[] {section.fields, section.planned, section.capacity, section.reclaimCooldown,
                    section.hostingIn, section.lastReaped, section.lastSown, section.crop}) {
                buffer.writeVarInt(value);
            }
            buffer.writeBoolean(section.hosting);
            ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()).encode(buffer, section.seeds);
        }

        private static Section decode(RegistryFriendlyByteBuf buffer) {
            List<Crop> rows = Crop.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buffer);
            int fields = buffer.readVarInt();
            int planned = buffer.readVarInt();
            int capacity = buffer.readVarInt();
            int reclaimCooldown = buffer.readVarInt();
            int hostingIn = buffer.readVarInt();
            int lastReaped = buffer.readVarInt();
            int lastSown = buffer.readVarInt();
            int crop = buffer.readVarInt();
            boolean hosting = buffer.readBoolean();
            List<Long> seeds = ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()).decode(buffer);
            return new Section(rows, fields, planned, capacity, reclaimCooldown, hosting, hostingIn, lastReaped,
                    lastSown, crop, seeds);
        }

        public long seedsOf(int crop) {
            if (crop != FarmState.AUTO_CROP) return crop < seeds.size() ? seeds.get(crop) : 0L;
            return seeds.stream().mapToLong(Long::longValue).sum();
        }

        @Override
        public @NotNull LandModule<?> module() { return INSTANCE; }
    }

    //region Actions [操作]
    public record Plan(int chunkX, int chunkZ) implements PanelAction {

        public static final PanelAction.Type<Plan> TYPE = new PanelAction.Type<>(Plan.class,
                StreamCodec.composite(ByteBufCodecs.VAR_INT, Plan::chunkX, ByteBufCodecs.VAR_INT, Plan::chunkZ,
                        Plan::new));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            Siting.Reason reason = FieldPlans.plan(context.level(), context.data(), chunkX, chunkZ);
            if (reason != Siting.Reason.OK) {
                return EarthSpiritPanel.notice("siting_refused", Component.translatable(reason.key()));
            }
            return EarthSpiritPanel.notice("planned", chunkX, chunkZ, context.data().farm().plans().size());
        }
    }

    public record Unplan(int chunkX, int chunkZ) implements PanelAction {

        public static final PanelAction.Type<Unplan> TYPE = new PanelAction.Type<>(Unplan.class,
                StreamCodec.composite(ByteBufCodecs.VAR_INT, Unplan::chunkX, ByteBufCodecs.VAR_INT, Unplan::chunkZ,
                        Unplan::new));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            return FieldPlans.unplan(context.data(), chunkX, chunkZ)
                    ? EarthSpiritPanel.notice("unplanned", chunkX, chunkZ) : EarthSpiritPanel.notice("no_plan");
        }
    }

    public record Remove(int chunkX, int chunkZ) implements PanelAction {

        public static final PanelAction.Type<Remove> TYPE = new PanelAction.Type<>(Remove.class,
                StreamCodec.composite(ByteBufCodecs.VAR_INT, Remove::chunkX, ByteBufCodecs.VAR_INT, Remove::chunkZ,
                        Remove::new));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            FieldPlot field = context.data().farm().fieldAt(chunkX, chunkZ);
            if (field == null) return EarthSpiritPanel.notice("no_field");
            Integer reaped = FarmWork.remove(context.level(), context.data(), field, context.player());
            if (reaped == null) return EarthSpiritPanel.notice("grief_denied");
            return EarthSpiritPanel.notice("field_removed", chunkX, chunkZ, reaped);
        }
    }

    public record Harvest() implements PanelAction {

        public static final PanelAction.Type<Harvest> TYPE = new PanelAction.Type<>(Harvest.class,
                StreamCodec.unit(new Harvest()));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            FarmWork.Harvest done = FarmWork.harvest(context.level(), context.data(), Integer.MAX_VALUE);
            return EarthSpiritPanel.notice("harvested", done.reaped(), done.sown());
        }
    }

    public record SetHosting(boolean on) implements PanelAction {

        public static final PanelAction.Type<SetHosting> TYPE = new PanelAction.Type<>(SetHosting.class,
                ByteBufCodecs.BOOL.map(SetHosting::new, SetHosting::on));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            context.data().farm().setHosting(on);
            return Component.empty();
        }
    }

    public record SetCrop(int crop) implements PanelAction {

        public static final PanelAction.Type<SetCrop> TYPE = new PanelAction.Type<>(SetCrop.class,
                ByteBufCodecs.VAR_INT.map(SetCrop::new, SetCrop::crop));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            context.data().farm().setCrop(crop);
            return Component.empty();
        }
    }
    //endregion

    @Override
    public @NotNull Page page() { return Page.INFO; }

    @Override
    public @NotNull StreamCodec<RegistryFriendlyByteBuf, Section> sectionCodec() { return Section.STREAM_CODEC; }

    @Override
    public @NotNull List<PanelAction.Type<?>> actions() {
        return List.of(Plan.TYPE, Unplan.TYPE, Remove.TYPE, Harvest.TYPE, SetHosting.TYPE, SetCrop.TYPE);
    }

    @Override
    public @NotNull Section section(@NotNull PanelContext context) {
        ServerLevel level = context.level();
        BlissfulLandData data = context.data();
        FarmState state = data.farm();
        List<Crop> rows = new ArrayList<>();
        for (Map.Entry<Block, FarmWork.Count> crop : LandCensus.crops(level, data).entrySet()) {
            rows.add(new Crop(BuiltInRegistries.BLOCK.getKey(crop.getKey()), crop.getValue().mature(),
                    crop.getValue().growing()));
        }
        List<Long> seeds = new ArrayList<>();
        for (int i = 0; i < FarmWork.CROPS.size(); i++) seeds.add(data.storage().count(FarmWork.seedFilter(i)));
        return new Section(rows, state.fields().size(), state.plans().size(), Siting.FIELD_CAP,
                Ticks.ceilSeconds(FarmWork.reclaimCooldown(level, data)), state.hosting(),
                BlissfulLands.secondsToHosting(level.getServer()), state.lastReaped(), state.lastSown(), state.crop(),
                seeds);
    }

    @Override
    public int host(@NotNull ServerLevel level, @NotNull BlissfulLandData data, @Nullable ServerPlayer owner) {
        if (!data.farm().hosting()) return 0;

        FarmWork.Harvest done = FarmWork.harvest(level, data, HOSTING_FIELDS);
        data.farm().setLastRound(done.reaped(), done.sown());
        return done.fields();
    }
}
