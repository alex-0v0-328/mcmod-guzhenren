package net.alex.guzhenren.gameplay.immortalaperture.managementpanel;

import java.util.List;
import java.util.Locale;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandNoticeEvent;
import net.alex.guzhenren.gameplay.immortalaperture.state.Territory;
import net.alex.guzhenren.gameplay.immortalaperture.territory.TerritoryWork;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

/**
 * 「疆域」页面板段：下发现疆域，客户端用同一份 {@link Territory} 规则画略图、算割弃预告；「割弃」由服务端复判。
 */

public final class TerritoryPart implements PanelSectionSource<TerritoryPart.Section> {

    public static final TerritoryPart INSTANCE = new TerritoryPart();

    private TerritoryPart() {}

    public record Section(@NotNull Territory territory) implements PanelSection {

        public static final Section EMPTY = new Section(Territory.whole(1, 1));
        public static final StreamCodec<RegistryFriendlyByteBuf, Section> STREAM_CODEC =
                Territory.STREAM_CODEC.map(Section::new, Section::territory).cast();

        @Override
        public @NotNull PanelSectionSource<?> source() { return INSTANCE; }
    }

    public record Cede(@NotNull Territory.Side side, int width) implements PanelAction {

        public static final PanelAction.Type<Cede> TYPE = new PanelAction.Type<>(Cede.class,
                StreamCodec.composite(Territory.Side.STREAM_CODEC, Cede::side, ByteBufCodecs.VAR_INT, Cede::width,
                        Cede::new));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            BlissfulLandData data = context.data();
            TerritoryWork.Voided voided = TerritoryWork.cede(context.level(), data, side, width);
            if (voided == null) return LandNoticeEvent.text("cede_refused");
            Component sideName = Component.translatable("guzhenren.blissful_land.side."
                    + side.name().toLowerCase(Locale.ROOT));
            if (voided.fields() + voided.plans() + voided.pastures() == 0) {
                return LandNoticeEvent.text("ceded", sideName, width, data.territory().acres());
            }
            return LandNoticeEvent.text("ceded_voided", sideName, width, data.territory().acres(), voided.fields(),
                    voided.plans(), voided.pastures());
        }
    }

    @Override
    public @NotNull Page page() { return Page.INFO; }

    @Override
    public @NotNull StreamCodec<RegistryFriendlyByteBuf, Section> sectionCodec() { return Section.STREAM_CODEC; }

    @Override
    public @NotNull List<PanelAction.Type<?>> actions() { return List.of(Cede.TYPE); }

    @Override
    public @NotNull Section section(@NotNull PanelContext context) { return new Section(context.data().territory()); }
}
