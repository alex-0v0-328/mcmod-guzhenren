package net.alex.guzhenren.gameplay.blissful.panel;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import java.util.function.Consumer;
import net.alex.guzhenren.gameplay.blissful.LandModule;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PanelSnapshotSectionsTest {

    private record Count(@NotNull Module owner, int value) implements PanelSection {

        @Override
        public @NotNull LandModule<?> module() { return owner; }
    }

    private static final class Module implements LandModule<Count> {

        private final Page page;
        private final StreamCodec<RegistryFriendlyByteBuf, Count> codec = ByteBufCodecs.VAR_INT
                .<RegistryFriendlyByteBuf>cast().map(value -> new Count(this, value), Count::value);

        private Module(Page page) { this.page = page; }

        @Override
        public @NotNull Page page() { return page; }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, Count> sectionCodec() { return codec; }

        @Override
        public @NotNull Count section(@NotNull PanelContext context) { return new Count(this, 0); }

        @Override
        public @NotNull List<PanelAction.Type<?>> actions() { return List.of(); }
    }

    private static final Module MAP = new Module(Page.MAP);
    private static final Module OVERVIEW = new Module(Page.INFO);
    private static final Module HERD = new Module(Page.INFO);
    private static final List<Module> MODULES = List.of(MAP, OVERVIEW, HERD);

    private static void withBuffer(Consumer<RegistryFriendlyByteBuf> body) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            body.accept(buffer);
        } finally {
            buffer.release();
        }
    }

    @Test
    @DisplayName("several sections of one page round-trip in order, each through its own module")
    void severalSectionsRoundTrip() {
        withBuffer(buffer -> {
            List<PanelSection> sections = List.of(new Count(OVERVIEW, 7), new Count(HERD, 300));
            PanelSnapshot.writeSections(buffer, sections, MODULES);
            assertEquals(sections, PanelSnapshot.readSections(buffer, MODULES));
            assertEquals(0, buffer.readableBytes());
        });
    }

    @Test
    @DisplayName("each section is tagged with its module's index in the module list")
    void tagIsModuleIndex() {
        withBuffer(buffer -> {
            PanelSnapshot.writeSections(buffer, List.of(new Count(HERD, 5)), MODULES);
            assertEquals(1, buffer.readVarInt());
            assertEquals(2, buffer.readVarInt());
            assertEquals(5, buffer.readVarInt());
        });
    }

    @Test
    @DisplayName("an empty section list round-trips")
    void emptyRoundTrip() {
        withBuffer(buffer -> {
            PanelSnapshot.writeSections(buffer, List.of(), MODULES);
            assertEquals(List.of(), PanelSnapshot.readSections(buffer, MODULES));
        });
    }

    @Test
    @DisplayName("a forged module index or section count fails to decode")
    void forgedTagsAreRefused() {
        withBuffer(buffer -> {
            buffer.writeVarInt(1);
            buffer.writeVarInt(MODULES.size());
            assertThrows(DecoderException.class, () -> PanelSnapshot.readSections(buffer, MODULES));
            buffer.clear();
            buffer.writeVarInt(1);
            buffer.writeVarInt(-1);
            assertThrows(DecoderException.class, () -> PanelSnapshot.readSections(buffer, MODULES));
            buffer.clear();
            buffer.writeVarInt(PanelSnapshot.MAX_SECTIONS + 1);
            assertThrows(DecoderException.class, () -> PanelSnapshot.readSections(buffer, MODULES));
        });
    }

    @Test
    @DisplayName("a section whose module is not registered cannot be encoded")
    void unregisteredSectionIsRejected() {
        withBuffer(buffer -> assertThrows(IllegalArgumentException.class, () -> PanelSnapshot.writeSections(buffer,
                List.of(new Count(new Module(Page.STORAGE), 1)), MODULES)));
    }
}
